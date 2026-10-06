package com.metro.clock.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.clock.R
import com.metro.clock.timers.TimerLogic
import com.metro.clock.timers.TimerState
import com.metro.system.MetroTileTemporalRender
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroCircleIconButton
import com.metro.ui.MetroPageHeader
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

@Composable
fun TimerDetailScreen(
    state: ClockState,
    timerId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    @Suppress("UNUSED_VARIABLE")
    val generation = state.generation
    val timer = state.timer(timerId)
    if (timer == null) {
        LaunchedEffect(timerId) { onBack() }
        return
    }
    val tick = rememberElapsedRealtimeTick(200)
    val remaining = TimerLogic.remainingMillis(timer, tick)
    val display = MetroTileTemporalRender.formatDuration(remaining, roundUp = true)
    val timerState = TimerState.fromName(timer.state)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
    ) {
        Row(
            modifier = Modifier.padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MetroCircleIconButton(
                type = MetroSystemIconType.Back,
                onClick = onBack,
                contentDescription = stringResource(R.string.back),
            )
        }
        MetroPageHeader(title = timer.label)
        Spacer(modifier = Modifier.height(24.dp))
        MetroText(text = display, style = MetroTextStyle.PageTitle)
        MetroText(
            text = timerState.name.lowercase(),
            style = MetroTextStyle.ListItemSubtitle,
            color = MetroTheme.colors.secondaryText,
        )
        Spacer(modifier = Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            when (timerState) {
                TimerState.RUNNING -> MetroBorderButton(
                    text = stringResource(R.string.timer_pause),
                    onClick = { state.pauseTimer(timer.id) },
                )
                TimerState.PAUSED -> MetroBorderButton(
                    text = stringResource(R.string.timer_resume),
                    onClick = { state.resumeTimer(timer.id) },
                )
                else -> MetroBorderButton(
                    text = stringResource(R.string.timer_start),
                    onClick = { state.startTimer(timer.id) },
                )
            }
            MetroBorderButton(
                text = stringResource(R.string.timer_reset),
                onClick = { state.resetTimer(timer.id) },
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetroBorderButton(
                text = stringResource(R.string.timer_delete),
                onClick = { state.deleteTimer(timer.id) },
            )
        }
    }
}
