package com.pranshulgg.weather_master_app.feature.main.ui.layouts

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.pranshulgg.weather_master_app.R
import com.pranshulgg.weather_master_app.core.model.domain.alerts.Alert
import com.pranshulgg.weather_master_app.core.model.domain.weather.Weather
import com.pranshulgg.weather_master_app.core.model.domain.weather.WeatherDaily
import com.pranshulgg.weather_master_app.core.model.domain.weather.WeatherUnits
import com.pranshulgg.weather_master_app.core.model.weather.TemperatureUnit
import com.pranshulgg.weather_master_app.core.model.weather.toLabel
import com.pranshulgg.weather_master_app.core.prefs.AppPrefsState
import com.pranshulgg.weather_master_app.core.ui.components.Symbol
import com.pranshulgg.weather_master_app.core.ui.components.WeatherGlyph
import com.pranshulgg.weather_master_app.core.ui.components.toGlyphKind
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroDimens
import com.metro.ui.MetroEmptyState
import com.metro.ui.MetroListItem
import com.metro.ui.MetroPivot
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.pranshulgg.weather_master_app.core.utils.formatters.getCurrentTimeFor
import com.pranshulgg.weather_master_app.core.utils.formatters.getLastUpdatedTimeString
import com.pranshulgg.weather_master_app.core.utils.formatters.to12HourTimeString
import com.pranshulgg.weather_master_app.core.utils.formatters.to24HourTimeString
import com.pranshulgg.weather_master_app.core.utils.formatters.toWeekdayString
import com.pranshulgg.weather_master_app.core.utils.weather.forecast.findMatchingHourly
import com.pranshulgg.weather_master_app.core.utils.weather.location.getFullLocationName
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * Windows 10 Mobile weather home: a `now · hourly · daily · places` pivot over a flat
 * black background. Content mirrors the wphone MSN Weather app, restyled to the Metro
 * language shared with the launcher.
 */
@Composable
fun PhoneLayout(
    weather: Weather,
    units: WeatherUnits,
    context: Context,
    alerts: List<Alert>,
    prefs: AppPrefsState,
    onLocationSelect: (com.pranshulgg.weather_master_app.core.model.domain.location.Location) -> Unit = {},
    onAddPlace: () -> Unit = {},
    onEditLocation: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var currentPage by remember { mutableIntStateOf(0) }
    val titles = listOf("now", "hourly", "daily", "places")
    val pagerState = rememberPagerState(pageCount = { titles.size })
    val scope = rememberCoroutineScope()

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { currentPage = it }
    }

    Column(modifier = modifier.fillMaxSize()) {
        MetroPivot(
            titles = titles,
            pagerState = pagerState,
            modifier = Modifier.weight(1f),
            header = { MetroAppTitle(title = "weather") },
            onTitleClick = { scope.launch { pagerState.animateScrollToPage(it) } },
            pageContent = { page ->
                when (page) {
                    0 -> NowPage(weather, units, context, alerts, prefs.is24HrTimeFormat)
                    1 -> HourlyPage(weather, units, context, prefs)
                    2 -> DailyPage(weather, units, context, prefs)
                    else -> PlacesPage(weather, units, onAddPlace, onEditLocation, onLocationSelect)
                }
            }
        )
    }
}

private data class Fact(val icon: Int, val label: String, val value: String)

@Composable
private fun NowPage(
    weather: Weather,
    units: WeatherUnits,
    context: Context,
    alerts: List<Alert>,
    is24: Boolean
) {
    val scroll = rememberScrollState()
    val current = weather.current
    val today = weather.daily.getOrNull(0)

    val windText = buildString {
        val speed = current.windSpeed?.roundToInt()
        if (speed != null) append("$speed ${units.windUnit.name.lowercase()}")
        if (current.windDirection != null) append(" ${current.windDirection.name}")
    }.ifBlank { "--" }

    val facts = listOf(
        Fact(R.drawable.air_24px, "Wind", windText),
        Fact(R.drawable.humidity_percentage_24px, "Humidity", current.humidity?.let { "${it.roundToInt()}%" } ?: "--"),
        Fact(R.drawable.compress_24px, "Pressure", current.pressureMsl?.let { "${it.roundToInt()} hPa" } ?: "--"),
        Fact(R.drawable.wb_sunny_24px, "UV index", current.uvIndex?.let { "${it.roundToInt()} ${uvLabel(it)}" } ?: "--"),
        Fact(R.drawable.visibility_24px, "Visibility", current.visibility?.let { v -> "${(v / 1000.0).let { km -> (km * 10).roundToInt() / 10.0 }} km" } ?: "--"),
        Fact(R.drawable.water_drop_24px, "Precipitation", today?.precipitationProbabilityMax?.let { "$it%" } ?: "--"),
        Fact(R.drawable.wb_sunny_24px, "Sunrise", formatTime(today?.sunrise, weather.location.timezone, is24)),
        Fact(R.drawable.bedtime_24px, "Sunset", formatTime(today?.sunset, weather.location.timezone, is24))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
    ) {
        WeatherHero(weather, units, context, today)

        if (alerts.isNotEmpty()) {
            SectionLabel("alerts")
            alerts.take(2).forEach { alert ->
                MetroListItem(title = alert.event ?: "Weather alert", subtitle = alert.description)
            }
        }

        SectionLabel("details")
        facts.forEach { fact ->
            MetroListItem(
                title = fact.value,
                subtitle = fact.label,
                leading = {
                    Symbol(fact.icon, color = MetroTheme.colors.secondaryText, size = 20.dp)
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        MetroText(
            text = getFullLocationName(weather.location),
            style = MetroTextStyle.Body,
            color = MetroTheme.colors.secondaryText,
            modifier = Modifier.padding(start = MetroDimens.ScreenHorizontalMargin)
        )
        MetroText(
            text = "Updated ${getLastUpdatedTimeString(context, current.lastUpdatedInMilli)} · Open-Meteo",
            style = MetroTextStyle.Body,
            color = MetroTheme.colors.secondaryText,
            modifier = Modifier.padding(
                start = MetroDimens.ScreenHorizontalMargin,
                bottom = 24.dp
            )
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    MetroText(
        text = text.uppercase(),
        style = MetroTextStyle.SectionHeader,
        color = MetroTheme.colors.secondaryText,
        modifier = Modifier.padding(
            start = MetroDimens.ScreenHorizontalMargin,
            top = 16.dp,
            bottom = 8.dp
        )
    )
}

@Composable
private fun WeatherHero(
    weather: Weather,
    units: WeatherUnits,
    context: Context,
    today: WeatherDaily?
) {
    val current = weather.current
    val temp = TemperatureUnit.CELSIUS.convert(current.temperature, units.tempUnit)?.roundToInt()
    val feels = TemperatureUnit.CELSIUS.convert(current.feelsLike, units.tempUnit)?.roundToInt()
    val high = TemperatureUnit.CELSIUS.convert(today?.temperatureMax, units.tempUnit)?.roundToInt()
    val low = TemperatureUnit.CELSIUS.convert(today?.temperatureMin, units.tempUnit)?.roundToInt()
    val kind = current.weatherCondition.toGlyphKind(
        daily = today,
        targetTimeMilli = getCurrentTimeFor(weather.location.timezone)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MetroDimens.ScreenHorizontalMargin)
            .padding(top = 20.dp, bottom = 18.dp),
        horizontalAlignment = Alignment.Start
    ) {
        WeatherGlyph(kind, size = 68.dp, color = MetroTheme.colors.primaryText)
        Spacer(modifier = Modifier.height(8.dp))
        MetroText(
            text = "${temp ?: "-"}°",
            style = MetroTextStyle.HubTitle,
            color = MetroTheme.colors.primaryText
        )
        MetroText(
            text = current.weatherCondition.toLabel(context),
            style = MetroTextStyle.ListItemTitle,
            color = MetroTheme.colors.primaryText
        )
        Spacer(modifier = Modifier.height(8.dp))
        MetroText(
            text = "High ${high ?: "-"}°   Low ${low ?: "-"}°   Feels like ${feels ?: "-"}°",
            style = MetroTextStyle.Body,
            color = MetroTheme.colors.secondaryText
        )
    }
}

@Composable
private fun HourlyPage(weather: Weather, units: WeatherUnits, context: Context, prefs: AppPrefsState) {
    val hours = findMatchingHourly(
        weather.hourly,
        System.currentTimeMillis(),
        weather.location.source,
        weather.location.timezone,
        alwaysReturn24Hrs = true,
        keepPastHour = false
    ).take(24)
    if (hours.isEmpty()) {
        MetroEmptyState("no hourly data", modifier = Modifier.padding(MetroDimens.ScreenHorizontalMargin))
        return
    }

    val today = weather.daily.getOrNull(0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MetroDimens.ScreenHorizontalMargin),
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        hours.forEachIndexed { index, item ->
            val temp = TemperatureUnit.CELSIUS.convert(item.temperature, units.tempUnit)?.roundToInt()
            val kind = item.weatherCondition.toGlyphKind(daily = today, targetTimeMilli = item.time)
            val timeLabel = if (index == 0) "now" else hourLabel(item.time, weather.location.timezone, prefs.is24HrTimeFormat)
            val precip = item.precipitationProbability
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MetroText(
                    text = timeLabel,
                    style = MetroTextStyle.ListItemTitle,
                    color = MetroTheme.colors.primaryText,
                    modifier = Modifier.width(72.dp),
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.weight(1f))
                if (precip != null && precip > 0) {
                    MetroText(
                        text = "$precip%",
                        style = MetroTextStyle.ListItemSubtitle,
                        color = MetroTheme.colors.secondaryText,
                        maxLines = 1,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                MetroText(
                    text = "${temp ?: "-"}°",
                    style = MetroTextStyle.ListItemTitle,
                    color = MetroTheme.colors.primaryText,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.width(12.dp))
                WeatherGlyph(kind, size = 28.dp, color = MetroTheme.colors.primaryText)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun DailyPage(
    weather: Weather,
    units: WeatherUnits,
    context: Context,
    prefs: AppPrefsState
) {
    val daily = weather.daily
    if (daily.isEmpty()) {
        MetroEmptyState("no daily data", modifier = Modifier.padding(MetroDimens.ScreenHorizontalMargin))
        return
    }
    val scroll = rememberScrollState()
    val lo = daily.mapNotNull { it.temperatureMin }.minOrNull() ?: 0.0
    val hi = daily.mapNotNull { it.temperatureMax }.maxOrNull() ?: 1.0
    val span = (hi - lo).takeIf { it > 0.0 } ?: 1.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = MetroDimens.ScreenHorizontalMargin)
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        daily.forEachIndexed { index, day ->
            val max = TemperatureUnit.CELSIUS.convert(day.temperatureMax, units.tempUnit)?.roundToInt()
            val min = TemperatureUnit.CELSIUS.convert(day.temperatureMin, units.tempUnit)?.roundToInt()
            val kind = day.weatherCondition.toGlyphKind(daily = day, targetTimeMilli = day.time)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MetroText(
                    text = if (index == 0) "Today" else toWeekdayString(day.time, weather.location.timezone),
                    style = MetroTextStyle.ListItemTitle,
                    color = MetroTheme.colors.primaryText,
                    modifier = Modifier.width(88.dp),
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.weight(1f))
                MetroText(
                    text = "${max ?: "-"}° / ${min ?: "-"}°",
                    style = MetroTextStyle.ListItemTitle,
                    color = MetroTheme.colors.primaryText,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.width(12.dp))
                WeatherGlyph(kind, size = 28.dp, color = MetroTheme.colors.primaryText)
            }
            RangeBar(
                startFraction = (((day.temperatureMin ?: lo) - lo) / span).toFloat().coerceIn(0f, 1f),
                endFraction = (((day.temperatureMax ?: lo) - lo) / span).toFloat().coerceIn(0f, 1f),
                modifier = Modifier.padding(top = 6.dp),
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun RangeBar(startFraction: Float, endFraction: Float, modifier: Modifier = Modifier) {
    val start = startFraction.coerceIn(0f, 1f)
    val end = endFraction.coerceIn(start, 1f)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(MetroTheme.colors.secondarySurface, RectangleShape)
    ) {
        if (start > 0.001f) {
            Spacer(modifier = Modifier.weight(start).fillMaxHeight())
        }
        Box(
            modifier = Modifier
                .weight((end - start).coerceAtLeast(0.05f))
                .fillMaxHeight()
                .background(MetroTheme.colors.accent, RectangleShape)
        )
        val tail = (1f - end).coerceAtLeast(0f)
        if (tail > 0.001f) {
            Spacer(modifier = Modifier.weight(tail).fillMaxHeight())
        }
    }
}

@Composable
private fun PlacesPage(
    weather: Weather,
    units: WeatherUnits,
    onAddPlace: () -> Unit,
    onEditLocation: () -> Unit,
    onLocationSelect: (com.pranshulgg.weather_master_app.core.model.domain.location.Location) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    com.pranshulgg.weather_master_app.feature.locations.ui.PlacesPivotContent(
        onLocationSelect = onLocationSelect,
        onAddPlace = onAddPlace,
        onEdit = onEditLocation,
        onPin = {
            com.pranshulgg.weather_master_app.synergy.WeatherTilePin.pin(
                context = context,
                location = weather.location,
                size = "4x2",
            )
        },
        modifier = Modifier.fillMaxSize()
    )
}

private fun hourLabel(millis: Long, timezone: String, is24: Boolean): String =
    if (is24) to24HourTimeString(millis, timezone) else to12HourTimeString(millis, timezone)

private fun formatTime(millis: Long?, timezone: String, is24: Boolean): String {
    if (millis == null) return "--"
    return if (is24) to24HourTimeString(millis, timezone) else to12HourTimeString(millis, timezone)
}

private fun uvLabel(v: Double?): String = when {
    v == null -> ""
    v < 3 -> "low"
    v < 6 -> "moderate"
    v < 8 -> "high"
    v < 11 -> "very high"
    else -> "extreme"
}
