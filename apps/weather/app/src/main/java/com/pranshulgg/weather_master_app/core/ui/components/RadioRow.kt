package com.pranshulgg.weather_master_app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

/** Square Windows radio row. */
@Composable
fun RadioRow(
    label: String,
    value: String,
    selected: Boolean,
    hasPadding: Boolean = true,
    onClick: (String) -> Unit
) {
    val accent = MetroTheme.colors.accent
    val subtle = MetroTheme.colors.secondaryText
    Row(
        modifier = Modifier
            .clickable { onClick(value) }
            .fillMaxWidth()
            .padding(
                horizontal = if (hasPadding) 16.dp else 0.dp,
                vertical = 10.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .border(2.dp, if (selected) accent else subtle, RectangleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(modifier = Modifier.size(10.dp).background(accent, RectangleShape))
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        MetroText(
            text = label,
            style = MetroTextStyle.Body,
            color = MetroTheme.colors.primaryText
        )
    }
}
