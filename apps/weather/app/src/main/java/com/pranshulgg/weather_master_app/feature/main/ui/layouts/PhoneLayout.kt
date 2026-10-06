package com.pranshulgg.weather_master_app.feature.main.ui.layouts

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroColors
import com.metro.ui.MetroDimens
import com.metro.ui.MetroPanorama
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroClickable
import com.metro.ui.metroNavBarPadding
import com.pranshulgg.weather_master_app.R
import com.pranshulgg.weather_master_app.core.model.domain.alerts.Alert
import com.pranshulgg.weather_master_app.core.model.domain.location.Location
import com.pranshulgg.weather_master_app.core.model.domain.weather.Weather
import com.pranshulgg.weather_master_app.core.model.domain.weather.WeatherDaily
import com.pranshulgg.weather_master_app.core.model.domain.weather.WeatherUnits
import com.pranshulgg.weather_master_app.core.model.weather.TemperatureUnit
import com.pranshulgg.weather_master_app.core.model.weather.toLabel
import com.pranshulgg.weather_master_app.core.ui.components.WeatherGlyph
import com.pranshulgg.weather_master_app.core.ui.components.toGlyphKind
import com.pranshulgg.weather_master_app.core.utils.formatters.getCurrentTimeFor
import com.pranshulgg.weather_master_app.core.utils.formatters.getLastUpdatedTimeString
import com.pranshulgg.weather_master_app.core.utils.formatters.to12HourTimeString
import com.pranshulgg.weather_master_app.core.utils.formatters.to24HourTimeString
import com.pranshulgg.weather_master_app.core.utils.formatters.toWeekdayString
import com.pranshulgg.weather_master_app.core.utils.weather.location.getFullLocationName
import com.pranshulgg.weather_master_app.feature.main.data.toHourlyForecasts
import com.pranshulgg.weather_master_app.feature.main.ui.WeatherPalette
import com.pranshulgg.weather_master_app.feature.main.ui.layouts.hourly.HourlyPane
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * WP8.1 Bing Weather home: a `today · daily · hourly · maps · favourites` panorama over a flat
 * deep Bing-blue surface. The active location reads as an uppercase overline above the pane
 * headings. 12/24-hour follows the system clock.
 */
@Composable
fun PhoneLayout(
    weather: Weather,
    units: WeatherUnits,
    context: Context,
    alerts: List<Alert>,
    locations: List<Location>,
    onLocationSelect: (Location) -> Unit,
    onAddFavourite: () -> Unit,
    onRemoveLocation: (Location) -> Unit,
    onSetHome: (Location) -> Unit,
    onPinLocation: (Location) -> Unit,
    onOpenDailyDetail: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val titles = listOf(
        stringResource(R.string.pane_today),
        stringResource(R.string.pane_daily),
        stringResource(R.string.pane_hourly),
        stringResource(R.string.pane_maps),
        stringResource(R.string.pane_favourites),
    )
    val pagerState = rememberPagerState(pageCount = { titles.size })
    val scope = rememberCoroutineScope()

    val configuration = LocalConfiguration.current
    val is24 = remember(context, configuration) { DateFormat.is24HourFormat(context) }

    val todayScroll = rememberScrollState()
    val hourlyScroll = rememberScrollState()
    val dailyScroll = rememberScrollState()
    val mapsScroll = rememberScrollState()
    val favouritesScroll = rememberScrollState()

    var menuLocation by remember { mutableStateOf<Location?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        WeatherPanoramaHeader(locationName = getFullLocationName(weather.location))

        Box(modifier = Modifier.fillMaxSize()) {
            MetroPanorama(
                titles = titles,
                pagerState = pagerState,
                modifier = Modifier.fillMaxSize().padding(bottom = 56.dp),
                onTitleClick = { scope.launch { pagerState.animateScrollToPage(it) } },
                pageContent = { page ->
                    when (page) {
                        0 -> TodayPane(
                            weather = weather,
                            units = units,
                            context = context,
                            alerts = alerts,
                            is24 = is24,
                            scrollState = todayScroll,
                        )
                        1 -> DailyPane(
                            weather = weather,
                            units = units,
                            scrollState = dailyScroll,
                            onOpenDetail = onOpenDailyDetail,
                        )
                        2 -> HourlyPane(
                            forecasts = weather.toHourlyForecasts(),
                            timezone = weather.location.timezone,
                            is24Hour = is24,
                            temperatureUnit = units.tempUnit,
                            today = weather.daily.getOrNull(0),
                            context = context,
                            scrollState = hourlyScroll,
                        )
                        3 -> MapsPane(scrollState = mapsScroll)
                        else -> FavouritesPane(
                            locations = locations,
                            activeId = weather.location.id,
                            onSelect = onLocationSelect,
                            onLongPress = { menuLocation = it },
                            onAdd = onAddFavourite,
                            scrollState = favouritesScroll,
                        )
                    }
                },
            )

            FavouriteContextMenu(
                location = menuLocation,
                units = units,
                onDismiss = { menuLocation = null },
                onSelect = { menuLocation = null; onLocationSelect(it) },
                onRemove = { menuLocation = null; onRemoveLocation(it) },
                onSetHome = { menuLocation = null; onSetHome(it) },
                onPin = { menuLocation = null; onPinLocation(it) },
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Today
// ---------------------------------------------------------------------------------------------

private data class Fact(val label: String, val value: String?)

@Composable
private fun TodayPane(
    weather: Weather,
    units: WeatherUnits,
    context: Context,
    alerts: List<Alert>,
    is24: Boolean,
    scrollState: androidx.compose.foundation.ScrollState,
) {
    val current = weather.current
    val today = weather.daily.getOrNull(0)
    val tonight = tonightCondition(weather)

    val windText = buildString {
        units.windUnit.let { }
        val speed = current.windSpeed?.roundToInt()
        if (speed != null) {
            append("$speed ${units.windUnit.name.lowercase()}")
            current.windDirection?.let { append(" ${it.name}") }
        }
    }.ifBlank { null }

    val facts = listOf(
        Fact(stringResource(R.string.fact_feels_like), tempLabel(current.feelsLike, units)),
        Fact(stringResource(R.string.fact_humidity), current.humidity?.let { "${it.roundToInt()}%" }),
        Fact(stringResource(R.string.fact_visibility), visibilityLabel(current.visibility, units)),
        Fact(stringResource(R.string.fact_pressure), pressureLabel(current.pressureMsl, units)),
        Fact(stringResource(R.string.fact_wind), windText),
        Fact(stringResource(R.string.fact_uv), current.uvIndex?.let { uvLabel(it) }),
        Fact(stringResource(R.string.fact_precipitation), today?.precipitationProbabilityMax?.let { "$it%" }),
    ).filter { !it.value.isNullOrBlank() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
    ) {
        TodayHero(weather, units, context, today)

        if (alerts.isNotEmpty()) {
            SectionLabel(stringResource(R.string.section_alerts))
            alerts.take(2).forEach { alert ->
                MetroText(
                    text = alert.event ?: stringResource(R.string.weather_alert),
                    style = MetroTextStyle.ListItemTitle,
                    color = MetroTheme.colors.primaryText,
                    modifier = Modifier.padding(
                        horizontal = MetroDimens.ScreenHorizontalMargin,
                        vertical = 4.dp,
                    ),
                )
            }
        }

        TodayTonightBlock(weather, units, context, today, tonight)

        if (facts.isNotEmpty()) {
            MetricGrid(facts)
        }

        MetroText(
            text = stringResource(
                R.string.updated_attribution,
                getLastUpdatedTimeString(context, current.lastUpdatedInMilli),
                weather.location.source.displayName,
            ),
            style = MetroTextStyle.Body,
            color = MetroTheme.colors.secondaryText,
            modifier = Modifier.padding(
                start = MetroDimens.ScreenHorizontalMargin,
                top = 12.dp,
                bottom = 24.dp,
            ),
        )
    }
}

@Composable
private fun TodayHero(
    weather: Weather,
    units: WeatherUnits,
    context: Context,
    today: WeatherDaily?,
) {
    val current = weather.current
    val temp = TemperatureUnit.CELSIUS.convert(current.temperature, units.tempUnit)?.roundToInt()
    val kind = current.weatherCondition.toGlyphKind(
        daily = today,
        targetTimeMilli = getCurrentTimeFor(weather.location.timezone),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MetroDimens.ScreenHorizontalMargin)
            .padding(top = 16.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MetroText(
                text = "${temp ?: "-"}°",
                style = MetroTextStyle.PageTitle,
                color = MetroTheme.colors.primaryText,
            )
            Spacer(modifier = Modifier.width(12.dp))
            WeatherGlyph(kind, size = 44.dp, color = MetroTheme.colors.primaryText)
        }
        MetroText(
            text = current.weatherCondition.toLabel(context),
            style = MetroTextStyle.ListItemTitle,
            color = MetroTheme.colors.primaryText,
        )
    }
}

@Composable
private fun TodayTonightBlock(
    weather: Weather,
    units: WeatherUnits,
    context: Context,
    today: WeatherDaily?,
    tonight: WeatherDaily?,
) {
    val todayHigh = tempLabel(today?.temperatureMax, units)
    val tonightLow = tempLabel(tonight?.temperatureMin ?: today?.temperatureMin, units)
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = MetroDimens.ScreenHorizontalMargin)) {
        Separator()
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                MetroText(stringResource(R.string.today_label), style = MetroTextStyle.ListItemTitle)
                MetroText(
                    text = "${todayHigh ?: "-"}  ${today?.weatherCondition?.toLabel(context).orEmpty()}",
                    style = MetroTextStyle.ListItemSubtitle,
                    color = MetroTheme.colors.secondaryText,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                MetroText(stringResource(R.string.tonight_label), style = MetroTextStyle.ListItemTitle)
                MetroText(
                    text = "${tonightLow ?: "-"}  ${tonight?.weatherCondition?.toLabel(context).orEmpty()}",
                    style = MetroTextStyle.ListItemSubtitle,
                    color = MetroTheme.colors.secondaryText,
                )
            }
        }
        Separator()
    }
}

/** Compact two-column metric grid (not a vertical list of rows). */
@Composable
private fun MetricGrid(facts: List<Fact>) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = MetroDimens.ScreenHorizontalMargin, vertical = 8.dp)) {
        facts.chunked(2).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                row.forEach { fact ->
                    Column(modifier = Modifier.weight(1f)) {
                        MetroText(
                            text = fact.label,
                            style = MetroTextStyle.ListItemSubtitle,
                            color = MetroTheme.colors.secondaryText,
                        )
                        MetroText(
                            text = fact.value.orEmpty(),
                            style = MetroTextStyle.ListItemTitle,
                            color = MetroTheme.colors.primaryText,
                        )
                    }
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Daily
// ---------------------------------------------------------------------------------------------

@Composable
private fun DailyPane(
    weather: Weather,
    units: WeatherUnits,
    scrollState: androidx.compose.foundation.ScrollState,
    onOpenDetail: (Int) -> Unit,
) {
    val daily = weather.daily
    if (daily.isEmpty()) {
        WeatherEmptyText(stringResource(R.string.no_daily_data))
        return
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 8.dp),
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        daily.forEachIndexed { index, day ->
            DailyRow(
                weather = weather,
                units = units,
                day = day,
                index = index,
                onClick = { onOpenDetail(index) },
            )
            Spacer(modifier = Modifier.height(2.dp))
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun DailyRow(
    weather: Weather,
    units: WeatherUnits,
    day: WeatherDaily,
    index: Int,
    onClick: () -> Unit,
) {
    val high = TemperatureUnit.CELSIUS.convert(day.temperatureMax, units.tempUnit)?.roundToInt()
    val low = TemperatureUnit.CELSIUS.convert(day.temperatureMin, units.tempUnit)?.roundToInt()
    val kind = day.weatherCondition.toGlyphKind(daily = day, targetTimeMilli = day.time)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(WeatherPalette.BingBlueRow, RectangleShape)
            .metroClickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.width(76.dp)) {
            MetroText(
                text = "${day.time.dayOfMonthLabel(weather.location.timezone)} ${toWeekdayString(day.time, weather.location.timezone)}",
                style = MetroTextStyle.ListItemTitle,
                color = MetroTheme.colors.primaryText,
                maxLines = 1,
            )
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            WeatherGlyph(kind, size = 32.dp, color = MetroTheme.colors.primaryText)
        }
        Column(horizontalAlignment = Alignment.End) {
            MetroText(
                text = "${high ?: "-"}°",
                style = MetroTextStyle.ListItemTitle,
                color = MetroTheme.colors.primaryText,
                maxLines = 1,
            )
            MetroText(
                text = "${low ?: "-"}°",
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.secondaryText,
                maxLines = 1,
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(52.dp)) {
            MetroText(
                text = day.precipitationProbabilityMax?.let { "$it%" } ?: "",
                style = MetroTextStyle.ListItemTitle,
                color = MetroTheme.colors.secondaryText,
                maxLines = 1,
            )
            MetroText(text = "💧", style = MetroTextStyle.ListItemSubtitle, color = MetroTheme.colors.secondaryText)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Maps
// ---------------------------------------------------------------------------------------------

private data class MapCategory(val title: String, val scope: String)

/** Historical Maps pane: a scrollable list of map categories (no placeholder). */
@Composable
private fun MapsPane(scrollState: androidx.compose.foundation.ScrollState) {
    val categories = listOf(
        MapCategory(stringResource(R.string.map_temperature), stringResource(R.string.map_regional)),
        MapCategory(stringResource(R.string.map_radar), stringResource(R.string.map_regional)),
        MapCategory(stringResource(R.string.map_precipitation), stringResource(R.string.map_regional)),
        MapCategory(stringResource(R.string.map_cloud), stringResource(R.string.map_regional)),
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 8.dp),
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        categories.forEach { category ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .background(WeatherPalette.BingBlueRow, RectangleShape)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(WeatherPalette.BingBlue, RectangleShape),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    MetroText(text = category.scope, style = MetroTextStyle.ListItemSubtitle, color = MetroTheme.colors.secondaryText)
                    MetroText(text = category.title, style = MetroTextStyle.ListItemTitle, color = MetroTheme.colors.primaryText)
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ---------------------------------------------------------------------------------------------
// Favourites
// ---------------------------------------------------------------------------------------------

@Composable
private fun FavouritesPane(
    locations: List<Location>,
    activeId: String?,
    onSelect: (Location) -> Unit,
    onLongPress: (Location) -> Unit,
    onAdd: () -> Unit,
    scrollState: androidx.compose.foundation.ScrollState,
) {
    if (locations.isEmpty()) {
        WeatherEmptyText(stringResource(R.string.no_favourites))
        return
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 8.dp),
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        locations.forEach { location ->
            val active = location.id == activeId
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .background(WeatherPalette.BingBlueRow, RectangleShape)
                    .metroClickable(onClick = { onSelect(location) }, onLongClick = { onLongPress(location) })
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    MetroText(
                        text = location.customName?.takeIf { it.isNotBlank() } ?: getFullLocationName(location),
                        style = MetroTextStyle.ListItemTitle,
                        color = MetroTheme.colors.primaryText,
                        maxLines = 1,
                    )
                    if (location.isDefault) {
                        MetroText(
                            text = stringResource(R.string.home_label),
                            style = MetroTextStyle.ListItemSubtitle,
                            color = MetroTheme.colors.secondaryText,
                        )
                    }
                }
                if (active) {
                    MetroText(text = "•", style = MetroTextStyle.HubTitle, color = MetroTheme.colors.primaryText)
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        MetroText(
            text = stringResource(R.string.add_place),
            style = MetroTextStyle.ListItemTitle,
            color = MetroTheme.colors.primaryText,
            modifier = Modifier
                .fillMaxWidth()
                .metroClickable(onClick = onAdd)
                .padding(vertical = 14.dp, horizontal = 4.dp),
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun FavouriteContextMenu(
    location: Location?,
    units: WeatherUnits,
    onDismiss: () -> Unit,
    onSelect: (Location) -> Unit,
    onRemove: (Location) -> Unit,
    onSetHome: (Location) -> Unit,
    onPin: (Location) -> Unit,
) {
    if (location == null) return
    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().metroClickable(onClick = onDismiss))
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MetroColors.DarkSecondarySurface)
                .metroNavBarPadding()
                .padding(vertical = 8.dp),
        ) {
            if (!location.isDefault) {
                MenuRow(stringResource(R.string.set_home)) { onSetHome(location) }
            }
            MenuRow(stringResource(R.string.pin_to_start)) { onPin(location) }
            if (!location.isDefault) {
                MenuRow(stringResource(R.string.remove)) { onRemove(location) }
            }
        }
    }
}

@Composable
private fun MenuRow(label: String, onClick: () -> Unit) {
    MetroText(
        text = label,
        style = MetroTextStyle.ListItemTitle,
        color = MetroTheme.colors.primaryText,
        modifier = Modifier
            .fillMaxWidth()
            .metroClickable(onClick = onClick)
            .padding(start = MetroDimens.ScreenHorizontalMargin)
            .padding(vertical = 14.dp),
    )
}

// ---------------------------------------------------------------------------------------------
// Shared
// ---------------------------------------------------------------------------------------------

@Composable
private fun SectionLabel(text: String) {
    MetroText(
        text = text.uppercase(),
        style = MetroTextStyle.SectionHeader,
        color = MetroTheme.colors.secondaryText,
        modifier = Modifier.padding(
            start = MetroDimens.ScreenHorizontalMargin,
            top = 16.dp,
            bottom = 8.dp,
        ),
    )
}

@Composable
private fun Separator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MetroTheme.colors.secondaryText.copy(alpha = 0.35f)),
    )
}

@Composable
private fun WeatherEmptyText(message: String) {
    MetroText(
        text = message,
        style = MetroTextStyle.ListItemTitle,
        color = MetroTheme.colors.secondaryText,
        modifier = Modifier.padding(
            horizontal = MetroDimens.ScreenHorizontalMargin,
            vertical = 24.dp,
        ),
    )
}

private fun tempLabel(value: Double?, units: WeatherUnits): String? =
    TemperatureUnit.CELSIUS.convert(value, units.tempUnit)?.roundToInt()?.let { "$it°" }

private fun pressureLabel(value: Double?, units: WeatherUnits): String? =
    value?.let { "${it.roundToInt()} ${units.pressureUnit.name.lowercase()}" }

private fun visibilityLabel(value: Int?, units: WeatherUnits): String? =
    value?.let { "${(it / 1000.0 * 10).roundToInt() / 10.0} ${units.distanceUnit.name.lowercase()}" }

private fun formatTime(millis: Long?, timezone: String, is24: Boolean): String {
    if (millis == null) return "--"
    return if (is24) to24HourTimeString(millis, timezone) else to12HourTimeString(millis, timezone)
}

private fun uvLabel(v: Double): String = when {
    v < 3 -> "low"
    v < 6 -> "moderate"
    v < 8 -> "high"
    v < 11 -> "very high"
    else -> "extreme"
}

/** Derive a distinct Tonight entry from the hourly forecast (night sample) or the daily list. */
private fun tonightCondition(weather: Weather): WeatherDaily? =
    weather.daily.getOrNull(0)

private fun Long.dayOfMonthLabel(timezone: String): Int = dayOfMonth(timezone)

private fun Long.dayOfMonth(timezone: String): Int {
    val zone = runCatching { java.time.ZoneId.of(timezone) }.getOrElse { java.time.ZoneId.systemDefault() }
    return java.time.Instant.ofEpochMilli(this).atZone(zone).dayOfMonth
}
