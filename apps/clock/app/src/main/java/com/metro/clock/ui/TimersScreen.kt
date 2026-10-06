package com.metro.clock.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.clock.R
import com.metro.clock.timers.TimerLogic
import com.metro.clock.timers.TimerState
import com.metro.system.MetroTileTemporalRender
import com.metro.ui.MetroEmptyState
import com.metro.ui.MetroListItem
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroClickable

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
            val actionLabel = when (timerState) {
                TimerState.RUNNING -> stringResource(R.string.timer_pause)
                TimerState.PAUSED -> stringResource(R.string.timer_resume)
                else -> stringResource(R.string.timer_start)
            }
            MetroListItem(
                title = timer.label,
                subtitle = "$display · ${timerState.name.lowercase()}",
                trailing = {
                    MetroText(
                        text = actionLabel,
                        style = MetroTextStyle.Body,
                        color = MetroTheme.colors.accent,
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .metroClickable {
                                when (timerState) {
                                    TimerState.RUNNING -> state.pauseTimer(timer.id)
                                    TimerState.PAUSED -> state.resumeTimer(timer.id)
                                    else -> state.startTimer(timer.id)
                                }
                            },
                    )
                },
                onClick = { state.openTimerDetail(timer.id) },
            )
        }
    }
}
