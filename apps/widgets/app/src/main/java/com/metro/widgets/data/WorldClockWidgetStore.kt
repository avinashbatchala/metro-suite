package com.metro.widgets.data

import android.content.Context
import com.metro.system.MetroWorldClockCatalog
import com.metro.system.MetroWorldClockCity

/**
 * Persists the World Clock widget's selected city ids (1–3) in app-private storage. The shared
 * [MetroWorldClockCatalog] is the source of truth for names/zones.
 */
class WorldClockWidgetStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): List<String> {
        if (!prefs.contains(KEY_CITIES)) return MetroWorldClockCatalog.DEFAULT_IDS
        val raw = prefs.getString(KEY_CITIES, "") ?: ""
        val ids = raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        return WorldClockSelectionLogic.withDefaults(ids)
    }

    fun save(ids: List<String>) {
        prefs.edit()
            .putString(KEY_CITIES, WorldClockSelectionLogic.sanitize(ids).joinToString(","))
            .apply()
    }

    fun resolvedCities(): List<MetroWorldClockCity> =
        MetroWorldClockCatalog.resolve(load())

    companion object {
        const val MAX_CITIES = 3
        private const val PREFS = "widgets_world_clock"
        private const val KEY_CITIES = "cities"
    }
}
