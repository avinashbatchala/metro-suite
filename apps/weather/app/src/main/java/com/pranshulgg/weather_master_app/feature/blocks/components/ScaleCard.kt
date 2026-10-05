package com.pranshulgg.weather_master_app.feature.blocks.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.pranshulgg.weather_master_app.core.ui.components.Gap
import com.pranshulgg.weather_master_app.core.ui.components.Symbol


@Composable
fun ScaleCard(items: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MetroDimens.ScreenHorizontalMargin)
            .background(MetroTheme.colors.secondarySurface, RectangleShape)
            .padding(bottom = 16.dp)
    ) {
        CardHeader()
        Gap(8.dp)
        items()
    }
}

@Composable
private fun CardHeader() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(
            5.dp,
            alignment = Alignment.CenterHorizontally
        ),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)
    ) {
        Symbol(
            R.drawable.linear_scale_24px,
            color = MetroTheme.colors.accent
        )
        MetroText(
            text = stringResource(R.string.text_scale),
            style = MetroTextStyle.SectionHeader,
            color = MetroTheme.colors.accent
        )
    }
}
