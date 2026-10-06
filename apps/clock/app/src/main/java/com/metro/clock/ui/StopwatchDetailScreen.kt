package com.metro.clock.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.clock.R
import com.metro.clock.data.StopwatchLapEntity
import com.metro.clock.stopwatch.StopwatchLogic
import com.metro.system.MetroTileTemporalRender
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroCircleIconButton
import com.metro.ui.MetroPageHeader
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroText
import com.metro.ui.MetroTextBox
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

@Composable
fun StopwatchDetailScreen(
    state: ClockState,
    stopwatchId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val generation = state.generation
    val stopwatch = state.stopwatch(stopwatchId)
    if (stopwatch == null) {
        LaunchedEffect(stopwatchId) { onBack() }
        return
    }
    val tick = rememberElapsedRealtimeTick(100)
    val elapsed = StopwatchLogic.elapsedMillis(stopwatch, tick)
    val display = MetroTileTemporalRender.formatStopwatchPrecise(elapsed)

    var laps by remember { mutableStateOf<List<StopwatchLapEntity>>(emptyList()) }
    var renaming by remember { mutableStateOf(false) }
    var renameValue by remember { mutableStateOf(stopwatch.name) }
    LaunchedEffect(stopwatchId, generation) {
        state.laps(stopwatchId) { laps = it }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
    ) {
        item {
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
        }
        item { MetroPageHeader(title = stopwatch.name) }
        item {
            MetroText(text = display, style = MetroTextStyle.PageTitle)
            MetroText(
                text = if (stopwatch.running) {
                    stringResource(R.string.stopwatch_running)
                } else {
                    stringResource(R.string.stopwatch_paused)
                },
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.secondaryText,
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetroBorderButton(
                    text = if (stopwatch.running) {
                        stringResource(R.string.stopwatch_pause)
                    } else {
                        stringResource(R.string.stopwatch_start)
                    },
                    onClick = {
                        if (stopwatch.running) state.pauseStopwatch(stopwatch.id) else state.startStopwatch(stopwatch.id)
                    },
                )
                MetroBorderButton(
                    text = stringResource(R.string.stopwatch_lap),
                    enabled = stopwatch.running,
                    onClick = { state.lapStopwatch(stopwatch.id) },
                )
                MetroBorderButton(
                    text = stringResource(R.string.stopwatch_reset),
                    onClick = { state.resetStopwatch(stopwatch.id) },
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetroBorderButton(
                    text = stringResource(R.string.stopwatch_rename),
                    onClick = { renaming = !renaming },
                )
                MetroBorderButton(
                    text = stringResource(R.string.stopwatch_delete),
                    onClick = { state.deleteStopwatch(stopwatch.id) },
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        if (renaming) {
            item {
                MetroTextBox(
                    value = renameValue,
                    onValueChange = { renameValue = it },
                    placeholder = stringResource(R.string.stopwatch_name_placeholder),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                MetroBorderButton(
                    text = stringResource(R.string.done),
                    onClick = {
                        state.renameStopwatch(stopwatch.id, renameValue)
                        renaming = false
                    },
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        item {
            ClockSectionHeader(
                text = stringResource(R.string.stopwatch_laps),
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        if (laps.isEmpty()) {
            item {
                MetroText(
                    text = stringResource(R.string.stopwatch_no_laps),
                    style = MetroTextStyle.ListItemSubtitle,
                    color = MetroTheme.colors.secondaryText,
                )
            }
        } else {
            items(laps, key = { it.id }) { lap ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MetroText(
                        text = stringResource(R.string.stopwatch_lap_number, lap.lapNumber),
                        style = MetroTextStyle.ListItemSubtitle,
                        color = MetroTheme.colors.secondaryText,
                        modifier = Modifier.weight(1f),
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        MetroText(
                            text = MetroTileTemporalRender.formatStopwatchPrecise(lap.lapDurationMillis),
                            style = MetroTextStyle.ListItemTitle,
                        )
                        MetroText(
                            text = MetroTileTemporalRender.formatStopwatchPrecise(lap.totalDurationMillis),
                            style = MetroTextStyle.ListItemSubtitle,
                            color = MetroTheme.colors.secondaryText,
                        )
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(48.dp)) }
    }
}
