package com.metro.clock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.metro.clock.R
import com.metro.ui.LocalMetroSubpageExit
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroAppBarTextButton
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroPivot
import com.metro.ui.MetroSubpageHost
import com.metro.ui.metroNavBarPadding

@Composable
fun ClockShell(
    state: ClockState,
    modifier: Modifier = Modifier,
) {
    @Suppress("UNUSED_VARIABLE")
    val generation = state.generation
    val context = LocalContext.current
    val pivotTitles = ClockPivot.entries.map { stringResource(it.titleRes) }
    val pagerState = rememberPagerState(
        initialPage = state.pivot.ordinal,
        pageCount = { ClockPivot.entries.size },
    )
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            state.selectPivot(ClockPivot.entries[page])
        }
    }
    LaunchedEffect(state.pivot) {
        val target = state.pivot.ordinal
        if (pagerState.currentPage != target) pagerState.animateScrollToPage(target)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .metroNavBarPadding(),
    ) {
        MetroSubpageHost(
            route = state.route,
            isRoot = { it == ClockRoute.Root },
            parentOf = { ClockRoute.Root },
            loadKeyOf = { subpageLoadKey(it) },
            onGoBack = state::closeRoute,
            modifier = Modifier.fillMaxSize(),
            rootContent = {
                MetroPivot(
                    titles = pivotTitles,
                    pagerState = pagerState,
                    header = { MetroAppTitle(title = stringResource(R.string.app_name)) },
                    onTitleClick = { state.selectPivot(ClockPivot.entries[it]) },
                    modifier = Modifier.fillMaxSize(),
                    pageContent = { page ->
                        val bottomPad = Modifier.padding(bottom = MetroAppBarDefaults.BarHeight)
                        when (ClockPivot.entries[page]) {
                            ClockPivot.Alarms -> AlarmsScreen(
                                state = state,
                                modifier = bottomPad,
                            )
                            ClockPivot.World -> WorldClockScreen(
                                state = state,
                                modifier = bottomPad,
                            )
                            ClockPivot.Timer -> TimersScreen(
                                state = state,
                                modifier = bottomPad,
                            )
                            ClockPivot.Stopwatch -> StopwatchesScreen(
                                state = state,
                                modifier = bottomPad,
                            )
                        }
                    },
                )
            },
            subpageContent = { route ->
                val exit = LocalMetroSubpageExit.current
                val onBack = { exit?.invoke() ?: state.closeRoute() }
                when (route) {
                    ClockRoute.Root -> Unit
                    is ClockRoute.AlarmEdit -> AlarmEditScreen(
                        state = state,
                        alarmId = route.alarmId,
                        onBack = onBack,
                        modifier = Modifier.fillMaxSize(),
                    )
                    ClockRoute.CityPicker -> CityPickerScreen(
                        state = state,
                        onBack = onBack,
                        modifier = Modifier.fillMaxSize(),
                    )
                    ClockRoute.TimerEdit -> TimerEditScreen(
                        state = state,
                        onBack = onBack,
                        modifier = Modifier.fillMaxSize(),
                    )
                    is ClockRoute.TimerDetail -> TimerDetailScreen(
                        state = state,
                        timerId = route.timerId,
                        onBack = onBack,
                        modifier = Modifier.fillMaxSize(),
                    )
                    is ClockRoute.StopwatchDetail -> StopwatchDetailScreen(
                        state = state,
                        stopwatchId = route.stopwatchId,
                        onBack = onBack,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            },
        )

        val showAppBar = state.route == ClockRoute.Root
        MetroAppBar(
            visible = showAppBar,
            textButtons = when (state.pivot) {
                ClockPivot.Alarms -> listOf(
                    MetroAppBarTextButton(stringResource(R.string.new_alarm)) { state.openAlarmEdit(null) },
                )
                ClockPivot.World -> listOf(
                    MetroAppBarTextButton(stringResource(R.string.add_city)) { state.openCityPicker() },
                )
                ClockPivot.Timer -> listOf(
                    MetroAppBarTextButton(stringResource(R.string.new_timer)) { state.openTimerEdit() },
                )
                ClockPivot.Stopwatch -> listOf(
                    MetroAppBarTextButton(stringResource(R.string.new_stopwatch)) {
                        state.createStopwatch(context.getString(R.string.stopwatch_default_name))
                    },
                )
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

private fun subpageLoadKey(route: ClockRoute): Any = when (route) {
    ClockRoute.Root -> "Root"
    is ClockRoute.AlarmEdit -> "AlarmEdit:${route.alarmId}"
    ClockRoute.CityPicker -> "CityPicker"
    ClockRoute.TimerEdit -> "TimerEdit"
    is ClockRoute.TimerDetail -> "TimerDetail:${route.timerId}"
    is ClockRoute.StopwatchDetail -> "StopwatchDetail:${route.stopwatchId}"
}
