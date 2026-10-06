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
