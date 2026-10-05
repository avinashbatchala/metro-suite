package com.pranshulgg.weather_master_app.feature.daily.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroDimens
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.pranshulgg.weather_master_app.core.model.domain.weather.WeatherUnits
import com.pranshulgg.weather_master_app.core.model.domain.location.Location
import com.pranshulgg.weather_master_app.core.model.domain.weather.WeatherDaily
import com.pranshulgg.weather_master_app.core.model.weather.TemperatureUnit
import com.pranshulgg.weather_master_app.core.model.weather.toIcon
import com.pranshulgg.weather_master_app.core.model.weather.toLabel
import com.pranshulgg.weather_master_app.core.ui.components.Gap
import com.pranshulgg.weather_master_app.core.ui.components.WeatherIconBox
import com.pranshulgg.weather_master_app.core.utils.formatters.toDateString
import kotlin.math.roundToInt


@Composable
fun DailyForecastHeroHeader(
    daily: WeatherDaily,
    location: Location,
    units: WeatherUnits
) {

    val date = toDateString(daily.time, location.timezone)
    val context = LocalContext.current

    val maxTemp =
        TemperatureUnit.CELSIUS.convert(daily.temperatureMax, units.tempUnit)?.roundToInt() ?: "-"
    val minTemp =
        TemperatureUnit.CELSIUS.convert(daily.temperatureMin, units.tempUnit)?.roundToInt() ?: "-"

    Column(
        Modifier.padding(
            start = MetroDimens.ScreenHorizontalMargin,
            end = MetroDimens.ScreenHorizontalMargin,
            top = 24.dp,
        )
    ) {
        MetroText(
            text = date,
            style = MetroTextStyle.ListItemSubtitle,
            color = MetroTheme.colors.secondaryText
        )
        MetroText(
            text = location.name,
            style = MetroTextStyle.SectionHeader,
            color = MetroTheme.colors.primaryText
        )
        Gap(5.dp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            MetroText(
                text = "${maxTemp}°",
                color = MetroTheme.colors.primaryText,
                style = MetroTextStyle.PageTitle
            )
            Gap(horizontal = 6.dp)
            MetroText(
                text = "${minTemp}°",
                color = MetroTheme.colors.secondaryText,
                style = MetroTextStyle.PageTitle
            )
            Gap(horizontal = 12.dp)
            WeatherIconBox(
                daily.weatherCondition.toIcon(targetTimeMilli = System.currentTimeMillis()),
                size = 54.dp
            )
        }
        MetroText(
            text = daily.weatherCondition.toLabel(context),
            style = MetroTextStyle.SectionHeader,
            color = MetroTheme.colors.accent
        )
    }

}
