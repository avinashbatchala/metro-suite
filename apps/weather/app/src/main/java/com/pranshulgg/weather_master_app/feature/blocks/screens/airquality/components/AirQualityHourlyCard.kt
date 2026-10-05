package com.pranshulgg.weather_master_app.feature.blocks.screens.airquality.components

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroDimens
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.pranshulgg.weather_master_app.R
import com.pranshulgg.weather_master_app.core.model.domain.airquality.AirQuality
import com.pranshulgg.weather_master_app.core.model.domain.airquality.AirQualityHourly
import com.pranshulgg.weather_master_app.core.model.weather.airquality.Pollutant
import com.pranshulgg.weather_master_app.core.prefs.LocalAppPrefs
import com.pranshulgg.weather_master_app.core.ui.components.Gap
import com.pranshulgg.weather_master_app.core.utils.extensions.DateTimeExtensions.secondsToMilliseconds
import com.pranshulgg.weather_master_app.core.utils.formatters.to12HourTimeString
import com.pranshulgg.weather_master_app.core.utils.formatters.to24HourTimeString
import kotlin.math.max

@Composable
fun AirQualityHourlyCard(data: List<AirQualityHourly>, zoneId: String, airQuality: AirQuality) {

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

    val aqiList = data.map {
        airQuality.getAqiFromValues(
            it.carbonMonoxide,
            it.pm25,
            it.nitrogenDioxide,
            it.ozone,
            it.pm10,
            it.sulphurDioxide
        )
    }

    val sharedScrollState = rememberScrollState()

    val max = aqiList.max().toDouble()

    val min = 0.0
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MetroDimens.ScreenHorizontalMargin)
            .background(MetroTheme.colors.secondarySurface, RectangleShape)
            .padding(bottom = 16.dp)
    ) {
        MetroText(
            text = stringResource(R.string.weather_air_quality_index).uppercase(),
            style = MetroTextStyle.SectionHeader,
            color = MetroTheme.colors.accent,
            modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .heightIn(200.dp)
                .horizontalScroll(sharedScrollState),
            verticalAlignment = Alignment.Bottom,
        ) {

            data.forEachIndexed { index, hourly ->


                val aqi = airQuality.getAqiFromValues(
                    hourly.carbonMonoxide,
                    hourly.pm25,
                    hourly.nitrogenDioxide,
                    hourly.ozone,
                    hourly.pm10,
                    hourly.sulphurDioxide
                )

                val percentage = ((aqi.toDouble().minus(min)).div((max - min))).times(
                    100
                )

                val barHeight = max((percentage.div(100.0)).times(140), 5.0)



                if (index == 0) Gap(horizontal = 16.dp)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    MetroText(
                        text = timeFormatter(hourly.time.secondsToMilliseconds()),
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
                        Box(
                            Modifier
                                .width(38.dp)
                                .height(barHeight.dp)
                                .background(MetroTheme.colors.accent, RectangleShape),
                        )
                    }
                    Gap(5.dp)
                    MetroText(
                        text = "$aqi",
                        color = MetroTheme.colors.accent,
                        style = MetroTextStyle.ListItemTitle
                    )
                }
                if (index == data.size - 1) Gap(horizontal = 16.dp)
            }
        }

        PollutantHourlyCard(data, zoneId, Pollutant.PM25, sharedScrollState)
        PollutantHourlyCard(data, zoneId, Pollutant.PM10, sharedScrollState)
        PollutantHourlyCard(data, zoneId, Pollutant.O3, sharedScrollState)
        PollutantHourlyCard(data, zoneId, Pollutant.NO2, sharedScrollState)
        PollutantHourlyCard(data, zoneId, Pollutant.CO, sharedScrollState)
        PollutantHourlyCard(data, zoneId, Pollutant.SO2, sharedScrollState)

    }


}
