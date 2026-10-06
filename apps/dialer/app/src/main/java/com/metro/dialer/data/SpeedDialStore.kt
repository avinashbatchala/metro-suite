package com.metro.dialer.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists speed-dial entries by durable contact identity ([SpeedDialEntry.contactLookupKey] /
 * [SpeedDialEntry.phoneDataId]) so renames, photo changes and label changes render automatically.
 *
 * [SpeedDialEntry.normalizedNumberFallback] is only used when no contact identity is available
 * (raw number entry) or when the contact can no longer be resolved.
 */
class SpeedDialStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): List<SpeedDialEntry> {
        val raw = prefs.getString(KEY_ENTRIES, null) ?: return emptyList()
        return runCatching { decode(raw) }.getOrDefault(emptyList())
    }

    fun save(entries: List<SpeedDialEntry>) {
        prefs.edit().putString(KEY_ENTRIES, encode(entries)).apply()
    }

    fun add(entry: SpeedDialEntry): List<SpeedDialEntry> {
        val current = load().toMutableList()
        current.removeAll { it.matches(entry) }
        current.add(entry)
        save(current)
        return current
    }

    fun removeById(id: String): List<SpeedDialEntry> {
        val current = load().toMutableList()
        current.removeAll { it.id == id }
        save(current)
        return current
    }

    fun contains(entry: SpeedDialEntry): Boolean = load().any { it.matches(entry) }

    private fun SpeedDialEntry.matches(other: SpeedDialEntry): Boolean {
        if (contactLookupKey != null && contactLookupKey == other.contactLookupKey) return true
        if (phoneDataId != null && phoneDataId == other.phoneDataId) return true
        val a = DialerCallLogic.canonicalDigits(normalizedNumberFallback.ifEmpty { phoneNumber })
        val b = DialerCallLogic.canonicalDigits(other.normalizedNumberFallback.ifEmpty { other.phoneNumber })
        return a.isNotEmpty() && a == b
    }

    private fun encode(entries: List<SpeedDialEntry>): String {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject().apply {
                    put("id", entry.id)
                    put("displayName", entry.displayName)
                    put("phoneNumber", entry.phoneNumber)
                    put("contactLookupKey", entry.contactLookupKey ?: JSONObject.NULL)
                    put("phoneDataId", entry.phoneDataId ?: JSONObject.NULL)
                    put("normalizedNumberFallback", entry.normalizedNumberFallback)
                },
            )
        }
        return array.toString()
    }

    private fun decode(raw: String): List<SpeedDialEntry> {
        val array = JSONArray(raw)
        val entries = mutableListOf<SpeedDialEntry>()
        for (index in 0 until array.length()) {
            val objectValue = array.optJSONObject(index) ?: continue
            entries.add(
                SpeedDialEntry(
                    id = objectValue.optString("id", index.toString()),
                    displayName = objectValue.optString("displayName"),
                    phoneNumber = objectValue.optString("phoneNumber"),
                    contactLookupKey = objectValue.optString("contactLookupKey").takeIf { it.isNotBlank() },
                    phoneDataId = if (objectValue.isNull("phoneDataId")) {
                        null
                    } else {
                        objectValue.optLong("phoneDataId")
                    },
                    normalizedNumberFallback = objectValue.optString("normalizedNumberFallback"),
                ),
            )
        }
        return entries
    }

    companion object {
        private const val PREFS = "metro_dialer_speed_dial"
        private const val KEY_ENTRIES = "entries_v2"
    }
}
