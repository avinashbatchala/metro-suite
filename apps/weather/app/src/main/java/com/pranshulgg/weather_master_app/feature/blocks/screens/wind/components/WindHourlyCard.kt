package com.pranshulgg.weather_master_app.feature.blocks.screens.wind.components

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroColors
import com.metro.ui.MetroDimens
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.pranshulgg.weather_master_app.R
import com.pranshulgg.weather_master_app.core.model.domain.weather.WeatherHourly
import com.pranshulgg.weather_master_app.core.model.weather.WindSpeedUnit
import com.pranshulgg.weather_master_app.core.model.weather.toName
import com.pranshulgg.weather_master_app.core.model.weather.wind.WindDirection
import com.pranshulgg.weather_master_app.core.prefs.LocalAppPrefs
import com.pranshulgg.weather_master_app.core.ui.components.Gap
import com.pranshulgg.weather_master_app.core.utils.formatters.to12HourTimeString
import com.pranshulgg.weather_master_app.core.utils.formatters.to24HourTimeString
import kotlin.math.max
import kotlin.math.roundToInt


@Composable
fun WindHourlyCard(
    data: List<WeatherHourly>,
    zoneId: String,
    unit: WindSpeedUnit,
    context: Context
) {

    val prefs = LocalAppPrefs.current

    val timeFormatter: (Long) -> String = {
        if (prefs.is24HrTimeFormat) to24HourTimeString(
            it,
            zoneId
        ) else to12HourTimeString(
            it,
            zoneId
        )
    }


    val max = data.maxOf { it.windSpeed!! }
    val min = data.minOf { it.windSpeed!! }
    val formatter: (Double) -> Double? = {
        WindSpeedUnit.KPH.convert(it, unit)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MetroDimens.ScreenHorizontalMargin)
            .background(MetroTheme.colors.secondarySurface, RectangleShape)
            .padding(bottom = 16.dp)
    ) {
        MetroText(
            text = "${stringResource(R.string.weather_hourly_forecast)} (${
                unit.toName(
                    context,
                    true
                )
            })".uppercase(),
            style = MetroTextStyle.SectionHeader,
            color = MetroTheme.colors.accent,
            modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.heightIn(200.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            items(
                data.size,
                key = { "${data[it].time}_$it" }) { index ->

                val item = data[index]

                val percentage = ((item.windSpeed!!.minus(min)).div((max - min))).times(100)

                val barHeight = if (!percentage.isNaN()) max(
                    (percentage.div(100)).times(140).roundToInt(),
                    48
                ) else 48

                if (index == 0) Gap(horizontal = 16.dp)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Box(contentAlignment = Alignment.BottomCenter) {
                        Box(
                            Modifier
                                .width(18.dp)
                                .height(140.dp)
                                .background(MetroTheme.colors.background, RectangleShape),
                        )
                        Box(
                            Modifier
                                .width(38.dp)
                                .height(barHeight.dp)
                                .background(MetroTheme.colors.accent, RectangleShape),
                        ) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .padding(top = 6.dp),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                if (item.windDirection != null) {
                                    Image(
                                        painter = painterResource(id = R.drawable.weather_wind_arrow_dominant),
                                        contentDescription = "",
                                        modifier = Modifier
                                            .size(24.dp)
                                            .rotate(
                                                WindDirection.toDegrees(item.windDirection)
                                                    ?.toFloat() ?: 0f
                                            ),
                                        colorFilter = ColorFilter.tint(
                                            MetroColors.tileContentColor(MetroTheme.colors.accent)
                                        )
                                    )
                                }
                            }
                        }
                    }
                    Gap(5.dp)
                    MetroText(
                        text = "${formatter(item.windSpeed)?.roundToInt()}",
                        color = MetroTheme.colors.accent,
                        style = MetroTextStyle.ListItemTitle
                    )
                    MetroText(
                        text = timeFormatter(item.time),
                        color = MetroTheme.colors.secondaryText,
                        style = MetroTextStyle.ListItemSubtitle
                    )
                }
                if (index == data.size - 1) Gap(horizontal = 16.dp)
            }
        }
    }
}
