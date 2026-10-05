package com.pranshulgg.weather_master_app.feature.blocks.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroTheme


@Composable
fun ChartBarItem(
    barBackgroundColor: Color = MetroTheme.colors.accent,
    height: Int,
) {
    Box(
        modifier = Modifier
            .background(
                color = barBackgroundColor,
                shape = RectangleShape,
            )
            .height(height.dp)
            .fillMaxWidth()
    ) {
    }

}
