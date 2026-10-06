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
import com.metro.clock.stopwatch.StopwatchLogic
import com.metro.ui.MetroEmptyState
import com.metro.ui.MetroListItem
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroClickable

@Composable
fun StopwatchesScreen(
    state: ClockState,
    modifier: Modifier = Modifier,
) {
    @Suppress("UNUSED_VARIABLE")
    val generation = state.generation
    val tick = rememberElapsedRealtimeTick(250)

    if (state.stopwatches.isEmpty()) {
        Column(modifier = modifier.fillMaxSize()) {
            MetroEmptyState(message = stringResource(R.string.stopwatch_empty))
        }
        return
    }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(state.stopwatches, key = { it.id }) { stopwatch ->
            val elapsed = StopwatchLogic.elapsedMillis(stopwatch, tick)
            val display = com.metro.system.MetroTileTemporalRender.formatDuration(elapsed, roundUp = false)
            val status = if (stopwatch.running) {
                stringResource(R.string.stopwatch_running)
            } else {
                stringResource(R.string.stopwatch_paused)
            }
            MetroListItem(
                title = stopwatch.name,
                subtitle = "$display · $status",
                trailing = {
                    MetroText(
                        text = if (stopwatch.running) {
                            stringResource(R.string.stopwatch_pause)
                        } else {
                            stringResource(R.string.stopwatch_start)
                        },
                        style = MetroTextStyle.Body,
                        color = MetroTheme.colors.accent,
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .metroClickable {
                                if (stopwatch.running) {
                                    state.pauseStopwatch(stopwatch.id)
                                } else {
                                    state.startStopwatch(stopwatch.id)
                                }
                            },
                    )
                },
                onClick = { state.openStopwatchDetail(stopwatch.id) },
            )
        }
    }
}
