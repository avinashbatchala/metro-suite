package com.pranshulgg.weather_master_app.feature.blocks.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.pranshulgg.weather_master_app.R
import com.pranshulgg.weather_master_app.core.ui.components.Gap
import com.pranshulgg.weather_master_app.core.ui.components.Symbol

@Composable
fun NoHourlyDataAvailable() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Symbol(R.drawable.info_24px, color = MetroTheme.colors.secondaryText)
        Gap(3.dp)
        MetroText(
            text = stringResource(R.string.weather_no_data),
            style = MetroTextStyle.Body,
            color = MetroTheme.colors.secondaryText
        )
    }
}
