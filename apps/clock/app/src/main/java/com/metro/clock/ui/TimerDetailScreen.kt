package com.metro.clock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.clock.R
import com.metro.clock.timers.TimerLogic
import com.metro.clock.timers.TimerState
import com.metro.system.MetroTileTemporalRender
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroAppBarMenuItem
import com.metro.ui.MetroDimens
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

/** WP-style timer detail: large countdown, controls in the ApplicationBar. */
@Composable
fun TimerDetailScreen(
    state: ClockState,
    timerId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    @Suppress("UNUSED_VARIABLE")
    val generation = state.generation
    val context = LocalContext.current
    val timer = state.timer(timerId)
    if (timer == null) {
        LaunchedEffect(timerId) { onBack() }
        return
    }
    val tick = rememberElapsedRealtimeTick(200)
    val remaining = TimerLogic.remainingMillis(timer, tick)
    val display = MetroTileTemporalRender.formatDuration(remaining, roundUp = true)
    val timerState = TimerState.fromName(timer.state)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = MetroDimens.ScreenHorizontalMargin),
        ) {
            MetroText(
                text = timer.label,
                style = MetroTextStyle.SectionHeader,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.padding(top = 8.dp),
            )
            Spacer(modifier = Modifier.height(24.dp))
            MetroText(text = display, style = MetroTextStyle.PageTitle)
            if (timerState == TimerState.PAUSED || timerState == TimerState.FINISHED) {
                MetroText(
                    text = stringResource(
                        if (timerState == TimerState.FINISHED) R.string.timer_finished else R.string.timer_paused,
                    ),
                    style = MetroTextStyle.ListItemSubtitle,
                    color = if (timerState == TimerState.FINISHED) {
                        MetroTheme.colors.accent
                    } else {
                        MetroTheme.colors.secondaryText
                    },
                )
            }
        }

        MetroAppBar(
            icons = listOf(
                MetroAppBarIcon(
                    type = if (timerState == TimerState.RUNNING) {
                        MetroSystemIconType.Pause
                    } else {
                        MetroSystemIconType.Play
                    },
                    label = when (timerState) {
                        TimerState.RUNNING -> stringResource(R.string.timer_pause)
                        TimerState.PAUSED -> stringResource(R.string.timer_resume)
                        else -> stringResource(R.string.timer_start)
                    },
                    onClick = {
                        when (timerState) {
                            TimerState.RUNNING -> state.pauseTimer(timer.id)
                            TimerState.PAUSED -> state.resumeTimer(timer.id)
                            else -> state.restartTimer(timer.id)
                        }
                    },
                ),
                MetroAppBarIcon(
                    type = MetroSystemIconType.Refresh,
                    label = stringResource(R.string.timer_reset),
                    onClick = { state.resetTimer(timer.id) },
                ),
            ),
            menuItems = listOf(
                MetroAppBarMenuItem(stringResource(R.string.pin_to_start)) {
                    ClockPin.pin(context, "timer:${timer.id}")
                },
                MetroAppBarMenuItem(stringResource(R.string.timer_delete)) {
                    state.deleteTimer(timer.id)
                },
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
