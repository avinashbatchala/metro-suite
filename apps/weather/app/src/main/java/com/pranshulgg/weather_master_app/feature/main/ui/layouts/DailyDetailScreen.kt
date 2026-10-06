package com.pranshulgg.weather_master_app.feature.main.ui.layouts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroDimens
import com.metro.ui.MetroPanorama
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.pranshulgg.weather_master_app.core.model.domain.weather.Weather
import com.pranshulgg.weather_master_app.core.model.domain.weather.WeatherUnits
import com.pranshulgg.weather_master_app.core.model.weather.TemperatureUnit
import com.pranshulgg.weather_master_app.core.model.weather.toLabel
import com.pranshulgg.weather_master_app.core.ui.components.WeatherGlyph
import com.pranshulgg.weather_master_app.core.ui.components.toGlyphKind
import com.pranshulgg.weather_master_app.core.utils.formatters.to12HourTimeString
import com.pranshulgg.weather_master_app.core.utils.formatters.to24HourTimeString
import com.pranshulgg.weather_master_app.core.utils.formatters.toWeekdayString
import com.pranshulgg.weather_master_app.feature.main.ui.WeatherPalette
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * Historical detailed-day view: a top track of dates, day/night columns with metrics, and an
 * hourly forecast graph for the selected day (custom Canvas — no third-party chart UI).
 */
@Composable
fun DailyDetailScreen(
    weather: Weather,
    units: WeatherUnits,
    is24: Boolean,
    initialIndex: Int,
    modifier: Modifier = Modifier,
) {
    val daily = weather.daily
    if (daily.isEmpty()) {
        MetroText(
            text = "no daily data",
            style = MetroTextStyle.ListItemTitle,
            color = MetroTheme.colors.secondaryText,
            modifier = Modifier.padding(MetroDimens.ScreenHorizontalMargin, 24.dp),
        )
        return
    }
    val titles = daily.map { day ->
        "${toWeekdayString(day.time, weather.location.timezone).lowercase()} ${
            day.time.dayOfMonth(weather.location.timezone)
        }"
    }
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, daily.lastIndex),
        pageCount = { daily.size },
    )
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    Box(modifier = modifier.fillMaxSize().background(WeatherPalette.BingBlue)) {
        MetroPanorama(
            titles = titles,
            pagerState = pagerState,
            modifier = Modifier.fillMaxSize().padding(bottom = 56.dp),
            onTitleClick = { scope.launch { pagerState.animateScrollToPage(it) } },
            pageContent = { page ->
                DayDetailPane(
                    weather = weather,
                    units = units,
                    is24 = is24,
                    dayIndex = page,
                    scrollState = scroll,
                )
            },
        )
    }
}

@Composable
private fun DayDetailPane(
    weather: Weather,
    units: WeatherUnits,
    is24: Boolean,
    dayIndex: Int,
    scrollState: androidx.compose.foundation.ScrollState,
) {
    val day = weather.daily.getOrNull(dayIndex) ?: return
    val dayKind = day.weatherCondition.toGlyphKind(daily = day, targetTimeMilli = day.time)
    val high = temp(day.temperatureMax, units)
    val low = temp(day.temperatureMin, units)
    val hours = remember(dayIndex) {
        weather.hourly.filter { hour ->
            sameLocalDay(hour.time, day.time, weather.location.timezone)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = MetroDimens.ScreenHorizontalMargin),
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            DayNightColumn(
                title = "Day",
                temperature = high,
                condition = day.weatherCondition,
                kind = dayKind,
                modifier = Modifier.weight(1f),
            )
            DayNightColumn(
                title = "Night",
                temperature = low,
                condition = day.weatherCondition,
                kind = dayKind,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        DetailFact("Wind", day.windSpeed?.let { "${it.roundToInt()} ${units.windUnit.name.lowercase()}" })
        DetailFact("Humidity", day.humidity?.let { "${it.roundToInt()}%" })
        DetailFact("Sunrise", formatTime(day.sunrise, weather.location.timezone, is24))
        DetailFact("Sunset", formatTime(day.sunset, weather.location.timezone, is24))
        DetailFact("Precipitation", day.precipitationProbabilityMax?.let { "$it%" })

        if (hours.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            MetroText("HOURLY FORECAST", style = MetroTextStyle.SectionHeader, color = MetroTheme.colors.secondaryText)
            Spacer(modifier = Modifier.height(8.dp))
            HourlyGraph(
                temperatures = hours.mapNotNull { it.temperature },
                precipitation = hours.map { it.precipitationProbability ?: 0 },
                timezone = weather.location.timezone,
                tempUnit = units.tempUnit,
                is24 = is24,
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun DayNightColumn(
    title: String,
    temperature: String?,
    condition: com.pranshulgg.weather_master_app.core.model.weather.WeatherCondition,
    kind: com.pranshulgg.weather_master_app.core.ui.components.WeatherGlyphKind,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(modifier = modifier) {
        MetroText(title.uppercase(), style = MetroTextStyle.SectionHeader, color = MetroTheme.colors.secondaryText)
        WeatherGlyph(kind, size = 40.dp, color = MetroTheme.colors.primaryText)
        MetroText("${temperature ?: "-"}", style = MetroTextStyle.PageTitle, color = MetroTheme.colors.primaryText)
        MetroText(condition.toLabel(context), style = MetroTextStyle.ListItemTitle, color = MetroTheme.colors.primaryText)
    }
}

@Composable
private fun DetailFact(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        MetroText(label, style = MetroTextStyle.ListItemSubtitle, color = MetroTheme.colors.secondaryText, modifier = Modifier.weight(1f))
        MetroText(value, style = MetroTextStyle.ListItemTitle, color = MetroTheme.colors.primaryText)
    }
}

/** Custom hourly chart: temperature polyline + points over precipitation bars. */
@Composable
private fun HourlyGraph(
    temperatures: List<Double>,
    precipitation: List<Int>,
    timezone: String,
    tempUnit: TemperatureUnit,
    is24: Boolean,
) {
    if (temperatures.isEmpty()) return
    val line = MetroTheme.colors.primaryText
    val bar = MetroTheme.colors.secondaryText.copy(alpha = 0.5f)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
    ) {
        val count = temperatures.size
        val step = size.width / (count - 1).coerceAtLeast(1)
        val minT = temperatures.min()
        val maxT = temperatures.max()
        val span = (maxT - minT).takeIf { it > 0.0 } ?: 1.0
        val topPad = size.height * 0.15f
        val chartH = size.height * 0.7f
        fun x(i: Int) = step * i
        fun y(t: Double) = topPad + chartH * (1f - ((t - minT) / span).toFloat())

        // precipitation bars at the baseline
        val baseline = size.height * 0.92f
        precipitation.forEachIndexed { i, p ->
            if (p > 0) {
                val h = chartH * (p / 100f) * 0.5f
                drawLine(bar, Offset(x(i), baseline), Offset(x(i), baseline - h), strokeWidth = 4f, cap = StrokeCap.Butt)
            }
        }
        // temperature polyline
        for (i in 0 until count - 1) {
            drawLine(line, Offset(x(i), y(temperatures[i])), Offset(x(i + 1), y(temperatures[i + 1])), strokeWidth = 3f, cap = StrokeCap.Round)
        }
        temperatures.forEachIndexed { i, t ->
            drawCircle(line, radius = 4f, center = Offset(x(i), y(t)))
        }
    }
}

private fun temp(value: Double?, units: WeatherUnits): String? =
    TemperatureUnit.CELSIUS.convert(value, units.tempUnit)?.roundToInt()?.let { "$it°" }

private fun formatTime(millis: Long?, timezone: String, is24: Boolean): String? {
    if (millis == null) return null
    return if (is24) to24HourTimeString(millis, timezone) else to12HourTimeString(millis, timezone)
}

private fun Long.dayOfMonth(timezone: String): Int {
    val zone = runCatching { java.time.ZoneId.of(timezone) }.getOrElse { java.time.ZoneId.systemDefault() }
    return java.time.Instant.ofEpochMilli(this).atZone(zone).dayOfMonth
}

private fun sameLocalDay(a: Long, b: Long, timezone: String): Boolean {
    val zone = runCatching { java.time.ZoneId.of(timezone) }.getOrElse { java.time.ZoneId.systemDefault() }
    return java.time.Instant.ofEpochMilli(a).atZone(zone).toLocalDate() ==
        java.time.Instant.ofEpochMilli(b).atZone(zone).toLocalDate()
}
