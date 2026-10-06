package com.metro.calendar.data

import android.content.Context
import com.metro.system.MetroTileContract

/** Current-conditions line sourced from the Weather app's live tile (no network of our own). */
data class CalendarWeather(
    val temperature: String,
    val condition: String?,
)

/**
 * Reads the Weather app's Start tile once so the day and week views can show today's
 * temperature. Best-effort: returns null when Weather is absent, has no cached location, or the
 * provider is not visible.
 */
object CalendarWeatherReader {
    const val WEATHER_PACKAGE = "com.metroweather.app"

    fun read(context: Context): CalendarWeather? = runCatching {
        val tile = MetroTileContract.readTile(context.contentResolver, WEATHER_PACKAGE) ?: return null
        val peek = tile.peeks?.firstOrNull() ?: return null
        val temperature = peek.subtitle?.takeIf { it.isNotBlank() } ?: return null
        CalendarWeather(
            temperature = temperature,
            condition = peek.body?.takeIf { it.isNotBlank() },
        )
    }.getOrNull()
}
