package com.pranshulgg.weather_master_app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.pranshulgg.weather_master_app.R

@Composable
fun AvatarMonogram(
    text: String,
    containerColor: Color = MetroTheme.colors.accent,
    contentColor: Color = MetroTheme.colors.primaryText,
    cornerRadius: Dp = 0.dp
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .background(containerColor)
    ) {
        MetroText(
            text = text,
            style = MetroTextStyle.Body,
            color = contentColor
        )
    }
}

@Composable
fun AvatarIcon(
    icon: Int,
    containerColor: Color = MetroTheme.colors.accent,
    contentColor: Color = MetroTheme.colors.primaryText,
    cornerRadius: Dp = 0.dp,
    avatarSize: Dp = 40.dp,
    iconSize: Dp = 24.dp
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(avatarSize)
            .background(containerColor)
    ) {
        Symbol(
            icon,
            color = contentColor,
            size = iconSize
        )
    }
}


@Composable
fun AvatarCheck(
    containerColor: Color = MetroTheme.colors.accent,
    contentColor: Color = MetroTheme.colors.primaryText
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .background(containerColor)
    ) {
        Symbol(R.drawable.check_24px, color = contentColor)
    }
}
