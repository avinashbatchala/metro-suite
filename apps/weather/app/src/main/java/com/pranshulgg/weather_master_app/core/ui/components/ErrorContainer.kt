package com.pranshulgg.weather_master_app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.metro.ui.MetroColors
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.pranshulgg.weather_master_app.R

@Composable
fun ErrorContainer(
    showRetryAction: Boolean = true,
    onRetry: () -> Unit = {},
    containerColor: Color = MetroTheme.colors.background,
    errorDescription: String = "Something went wrong",
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .zIndex(100000f)
            .background(containerColor),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Symbol(R.drawable.info_24px, color = MetroColors.AccentRed, size = 54.dp)
        Gap(26.dp)
        MetroText(
            text = "Error occurred",
            style = MetroTextStyle.DialogTitle,
            color = MetroTheme.colors.primaryText,
            textAlign = TextAlign.Center
        )
        Gap(8.dp)
        MetroText(
            text = errorDescription,
            style = MetroTextStyle.Body,
            color = MetroTheme.colors.secondaryText,
            textAlign = TextAlign.Center
        )
        if (showRetryAction) {
            Gap(34.dp)
            M3eButton(
                size = 40.dp,
                text = "Try again",
                onClick = { onRetry() },
                icon = R.drawable.refresh_24px
            )
        }
    }
}
