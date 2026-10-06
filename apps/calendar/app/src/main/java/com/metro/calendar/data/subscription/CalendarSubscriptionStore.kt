package com.metro.calendar.data.subscription

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Persists subscription metadata in app-private SharedPreferences and the last downloaded `.ics`
 * payload (per subscription) in app-private files. URLs are stored but never logged.
 */
class CalendarSubscriptionStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val cacheDir = File(appContext.filesDir, CACHE_DIR).apply { mkdirs() }

    fun newId(): String = UUID.randomUUID().toString()

    fun loadAll(): List<CalendarSubscription> {
        val raw = prefs.getString(KEY_SUBSCRIPTIONS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    array.optJSONObject(i)?.let { add(it.toSubscription()) }
                }
            }
        }.getOrDefault(emptyList())
    }

    fun get(id: String): CalendarSubscription? = loadAll().firstOrNull { it.id == id }

    fun upsert(subscription: CalendarSubscription) {
        val all = loadAll().toMutableList()
        val index = all.indexOfFirst { it.id == subscription.id }
        if (index >= 0) all[index] = subscription else all.add(subscription)
        save(all)
    }

    fun delete(id: String) {
        save(loadAll().filterNot { it.id == id })
        removeCache(id)
    }

    fun setEnabled(id: String, enabled: Boolean) {
        get(id)?.let { upsert(it.copy(enabled = enabled)) }
    }

    fun recordSuccess(id: String, etag: String?, lastModified: String?, nowMillis: Long) {
        get(id)?.let {
            upsert(it.copy(lastSyncMillis = nowMillis, lastSuccessMillis = nowMillis, lastError = null, etag = etag, lastModified = lastModified))
        }
    }

    fun recordError(id: String, message: String?, nowMillis: Long) {
        get(id)?.let { upsert(it.copy(lastSyncMillis = nowMillis, lastError = message)) }
    }

    private fun save(all: List<CalendarSubscription>) {
        val array = JSONArray()
        all.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_SUBSCRIPTIONS, array.toString()).apply()
    }

    fun readCache(id: String): String? = cacheFile(id).takeIf { it.exists() }?.readText()

    fun writeCache(id: String, body: String) {
        runCatching { cacheFile(id).writeText(body) }
    }

    fun removeCache(id: String) {
        runCatching { cacheFile(id).delete() }
    }

    private fun cacheFile(id: String): File = File(cacheDir, "$id.ics")

    private fun CalendarSubscription.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("url", url)
        put("colorHex", colorHex)
        put("enabled", enabled)
        put("lastSyncMillis", lastSyncMillis ?: JSONObject.NULL)
        put("lastSuccessMillis", lastSuccessMillis ?: JSONObject.NULL)
        put("lastError", lastError ?: JSONObject.NULL)
        put("etag", etag ?: JSONObject.NULL)
        put("lastModified", lastModified ?: JSONObject.NULL)
    }

    private fun JSONObject.toSubscription(): CalendarSubscription = CalendarSubscription(
        id = getString("id"),
        name = getString("name"),
        url = getString("url"),
        colorHex = optString("colorHex", "#0078D7"),
        enabled = optBoolean("enabled", true),
        lastSyncMillis = optLongOrNull("lastSyncMillis"),
        lastSuccessMillis = optLongOrNull("lastSuccessMillis"),
        lastError = optStringOrNull("lastError"),
        etag = optStringOrNull("etag"),
        lastModified = optStringOrNull("lastModified"),
    )

    private fun JSONObject.optLongOrNull(key: String): Long? =
        if (isNull(key)) null else optLong(key)

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

    private companion object {
        const val PREFS = "calendar_subscriptions"
        const val KEY_SUBSCRIPTIONS = "subscriptions"
        const val CACHE_DIR = "calendar-subscriptions"
    }
}
