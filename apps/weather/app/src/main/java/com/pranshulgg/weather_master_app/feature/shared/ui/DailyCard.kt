package com.pranshulgg.weather_master_app.feature.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.pranshulgg.weather_master_app.R
import com.pranshulgg.weather_master_app.core.model.domain.weather.Weather
import com.pranshulgg.weather_master_app.core.model.domain.weather.WeatherUnits
import com.pranshulgg.weather_master_app.core.model.weather.TemperatureUnit
import com.pranshulgg.weather_master_app.core.model.weather.toIcon
import com.pranshulgg.weather_master_app.core.ui.components.Gap
import com.pranshulgg.weather_master_app.core.ui.components.WeatherIconBox
import com.pranshulgg.weather_master_app.core.ui.navigation.NavRoutes
import com.pranshulgg.weather_master_app.core.utils.formatters.toDateString
import com.pranshulgg.weather_master_app.core.utils.formatters.toWeekdayString
import com.pranshulgg.weather_master_app.core.utils.weather.cache.isWeatherDailyDomainSafe
import com.pranshulgg.weather_master_app.feature.shared.components.CardsHeader
import kotlin.math.roundToInt


@Composable
fun DailyCard(weather: Weather, units: WeatherUnits, navController: NavController) {

    if (!isWeatherDailyDomainSafe(weather)) return


    val daily = weather.daily

    Column(
        modifier = Modifier
            .padding(bottom = 16.dp)
            .fillMaxWidth()
    ) {
        CardsHeader(stringResource(R.string.weather_daily_forecast), R.drawable.date_range_24px)

        Gap(14.dp)

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(daily.size, key = { "${daily[it].time}_$it" }) { index ->

                val item = daily[index]
                val weekDay = toWeekdayString(
                    item.time,
                    weather.location.timezone
                )

                if (index == 0) Gap(horizontal = 16.dp)

                DailyItem(
                    weekDay,
                    item.temperatureMax,
                    item.temperatureMin,
                    item.weatherCondition.toIcon(targetTimeMilli = System.currentTimeMillis()) /* always use day icons */,
                    item.precipitationProbabilityMax,
                    units,
                    onDailyItemClick = {
                        navController.navigate(
                            NavRoutes.daily(index)
                        )
                    },
                    date = toDateString(
                        item.time,
                        weather.location.timezone,
                        pattern = "Mdd"
                    )
                )

                if (index == daily.size - 1) Gap(horizontal = 16.dp)

            }
        }
    }
}

@Composable
private fun DailyItem(
    weekday: String,
    maxTemp: Double?,
    minTemp: Double?,
    icon: Int,
    precipitationProbability: Int?,
    units: WeatherUnits,
    onDailyItemClick: () -> Unit,
    date: String
) {


    val maxTemp = TemperatureUnit.CELSIUS.convert(maxTemp, units.tempUnit)?.roundToInt() ?: "-"
    val minTemp = TemperatureUnit.CELSIUS.convert(minTemp, units.tempUnit)?.roundToInt() ?: "-"

    Column(
        Modifier
            .background(MetroTheme.colors.secondarySurface)
            .clickable(onClick = onDailyItemClick)
            .heightIn(150.dp)
            .width(84.dp)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MetroText(
                text = "${maxTemp}°",
                style = MetroTextStyle.Body,
                color = MetroTheme.colors.primaryText
            )
            MetroText(
                text = "${minTemp}°",
                style = MetroTextStyle.Body,
                color = MetroTheme.colors.secondaryText
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WeatherIconBox(icon, size = 38.dp)
            Gap(8.dp)
            MetroText(
                text = "${precipitationProbability}%",
                modifier = Modifier.alpha(if (precipitationProbability == null) 0f else 1f),
                color = MetroTheme.colors.accent,
                style = MetroTextStyle.Body
            )
            Gap(4.dp)
            MetroText(
                text = weekday,
                color = MetroTheme.colors.primaryText,
                style = MetroTextStyle.Body
            )
            MetroText(
                text = date,
                color = MetroTheme.colors.secondaryText,
                style = MetroTextStyle.ListItemSubtitle
            )
        }
    }
}
