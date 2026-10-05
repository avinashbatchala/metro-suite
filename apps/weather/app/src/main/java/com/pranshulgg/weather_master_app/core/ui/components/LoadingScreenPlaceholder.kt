package com.pranshulgg.weather_master_app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import com.metro.ui.MetroLoadingDots
import com.metro.ui.MetroTheme

@Composable
fun LoadingScreenPlaceholder(
    fraction: Float = 1f,
    containerColor: Color = MetroTheme.colors.background
) {
    Box(
        modifier = Modifier
            .background(containerColor)
            .zIndex(10000f)
            .fillMaxWidth()
            .fillMaxHeight(fraction = fraction),
        contentAlignment = Alignment.Center
    ) {
        MetroLoadingDots()
    }
}
