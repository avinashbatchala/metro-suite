package com.metro.calendar.ui

import androidx.compose.ui.graphics.Color

/** Fixed palette offered when adding a subscription (kept deterministic for tests/screens). */
object SubscriptionColors {
    val palette: List<Pair<String, String>> = listOf(
        "Blue" to "#0078D7",
        "Red" to "#E81123",
        "Green" to "#00B294",
        "Orange" to "#FF8C00",
        "Purple" to "#7A3E9D",
        "Teal" to "#00838F",
        "Crimson" to "#C50F1F",
        "Amber" to "#FFB900",
    )

    val labels: List<String> = palette.map { it.first }

    fun defaultHex(): String = palette.first().second

    private val fallback = Color(0xFF0078D7)

    fun colorFor(hex: String): Color =
        runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(fallback)
}
