package com.metro.calendar.data

/** A calendar the user is allowed to write to (device provider). */
data class WritableCalendar(
    val id: Long,
    val displayName: String,
    val accountName: String?,
    val colorHex: String,
    val isVisible: Boolean,
)

/** Recurrence presets exposed in the appointment editor (v1: whole-series only). */
enum class RecurrenceRule(val rrule: String?) {
    None(null),
    Daily("FREQ=DAILY"),
    Weekly("FREQ=WEEKLY"),
    Monthly("FREQ=MONTHLY"),
    Yearly("FREQ=YEARLY"),
}

/** Editable appointment state. [id] is null for a new event. */
data class CalendarDraft(
    val id: Long? = null,
    val calendarId: Long = -1L,
    val title: String = "",
    val location: String = "",
    val notes: String = "",
    val allDay: Boolean = false,
    val startMillis: Long,
    val endMillis: Long,
    val reminderMinutes: Int? = null,
    val recurrence: RecurrenceRule = RecurrenceRule.None,
    /** True when the source event is read-only (subscription) — editor is disabled for it. */
    val readOnly: Boolean = false,
)
