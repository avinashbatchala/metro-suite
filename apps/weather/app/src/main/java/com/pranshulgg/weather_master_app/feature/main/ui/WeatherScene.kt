package com.pranshulgg.weather_master_app.feature.main.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.pranshulgg.weather_master_app.core.model.domain.weather.Weather
import com.pranshulgg.weather_master_app.core.ui.theme.isThemeDark

/** Foreground colour that sits on top of the active weather scene. */
val LocalWeatherForeground = staticCompositionLocalOf { Color.White }

@Composable
fun weatherForeground(): Color = if (isThemeDark()) Color.White else Color(0xFF1F1F1F)

/**
 * Home background. The Metro house style is a flat full-bleed black page, so the old
 * condition scene / gradient / animated overlay are intentionally disabled.
 */
@Composable
fun WeatherBackground(
    weather: Weather?,
    showAnimations: Boolean = true,
    parallaxPx: Float = 0f
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black))
}
