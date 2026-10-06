package com.metro.widgets.data

import com.metro.system.MetroWorldClockCatalog

/** Pure World Clock widget selection rules (1–3 valid, de-duplicated cities). Testable without Context. */
object WorldClockSelectionLogic {
    const val MAX = 3

    fun sanitize(ids: List<String>): List<String> =
        ids.filter { MetroWorldClockCatalog.byId(it) != null }.distinct().take(MAX)

    fun isFull(ids: List<String>): Boolean = sanitize(ids).size >= MAX

    /** Toggles [id]: adds when absent (if room), removes when present. Invalid ids are ignored. */
    fun toggle(ids: List<String>, id: String): List<String> {
        if (MetroWorldClockCatalog.byId(id) == null) return sanitize(ids)
        val base = sanitize(ids)
        return if (id in base) base - id else (base + id).take(MAX)
    }

    /** Falls back to the catalog defaults when nothing valid is selected. */
    fun withDefaults(ids: List<String>): List<String> {
        val sanitized = sanitize(ids)
        return sanitized.ifEmpty { MetroWorldClockCatalog.DEFAULT_IDS }
    }
}
