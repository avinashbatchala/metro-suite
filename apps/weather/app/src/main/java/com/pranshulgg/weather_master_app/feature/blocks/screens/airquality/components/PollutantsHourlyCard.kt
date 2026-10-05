package com.pranshulgg.weather_master_app.feature.blocks.screens.airquality.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroColors
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.pranshulgg.weather_master_app.core.model.domain.airquality.AirQualityHourly
import com.pranshulgg.weather_master_app.core.model.weather.airquality.Pollutant
import com.pranshulgg.weather_master_app.core.ui.components.Gap
import com.pranshulgg.weather_master_app.core.utils.formatters.formatLocalizedNumber
import com.pranshulgg.weather_master_app.core.utils.locale.getCurrentAppLocale
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun PollutantHourlyCard(
    data: List<AirQualityHourly>,
    zoneId: String,
    pollutant: Pollutant,
    sharedScrollState: ScrollState
) {


    val pollutantList = data.map {

        val value = when (pollutant) {
            Pollutant.PM25 -> it.pm25
            Pollutant.PM10 -> it.pm10
            Pollutant.CO -> it.carbonMonoxide
            Pollutant.NO2 -> it.nitrogenDioxide
            Pollutant.SO2 -> it.sulphurDioxide
            Pollutant.O3 -> it.ozone
        }

        Pollutant.getLevelIndex(
            value!!,
            pollutant.thresholds
        )
    }

    val headerText = when (pollutant) {
        Pollutant.PM25 -> "PM2.5 (μg/m³)"
        Pollutant.PM10 -> "PM10 (μg/m³)"
        Pollutant.CO -> "C0 (mg/m³)"
        Pollutant.NO2 -> "N02 (μg/m³)"
        Pollutant.SO2 -> "SO2 (μg/m³)"
        Pollutant.O3 -> "03 (μg/m³)"
    }

    val headerSecondaryText = when (pollutant) {
        Pollutant.PM25 -> "Particulate matter less than 2.5 microns"
        Pollutant.PM10 -> "Particulate matter less than 10 microns"
        Pollutant.CO -> "Carbon monoxide"
        Pollutant.NO2 -> "Nitrogen Dioxide"
        Pollutant.SO2 -> "Sulfur Dioxide"
        Pollutant.O3 -> "Ground surface"
    }

    val max = pollutantList.max().toDouble()
    val min = 0.0

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        MetroText(
            text = headerText.uppercase(),
            style = MetroTextStyle.SectionHeader,
            color = MetroTheme.colors.accent,
            modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)
        )
        MetroText(
            text = headerSecondaryText,
            color = MetroTheme.colors.secondaryText,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp),
            style = MetroTextStyle.Body,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .heightIn(180.dp)
                .horizontalScroll(sharedScrollState),
            verticalAlignment = Alignment.Bottom,
        ) {

            data.forEachIndexed { index, hourly ->


                val value = when (pollutant) {
                    Pollutant.PM25 -> hourly.pm25
                    Pollutant.PM10 -> hourly.pm10
                    Pollutant.CO -> hourly.carbonMonoxide
                    Pollutant.NO2 -> hourly.nitrogenDioxide
                    Pollutant.SO2 -> hourly.sulphurDioxide
                    Pollutant.O3 -> hourly.ozone
                }
                val indexPollutant = Pollutant.getLevelIndex(
                    if (pollutant == Pollutant.CO) (value!! / 1000) else value!!,
                    pollutant.thresholds
                )

                val percentage = ((indexPollutant.toDouble().minus(min)).div((max - min))).times(
                    100.0
                )

                val barHeight =
                    max((percentage.div(100.0)).times(140), 45.0).takeIf { !it.isNaN() } ?: 45.0




                if (index == 0) Gap(horizontal = 16.dp)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // duplicate stuff
                    MetroText(
                        text = if (pollutant == Pollutant.CO) formatLocalizedNumber(
                            getCurrentAppLocale(),
                            (value / 1000),
                            decimalPlaces = 1
                        ) else if (pollutant == Pollutant.NO2) "${value.roundToInt()}" else formatLocalizedNumber(
                            getCurrentAppLocale(),
                            value,
                            decimalPlaces = 1
                        ),
                        color = MetroTheme.colors.secondaryText,
                        style = MetroTextStyle.ListItemSubtitle
                    )
                    Gap(5.dp)

                    Box(contentAlignment = Alignment.BottomCenter) {
                        Box(
                            Modifier
                                .width(18.dp)
                                .height(140.dp)
                                .background(MetroTheme.colors.background, RectangleShape),
                        )
                        Box() {
                            Box(
                                Modifier
                                    .width(38.dp)
                                    .height(barHeight.dp)
                                    .background(MetroTheme.colors.accent, RectangleShape),
                            )
                            MetroText(
                                text = "$indexPollutant",
                                color = MetroColors.tileContentColor(MetroTheme.colors.accent),
                                style = MetroTextStyle.ListItemTitle,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 8.dp)
                            )
                        }
                    }
                }
                if (index == data.size - 1) Gap(horizontal = 16.dp)
            }
        }
    }


}
