package com.metro.clock.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.metro.clock.R
import com.metro.clock.timers.TimerLogic
import com.metro.clock.timers.TimerState
import com.metro.system.MetroTileTemporalRender
import com.metro.ui.MetroEmptyState
import com.metro.ui.MetroListItem
import com.metro.ui.MetroTheme

@Composable
fun TimersScreen(
    state: ClockState,
    modifier: Modifier = Modifier,
) {
    @Suppress("UNUSED_VARIABLE")
    val generation = state.generation
    val tick = rememberElapsedRealtimeTick(250)

    if (state.timers.isEmpty()) {
        Column(modifier = modifier.fillMaxSize()) {
            MetroEmptyState(message = stringResource(R.string.timer_empty))
        }
        return
    }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(state.timers, key = { it.id }) { timer ->
            val remaining = TimerLogic.remainingMillis(timer, tick)
            val display = MetroTileTemporalRender.formatDuration(remaining, roundUp = true)
            val timerState = TimerState.fromName(timer.state)
            val suffix = when (timerState) {
                TimerState.PAUSED -> "  ·  ${stringResource(R.string.timer_paused)}"
                TimerState.FINISHED -> "  ·  ${stringResource(R.string.timer_finished)}"
                else -> ""
            }
            MetroListItem(
                title = timer.label,
                subtitle = "$display$suffix",
                titleColor = if (timerState == TimerState.FINISHED) MetroTheme.colors.accent else null,
                onClick = { state.openTimerDetail(timer.id) },
            )
        }
    }
}
