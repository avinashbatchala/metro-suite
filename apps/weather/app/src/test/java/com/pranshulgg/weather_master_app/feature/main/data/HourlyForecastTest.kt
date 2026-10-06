package com.pranshulgg.weather_master_app.feature.main.data

import com.pranshulgg.weather_master_app.core.model.domain.location.Location
import com.pranshulgg.weather_master_app.core.model.domain.weather.Weather
import com.pranshulgg.weather_master_app.core.model.domain.weather.WeatherCurrent
import com.pranshulgg.weather_master_app.core.model.domain.weather.WeatherHourly
import com.pranshulgg.weather_master_app.core.model.weather.TemperatureUnit
import com.pranshulgg.weather_master_app.core.model.weather.WeatherCondition
import com.pranshulgg.weather_master_app.core.ui.components.WeatherGlyphKind
import com.pranshulgg.weather_master_app.core.ui.components.toGlyphKind
import com.pranshulgg.weather_master_app.core.utils.weather.location.getFullLocationName
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HourlyForecastTest {

    private val utc = ZoneId.of("UTC")

    private fun location() = Location(
        id = "test",
        name = "WEALDEN",
        latitude = 50.9,
        longitude = 0.2,
        country = "ENGLAND",
        timezone = "UTC",
        countryCode = "GB",
        state = "",
        isDefault = true,
    )

    private fun hourlyAt(
        hour: Int,
        temperature: Double?,
        condition: WeatherCondition = WeatherCondition.PARTLY_CLOUDY,
        precipitation: Int? = null,
        humidity: Double? = null,
    ): WeatherHourly {
        val time = java.time.LocalDate.of(2024, 6, 15)
            .atTime(hour, 0)
            .atZone(utc)
            .toInstant()
            .toEpochMilli()
        return WeatherHourly(
            temperature = temperature,
            windSpeed = null,
            windDirection = null,
            rain = 0.0,
            snowfall = null,
            uvIndex = null,
            pressureMsl = null,
            visibility = null,
            humidity = humidity,
            dewPoint = null,
            weatherCondition = condition,
            time = time,
            precipitationProbability = precipitation,
        )
    }

    private fun weather(hourly: List<WeatherHourly>): Weather {
        val now = 0L
        return Weather(
            location = location(),
            current = WeatherCurrent(
                temperature = 20.0,
                humidity = null,
                windSpeed = null,
                windDirection = null,
                pressureMsl = null,
                visibility = null,
                cloudCover = null,
                uvIndex = null,
                weatherCondition = WeatherCondition.PARTLY_CLOUDY,
                feelsLike = null,
                dewPoint = null,
                utcOffsetSeconds = null,
                lastUpdatedInMilli = now,
            ),
            hourly = hourly,
            daily = emptyList(),
        )
    }

    @Test
    fun mapsRawHourlyToNormalizedForecasts() {
        val raw = weather(
            listOf(
                hourlyAt(14, 21.0, WeatherCondition.PARTLY_CLOUDY, precipitation = 0, humidity = 55.4),
                hourlyAt(15, 20.6, WeatherCondition.RAIN, precipitation = 30),
            )
        )

        val result = raw.toHourlyForecasts()

        assertEquals(2, result.size)
        assertEquals(21.0, result[0].temperature, 0.0)
        assertEquals(WeatherCondition.PARTLY_CLOUDY, result[0].condition)
        assertEquals(0, result[0].precipitationProbability)
        assertEquals(55, result[0].humidity)
        assertEquals(WeatherCondition.RAIN, result[1].condition)
        assertEquals(30, result[1].precipitationProbability)
        assertTrue(result[1].humidity == null)
    }

    @Test
    fun missingTemperatureMapsToNaN() {
        val result = weather(listOf(hourlyAt(14, null))).toHourlyForecasts()
        assertTrue(result.single().temperature.isNaN())
        assertEquals("--", formatTemperature(result.single().temperature, TemperatureUnit.CELSIUS))
    }

    @Test
    fun emptyHourlyMapsToEmptyList() {
        assertTrue(weather(emptyList()).toHourlyForecasts().isEmpty())
    }

    @Test
    fun formatsCelsiusAndFahrenheit() {
        assertEquals("21°", formatTemperature(21.0, TemperatureUnit.CELSIUS))
        assertEquals("70°", formatTemperature(21.0, TemperatureUnit.FAHRENHEIT))
        assertEquals("0°", formatTemperature(0.0, TemperatureUnit.CELSIUS))
        assertEquals("32°", formatTemperature(0.0, TemperatureUnit.FAHRENHEIT))
    }

    @Test
    fun formats24HourAnd12HourTime() {
        val twoPm = Instant.parse("2024-06-15T14:00:00Z").toEpochMilli()

        assertEquals("14:00", formatHourlyTime(twoPm, "UTC", is24Hour = true, locale = Locale.US))
        assertEquals("2:00 PM", formatHourlyTime(twoPm, "UTC", is24Hour = false, locale = Locale.US))
    }

    @Test
    fun formatsPrecipitationNullZeroAndPositive() {
        assertEquals("--", formatPrecipitation(null))
        assertEquals("0%", formatPrecipitation(0))
        assertEquals("45%", formatPrecipitation(45))
    }

    @Test
    fun buildsSingleRowContentDescription() {
        assertEquals(
            "2:00 PM, partly cloudy, 21 degrees, 0 percent chance of precipitation",
            hourlyContentDescription("2:00 PM", "partly cloudy", "21°", "0%"),
        )
    }

    @Test
    fun conditionGlyphKindHonoursDayAndNight() {
        assertEquals(
            WeatherGlyphKind.PARTLY_CLOUDY,
            WeatherCondition.PARTLY_CLOUDY.toGlyphKind(isDay = true),
        )
        assertEquals(
            WeatherGlyphKind.CLEAR_DAY,
            WeatherCondition.CLEAR_SKY.toGlyphKind(isDay = true),
        )
        assertEquals(
            WeatherGlyphKind.CLEAR_NIGHT,
            WeatherCondition.CLEAR_SKY.toGlyphKind(isDay = false),
        )
        assertEquals(
            WeatherGlyphKind.CLOUDY,
            WeatherCondition.OVERCAST.toGlyphKind(isDay = true),
        )
        assertEquals(
            WeatherGlyphKind.RAIN,
            WeatherCondition.RAIN.toGlyphKind(isDay = true),
        )
        assertEquals(
            WeatherGlyphKind.THUNDER,
            WeatherCondition.THUNDERSTORM.toGlyphKind(isDay = true),
        )
    }

    @Test
    fun referenceFixtureMatchesWealdenAfternoon() {
        val fixture = hourlyReferenceFixture()

        assertEquals("WEALDEN, ENGLAND", getFullLocationName(fixture.location))
        assertEquals(5, fixture.forecasts.size)

        val first = fixture.forecasts.first()
        assertEquals(21.0, first.temperature, 0.0)
        assertEquals(WeatherCondition.PARTLY_CLOUDY, first.condition)
        assertEquals(0, first.precipitationProbability)
        assertEquals(
            "14:00",
            formatHourlyTime(first.timestamp, fixture.location.timezone, is24Hour = true, locale = Locale.US),
        )

        val last = fixture.forecasts.last()
        assertEquals(20.0, last.temperature, 0.0)
        assertEquals(
            "18:00",
            formatHourlyTime(last.timestamp, fixture.location.timezone, is24Hour = true, locale = Locale.US),
        )
    }
}
