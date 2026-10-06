package com.metro.calendar.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import java.time.LocalDate
import java.time.ZoneId

/**
 * Writes appointments to the device calendar provider.
 *
 * On devices with no writable calendar (e.g. GrapheneOS with no accounts), [ensureWritableCalendar]
 * lazily creates a local "Phone" calendar (`ACCOUNT_TYPE_LOCAL`) so the user can still create
 * events.
 */
class CalendarWriteRepository(context: Context) {
    private val appContext = context.applicationContext
    private val resolver get() = appContext.contentResolver
    private val zoneId: ZoneId = ZoneId.systemDefault()

    companion object {
        private const val LOCAL_ACCOUNT_NAME = "local"
        private const val LOCAL_CALENDAR_NAME = "Phone"
        private const val DEFAULT_COLOR = 0xFF1BA1E2.toInt()
        /** Provider access levels: 500 = contributor (writable), 700 = owner. */
        private const val ACCESS_CONTRIBUTOR = 500
        private const val ACCESS_OWNER = 700
    }

    fun writableCalendars(): List<WritableCalendar> {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR,
            CalendarContract.Calendars.VISIBLE,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
        )
        val result = mutableListOf<WritableCalendar>()
        runCatching {
            resolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                null,
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
                val nameIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                val accountIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
                val colorIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_COLOR)
                val visibleIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.VISIBLE)
                val accessIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)
                while (cursor.moveToNext()) {
                    val access = cursor.getInt(accessIdx)
                    if (access < ACCESS_CONTRIBUTOR) continue
                    result += WritableCalendar(
                        id = cursor.getLong(idIdx),
                        displayName = cursor.getString(nameIdx) ?: "Calendar",
                        accountName = cursor.getString(accountIdx),
                        colorHex = colorIntToHex(cursor.getInt(colorIdx)),
                        isVisible = cursor.getInt(visibleIdx) != 0,
                    )
                }
            }
        }
        return result
    }

    /** Returns the id of a writable calendar, creating a local "Phone" calendar if needed. */
    fun ensureWritableCalendar(): Long? {
        writableCalendars().firstOrNull()?.let { return it.id }
        return createLocalCalendar()
    }

    private fun createLocalCalendar(): Long? {
        val uri = CalendarContract.Calendars.CONTENT_URI.buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, LOCAL_ACCOUNT_NAME)
            .appendQueryParameter(
                CalendarContract.Calendars.ACCOUNT_TYPE,
                CalendarContract.ACCOUNT_TYPE_LOCAL,
            )
            .build()
        val values = ContentValues().apply {
            put(CalendarContract.Calendars.ACCOUNT_NAME, LOCAL_ACCOUNT_NAME)
            put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            put(CalendarContract.Calendars.NAME, LOCAL_CALENDAR_NAME)
            put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, LOCAL_CALENDAR_NAME)
            put(CalendarContract.Calendars.CALENDAR_COLOR, DEFAULT_COLOR)
            put(
                CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
                ACCESS_OWNER,
            )
            put(CalendarContract.Calendars.OWNER_ACCOUNT, LOCAL_ACCOUNT_NAME)
            put(CalendarContract.Calendars.SYNC_EVENTS, 1)
            put(CalendarContract.Calendars.VISIBLE, 1)
        }
        return runCatching {
            resolver.insert(uri, values)?.lastPathSegment?.toLongOrNull()
        }.getOrNull()
    }

    fun createEvent(draft: CalendarDraft): Long? {
        val calendarId = if (draft.calendarId > 0) {
            draft.calendarId
        } else {
            ensureWritableCalendar() ?: return null
        }
        val values = eventValues(draft, calendarId)
        val eventId = runCatching {
            resolver.insert(CalendarContract.Events.CONTENT_URI, values)
                ?.lastPathSegment?.toLongOrNull()
        }.getOrNull() ?: return null
        writeReminder(eventId, draft.reminderMinutes)
        return eventId
    }

    fun updateEvent(draft: CalendarDraft): Boolean {
        val id = draft.id ?: return false
        val calendarId = if (draft.calendarId > 0) draft.calendarId else ensureWritableCalendar() ?: return false
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id)
        val updated = runCatching {
            resolver.update(uri, eventValues(draft, calendarId), null, null) > 0
        }.getOrDefault(false)
        if (updated) {
            deleteReminders(id)
            writeReminder(id, draft.reminderMinutes)
        }
        return updated
    }

    fun deleteEvent(eventId: Long): Boolean = runCatching {
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        resolver.delete(uri, null, null) > 0
    }.getOrDefault(false)

    /** Calendar id owning [eventId], for seeding the editor when editing an existing event. */
    fun calendarIdForEvent(eventId: Long): Long? = runCatching {
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        resolver.query(uri, arrayOf(CalendarContract.Events.CALENDAR_ID), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else null
        }
    }.getOrNull()

    fun setCalendarVisible(calendarId: Long, visible: Boolean): Boolean = runCatching {
        val uri = ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, calendarId)
        val values = ContentValues().apply {
            put(CalendarContract.Calendars.VISIBLE, if (visible) 1 else 0)
        }
        resolver.update(uri, values, null, null) > 0
    }.getOrDefault(false)

    fun setCalendarColor(calendarId: Long, colorHex: String): Boolean = runCatching {
        val color = android.graphics.Color.parseColor(colorHex)
        val uri = ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, calendarId)
        val values = ContentValues().apply {
            put(CalendarContract.Calendars.CALENDAR_COLOR, color)
        }
        resolver.update(uri, values, null, null) > 0
    }.getOrDefault(false)

    private fun eventValues(draft: CalendarDraft, calendarId: Long): ContentValues = ContentValues().apply {
        put(CalendarContract.Events.CALENDAR_ID, calendarId)
        put(CalendarContract.Events.TITLE, draft.title.ifBlank { "(No title)" })
        put(CalendarContract.Events.EVENT_LOCATION, draft.location.takeIf { it.isNotBlank() })
        put(CalendarContract.Events.DESCRIPTION, draft.notes.takeIf { it.isNotBlank() })
        put(CalendarContract.Events.ALL_DAY, if (draft.allDay) 1 else 0)
        if (draft.allDay) {
            val startDay = toLocalDate(draft.startMillis)
            val endDay = toLocalDate(draft.endMillis).let { if (it < startDay) startDay else it }
            put(CalendarContract.Events.DTSTART, startDay.atStartOfDay(utc).toInstant().toEpochMilli())
            put(CalendarContract.Events.DTEND, endDay.plusDays(1).atStartOfDay(utc).toInstant().toEpochMilli())
            put(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
        } else {
            put(CalendarContract.Events.DTSTART, draft.startMillis)
            val end = if (draft.endMillis > draft.startMillis) draft.endMillis else draft.startMillis + 60_000L
            put(CalendarContract.Events.DTEND, end)
            put(CalendarContract.Events.EVENT_TIMEZONE, zoneId.id)
        }
        draft.recurrence.rrule?.let { put(CalendarContract.Events.RRULE, it) }
    }

    private fun writeReminder(eventId: Long, minutes: Int?) {
        if (minutes == null) return
        runCatching {
            val values = ContentValues().apply {
                put(CalendarContract.Reminders.EVENT_ID, eventId)
                put(CalendarContract.Reminders.MINUTES, minutes)
                put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
            }
            resolver.insert(CalendarContract.Reminders.CONTENT_URI, values)
        }
    }

    private fun deleteReminders(eventId: Long) {
        runCatching {
            resolver.delete(
                CalendarContract.Reminders.CONTENT_URI,
                "${CalendarContract.Reminders.EVENT_ID} = ?",
                arrayOf(eventId.toString()),
            )
        }
    }

    private fun toLocalDate(millis: Long): LocalDate =
        java.time.Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate()

    private fun colorIntToHex(color: Int): String = String.format("#%06X", 0xFFFFFF and color)

    private val utc: ZoneId = ZoneId.of("UTC")
}
