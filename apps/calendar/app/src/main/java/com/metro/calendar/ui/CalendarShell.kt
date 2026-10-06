package com.metro.calendar.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.metro.calendar.R
import com.metro.calendar.data.CalendarPivot
import com.metro.ui.LocalMetroSubpageExit
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarMenuItem
import com.metro.ui.MetroAppBarTextButton
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroPagePivotLoad
import com.metro.ui.MetroPivot
import com.metro.ui.MetroSubpageHost
import com.metro.ui.metroNavBarPadding

@Composable
fun CalendarShell(
    state: CalendarState,
    modifier: Modifier = Modifier,
) {
    // Observe state so the shell recomposes on any model change.
    @Suppress("UNUSED_VARIABLE")
    val generation = state.generation

    MetroSubpageHost(
        route = state.route,
        isRoot = { it is CalendarRoute.Root },
        parentOf = { route ->
            when (route) {
                is CalendarRoute.SubscriptionDetail -> CalendarRoute.Calendars
                CalendarRoute.AddSubscription -> CalendarRoute.Calendars
                CalendarRoute.Calendars -> CalendarRoute.Root
                CalendarRoute.Root -> CalendarRoute.Root
            }
        },
        onGoBack = state::routeBack,
        modifier = modifier,
        rootContent = { CalendarRoot(state = state, modifier = Modifier.fillMaxSize()) },
        subpageContent = { route ->
            val exit = LocalMetroSubpageExit.current
            when (route) {
                CalendarRoute.Calendars -> CalendarsScreen(
                    state = state,
                    onBack = { exit?.invoke() ?: state.routeBack() },
                    modifier = Modifier.fillMaxSize(),
                )

                CalendarRoute.AddSubscription -> AddSubscriptionScreen(
                    state = state,
                    onBack = { exit?.invoke() ?: state.routeBack() },
                    modifier = Modifier.fillMaxSize(),
                )

                is CalendarRoute.SubscriptionDetail -> SubscriptionDetailScreen(
                    state = state,
                    subscriptionId = route.id,
                    onBack = { exit?.invoke() ?: state.routeBack() },
                    onRemoved = { exit?.invoke() ?: state.routeBack() },
                    modifier = Modifier.fillMaxSize(),
                )

                CalendarRoute.Root -> Unit
            }
        },
    )
}

@Composable
private fun CalendarRoot(
    state: CalendarState,
    modifier: Modifier = Modifier,
) {
    val pivotTitles = CalendarPivot.entries.map { it.title }
    val pagerState = rememberPagerState(
        initialPage = state.pivot.ordinal,
        pageCount = { CalendarPivot.entries.size },
    )

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            state.selectPivot(CalendarPivot.fromIndex(page))
        }
    }
    LaunchedEffect(state.pivot) {
        val target = state.pivot.ordinal
        if (pagerState.currentPage != target) {
            pagerState.animateScrollToPage(target)
        }
    }

    BackHandler(enabled = state.eventDetail != null && !state.eventDetailExiting) {
        state.beginCloseEventDetail()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .metroNavBarPadding(),
    ) {
        MetroPivot(
            titles = pivotTitles,
            pagerState = pagerState,
            header = { MetroAppTitle(title = stringResource(R.string.app_name)) },
            onTitleClick = { index -> state.selectPivot(CalendarPivot.fromIndex(index)) },
            pageContent = { page ->
                when (CalendarPivot.fromIndex(page)) {
                    CalendarPivot.Agenda -> AgendaScreen(
                        buckets = state.agendaBuckets,
                        usingDemoData = state.usingDemoData,
                        loadFailed = state.loadFailed,
                        scrollRequestId = state.agendaScrollRequestId,
                        targetEpochDay = state.selectedEpochDay,
                        onEventClick = state::openEventDetail,
                    )
                    CalendarPivot.Day -> DayScreen(
                        epochDay = state.selectedEpochDay,
                        allDayEvents = state.dayAllDayEvents,
                        hourSlots = state.dayHourSlots,
                        usingDemoData = state.usingDemoData,
                        loadFailed = state.loadFailed,
                        onEventClick = state::openEventDetail,
                        onPreviousDay = { state.shiftDay(-1) },
                        onNextDay = { state.shiftDay(1) },
                    )
                    CalendarPivot.Month -> MonthScreen(
                        epochDay = state.selectedEpochDay,
                        grid = state.monthGrid,
                        usingDemoData = state.usingDemoData,
                        loadFailed = state.loadFailed,
                        onSelectDay = state::selectDay,
                        onPreviousMonth = { state.shiftMonth(-1) },
                        onNextMonth = { state.shiftMonth(1) },
                    )
                }
            },
        )

        MetroAppBar(
            textButtons = listOf(
                MetroAppBarTextButton(
                    text = stringResource(R.string.today),
                    onClick = state::goToToday,
                ),
            ),
            menuItems = listOf(
                MetroAppBarMenuItem(
                    text = stringResource(R.string.calendars),
                    onClick = state::openCalendars,
                ),
                MetroAppBarMenuItem(
                    text = stringResource(R.string.sync_calendars),
                    onClick = state::syncNow,
                ),
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        // Read-only event detail overlay (Blueprint §Event detail).
        val detail = state.eventDetail
        if (detail != null) {
            MetroPagePivotLoad(
                modifier = Modifier.fillMaxSize(),
                loadKey = state.eventDetailEpoch,
                exiting = state.eventDetailExiting,
                onExitComplete = state::finishCloseEventDetail,
            ) {
                EventDetailScreen(
                    event = detail,
                    onBack = state::beginCloseEventDetail,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
