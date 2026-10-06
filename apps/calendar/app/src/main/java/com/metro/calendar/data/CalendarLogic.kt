package com.metro.calendar.data

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import java.util.concurrent.TimeUnit

object CalendarLogic {
    /** Region-aware first day of week (Sunday in the US, Monday in the UK, …). */
    fun firstDayOfWeek(locale: Locale = Locale.getDefault()): DayOfWeek =
        WeekFields.of(locale).firstDayOfWeek

    fun monthStartOffset(firstOfMonth: LocalDate, firstDayOfWeek: DayOfWeek): Int {
        val diff = firstOfMonth.dayOfWeek.value - firstDayOfWeek.value
        return if (diff >= 0) diff else diff + 7
    }

    /** Android stores all-day event boundaries in UTC regardless of the device zone. */
    private val utc: ZoneId = ZoneId.of("UTC")

    fun epochDayFromMillis(millis: Long, zoneId: ZoneId = ZoneId.systemDefault()): Long =
        // LocalDate.ofInstant is API 34+; atZone/toLocalDate is available back to API 26.
        Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate().toEpochDay()

    fun millisFromEpochDay(epochDay: Long, zoneId: ZoneId = ZoneId.systemDefault()): Long =
        LocalDate.ofEpochDay(epochDay).atStartOfDay(zoneId).toInstant().toEpochMilli()

    fun todayEpochDay(zoneId: ZoneId = ZoneId.systemDefault()): Long =
        LocalDate.now(zoneId).toEpochDay()

    fun dateHeaderLabel(epochDay: Long, zoneId: ZoneId = ZoneId.systemDefault(), locale: Locale = Locale.US): String {
        val date = LocalDate.ofEpochDay(epochDay)
        return date.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", locale)).uppercase(locale)
    }

    fun dayNameLower(epochDay: Long, zoneId: ZoneId = ZoneId.systemDefault(), locale: Locale = Locale.US): String =
        LocalDate.ofEpochDay(epochDay)
            .dayOfWeek
            .getDisplayName(TextStyle.FULL, locale)
            .lowercase(locale)

    fun dayNameShort(epochDay: Long, zoneId: ZoneId = ZoneId.systemDefault(), locale: Locale = Locale.US): String =
        LocalDate.ofEpochDay(epochDay)
            .dayOfWeek
            .getDisplayName(TextStyle.SHORT, locale)
            .lowercase(locale)

    fun monthNameLower(epochDay: Long, zoneId: ZoneId = ZoneId.systemDefault(), locale: Locale = Locale.US): String =
        LocalDate.ofEpochDay(epochDay)
            .month
            .getDisplayName(TextStyle.FULL, locale)
            .lowercase(locale)

    fun yearLabel(epochDay: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
        LocalDate.ofEpochDay(epochDay).year.toString()

    /** First day of a month as an epoch day (Year → Month drill-down anchor). */
    fun monthAnchorEpochDay(year: Int, month: Int): Long = LocalDate.of(year, month, 1).toEpochDay()

    fun formatEventTime(
        event: CalendarEvent,
        zoneId: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.US,
        use24Hour: Boolean = true,
    ): String {
        if (event.allDay) return "All day"
        val pattern = if (use24Hour) "HH:mm" else "h:mm a"
        return Instant.ofEpochMilli(event.startMillis)
            .atZone(zoneId)
            .toLocalTime()
            .format(DateTimeFormatter.ofPattern(pattern, locale))
    }

    /**
     * Epoch day the event *starts* on in the local calendar. All-day events are anchored in UTC
     * (provider storage), so a Monday all-day event is not shifted to Sunday in negative-offset
     * zones. Timed events use the device zone.
     */
    fun eventStartEpochDay(event: CalendarEvent, zoneId: ZoneId = ZoneId.systemDefault()): Long =
        epochDayFromMillis(event.startMillis, if (event.allDay) utc else zoneId)

    /**
     * Inclusive last epoch day the event covers, for agenda range filtering. Android's END is
     * exclusive, so step back one millisecond before mapping to a day.
     */
    fun eventEndEpochDay(event: CalendarEvent, zoneId: ZoneId = ZoneId.systemDefault()): Long {
        val zone = if (event.allDay) utc else zoneId
        val endExclusive = if (event.endMillis > event.startMillis) event.endMillis else event.startMillis + 1
        return epochDayFromMillis(endExclusive - 1L, zone)
    }

    fun formatEventDuration(event: CalendarEvent, zoneId: ZoneId = ZoneId.systemDefault()): String {
        if (event.allDay) {
            val startDay = epochDayFromMillis(event.startMillis, utc)
            val endDay = epochDayFromMillis(event.endMillis, utc)
            val days = (endDay - startDay).coerceAtLeast(1)
            return if (days == 1L) "1 day" else "$days days"
        }
        val minutes = ((event.endMillis - event.startMillis) / 60_000L).coerceAtLeast(0)
        return when {
            minutes < 60 -> "$minutes minutes"
            minutes % 60 == 0L -> "${minutes / 60} hour${if (minutes / 60 == 1L) "" else "s"}"
            else -> "${minutes / 60}h ${minutes % 60}m"
        }
    }

    fun eventsForDay(events: List<CalendarEvent>, epochDay: Long, zoneId: ZoneId = ZoneId.systemDefault()): List<CalendarEvent> {
        val dayStart = millisFromEpochDay(epochDay, zoneId)
        val dayEnd = millisFromEpochDay(epochDay + 1, zoneId)
        return events.filter { event ->
            event.endMillis > dayStart && event.startMillis < dayEnd
        }.sortedBy { it.startMillis }
    }

    fun allDayEventsForDay(events: List<CalendarEvent>, epochDay: Long, zoneId: ZoneId = ZoneId.systemDefault()): List<CalendarEvent> =
        eventsForDay(events, epochDay, zoneId).filter { it.allDay }

    fun timedEventsForDay(events: List<CalendarEvent>, epochDay: Long, zoneId: ZoneId = ZoneId.systemDefault()): List<CalendarEvent> =
        eventsForDay(events, epochDay, zoneId).filterNot { it.allDay }

    fun groupIntoAgendaBuckets(
        events: List<CalendarEvent>,
        startEpochDay: Long,
        dayCount: Int = 60,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<DayBucket> {
        val endEpochDay = startEpochDay + dayCount
        val grouped = sortedMapOf<Long, MutableList<CalendarEvent>>()
        events.forEach { event ->
            val startDay = eventStartEpochDay(event, zoneId)
            val endDay = eventEndEpochDay(event, zoneId)
            // Keep multi-day events that began before the window but still overlap it; drop
            // events entirely outside the window.
            if (startDay >= endEpochDay || endDay < startEpochDay) return@forEach
            val bucketDay = startDay.coerceIn(startEpochDay, endEpochDay - 1)
            grouped.getOrPut(bucketDay) { mutableListOf() }.add(event)
        }

        return grouped.map { (day, dayEvents) ->
            DayBucket(
                epochDay = day,
                headerLabel = dateHeaderLabel(day, zoneId),
                events = dayEvents.sortedBy { it.startMillis },
            )
        }
    }

    fun buildHourSlots(
        events: List<CalendarEvent>,
        epochDay: Long,
        startHour: Int = 0,
        endHour: Int = 23,
        zoneId: ZoneId = ZoneId.systemDefault(),
        use24Hour: Boolean = false,
    ): List<HourSlot> {
        val timed = timedEventsForDay(events, epochDay, zoneId)
        val nowHour = if (epochDay == todayEpochDay(zoneId)) LocalTime.now(zoneId).hour else -1
        return (startHour..endHour).map { hour ->
            val hourStart = millisFromEpochDay(epochDay, zoneId) + TimeUnit.HOURS.toMillis(hour.toLong())
            val hourEnd = hourStart + TimeUnit.HOURS.toMillis(1)
            val label = formatHourLabel(hour, use24Hour)
            val inHour = timed.filter { event ->
                event.startMillis < hourEnd && event.endMillis > hourStart
            }
            HourSlot(hour = hour, label = label, events = inHour, isNow = hour == nowHour)
        }
    }

    /** Per-calendar colours for a day, de-duplicated and capped for month / mini-month bars. */
    fun dayEventColors(
        events: List<CalendarEvent>,
        epochDay: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
        limit: Int = 3,
    ): List<String> =
        eventsForDay(events, epochDay, zoneId)
            .map { it.calendarColorHex }
            .distinct()
            .take(limit)

    fun buildMonthGrid(
        year: Int,
        month: Int,
        events: List<CalendarEvent>,
        selectedEpochDay: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
        firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    ): List<MonthGridCell> {
        val firstOfMonth = LocalDate.of(year, month, 1)
        val today = todayEpochDay(zoneId)
        val startOffset = monthStartOffset(firstOfMonth, firstDayOfWeek)
        val gridStart = firstOfMonth.minusDays(startOffset.toLong())

        return (0 until 42).map { index ->
            val date = gridStart.plusDays(index.toLong())
            val epochDay = date.toEpochDay()
            MonthGridCell(
                dayOfMonth = date.dayOfMonth,
                epochDay = epochDay,
                inCurrentMonth = date.monthValue == month,
                isToday = epochDay == today,
                isSelected = epochDay == selectedEpochDay,
                eventColors = dayEventColors(events, epochDay, zoneId),
            )
        }
    }

    /** WP8.1 week view: seven day tiles starting at the region's first day of week. */
    fun buildWeek(
        anchorEpochDay: Long,
        events: List<CalendarEvent>,
        zoneId: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.US,
        selectedEpochDay: Long = -1L,
    ): CalendarWeek {
        val firstDay = firstDayOfWeek(locale)
        val anchor = LocalDate.ofEpochDay(anchorEpochDay)
        val offset = (anchor.dayOfWeek.value - firstDay.value + 7) % 7
        val weekStart = anchor.minusDays(offset.toLong())
        val today = todayEpochDay(zoneId)
        val days = (0 until 7).map { index ->
            val date = weekStart.plusDays(index.toLong())
            val epochDay = date.toEpochDay()
            val dayEvents = eventsForDay(events, epochDay, zoneId)
            WeekDayTile(
                epochDay = epochDay,
                dayNumber = date.dayOfMonth,
                weekdayShort = date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).uppercase(locale),
                isToday = epochDay == today,
                isSelected = epochDay == selectedEpochDay,
                isWeekend = date.dayOfWeek.value >= 6,
                events = dayEvents,
            )
        }
        return CalendarWeek(startEpochDay = weekStart.toEpochDay(), days = days)
    }

    /**
     * Day timeline: one [TimelineEvent] per appointment with absolute minute offsets and a
     * de-overlapped lane. Multi-hour events appear **once** at their true duration.
     */
    fun buildTimeline(
        events: List<CalendarEvent>,
        epochDay: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<TimelineEvent> {
        val dayStart = millisFromEpochDay(epochDay, zoneId)
        val timed = timedEventsForDay(events, epochDay, zoneId)
        val laid = timed.map { event ->
            val startMinute = (((event.startMillis - dayStart) / 60_000L).coerceIn(0L, 1440L)).toInt()
            val endMinute = (((event.endMillis - dayStart) / 60_000L).coerceIn(0L, 1440L)).toInt()
            TimelineEvent(
                event = event,
                startMinute = startMinute,
                endMinute = endMinute.coerceAtLeast(startMinute + 15),
                lane = 0,
                laneCount = 1,
            )
        }.sortedBy { it.startMinute }

        // Greedy overlap lanes: assign each event the first lane whose last end <= its start.
        val laneEnds = mutableListOf<Int>()
        val laneOf = IntArray(laid.size)
        laid.forEachIndexed { index, item ->
            var lane = laneEnds.indexOfFirst { end -> end <= item.startMinute }
            if (lane == -1) {
                lane = laneEnds.size
                laneEnds.add(item.endMinute)
            } else {
                laneEnds[lane] = item.endMinute
            }
            laneOf[index] = lane
        }
        // laneCount per event = size of its connected overlap cluster.
        val result = ArrayList<TimelineEvent>(laid.size)
        var clusterStart = 0
        var clusterMaxEnd = 0
        var clusterMaxLane = 0
        fun flush(endIndex: Int) {
            val laneCount = clusterMaxLane + 1
            for (i in clusterStart until endIndex) {
                result.add(laid[i].copy(lane = laneOf[i], laneCount = laneCount))
            }
        }
        laid.forEachIndexed { index, item ->
            if (index > clusterStart && item.startMinute >= clusterMaxEnd) {
                flush(index)
                clusterStart = index
                clusterMaxLane = 0
            }
            clusterMaxEnd = maxOf(clusterMaxEnd, item.endMinute)
            clusterMaxLane = maxOf(clusterMaxLane, laneOf[index])
        }
        if (laid.isNotEmpty()) flush(laid.size)
        return result
    }

    /** All-day (and multi-day spanning) events for the compact section above the day timeline. */
    fun timelineAllDayEvents(
        events: List<CalendarEvent>,
        epochDay: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<CalendarEvent> = allDayEventsForDay(events, epochDay, zoneId)

    /** Epoch-day window that must be loaded for a given view (inclusive start, exclusive end). */
    fun visibleRangeDays(
        view: CalendarView,
        epochDay: Long,
    ): Pair<Long, Long> = when (view) {
        CalendarView.Day -> (epochDay - 1) to (epochDay + 2)
        CalendarView.Week -> (epochDay - 7) to (epochDay + 8)
        CalendarView.Month -> {
            val date = LocalDate.ofEpochDay(epochDay)
            val gridStart = date.withDayOfMonth(1).minusDays(
                ((date.withDayOfMonth(1).dayOfWeek.value + 6) % 7).toLong(),
            )
            gridStart.toEpochDay() to (gridStart.plusDays(42).toEpochDay())
        }
        CalendarView.Year -> {
            val year = LocalDate.ofEpochDay(epochDay).year
            LocalDate.of(year, 1, 1).minusDays(7).toEpochDay() to
                LocalDate.of(year, 12, 31).plusDays(8).toEpochDay()
        }
    }

    fun buildMiniMonth(
        year: Int,
        monthValue: Int,
        events: List<CalendarEvent>,
        zoneId: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.US,
        firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    ): MiniMonth {
        val firstOfMonth = LocalDate.of(year, monthValue, 1)
        val today = todayEpochDay(zoneId)
        val startOffset = monthStartOffset(firstOfMonth, firstDayOfWeek)
        val gridStart = firstOfMonth.minusDays(startOffset.toLong())
        val days = (0 until 42).map { index ->
            val date = gridStart.plusDays(index.toLong())
            val epochDay = date.toEpochDay()
            MiniMonthDay(
                dayOfMonth = date.dayOfMonth,
                epochDay = epochDay,
                inCurrentMonth = date.monthValue == monthValue,
                isToday = epochDay == today,
                eventColors = dayEventColors(events, epochDay, zoneId, limit = 1),
            )
        }
        return MiniMonth(
            year = year,
            monthValue = monthValue,
            label = firstOfMonth.month.getDisplayName(TextStyle.SHORT, locale),
            days = days,
        )
    }

    /** Year view: twelve mini-months (Jan–Dec) for [year]. */
    fun buildYear(
        year: Int,
        events: List<CalendarEvent>,
        zoneId: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.US,
        firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    ): List<MiniMonth> = (1..12).map { month ->
        buildMiniMonth(year, month, events, zoneId, locale, firstDayOfWeek)
    }

    fun eventsTodayCount(events: List<CalendarEvent>, zoneId: ZoneId = ZoneId.systemDefault()): Int =
        eventsForDay(events, todayEpochDay(zoneId), zoneId).size

    fun nextUpcomingEvent(
        events: List<CalendarEvent>,
        nowMillis: Long = System.currentTimeMillis(),
    ): CalendarEvent? =
        events
            .filter { it.endMillis > nowMillis }
            .minByOrNull { it.startMillis }

    fun nextEventTimeLabel(
        event: CalendarEvent,
        zoneId: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.US,
        use24Hour: Boolean = false,
    ): String {
        if (event.allDay) return "All day"
        return formatEventTime(event, zoneId, locale, use24Hour)
    }

    /** Short capitalised weekday for a live-tile date badge, e.g. `Thu`. */
    fun tileDayLabel(epochDay: Long, zoneId: ZoneId = ZoneId.systemDefault(), locale: Locale = Locale.US): String =
        LocalDate.ofEpochDay(epochDay)
            .dayOfWeek
            .getDisplayName(TextStyle.SHORT, locale)

    /** Day-of-month for a live-tile date badge, e.g. `15`. */
    fun tileDayNumber(epochDay: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
        LocalDate.ofEpochDay(epochDay).dayOfMonth.toString()

    /** 12-hour time range for a live-tile event line, e.g. `3:00 PM - 4:00 PM` or `All day`. */
    fun tileTimeRange(
        event: CalendarEvent,
        zoneId: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.US,
        use24Hour: Boolean = false,
    ): String {
        if (event.allDay) return "All day"
        val formatter = DateTimeFormatter.ofPattern(if (use24Hour) "HH:mm" else "h:mm a", locale)
        val start = Instant.ofEpochMilli(event.startMillis).atZone(zoneId).toLocalTime().format(formatter)
        val end = Instant.ofEpochMilli(event.endMillis).atZone(zoneId).toLocalTime().format(formatter)
        return "$start - $end"
    }

    /**
     * Event content lines for the WP8.1 Calendar live tile, in priority order:
     * `[title, location?, time]`. When the event is not today the time line is prefixed with its
     * weekday (e.g. `Mon: All day`), matching the device tile.
     */
    fun tileEventLines(
        event: CalendarEvent,
        todayEpochDay: Long = todayEpochDay(),
        zoneId: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.US,
        use24Hour: Boolean = false,
    ): List<String> {
        val eventDay = eventStartEpochDay(event, zoneId)
        val timeText = tileTimeRange(event, zoneId, locale, use24Hour)
        val timeLine = if (eventDay != todayEpochDay) {
            "${tileDayLabel(eventDay, zoneId, locale)}: $timeText"
        } else {
            timeText
        }
        return buildList {
            add(event.title)
            event.location?.takeIf { it.isNotBlank() }?.let { add(it) }
            add(timeLine)
        }
    }

    fun formatHourLabel(hour: Int, use24Hour: Boolean = false): String {
        val normalized = hour % 24
        if (use24Hour) return "$normalized:00"
        return when {
            normalized == 0 -> "12 AM"
            normalized < 12 -> "$normalized AM"
            normalized == 12 -> "12 PM"
            else -> "${normalized - 12} PM"
        }
    }
}
