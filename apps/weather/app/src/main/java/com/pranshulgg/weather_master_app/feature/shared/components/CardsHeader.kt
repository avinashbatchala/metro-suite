package com.pranshulgg.weather_master_app.feature.shared.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.pranshulgg.weather_master_app.core.ui.components.Symbol

@Composable
fun CardsHeader(text: String, icon: Int? = null) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(
            5.dp,
            alignment = Alignment.CenterHorizontally
        ),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)
    ) {
        if (icon != null) {
            Symbol(
                icon,
                color = MetroTheme.colors.accent
            )
        }

        MetroText(
            text = text.uppercase(),
            style = MetroTextStyle.SectionHeader,
            color = MetroTheme.colors.accent
        )

    }
}
