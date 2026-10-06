package com.metro.dialer.data

import android.content.Context

/**
 * Local Phone preferences: text-reply configuration and the smart-dial (T9) MetroSuite extension.
 */
class PhonePreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var textReplyEnabled: Boolean
        get() = prefs.getBoolean(KEY_TEXT_REPLY_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_TEXT_REPLY_ENABLED, value).apply()

    /** Smart dial / T9 is a MetroSuite extension — off by default to match stock WP8.1. */
    var smartDialEnabled: Boolean
        get() = prefs.getBoolean(KEY_SMART_DIAL, false)
        set(value) = prefs.edit().putBoolean(KEY_SMART_DIAL, value).apply()

    fun textReplies(): List<String> {
        val stored = prefs.getString(KEY_TEXT_REPLIES, null) ?: return DEFAULT_REPLIES
        val parsed = stored.split(SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() }
        return parsed.ifEmpty { DEFAULT_REPLIES }.take(4)
    }

    fun setTextReplies(replies: List<String>) {
        val normalized = replies.map { it.trim() }.filter { it.isNotEmpty() }.take(4)
        prefs.edit().putString(KEY_TEXT_REPLIES, normalized.joinToString(SEPARATOR)).apply()
    }

    companion object {
        private const val PREFS = "metro_dialer_phone_prefs"
        private const val KEY_TEXT_REPLY_ENABLED = "text_reply_enabled"
        private const val KEY_SMART_DIAL = "smart_dial_enabled"
        private const val KEY_TEXT_REPLIES = "text_replies"
        private const val SEPARATOR = "\n"

        val DEFAULT_REPLIES: List<String> = listOf(
            "I'll call you back.",
            "Please text me.",
            "Can't talk right now.",
            "I'm in a meeting.",
        )
    }
}
