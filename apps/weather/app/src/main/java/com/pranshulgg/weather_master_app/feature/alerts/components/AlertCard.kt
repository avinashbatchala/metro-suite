package com.pranshulgg.weather_master_app.feature.alerts.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.pranshulgg.weather_master_app.R
import com.pranshulgg.weather_master_app.core.model.domain.alerts.Alert
import com.pranshulgg.weather_master_app.core.model.weather.alerts.AlertSeverity
import com.pranshulgg.weather_master_app.core.prefs.AppPrefsState
import com.pranshulgg.weather_master_app.core.ui.components.Gap
import com.pranshulgg.weather_master_app.core.utils.formatters.getLocalizedPattern
import com.pranshulgg.weather_master_app.core.utils.formatters.safeZoneId
import java.time.Instant
import java.time.format.DateTimeFormatter


@Composable
fun AlertCard(alert: Alert, prefs: AppPrefsState, zoneId: String, shape: Shape) {
    val pattern = getLocalizedPattern(
        if (prefs.is24HrTimeFormat) "MMMddHmm" else "MMMddhmma"
    )

    val formatter: (Long?) -> String? = {
        it?.let {
            val formatter = DateTimeFormatter.ofPattern(pattern)
            val instant = Instant.ofEpochMilli(it)
            val dateTime = instant.atZone(safeZoneId(zoneId)).toLocalDateTime()
            formatter.format(dateTime)
        } ?: "Unknown"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MetroTheme.colors.secondarySurface, shape)
            .padding(bottom = 16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(
                5.dp,
                alignment = Alignment.CenterHorizontally
            ),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)
        ) {
            Column() {
                MetroText(
                    text = alert.event,
                    style = MetroTextStyle.ListItemTitle,
                    color = MetroTheme.colors.primaryText
                )
                Gap(6.dp)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(
                        5.dp,
                        alignment = Alignment.CenterVertically
                    )
                ) {
                    Chip(
                        stringResource(alert.severity?.label ?: AlertSeverity.UNKNOWN.label),
                        alert.severity?.color ?: AlertSeverity.UNKNOWN.color,
                        alert.severity?.contentColor ?: AlertSeverity.UNKNOWN.contentColor
                    )
                    Chip(
                        "Effective ${formatter(alert.effective)}",
                        MetroTheme.colors.background,
                        MetroTheme.colors.primaryText
                    )
                    Chip(
                        "Expires ${formatter(alert.expires)}",
                        MetroTheme.colors.accent,
                        MetroTheme.colors.primaryText
                    )
                }
            }
        }
        Gap(12.dp)
        SquareDivider()
        Gap(12.dp)
        MetroText(
            text = alert.description,
            color = MetroTheme.colors.secondaryText,
            style = MetroTextStyle.Body,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Gap(12.dp)
        SquareDivider()
        Gap(12.dp)
        MetroText(
            text = "Source ${alert.source}",
            color = MetroTheme.colors.secondaryText,
            style = MetroTextStyle.ListItemSubtitle,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun Chip(text: String, color: Color, textColor: Color) {
    Box(
        modifier = Modifier.background(color, RectangleShape)
    ) {
        MetroText(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MetroTextStyle.Body,
            color = textColor
        )
    }
}

@Composable
private fun SquareDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(MetroTheme.colors.secondaryText, RectangleShape)
    )
}
