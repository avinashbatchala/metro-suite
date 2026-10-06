package com.pranshulgg.weather_master_app.feature.main.data

import com.pranshulgg.weather_master_app.core.model.domain.location.Location
import com.pranshulgg.weather_master_app.core.model.domain.weather.Weather
import com.pranshulgg.weather_master_app.core.model.weather.TemperatureUnit
import com.pranshulgg.weather_master_app.core.model.weather.WeatherCondition
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Weather-local, presentation-ready hourly row.
 *
 * The raw [com.pranshulgg.weather_master_app.core.model.domain.weather.WeatherHourly] API
 * model is intentional: the hourly pane renders only time, condition glyph, temperature and
 * precipitation, so this type narrows the contract without leaking the network shape into the
 * UI. Missing temperature is carried as [Double.NaN] (rendered `--`).
 */
data class HourlyForecast(
    val timestamp: Long,
    val temperature: Double,
    val condition: WeatherCondition,
    val precipitationProbability: Int?,
    val windSpeed: Double?,
    val humidity: Int?,
)

/** Maps the raw API model to the normalized hourly list the home pane consumes. */
fun Weather.toHourlyForecasts(): List<HourlyForecast> = hourly.map { hour ->
    HourlyForecast(
        timestamp = hour.time,
        temperature = hour.temperature ?: Double.NaN,
        condition = hour.weatherCondition,
        precipitationProbability = hour.precipitationProbability,
        windSpeed = hour.windSpeed,
        humidity = hour.humidity?.roundToInt(),
    )
}

/**
 * Locale-aware clock label for an hourly row (`14:00` for 24-hour, `4:00 PM` for 12-hour).
 * Pure `java.time` so it is exercisable from JVM unit tests.
 */
fun formatHourlyTime(
    timestampMillis: Long,
    timezoneId: String,
    is24Hour: Boolean,
    locale: Locale = Locale.getDefault(),
): String {
    val zone = safeHourlyZone(timezoneId)
    val pattern = if (is24Hour) "H:mm" else "h:mm a"
    return DateTimeFormatter
        .ofPattern(pattern, locale)
        .withZone(zone)
        .format(Instant.ofEpochMilli(timestampMillis))
}

/** Formats an always-Celsius temperature into the active unit, e.g. `21°`. */
fun formatTemperature(celsius: Double, unit: TemperatureUnit): String {
    if (celsius.isNaN()) return "--"
    val converted = TemperatureUnit.CELSIUS.convert(celsius, unit) ?: return "--"
    return "${converted.roundToInt()}°"
}

/** Formats a precipitation probability, e.g. `0%`. `null` (unknown) renders `--`. */
fun formatPrecipitation(probability: Int?): String =
    if (probability == null) "--" else "$probability%"

/**
 * Single accessible description for an hourly row, e.g.
 * `2 PM, partly cloudy, 21 degrees, 0 percent chance of precipitation`.
 */
fun hourlyContentDescription(
    time: String,
    conditionLabel: String,
    temperature: String,
    precipitation: String,
): String {
    val degrees = temperature.removeSuffix("°")
    val percent = precipitation.removeSuffix("%")
    return "$time, $conditionLabel, $degrees degrees, $percent percent chance of precipitation"
}

private fun safeHourlyZone(id: String): ZoneId =
    try {
        ZoneId.of(id)
    } catch (e: Exception) {
        ZoneId.systemDefault()
    }

/**
 * Deterministic hourly data set for screenshot fixtures / previews — Bing Weather's
 * 2024 Wealden reference: a flat, partly-cloudy afternoon.
 */
data class HourlyReferenceFixture(
    val location: Location,
    val forecasts: List<HourlyForecast>,
)

fun hourlyReferenceFixture(): HourlyReferenceFixture {
    val zone = ZoneId.of("Europe/London")
    val date = LocalDate.of(2024, 6, 15)
    fun at(hour: Int): Long =
        date.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()

    val location = Location(
        id = "wealden",
        name = "WEALDEN",
        latitude = 50.9276,
        longitude = 0.2835,
        country = "ENGLAND",
        timezone = zone.id,
        countryCode = "GB",
        state = "",
        isDefault = true,
    )

    val forecasts = listOf(
        HourlyForecast(at(14), 21.0, WeatherCondition.PARTLY_CLOUDY, 0, null, null),
        HourlyForecast(at(15), 21.0, WeatherCondition.PARTLY_CLOUDY, 0, null, null),
        HourlyForecast(at(16), 21.0, WeatherCondition.PARTLY_CLOUDY, 0, null, null),
        HourlyForecast(at(17), 21.0, WeatherCondition.PARTLY_CLOUDY, 0, null, null),
        HourlyForecast(at(18), 20.0, WeatherCondition.PARTLY_CLOUDY, 0, null, null),
    )

    return HourlyReferenceFixture(location = location, forecasts = forecasts)
}
