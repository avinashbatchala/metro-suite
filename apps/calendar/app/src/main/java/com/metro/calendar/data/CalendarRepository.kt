package com.metro.calendar.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.metro.calendar.data.subscription.CalendarSubscription
import com.metro.calendar.data.subscription.CalendarSubscriptionStore
import com.metro.calendar.data.subscription.IcsCalendarSource
import com.metro.system.MetroPreferences
import java.time.ZoneId

class CalendarRepository(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = MetroPreferences(appContext)
    private val calendarPrefs = CalendarPreferences(appContext)
    private val writeRepository = CalendarWriteRepository(appContext)
    private val zoneId: ZoneId = ZoneId.systemDefault()
    private val subscriptionStore = CalendarSubscriptionStore(appContext)
    private val subscriptions = IcsCalendarSource(subscriptionStore)

    fun hasDevicePermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    fun subscriptions(): List<CalendarSubscription> = subscriptions.subscriptions()

    fun addSubscription(name: String, url: String, colorHex: String): CalendarSubscription? =
        subscriptions.add(name, url, colorHex, System.currentTimeMillis())

    fun removeSubscription(id: String) = subscriptions.remove(id)

    fun setSubscriptionEnabled(id: String, enabled: Boolean) =
        subscriptions.setEnabled(id, enabled)

    fun syncSubscription(id: String) = subscriptions.sync(id)

    fun syncAllSubscriptions(): Int = subscriptions.syncAll()

    /**
     * Merged event stream: device provider events (when permitted) plus every enabled ICS
     * subscription from its last-good cache. Applies local visibility/colour overrides, then
     * sorts chronologically.
     */
    fun loadEvents(startMillis: Long, endMillis: Long): List<CalendarEvent> {
        val merged = ArrayList<CalendarEvent>()
        if (hasDevicePermission()) {
            runCatching { loadDeviceEvents(startMillis, endMillis) }.getOrNull()?.let { merged.addAll(it) }
        }
        merged.addAll(subscriptions.loadEvents(startMillis, endMillis))
        return merged
            .distinctBy { event -> "${event.sourceType}-${event.sourceId}-${event.id}-${event.startMillis}" }
            .filter { isVisible(it) }
            .map { applyColorOverride(it) }
            .sortedWith(compareBy({ it.startMillis }, { it.endMillis }))
    }

    fun loadEventsAround(epochDay: Long, dayRadius: Int = 90): List<CalendarEvent> {
        val start = CalendarLogic.millisFromEpochDay(epochDay - dayRadius, zoneId)
        val end = CalendarLogic.millisFromEpochDay(epochDay + dayRadius + 1, zoneId)
        return loadEvents(start, end)
    }

    /** Loads exactly the days the visible view needs (inclusive [startDay], exclusive [endDay]). */
    fun loadEventsForDays(startDay: Long, endDay: Long): List<CalendarEvent> =
        loadEvents(
            CalendarLogic.millisFromEpochDay(startDay, zoneId),
            CalendarLogic.millisFromEpochDay(endDay, zoneId),
        )

    fun loadDemoEvents(): List<CalendarEvent> = StubCalendarDataSource.demoEvents(zoneId)

    /** Attendees for an event, from `CalendarContract.Attendees`. */
    fun attendees(eventId: Long): List<CalendarAttendee> {
        if (!hasDevicePermission()) return emptyList()
        val projection = arrayOf(
            CalendarContract.Attendees.ATTENDEE_NAME,
            CalendarContract.Attendees.ATTENDEE_EMAIL,
            CalendarContract.Attendees.ATTENDEE_TYPE,
        )
        val result = mutableListOf<CalendarAttendee>()
        runCatching {
            resolver().query(
                CalendarContract.Attendees.CONTENT_URI,
                projection,
                "${CalendarContract.Attendees.EVENT_ID} = ?",
                arrayOf(eventId.toString()),
                null,
            )?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(CalendarContract.Attendees.ATTENDEE_NAME)
                val emailIdx = cursor.getColumnIndex(CalendarContract.Attendees.ATTENDEE_EMAIL)
                while (cursor.moveToNext()) {
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx) else null
                    val email = if (emailIdx >= 0) cursor.getString(emailIdx) else null
                    if (!name.isNullOrBlank() || !email.isNullOrBlank()) {
                        result += CalendarAttendee(name = name, email = email)
                    }
                }
            }
        }
        return result
    }

    private fun resolver() = appContext.contentResolver

    private fun keyFor(event: CalendarEvent): String = when (event.sourceType) {
        CalendarSourceType.SUBSCRIPTION ->
            CalendarPreferences.subscriptionKey(event.sourceId.orEmpty())
        else -> CalendarPreferences.deviceKey(event.calendarId)
    }

    private fun isVisible(event: CalendarEvent): Boolean = calendarPrefs.isVisible(keyFor(event))

    private fun applyColorOverride(event: CalendarEvent): CalendarEvent =
        calendarPrefs.colorOverride(keyFor(event))?.let { event.copy(calendarColorHex = it) } ?: event

    private fun loadDeviceEvents(startMillis: Long, endMillis: Long): List<CalendarEvent> {
        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.EVENT_COLOR,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.ORGANIZER,
            CalendarContract.Instances.RRULE,
            CalendarContract.Instances.AVAILABILITY,
        )

        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, startMillis)
        ContentUris.appendId(builder, endMillis)
        val uri = builder.build()
        val accessMap = writeRepository.calendarAccessMap()

        val events = mutableListOf<CalendarEvent>()
        appContext.contentResolver.query(uri, projection, null, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { cursor ->
            fun idx(column: String) = cursor.getColumnIndex(column)
            val idIdx = idx(CalendarContract.Instances.EVENT_ID)
            val calIdIdx = idx(CalendarContract.Instances.CALENDAR_ID)
            val titleIdx = idx(CalendarContract.Instances.TITLE)
            val descIdx = idx(CalendarContract.Instances.DESCRIPTION)
            val beginIdx = idx(CalendarContract.Instances.BEGIN)
            val endIdx = idx(CalendarContract.Instances.END)
            val allDayIdx = idx(CalendarContract.Instances.ALL_DAY)
            val colorIdx = idx(CalendarContract.Instances.EVENT_COLOR)
            val calNameIdx = idx(CalendarContract.Instances.CALENDAR_DISPLAY_NAME)
            val locationIdx = idx(CalendarContract.Instances.EVENT_LOCATION)
            val organizerIdx = idx(CalendarContract.Instances.ORGANIZER)
            val rruleIdx = idx(CalendarContract.Instances.RRULE)
            val availIdx = idx(CalendarContract.Instances.AVAILABILITY)

            while (cursor.moveToNext()) {
                val id = if (idIdx >= 0) cursor.getLong(idIdx) else cursor.position.toLong()
                val calendarId = if (calIdIdx >= 0) cursor.getLong(calIdIdx) else -1L
                val title = if (titleIdx >= 0) cursor.getString(titleIdx).orEmpty() else "(No title)"
                val begin = if (beginIdx >= 0) cursor.getLong(beginIdx) else 0L
                val end = if (endIdx >= 0) cursor.getLong(endIdx) else begin
                val allDay = allDayIdx >= 0 && cursor.getInt(allDayIdx) == 1
                val colorInt = if (colorIdx >= 0 && !cursor.isNull(colorIdx)) cursor.getInt(colorIdx) else 0
                val calName = if (calNameIdx >= 0) cursor.getString(calNameIdx) else null
                val location = if (locationIdx >= 0) cursor.getString(locationIdx) else null
                val description = if (descIdx >= 0) cursor.getString(descIdx) else null
                val organizer = if (organizerIdx >= 0) cursor.getString(organizerIdx) else null
                val rrule = if (rruleIdx >= 0) cursor.getString(rruleIdx) else null
                val availability = if (availIdx >= 0 && !cursor.isNull(availIdx)) {
                    EventAvailability.fromProvider(cursor.getInt(availIdx))
                } else {
                    EventAvailability.Busy
                }
                val canWrite = accessMap[calendarId] == true

                events += CalendarEvent(
                    id = id,
                    title = title.ifBlank { "(No title)" },
                    startMillis = begin,
                    endMillis = end,
                    allDay = allDay,
                    calendarColorHex = colorIntToHex(colorInt),
                    calendarName = calName,
                    location = location,
                    sourceType = CalendarSourceType.DEVICE,
                    calendarId = calendarId,
                    canEdit = canWrite,
                    canDelete = canWrite,
                    description = description,
                    organizer = organizer,
                    availability = availability,
                    recurring = !rrule.isNullOrBlank(),
                )
            }
        }
        return events.distinctBy { "${it.id}-${it.startMillis}" }
    }

    private fun colorIntToHex(color: Int): String {
        if (color == 0) return prefs.accentColorHex
        return String.format("#%06X", color and 0xFFFFFF)
    }
}

/** One attendee row from the provider. */
data class CalendarAttendee(
    val name: String?,
    val email: String?,
)

