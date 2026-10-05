package com.pranshulgg.weather_master_app.feature.main.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.pranshulgg.weather_master_app.R
import com.pranshulgg.weather_master_app.core.model.domain.alerts.Alert
import com.pranshulgg.weather_master_app.core.model.weather.alerts.AlertSeverity
import com.pranshulgg.weather_master_app.core.prefs.AppPrefsState
import com.pranshulgg.weather_master_app.core.ui.components.Symbol
import com.pranshulgg.weather_master_app.core.utils.formatters.getLocalizedPattern
import com.pranshulgg.weather_master_app.core.utils.formatters.safeZoneId
import java.time.Instant
import java.time.format.DateTimeFormatter

@Composable
fun AlertsSection(
    alerts: List<Alert>,
    prefs: AppPrefsState,
    zoneId: String,
    onAlertClick: () -> Unit
) {
    val pattern = getLocalizedPattern(
        if (prefs.is24HrTimeFormat) "MMMddHmm" else "MMddhmma"
    )
    val formatter: (Long) -> String = {
        val fmt = DateTimeFormatter.ofPattern(pattern)
        val instant = Instant.ofEpochMilli(it)
        val dateTime = instant.atZone(safeZoneId(zoneId)).toLocalDateTime()
        fmt.format(dateTime)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        alerts.forEach { alert ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onAlertClick)
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Symbol(
                    R.drawable.warning_24px,
                    color = alert.severity?.color ?: AlertSeverity.UNKNOWN.color,
                    size = 28.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    MetroText(
                        text = alert.event,
                        style = MetroTextStyle.ListItemTitle,
                        color = MetroTheme.colors.primaryText
                    )
                    if (alert.effective != null && alert.expires != null) {
                        MetroText(
                            text = "${formatter(alert.effective)} • ${formatter(alert.expires)}",
                            style = MetroTextStyle.ListItemSubtitle,
                            color = MetroTheme.colors.secondaryText
                        )
                    }
                }
            }
        }
    }
}
