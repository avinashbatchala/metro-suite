package com.metro.clock.ui

import android.content.Context
import android.os.SystemClock
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.system.MetroIntents
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import kotlinx.coroutines.delay

/** Local tick of the monotonic clock while a screen is visible (never persisted, never broadcast). */
@Composable
fun rememberElapsedRealtimeTick(intervalMs: Long = 250L): Long {
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(intervalMs) {
        while (true) {
            now = SystemClock.elapsedRealtime()
            delay(intervalMs)
        }
    }
    return now
}

@Composable
fun rememberWallClockTick(intervalMs: Long = 1000L): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(intervalMs) {
        while (true) {
            now = System.currentTimeMillis()
            delay(intervalMs)
        }
    }
    return now
}

/** Minute-aligned wall clock: recomputes exactly on minute boundaries (world clock shows minutes). */
@Composable
fun rememberMinuteTick(): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            val toNextMinute = 60_000L - (now % 60_000L)
            delay(toNextMinute.coerceAtLeast(200L))
        }
    }
    return now
}

/** System 12/24-hour preference; recomputed on configuration change so it never goes stale. */
@Composable
fun rememberSystem24Hour(): Boolean {
    val context = androidx.compose.ui.platform.LocalContext.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    return remember(context, configuration) {
        android.text.format.DateFormat.is24HourFormat(context)
    }
}

/** Localized repeat summary for the alarm list / editor. */
@Composable
fun alarmRepeatSummary(mask: Int): String {
    val weekdays = com.metro.clock.alarms.AlarmSchedule.WEEKDAYS
    val weekends = com.metro.clock.alarms.AlarmSchedule.WEEKENDS
    return when (mask) {
        0 -> stringResource(com.metro.clock.R.string.alarm_repeat_only_once)
        weekdays -> stringResource(com.metro.clock.R.string.alarm_repeat_weekdays)
        weekends -> stringResource(com.metro.clock.R.string.alarm_repeat_weekends)
        weekdays or weekends -> stringResource(com.metro.clock.R.string.alarm_repeat_daily)
        else -> java.time.DayOfWeek.values()
            .filter { mask and (1 shl (it.value - 1)) != 0 }
            .joinToString(", ") { it.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault()) }
    }
}


@Composable
fun ClockSectionHeader(text: String, modifier: Modifier = Modifier) {
    MetroText(
        text = text.uppercase(),
        style = MetroTextStyle.SectionHeader,
        color = MetroTheme.colors.secondaryText,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
    )
}

object ClockPin {
    const val PACKAGE = "com.metro.clock"

    fun pin(context: Context, tileId: String) {
        MetroIntents.requestPinTile(context, PACKAGE, tileId)
    }

    fun pinWorld(context: Context, cityId: String) = pin(context, "world:$cityId")
}

@Composable
fun SecondaryText(text: String, modifier: Modifier = Modifier, color: Color = MetroTheme.colors.secondaryText) {
    MetroText(text = text, style = MetroTextStyle.ListItemSubtitle, color = color, modifier = modifier)
}
