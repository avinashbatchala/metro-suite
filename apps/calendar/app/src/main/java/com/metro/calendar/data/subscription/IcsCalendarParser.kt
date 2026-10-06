package com.metro.calendar.data.subscription

import biweekly.Biweekly
import biweekly.component.VEvent
import java.util.TimeZone

/**
 * Parses iCalendar (RFC 5545) text and expands recurring events into concrete occurrences clipped
 * to a time window. Read-only: only event start/end/title/location are extracted, and no calendar
 * write-back is ever performed.
 *
 * Recurrence is expanded with biweekly's RRULE/RDATE/EXDATE iterator. `RECURRENCE-ID` overrides
 * replace the matching occurrence of their series, and `STATUS:CANCELLED` events are dropped.
 */
object IcsCalendarParser {

    data class ParsedEvent(
        val uid: String,
        val title: String,
        val startMillis: Long,
        val endMillis: Long,
        val allDay: Boolean,
        val location: String?,
    )

    private const val MAX_OCCURRENCES_PER_SERIES = 1500
    private const val MAX_EVENTS = 20_000
    private const val DAY_MILLIS = 24L * 60 * 60 * 1000

    fun parse(
        body: String,
        windowStartMillis: Long,
        windowEndMillis: Long,
    ): List<ParsedEvent> {
        val calendar = runCatching { Biweekly.parse(body).first() }.getOrNull() ?: return emptyList()
        val out = ArrayList<ParsedEvent>()

        val groups = calendar.events.groupBy { event ->
            event.uid?.value?.takeIf { it.isNotBlank() } ?: "anon-${event.hashCode()}"
        }

        for ((uid, events) in groups) {
            val overrides = events.filter { it.recurrenceId != null }
            val overriddenOccurrences = HashSet<Long>()
            for (override in overrides) {
                override.recurrenceId?.value?.time?.let { overriddenOccurrences.add(it) }
            }

            val base = events.firstOrNull { it.recurrenceId == null }
            if (base != null && !base.isCancelled()) {
                expandSeries(base, uid, windowStartMillis, windowEndMillis, overriddenOccurrences, out)
            }

            for (override in overrides) {
                if (override.isCancelled()) continue
                val start = override.dateStart?.value?.time ?: continue
                val end = override.resolveEnd(start)
                if (overlaps(start, end, windowStartMillis, windowEndMillis)) {
                    out += override.toParsed(uid, start, end)
                }
            }

            if (out.size >= MAX_EVENTS) break
        }
        return out
    }

    private fun expandSeries(
        event: VEvent,
        uid: String,
        windowStartMillis: Long,
        windowEndMillis: Long,
        overrideStarts: Set<Long>,
        out: MutableList<ParsedEvent>,
    ) {
        val start = event.dateStart?.value?.time ?: return
        val baseEnd = event.resolveEnd(start)
        val duration = (baseEnd - start).coerceAtLeast(if (event.isAllDay()) DAY_MILLIS else 0L)

        val occurrences = runCatching { event.getDateIterator(event.iteratorZone()) }.getOrNull()
        if (occurrences == null) {
            if (overlaps(start, baseEnd, windowStartMillis, windowEndMillis)) {
                out += event.toParsed(uid, start, baseEnd)
            }
            return
        }

        var guard = 0
        while (occurrences.hasNext() && guard < MAX_OCCURRENCES_PER_SERIES && out.size < MAX_EVENTS) {
            val occurrenceStart = occurrences.next().time
            guard++
            if (occurrenceStart > windowEndMillis) break
            val occurrenceEnd = occurrenceStart + duration
            if (!overlaps(occurrenceStart, occurrenceEnd, windowStartMillis, windowEndMillis)) continue
            if (overrideStarts.contains(occurrenceStart)) continue
            out += event.toParsed(uid, occurrenceStart, occurrenceEnd)
        }
    }

    private fun VEvent.resolveEnd(startMillis: Long): Long {
        dateEnd?.value?.time?.let { return it.coerceAtLeast(startMillis) }
        duration?.value?.toMillis()?.let { return startMillis + it }
        return if (isAllDay()) startMillis + DAY_MILLIS else startMillis
    }

    private fun VEvent.toParsed(uid: String, start: Long, end: Long): ParsedEvent = ParsedEvent(
        uid = uid,
        title = summary?.value?.takeIf { it.isNotBlank() } ?: "(no title)",
        startMillis = start,
        endMillis = end,
        allDay = isAllDay(),
        location = location?.value?.takeIf { it.isNotBlank() },
    )

    private fun VEvent.isAllDay(): Boolean = dateStart?.value?.hasTime() == false

    /**
     * UTC feeds expand in UTC so DST-free recurrence math matches the wall clock they publish;
     * floating/TZID/date-only events expand in the device timezone.
     */
    private fun VEvent.iteratorZone(): TimeZone {
        val start = dateStart?.value ?: return TimeZone.getDefault()
        return if (start.rawComponents?.isUtc == true) {
            TimeZone.getTimeZone("UTC")
        } else {
            TimeZone.getDefault()
        }
    }

    private fun VEvent.isCancelled(): Boolean =
        status?.value?.equals("CANCELLED", ignoreCase = true) == true

    private fun overlaps(start: Long, end: Long, windowStart: Long, windowEnd: Long): Boolean {
        val effectiveEnd = if (end > start) end else start + 1
        return start < windowEnd && effectiveEnd > windowStart
    }
}
