package com.metro.widgets.data

import com.metro.system.MetroWorldClockCatalog

/** Pure World Clock widget selection rules (1–3 valid, de-duplicated cities). Testable without Context. */
object WorldClockSelectionLogic {
    const val MIN = 1
    const val MAX = 3

    fun sanitize(ids: List<String>): List<String> =
        ids.filter { MetroWorldClockCatalog.byId(it) != null }.distinct().take(MAX)

    fun isFull(ids: List<String>): Boolean = sanitize(ids).size >= MAX

    /** True when [id] is the only selected city (removing it must be blocked — minimum 1). */
    fun isLastRemaining(ids: List<String>, id: String): Boolean {
        val base = sanitize(ids)
        return base.size <= MIN && id in base
    }

    /**
     * Toggles [id]: adds when absent (if room), removes when present. Invalid ids are ignored and
     * the final remaining city cannot be removed (contract minimum is 1).
     */
    fun toggle(ids: List<String>, id: String): List<String> {
        if (MetroWorldClockCatalog.byId(id) == null) return sanitize(ids)
        val base = sanitize(ids)
        return when {
            id in base -> if (base.size <= MIN) base else base - id
            else -> (base + id).take(MAX)
        }
    }

    /** Keeps a non-empty selection: real selections are preserved; empty falls back to defaults. */
    fun withDefaults(ids: List<String>): List<String> {
        val sanitized = sanitize(ids)
        return sanitized.ifEmpty { MetroWorldClockCatalog.DEFAULT_IDS }
    }
}
