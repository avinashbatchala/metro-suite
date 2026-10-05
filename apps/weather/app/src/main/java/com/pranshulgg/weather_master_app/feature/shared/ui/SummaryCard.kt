package com.pranshulgg.weather_master_app.feature.shared.ui

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.pranshulgg.weather_master_app.R
import com.pranshulgg.weather_master_app.core.model.domain.weather.Weather
import com.pranshulgg.weather_master_app.core.model.domain.weather.WeatherUnits
import com.pranshulgg.weather_master_app.core.ui.components.Gap
import com.pranshulgg.weather_master_app.core.utils.weather.computing.summary.computeDaySummary
import com.pranshulgg.weather_master_app.feature.shared.components.CardsHeader


@Composable
fun SummaryCard(
    weather: Weather,
    dailyIndex: Int = 0,
    context: Context,
    units: WeatherUnits
) {

    val summary = computeDaySummary(weather, context, dailyIndex, units)

    Column(
        modifier = Modifier
            .padding(bottom = 16.dp)
            .fillMaxWidth()
    ) {

        CardsHeader(stringResource(R.string.setting_day_summary), R.drawable.article_24px)
        Gap(8.dp)
        MetroText(
            text = summary,
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MetroTextStyle.Body,
            color = MetroTheme.colors.primaryText
        )
    }
}
