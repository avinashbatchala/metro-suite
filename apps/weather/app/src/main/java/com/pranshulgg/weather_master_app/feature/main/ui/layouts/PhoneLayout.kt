package com.pranshulgg.weather_master_app.feature.main.ui.layouts

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pranshulgg.weather_master_app.R
import com.pranshulgg.weather_master_app.core.model.domain.alerts.Alert
import com.pranshulgg.weather_master_app.core.model.domain.weather.Weather
import com.pranshulgg.weather_master_app.core.model.domain.weather.WeatherUnits
import com.pranshulgg.weather_master_app.core.model.weather.TemperatureUnit
import com.pranshulgg.weather_master_app.core.model.weather.toIcon
import com.pranshulgg.weather_master_app.core.model.weather.toLabel
import com.pranshulgg.weather_master_app.core.prefs.AppPrefsState
import com.pranshulgg.weather_master_app.core.ui.components.Symbol
import com.pranshulgg.weather_master_app.core.ui.components.WeatherIconBox
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroDimens
import com.metro.ui.MetroEmptyState
import com.metro.ui.MetroListItem
import com.metro.ui.MetroPivot
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.pranshulgg.weather_master_app.core.ui.navigation.NavRoutes
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
    navController: NavController,
    alerts: List<Alert>,
    prefs: AppPrefsState,
    onLocationSelect: (com.pranshulgg.weather_master_app.core.model.domain.location.Location) -> Unit = {},
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
                    else -> PlacesPage(weather, units, navController, onLocationSelect)
                }
            }
        )
    }
}

@Composable
private fun NowPage(
    weather: Weather,
    units: WeatherUnits,
    context: Context,
    alerts: List<Alert>,
    is24: Boolean
) {
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = MetroDimens.ScreenHorizontalMargin)
    ) {
        WeatherHero(weather, units, context)

        if (alerts.isNotEmpty()) {
            MetroText(
                text = "alerts".uppercase(),
                style = MetroTextStyle.SectionHeader,
                color = MetroTheme.colors.accent
            )
            alerts.take(2).forEach { alert ->
                MetroListItem(title = alert.event ?: "Weather alert", subtitle = alert.description)
            }
        }

        val current = weather.current
        val today = weather.daily.getOrNull(0)

        val windText = buildString {
            val speed = current.windSpeed?.roundToInt()
            if (speed != null) append("$speed ${units.windUnit.name.lowercase()}")
            if (current.windDirection != null) append(" ${current.windDirection.name}")
        }.ifBlank { "--" }

        MetroText(
            text = "details".uppercase(),
            style = MetroTextStyle.SectionHeader,
            color = MetroTheme.colors.accent
        )
        DetailGrid(
            entries = listOf(
                Triple(R.drawable.air_24px, "Wind", windText),
                Triple(R.drawable.humidity_percentage_24px, "Humidity", current.humidity?.let { "${it.roundToInt()}%" } ?: "--"),
                Triple(R.drawable.compress_24px, "Pressure", current.pressureMsl?.let { "${it.roundToInt()} hPa" } ?: "--"),
                Triple(R.drawable.wb_sunny_24px, "UV index", current.uvIndex?.let { "${it.roundToInt()} ${uvLabel(it)}" } ?: "--"),
                Triple(R.drawable.visibility_24px, "Visibility", current.visibility?.let { v -> "${(v / 1000.0).let { km -> (km * 10).roundToInt() / 10.0 }} km" } ?: "--"),
                Triple(R.drawable.water_drop_24px, "Precipitation", today?.precipitationProbabilityMax?.let { "$it%" } ?: "--"),
                Triple(R.drawable.wb_sunny_24px, "Sunrise", formatTime(today?.sunrise, weather.location.timezone, is24)),
                Triple(R.drawable.bedtime_24px, "Sunset", formatTime(today?.sunset, weather.location.timezone, is24))
            )
        )

        Spacer(modifier = Modifier.height(12.dp))
        MetroText(
            text = getFullLocationName(weather.location),
            style = MetroTextStyle.Body,
            color = MetroTheme.colors.secondaryText
        )
        MetroText(
            text = "Updated ${getLastUpdatedTimeString(context, current.lastUpdatedInMilli)} · Open-Meteo",
            style = MetroTextStyle.Body,
            color = MetroTheme.colors.secondaryText,
            modifier = Modifier.padding(bottom = 24.dp)
        )
    }
}

@Composable
private fun WeatherHero(weather: Weather, units: WeatherUnits, context: Context) {
    val current = weather.current
    val today = weather.daily.getOrNull(0)
    val temp = TemperatureUnit.CELSIUS.convert(current.temperature, units.tempUnit)?.roundToInt()
    val feels = TemperatureUnit.CELSIUS.convert(current.feelsLike, units.tempUnit)?.roundToInt()
    val high = TemperatureUnit.CELSIUS.convert(today?.temperatureMax, units.tempUnit)?.roundToInt()
    val low = TemperatureUnit.CELSIUS.convert(today?.temperatureMin, units.tempUnit)?.roundToInt()
    val icon = current.weatherCondition.toIcon(
        daily = today,
        targetTimeMilli = getCurrentTimeFor(weather.location.timezone)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 18.dp),
        horizontalAlignment = Alignment.Start
    ) {
        WeatherIconBox(icon, size = 68.dp)
        Spacer(modifier = Modifier.height(8.dp))
        MetroText(
            text = "${temp ?: "-"}°",
            style = MetroTextStyle.PageTitle,
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
private fun DetailGrid(entries: List<Triple<Int, String, String>>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        entries.chunked(2).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (icon, label, value) ->
                    DetailTile(icon, label, value, Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DetailTile(icon: Int, label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MetroTheme.colors.secondarySurface, RectangleShape)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Symbol(icon, color = MetroTheme.colors.secondaryText, size = 15.dp)
            Spacer(modifier = Modifier.width(6.dp))
            MetroText(
                text = label,
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.secondaryText,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        MetroText(
            text = value,
            style = MetroTextStyle.ListItemTitle,
            color = MetroTheme.colors.primaryText,
            maxLines = 1
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

    val foreground = MetroTheme.colors.primaryText
    val accent = MetroTheme.colors.accent
    val columnWidth = 66.dp
    val temps = hours.map { TemperatureUnit.CELSIUS.convert(it.temperature, units.tempUnit)?.roundToInt() }
    val pops = hours.map { it.precipitationProbability }
    val valid = temps.filterNotNull()
    val lo = valid.minOrNull() ?: 0
    val hi = valid.maxOrNull() ?: 1
    val span = (hi - lo).takeIf { it > 0 } ?: 1
    val totalWidth = columnWidth * hours.size

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .horizontalScroll(rememberScrollState())
        ) {
            Row(modifier = Modifier.width(totalWidth)) {
                hours.forEachIndexed { index, item ->
                    Box(modifier = Modifier.width(columnWidth), contentAlignment = Alignment.Center) {
                        MetroText(
                            text = if (index == 0) "Now" else hourLabel(item.time, weather.location.timezone, prefs.is24HrTimeFormat),
                            style = MetroTextStyle.ListItemSubtitle,
                            color = MetroTheme.colors.secondaryText,
                            maxLines = 1
                        )
                    }
                }
            }

            Row(modifier = Modifier.width(totalWidth).padding(top = 16.dp)) {
                hours.forEach { item ->
                    Box(modifier = Modifier.width(columnWidth).height(36.dp), contentAlignment = Alignment.Center) {
                        WeatherIconBox(item.weatherCondition.toIcon(targetTimeMilli = item.time), size = 30.dp)
                    }
                }
            }

            Box(modifier = Modifier.width(totalWidth).height(150.dp).padding(top = 12.dp)) {
                HourlyCurve(temps = temps, min = lo, span = span, color = accent, modifier = Modifier.fillMaxSize())
            }

            Row(modifier = Modifier.width(totalWidth).padding(top = 10.dp)) {
                temps.forEach { t ->
                    Box(modifier = Modifier.width(columnWidth), contentAlignment = Alignment.Center) {
                        MetroText(
                            text = "${t ?: "-"}°",
                            style = MetroTextStyle.ListItemTitle,
                            color = foreground
                        )
                    }
                }
            }

            Row(modifier = Modifier.width(totalWidth).padding(top = 4.dp)) {
                pops.forEach { pop ->
                    Box(modifier = Modifier.width(columnWidth), contentAlignment = Alignment.Center) {
                        MetroText(
                            text = if (pop != null && pop > 0) "$pop%" else "",
                            style = MetroTextStyle.ListItemSubtitle,
                            color = accent,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HourlyCurve(temps: List<Int?>, min: Int, span: Int, color: Color, modifier: Modifier = Modifier) {
    val dotColor = MetroTheme.colors.primaryText
    Canvas(modifier = modifier) {
        if (temps.size < 2) return@Canvas
        val stepX = size.width / temps.size
        val padTop = 20f
        val padBottom = 20f
        val usable = (size.height - padTop - padBottom).coerceAtLeast(1f)
        val points = temps.mapIndexed { index, value ->
            val fraction = if (value == null) 0.5f else (value - min).toFloat() / span
            Offset(
                x = stepX * (index + 0.5f),
                y = padTop + (1f - fraction.coerceIn(0f, 1f)) * usable
            )
        }

        val area = Path().apply {
            moveTo(points.first().x, size.height)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(points.last().x, size.height)
            close()
        }
        drawPath(
            path = area,
            brush = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.35f), Color.Transparent),
                startY = 0f,
                endY = size.height
            )
        )

        val line = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(
            path = line,
            color = color,
            style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        points.forEach { point ->
            drawCircle(color = dotColor, radius = 5.5f, center = point)
            drawCircle(color = color, radius = 3.5f, center = point)
        }
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
    val foreground = MetroTheme.colors.primaryText
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
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WeatherIconBox(
                        day.weatherCondition.toIcon(targetTimeMilli = day.time),
                        size = 32.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        MetroText(
                            text = if (index == 0) "Today" else toWeekdayString(day.time, weather.location.timezone),
                            style = MetroTextStyle.ListItemTitle,
                            color = foreground
                        )
                        MetroText(
                            text = buildString {
                                append(day.weatherCondition.toLabel(context))
                                val pop = day.precipitationProbabilityMax
                                if (pop != null && pop > 0) append(" · $pop%")
                            },
                            style = MetroTextStyle.ListItemSubtitle,
                            color = MetroTheme.colors.secondaryText
                        )
                    }
                    MetroText(
                        text = "$max° / $min°",
                        style = MetroTextStyle.ListItemTitle,
                        color = foreground
                    )
                }
                RangeBar(
                    startFraction = (((day.temperatureMin ?: lo) - lo) / span).toFloat().coerceIn(0f, 1f),
                    endFraction = (((day.temperatureMax ?: lo) - lo) / span).toFloat().coerceIn(0f, 1f),
                    foreground = foreground,
                    modifier = Modifier.padding(top = 6.dp, start = 44.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun RangeBar(startFraction: Float, endFraction: Float, foreground: Color, modifier: Modifier = Modifier) {
    val start = startFraction.coerceIn(0f, 1f)
    val end = endFraction.coerceIn(start, 1f)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(foreground.copy(alpha = 0.25f), RectangleShape)
    ) {
        if (start > 0.001f) {
            Spacer(modifier = Modifier.weight(start).fillMaxHeight())
        }
        Box(
            modifier = Modifier
                .weight((end - start).coerceAtLeast(0.05f))
                .fillMaxHeight()
                .background(foreground, RectangleShape)
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
    navController: NavController,
    onLocationSelect: (com.pranshulgg.weather_master_app.core.model.domain.location.Location) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    com.pranshulgg.weather_master_app.feature.locations.ui.PlacesPivotContent(
        onLocationSelect = onLocationSelect,
        onAddPlace = { navController.navigate(NavRoutes.SEARCH) },
        onEdit = { navController.navigate(NavRoutes.EDIT_LOCATION) },
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
