package com.pranshulgg.weather_master_app.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroSlider
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar
import kotlin.math.roundToInt

/** Windows-style time picker built from thin sliders (no Material TimePicker). */
@Composable
fun BasicTimePicker(
    show: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
    is24Hour: Boolean,
    initialTime: Long? = null,
) {
    val time = Instant.ofEpochMilli(initialTime ?: System.currentTimeMillis())
        .atZone(ZoneId.systemDefault()).toLocalTime()

    var hour by remember(initialTime) { mutableIntStateOf(time.hour) }
    var minute by remember(initialTime) { mutableIntStateOf(time.minute) }

    val hourLabel = if (is24Hour) {
        hour.toString().padStart(2, '0')
    } else {
        val h = hour % 12
        if (h == 0) "12" else h.toString()
    }
    val minuteLabel = minute.toString().padStart(2, '0')
    val suffix = if (is24Hour) "" else if (hour < 12) " AM" else " PM"

    DialogBasic(
        show = show,
        onDismiss = onDismiss,
        title = "Select time",
        onConfirm = {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            onConfirm(calendar.timeInMillis)
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            MetroText(
                text = "$hourLabel:$minuteLabel$suffix",
                style = MetroTextStyle.DialogTitle,
                color = MetroTheme.colors.accent
            )
            Spacer(modifier = Modifier.height(8.dp))
            MetroText(
                text = "hour",
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.secondaryText
            )
            MetroSlider(
                value = hour / 23f,
                onValueChange = { hour = (it * 23f).roundToInt().coerceIn(0, 23) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            MetroText(
                text = "minute",
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.secondaryText
            )
            MetroSlider(
                value = minute / 59f,
                onValueChange = { minute = (it * 59f).roundToInt().coerceIn(0, 59) }
            )
        }
    }
}
