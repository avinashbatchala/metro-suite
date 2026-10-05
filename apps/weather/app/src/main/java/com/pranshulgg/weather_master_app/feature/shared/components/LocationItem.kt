package com.pranshulgg.weather_master_app.feature.shared.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroColors
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.pranshulgg.weather_master_app.R
import com.pranshulgg.weather_master_app.core.ui.components.Symbol
import com.pranshulgg.weather_master_app.core.ui.components.WeatherIconBox

/** Flat Windows 10 location row (weather glyph chip + light title + subtle subtitle). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LocationItem(
    title: String,
    description: String,
    icon: Int,
    onClick: () -> Unit,
    isSelected: Boolean = false,
    isDefault: Boolean = false,
    onLongClick: () -> Unit,
    isDeviceLocation: Boolean = false,
    shape: androidx.compose.foundation.shape.RoundedCornerShape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
    isAlertAvailable: Boolean = false
) {
    val onSurface = MetroTheme.colors.primaryText
    val subtle = MetroTheme.colors.secondaryText
    val contentColor = if (isSelected) MetroTheme.colors.accent else onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(MetroTheme.colors.secondarySurface),
            contentAlignment = Alignment.Center
        ) {
            WeatherIconBox(icon, size = 30.dp)
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MetroText(
                    text = title,
                    color = contentColor,
                    style = MetroTextStyle.ListItemTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isDefault) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Symbol(R.drawable.home_pin_24px, color = contentColor, size = 16.dp)
                }
            }
            if (isAlertAvailable) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Symbol(R.drawable.warning_24px, color = MetroColors.AccentRed, size = 14.dp)
                    Spacer(modifier = Modifier.width(4.dp))
                    MetroText(
                        text = "Active alerts",
                        color = MetroColors.AccentRed,
                        style = MetroTextStyle.ListItemSubtitle
                    )
                }
            } else {
                MetroText(
                    text = description,
                    color = subtle,
                    style = MetroTextStyle.ListItemSubtitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (isDeviceLocation) {
            Symbol(R.drawable.circle_circle_24px, color = subtle)
        }
    }
}

@Composable
private fun TitleForDefaultLocation(contentColor: Color, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        MetroText(
            text = title,
            color = contentColor,
            style = MetroTextStyle.ListItemTitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(4.dp))
        Symbol(R.drawable.home_pin_24px, color = contentColor, size = 16.dp)
    }
}
