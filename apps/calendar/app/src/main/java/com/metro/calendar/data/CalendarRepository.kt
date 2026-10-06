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
     * subscription from its last-good cache. Sorted chronologically.
     */
    fun loadEvents(startMillis: Long, endMillis: Long): List<CalendarEvent> {
        val merged = ArrayList<CalendarEvent>()
        if (hasDevicePermission()) {
            runCatching { loadDeviceEvents(startMillis, endMillis) }.getOrNull()?.let { merged.addAll(it) }
        }
        merged.addAll(subscriptions.loadEvents(startMillis, endMillis))
        return merged
            .distinctBy { event -> "${event.sourceType}-${event.sourceId}-${event.id}-${event.startMillis}" }
            .sortedWith(compareBy({ it.startMillis }, { it.endMillis }))
    }

    fun loadEventsAround(epochDay: Long, dayRadius: Int = 90): List<CalendarEvent> {
        val start = CalendarLogic.millisFromEpochDay(epochDay - dayRadius, zoneId)
        val end = CalendarLogic.millisFromEpochDay(epochDay + dayRadius + 1, zoneId)
        return loadEvents(start, end)
    }

    fun loadDemoEvents(): List<CalendarEvent> = StubCalendarDataSource.demoEvents(zoneId)

    private fun loadDeviceEvents(startMillis: Long, endMillis: Long): List<CalendarEvent> {
        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.EVENT_COLOR,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
            CalendarContract.Instances.EVENT_LOCATION,
        )

        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, startMillis)
        ContentUris.appendId(builder, endMillis)
        val uri = builder.build()

        val events = mutableListOf<CalendarEvent>()
        appContext.contentResolver.query(uri, projection, null, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { cursor ->
            val idIdx = cursor.getColumnIndex(CalendarContract.Instances.EVENT_ID)
            val titleIdx = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
            val beginIdx = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
            val endIdx = cursor.getColumnIndex(CalendarContract.Instances.END)
            val allDayIdx = cursor.getColumnIndex(CalendarContract.Instances.ALL_DAY)
            val colorIdx = cursor.getColumnIndex(CalendarContract.Instances.EVENT_COLOR)
            val calNameIdx = cursor.getColumnIndex(CalendarContract.Instances.CALENDAR_DISPLAY_NAME)
            val locationIdx = cursor.getColumnIndex(CalendarContract.Instances.EVENT_LOCATION)

            while (cursor.moveToNext()) {
                val id = if (idIdx >= 0) cursor.getLong(idIdx) else cursor.position.toLong()
                val title = if (titleIdx >= 0) cursor.getString(titleIdx).orEmpty() else "(No title)"
                val begin = if (beginIdx >= 0) cursor.getLong(beginIdx) else 0L
                val end = if (endIdx >= 0) cursor.getLong(endIdx) else begin
                val allDay = allDayIdx >= 0 && cursor.getInt(allDayIdx) == 1
                val colorInt = if (colorIdx >= 0 && !cursor.isNull(colorIdx)) cursor.getInt(colorIdx) else 0
                val calName = if (calNameIdx >= 0) cursor.getString(calNameIdx) else null
                val location = if (locationIdx >= 0) cursor.getString(locationIdx) else null

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
