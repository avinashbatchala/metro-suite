package com.pranshulgg.weather_master_app.core.ui.components

import androidx.compose.runtime.Composable
import com.metro.ui.MetroColors
import com.metro.ui.MetroTheme

@Composable
fun SettingsTileIcon(icon: Int, dangerColor: Boolean = false) {
    Symbol(
        icon,
        color = if (dangerColor) MetroColors.AccentRed else MetroTheme.colors.secondaryText
    )
}
