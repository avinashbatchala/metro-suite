package com.metro.calendar.data

/** Where a merged [CalendarEvent] came from. */
enum class CalendarSourceType {
    DEVICE,
    SUBSCRIPTION,
    DEMO,
}

/** WP8.1 event status / availability, mapped to `CalendarContract.Events.AVAILABILITY_*`. */
enum class EventAvailability(val providerValue: Int, val title: String) {
    Busy(0, "busy"),
    Free(1, "free"),
    Tentative(2, "tentative"),
    OutOfOffice(3, "out of office"),
    ;

    companion object {
        fun fromProvider(value: Int): EventAvailability =
            entries.firstOrNull { it.providerValue == value } ?: Busy
    }
}

/**
 * Normalized event regardless of provider source. Write affordances are derived from the
 * calendar's real access level ([canEdit]/[canDelete]) — never from the source type alone.
 */
data class CalendarEvent(
    val id: Long,
    val title: String,
    val startMillis: Long,
    val endMillis: Long,
    val allDay: Boolean,
    val calendarColorHex: String,
    val calendarName: String?,
    val location: String?,
    val sourceType: CalendarSourceType = CalendarSourceType.DEVICE,
    /** Subscription id when [sourceType] is [CalendarSourceType.SUBSCRIPTION]. */
    val sourceId: String? = null,
    val calendarId: Long = -1L,
    val canEdit: Boolean = false,
    val canDelete: Boolean = false,
    val description: String? = null,
    val organizer: String? = null,
    val attendeeCount: Int = 0,
    val availability: EventAvailability = EventAvailability.Busy,
    val recurring: Boolean = false,
) {
    /** Read-only sources (subscriptions / read-only device calendars) hide destructive actions. */
    val readOnly: Boolean get() = !canEdit
}

data class DayBucket(
    val epochDay: Long,
    val headerLabel: String,
    val events: List<CalendarEvent>,
)

data class MonthGridCell(
    val dayOfMonth: Int,
    val epochDay: Long,
    val inCurrentMonth: Boolean,
    val isToday: Boolean,
    val isSelected: Boolean,
    /** Up to three per-calendar colour bars shown under the date number. */
    val eventColors: List<String>,
)

/** One day tile in the WP8.1 week view (4×2 grid: seven days + a mini-month tile). */
data class WeekDayTile(
    val epochDay: Long,
    val dayNumber: Int,
    val weekdayShort: String,
    val isToday: Boolean,
    val isSelected: Boolean,
    val isWeekend: Boolean,
    val events: List<CalendarEvent>,
)

/** A week of seven [WeekDayTile]s starting at the region's first day of week. */
data class CalendarWeek(
    val startEpochDay: Long,
    val days: List<WeekDayTile>,
)

/** One mini-month cell for the year view / week mini-month tile. */
data class MiniMonthDay(
    val dayOfMonth: Int,
    val epochDay: Long,
    val inCurrentMonth: Boolean,
    val isToday: Boolean,
    val eventColors: List<String>,
)

/** A mini-month (year view 3×4 grid and the week view's bottom-right tile). */
data class MiniMonth(
    val year: Int,
    val monthValue: Int,
    val label: String,
    val days: List<MiniMonthDay>,
)

/**
 * One appointment in the Day timeline: absolute position (minutes from midnight) and the
 * overlap lane it occupies so multi-hour events render **once** at their real duration.
 */
data class TimelineEvent(
    val event: CalendarEvent,
    val startMinute: Int,
    val endMinute: Int,
    val lane: Int,
    val laneCount: Int,
)

/** Legacy hour bucket (kept for `buildHourSlots` unit tests; the Day UI uses [TimelineEvent]). */
data class HourSlot(
    val hour: Int,
    val label: String,
    val events: List<CalendarEvent>,
    val isNow: Boolean = false,
)

/** WP8.1 Calendar top-level scales. */
enum class CalendarView(val title: String) {
    Day("day"),
    Week("week"),
    Month("month"),
    Year("year"),
    ;

    companion object {
        val scaleViews: List<CalendarView> = entries.toList()
    }
}

/**
 * Calendar vs Agenda presentation. Agenda is an alternate Day/Week presentation (the later
 * WP8.1 update), toggled from the ellipsis menu — **not** a fifth scale.
 */
enum class CalendarPresentation {
    Calendar,
    Agenda,
    ;

    fun toggled(): CalendarPresentation = if (this == Calendar) Agenda else Calendar
}
