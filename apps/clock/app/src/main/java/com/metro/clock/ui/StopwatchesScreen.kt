package com.metro.clock.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.metro.clock.R
import com.metro.clock.stopwatch.StopwatchLogic
import com.metro.system.MetroTileTemporalRender
import com.metro.ui.MetroEmptyState
import com.metro.ui.MetroListItem

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
            val display = MetroTileTemporalRender.formatStopwatchPrecise(elapsed)
            val suffix = if (stopwatch.running) "" else "  ·  ${stringResource(R.string.stopwatch_paused)}"
            MetroListItem(
                title = stopwatch.name,
                subtitle = "$display$suffix",
                onClick = { state.openStopwatchDetail(stopwatch.id) },
            )
        }
    }
}
