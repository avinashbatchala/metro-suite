package com.pranshulgg.weather_master_app.feature.daily.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroColors
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroClickable
import com.pranshulgg.weather_master_app.core.model.weather.WeatherCondition
import com.pranshulgg.weather_master_app.core.model.weather.toIcon
import com.pranshulgg.weather_master_app.core.ui.components.Gap
import com.pranshulgg.weather_master_app.core.ui.components.WeatherIconBox

@Composable
fun SelectableDayItem(
    weekday: String,
    minTemp: String,
    maxTemp: String,
    conditions: WeatherCondition,
    onSelect: () -> Unit,
    isSelected: Boolean,
    motionScheme: MotionScheme
) {


    val animatedColor by animateColorAsState(
        targetValue = if (isSelected) MetroTheme.colors.accent else MetroTheme.colors.secondarySurface,
        animationSpec = motionScheme.defaultEffectsSpec(),
        label = "color"
    )

    val selectedOnSurface =
        if (isSelected) MetroColors.tileContentColor(MetroTheme.colors.accent) else MetroTheme.colors.primaryText
    val selectedOnSurfaceVariant =
        if (isSelected) MetroColors.tileContentColor(MetroTheme.colors.accent).copy(0.8f) else MetroTheme.colors.secondaryText


    Column(
        modifier = Modifier
            .width(65.dp)
            .background(animatedColor, RectangleShape)
            .metroClickable(onClick = onSelect)
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MetroText(
                text = "${maxTemp}°",
                style = MetroTextStyle.Body,
                color = selectedOnSurface
            )
            MetroText(
                text = "${minTemp}°",
                style = MetroTextStyle.Body,
                color = selectedOnSurfaceVariant
            )
        }
        Gap(5.dp)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WeatherIconBox(
                conditions.toIcon(targetTimeMilli = System.currentTimeMillis()),
                size = 34.dp
            )
            Gap(8.dp)
            MetroText(
                text = weekday,
                color = selectedOnSurface,
                style = MetroTextStyle.Body
            )
        }
    }
}
