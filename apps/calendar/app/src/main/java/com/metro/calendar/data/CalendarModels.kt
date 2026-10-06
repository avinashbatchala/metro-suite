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
    /** Up to three accent bars shown for days that have events. */
    val eventIndicatorCount: Int,
)

data class HourSlot(
    val hour: Int,
    val label: String,
    val events: List<CalendarEvent>,
)

/** WP8.1 Calendar top-level pivots (blueprint): agenda → day → month. */
enum class CalendarPivot(val title: String) {
    Agenda("agenda"),
    Day("day"),
    Month("month"),
    ;

    companion object {
        fun fromIndex(index: Int): CalendarPivot = entries.getOrElse(index) { Agenda }
    }
}
