package com.pranshulgg.weather_master_app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme


@Composable
fun EmptyContainerPlaceholder(
    icon: Int,
    text: String,
    description: String = "",
    fraction: Float = 0.8f,
    size: Float = 1f
) {

    val containerSize = 160.dp * size
    val iconSize = 76.dp * size
    val spacingLarge = 16.dp * size
    val spacingSmall = 5.dp * size

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(fraction = fraction),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(containerSize)
                .background(MetroTheme.colors.secondarySurface),
            contentAlignment = Alignment.Center
        ) {
            Symbol(icon, size = iconSize, color = MetroTheme.colors.accent)
        }

        Spacer(Modifier.height(spacingLarge))
        MetroText(
            text = text,
            style = MetroTextStyle.ListItemTitle,
            color = MetroTheme.colors.primaryText
        )
        if (description != "") {
            Spacer(Modifier.height(spacingSmall))
            MetroText(
                text = description,
                style = MetroTextStyle.Body,
                color = MetroTheme.colors.secondaryText,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }

}
