package com.metro.system

/**
 * A selectable world-clock city. The IANA [zoneId] is the single source of truth for time and DST —
 * fixed UTC offsets are never stored.
 */
data class MetroWorldClockCity(
    val id: String,
    val name: String,
    val region: String,
    val zoneId: String,
    val aliases: List<String> = emptyList(),
) {
    val hasContent: Boolean
        get() = id.isNotBlank() && zoneId.isNotBlank()
}
