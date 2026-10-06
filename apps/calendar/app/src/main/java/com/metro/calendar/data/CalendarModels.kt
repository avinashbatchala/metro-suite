package com.metro.calendar.data

/** Where a merged [CalendarEvent] came from. */
enum class CalendarSourceType {
    DEVICE,
    SUBSCRIPTION,
    DEMO,
}

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
) {
    /** Subscribed events are read-only (no edit/delete affordances). */
    val readOnly: Boolean get() = sourceType == CalendarSourceType.SUBSCRIPTION
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

/** One day tile in the WP8.1 week view (2×4 grid: seven days + a mini-month tile). */
data class WeekDayTile(
    val epochDay: Long,
    val dayNumber: Int,
    val weekdayShort: String,
    val isToday: Boolean,
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

data class HourSlot(
    val hour: Int,
    val label: String,
    val events: List<CalendarEvent>,
    /** True for the hour containing "now" on the current day (accent highlight). */
    val isNow: Boolean = false,
)

/** WP8.1 Calendar top-level views. Agenda is reached via the app-bar list icon. */
enum class CalendarView(val title: String) {
    Day("day"),
    Week("week"),
    Month("month"),
    Year("year"),
    Agenda("agenda"),
}
