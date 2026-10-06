package com.metro.calendar.data

import android.content.Context

/**
 * App-local visibility + colour overrides for device calendars.
 *
 * WP8.1 Calendar Settings lets the user show/hide and recolour calendars. On Android (and
 * especially GrapheneOS with no sync accounts) mutating provider calendar metadata is unsafe or
 * impossible, so these preferences live in app-private storage and are applied when the event
 * stream is built. Subscribed ICS calendars are keyed by their subscription id (prefixed `sub:`).
 */
class CalendarPreferences(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("calendar_settings", Context.MODE_PRIVATE)

    fun isVisible(calendarKey: String): Boolean =
        prefs.getBoolean("visible_$calendarKey", true)

    fun setVisible(calendarKey: String, visible: Boolean) {
        prefs.edit().putBoolean("visible_$calendarKey", visible).apply()
    }

    fun colorOverride(calendarKey: String): String? =
        prefs.getString("color_$calendarKey", null)

    fun setColor(calendarKey: String, colorHex: String) {
        prefs.edit().putString("color_$calendarKey", colorHex).apply()
    }

    companion object {
        fun deviceKey(calendarId: Long): String = "cal:$calendarId"
        fun subscriptionKey(subscriptionId: String): String = "sub:$subscriptionId"
    }
}
