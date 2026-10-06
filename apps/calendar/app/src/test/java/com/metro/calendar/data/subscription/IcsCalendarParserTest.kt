package com.metro.calendar.data.subscription

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class IcsCalendarParserTest {

    private val wideStart = Instant.parse("2024-01-01T00:00:00Z").toEpochMilli()
    private val wideEnd = Instant.parse("2025-01-01T00:00:00Z").toEpochMilli()

    private fun fixture(name: String): String =
        requireNotNull(javaClass.getResourceAsStream("/ics/$name")) {
            "missing fixture $name"
        }.readBytes().decodeToString()

    private fun millis(iso: String): Long = Instant.parse(iso).toEpochMilli()

    @Test
    fun `parses simple timed events`() {
        val events = IcsCalendarParser.parse(fixture("simple.ics"), wideStart, wideEnd)

        assertEquals(2, events.size)
        val standup = events.first { it.title == "Standup" }
        assertEquals(millis("2024-01-10T09:00:00Z"), standup.startMillis)
        assertEquals(millis("2024-01-10T09:30:00Z"), standup.endMillis)
        assertFalse(standup.allDay)
        assertEquals("Room 4", standup.location)
    }

    @Test
    fun `expands weekly rule and honors exdate`() {
        val events = IcsCalendarParser.parse(fixture("recurring.ics"), wideStart, wideEnd)

        // COUNT=6 minus one EXDATE.
        assertEquals(5, events.size)
        assertTrue(events.all { it.title == "Standup" })
        assertTrue(events.none { it.startMillis == millis("2024-01-03T09:00:00Z") })
        assertEquals(millis("2024-01-01T09:00:00Z"), events.minOf { it.startMillis })
        assertEquals(millis("2024-01-12T09:00:00Z"), events.maxOf { it.startMillis })
    }

    @Test
    fun `expands all day yearly rule`() {
        val events = IcsCalendarParser.parse(
            fixture("allday.ics"),
            wideStart,
            millis("2027-01-01T00:00:00Z"),
        ).sortedBy { it.startMillis }

        assertEquals(3, events.size)
        assertTrue(events.all { it.allDay })
        // All-day occurrences span exactly one day (midnight-to-midnight, no DST transition here).
        assertEquals(24L * 60 * 60 * 1000, events.first().endMillis - events.first().startMillis)
    }

    @Test
    fun `applies recurrence overrides and cancellations`() {
        val events = IcsCalendarParser.parse(fixture("overrides.ics"), wideStart, wideEnd)
            .sortedBy { it.startMillis }

        assertEquals(2, events.size)
        assertEquals("Original", events[0].title)
        assertEquals(millis("2024-01-01T10:00:00Z"), events[0].startMillis)
        assertEquals("Moved", events[1].title)
        assertEquals(millis("2024-01-08T14:00:00Z"), events[1].startMillis)
        assertEquals(millis("2024-01-08T15:00:00Z"), events[1].endMillis)
        // The cancelled Jan 15 occurrence is dropped.
        assertTrue(events.none { it.startMillis == millis("2024-01-15T10:00:00Z") })
    }

    @Test
    fun `clips occurrences to the requested window`() {
        val events = IcsCalendarParser.parse(
            fixture("recurring.ics"),
            millis("2024-01-08T00:00:00Z"),
            millis("2024-01-13T00:00:00Z"),
        )
        assertEquals(3, events.size)
        assertTrue(events.all { it.startMillis >= millis("2024-01-08T00:00:00Z") })
    }

    @Test
    fun `ignores events entirely outside the window`() {
        val events = IcsCalendarParser.parse(
            fixture("simple.ics"),
            millis("2024-02-01T00:00:00Z"),
            millis("2024-03-01T00:00:00Z"),
        )
        assertTrue(events.isEmpty())
    }

    @Test
    fun `returns empty for malformed input`() {
        assertTrue(IcsCalendarParser.parse("this is not a calendar", wideStart, wideEnd).isEmpty())
    }
}
