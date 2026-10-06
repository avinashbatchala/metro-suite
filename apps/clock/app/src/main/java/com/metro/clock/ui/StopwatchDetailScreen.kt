package com.metro.clock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.clock.R
import com.metro.clock.data.StopwatchLapEntity
import com.metro.clock.stopwatch.StopwatchLogic
import com.metro.system.MetroTileTemporalRender
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroAppBarMenuItem
import com.metro.ui.MetroColors
import com.metro.ui.MetroDimens
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroTextBox
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroClickable
import com.metro.ui.metroNavBarPadding

/** WP-style stopwatch detail: large elapsed time, controls in the ApplicationBar, lap list. */
@Composable
fun StopwatchDetailScreen(
    state: ClockState,
    stopwatchId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val generation = state.generation
    val context = LocalContext.current
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

    Box(modifier = modifier.fillMaxSize().background(MetroTheme.colors.background)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = MetroDimens.ScreenHorizontalMargin),
        ) {
            item {
                MetroText(
                    text = stopwatch.name,
                    style = MetroTextStyle.SectionHeader,
                    color = MetroTheme.colors.secondaryText,
                    modifier = Modifier.padding(top = 8.dp),
                )
                MetroText(text = display, style = MetroTextStyle.PageTitle)
                if (!stopwatch.running) {
                    MetroText(
                        text = stringResource(R.string.stopwatch_paused),
                        style = MetroTextStyle.ListItemSubtitle,
                        color = MetroTheme.colors.secondaryText,
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            item {
                ClockSectionHeader(text = stringResource(R.string.stopwatch_laps))
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
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
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
            item { Spacer(modifier = Modifier.height(96.dp)) }
        }

        MetroAppBar(
            icons = listOf(
                MetroAppBarIcon(
                    type = if (stopwatch.running) MetroSystemIconType.Pause else MetroSystemIconType.Play,
                    label = if (stopwatch.running) {
                        stringResource(R.string.stopwatch_pause)
                    } else {
                        stringResource(R.string.stopwatch_start)
                    },
                    onClick = {
                        if (stopwatch.running) state.pauseStopwatch(stopwatch.id) else state.startStopwatch(stopwatch.id)
                    },
                ),
                MetroAppBarIcon(
                    type = MetroSystemIconType.Add,
                    label = stringResource(R.string.stopwatch_lap),
                    enabled = stopwatch.running,
                    onClick = { state.lapStopwatch(stopwatch.id) },
                ),
                MetroAppBarIcon(
                    type = MetroSystemIconType.Refresh,
                    label = stringResource(R.string.stopwatch_reset),
                    onClick = { state.resetStopwatch(stopwatch.id) },
                ),
            ),
            menuItems = listOf(
                MetroAppBarMenuItem(stringResource(R.string.pin_to_start)) {
                    ClockPin.pin(context, "stopwatch:${stopwatch.id}")
                },
                MetroAppBarMenuItem(stringResource(R.string.stopwatch_rename)) {
                    renameValue = stopwatch.name
                    renaming = true
                },
                MetroAppBarMenuItem(stringResource(R.string.stopwatch_delete)) {
                    state.deleteStopwatch(stopwatch.id)
                },
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        if (renaming) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier.fillMaxSize().metroClickable { renaming = false },
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(MetroColors.DarkSecondarySurface)
                        .metroNavBarPadding()
                        .padding(vertical = 12.dp),
                ) {
                    MetroTextBox(
                        value = renameValue,
                        onValueChange = { renameValue = it },
                        placeholder = stringResource(R.string.stopwatch_name_placeholder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MetroDimens.ScreenHorizontalMargin, vertical = 8.dp),
                    )
                    MetroText(
                        text = stringResource(R.string.done),
                        style = MetroTextStyle.ListItemTitle,
                        color = MetroTheme.colors.accent,
                        modifier = Modifier
                            .align(Alignment.End)
                            .metroClickable {
                                state.renameStopwatch(stopwatch.id, renameValue)
                                renaming = false
                            }
                            .padding(horizontal = MetroDimens.ScreenHorizontalMargin, vertical = 10.dp),
                    )
                }
            }
        }
    }
}
