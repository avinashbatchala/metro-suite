package com.metro.calendar.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metro.calendar.R
import com.metro.calendar.data.CalendarEvent
import com.metro.calendar.data.CalendarLogic
import com.metro.calendar.data.CalendarPresentation
import com.metro.calendar.data.CalendarView
import com.metro.ui.LocalMetroSubpageExit
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroAppBarMenuItem
import com.metro.ui.MetroColors
import com.metro.ui.MetroDimens
import com.metro.ui.MetroListItem
import com.metro.ui.MetroPivotTitleWindow
import com.metro.ui.MetroSubpageHost
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroClickable
import com.metro.ui.metroNavBarPadding
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DayAnchorEpochDay = LocalDate.of(1970, 1, 1).toEpochDay()
private const val DayPageCount = 200_000
private const val WeekPageCount = 30_000
private const val MonthRangeMonths = 3_000
private const val YearRangeYears = 400
private val MonthAnchor = LocalDate.of(1970, 1, 1)
private const val MonthAnchorYear = 1970
private const val YearAnchor = 1970

@Composable
fun CalendarShell(
    state: CalendarState,
    onRequestReadPermission: () -> Unit,
    onRequestWritePermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    @Suppress("UNUSED_VARIABLE")
    val generation = state.generation

    MetroSubpageHost(
        route = state.route,
        isRoot = { it is CalendarRoute.Root },
        parentOf = { route ->
            when (route) {
                CalendarRoute.Settings -> CalendarRoute.Root
                CalendarRoute.AddSubscription -> CalendarRoute.Settings
                is CalendarRoute.SubscriptionDetail -> CalendarRoute.Settings
                CalendarRoute.EventDetail -> CalendarRoute.Root
                CalendarRoute.EventEdit -> CalendarRoute.EventDetail
                CalendarRoute.EventEditDetails -> CalendarRoute.EventEdit
                CalendarRoute.Root -> CalendarRoute.Root
            }
        },
        onGoBack = state::routeBack,
        modifier = modifier,
        rootContent = {
            CalendarRoot(
                state = state,
                onRequestReadPermission = onRequestReadPermission,
                onRequestWritePermission = onRequestWritePermission,
                modifier = Modifier.fillMaxSize(),
            )
        },
        subpageContent = { route ->
            val exit = LocalMetroSubpageExit.current
            when (route) {
                CalendarRoute.Settings -> SettingsScreen(
                    state = state,
                    onRequestRead = onRequestReadPermission,
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

                CalendarRoute.EventDetail -> {
                    val event = state.eventDetail
                    if (event != null) {
                        EventDetailScreen(
                            event = event,
                            attendees = state.eventAttendees,
                            onBack = { exit?.invoke() ?: state.routeBack() },
                            onEdit = { state.openEditEvent() },
                            onDelete = { state.deleteEvent(event.id) },
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        LaunchedEffect(Unit) { exit?.invoke() ?: state.routeBack() }
                    }
                }

                CalendarRoute.EventEdit, CalendarRoute.EventEditDetails -> EventEditScreen(
                    state = state,
                    advanced = route == CalendarRoute.EventEditDetails,
                    onBack = {
                        if (route == CalendarRoute.EventEditDetails) state.closeEditorDetails()
                        else exit?.invoke() ?: state.routeBack()
                    },
                    onMoreDetails = state::openEditorDetails,
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
    onRequestReadPermission: () -> Unit,
    onRequestWritePermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .metroNavBarPadding(),
    ) {
        when {
            !state.permissionsChecked -> Unit
            !state.hasAnySource -> OnboardingEmptyState(
                onAllowDevice = onRequestReadPermission,
                onSubscribe = state::openAddSubscription,
                modifier = Modifier.fillMaxSize(),
            )
            else -> Column(modifier = Modifier.fillMaxSize()) {
                if (state.presentation == CalendarPresentation.Agenda) {
                    AgendaArea(state = state)
                } else {
                    AnimatedContent(
                        targetState = state.view,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        modifier = Modifier.fillMaxSize(),
                        label = "calendarView",
                    ) { view ->
                        PivotViewHost(state = state, view = view)
                    }
                }
            }
        }

        if (state.hasAnySource) {
            MetroAppBar(
                icons = listOf(
                    MetroAppBarIcon(
                        label = stringResource(R.string.today),
                        onClick = state::goToToday,
                        icon = { color -> TodayDateBubbleIcon(color) },
                    ),
                    MetroAppBarIcon(
                        type = MetroSystemIconType.Add,
                        label = stringResource(R.string.new_event),
                        onClick = { state.openNewEvent() },
                    ),
                    MetroAppBarIcon(
                        type = MetroSystemIconType.CalendarView,
                        label = stringResource(R.string.view),
                        onClick = { state.viewMenuOpen = true },
                    ),
                ),
                menuItems = buildList {
                    if (state.view == CalendarView.Day || state.view == CalendarView.Week) {
                        add(
                            MetroAppBarMenuItem(
                                text = stringResource(
                                    if (state.presentation == CalendarPresentation.Agenda) {
                                        R.string.show_calendar
                                    } else {
                                        R.string.show_agenda
                                    },
                                ),
                                onClick = state::togglePresentation,
                            ),
                        )
                    }
                    add(
                        MetroAppBarMenuItem(
                            text = stringResource(R.string.settings),
                            onClick = state::openSettings,
                        ),
                    )
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        ViewSelector(state = state)
        EventLongPressMenu(state = state)
        InlineStatus(state = state)

        if (state.requestWrite) {
            LaunchedEffect(state.requestWrite) {
                onRequestWritePermission()
                state.consumeWriteRequest()
            }
        }
    }
}

@Composable
private fun AgendaArea(state: CalendarState) {
    Column(modifier = Modifier.fillMaxSize()) {
        MetroText(
            text = stringResource(
                if (state.view == CalendarView.Week) R.string.this_week else R.string.agenda,
            ).uppercase(),
            style = MetroTextStyle.AppTitle,
            color = MetroTheme.colors.secondaryText,
            modifier = Modifier.padding(
                start = MetroDimens.ScreenHorizontalMargin,
                top = 8.dp,
            ),
        )
        AgendaScreen(
            buckets = state.agendaBuckets,
            loadFailed = state.loadFailed,
            scrollRequestId = state.scrollToNowRequestId,
            targetEpochDay = state.selectedEpochDay,
            onEventClick = state::openEventDetail,
            onEventLongClick = state::openEventMenu,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Per-view pager: overline + context pivot + content, all driven by the pager's current page. */
private class ViewPivot(
    val pageCount: Int,
    val pageFor: (Long) -> Int,
    val epochDayFor: (Int) -> Long,
    val overlineFor: (Int) -> String,
    val titleFor: (Int) -> String,
)

@Composable
private fun PivotViewHost(
    state: CalendarState,
    view: CalendarView,
) {
    val locale = remember { Locale.getDefault() }
    val today = remember { CalendarLogic.todayEpochDay() }
    val weekAnchor = remember(locale) {
        LocalDate.of(1970, 1, 1)
            .with(java.time.temporal.TemporalAdjusters.previousOrSame(CalendarLogic.firstDayOfWeek(locale)))
            .toEpochDay()
    }
    val monthShort = remember(locale) { DateTimeFormatter.ofPattern("MMM d", locale) }
    val yearOverline = stringResource(R.string.year_overline)

    val spec = remember(view, locale, today, weekAnchor, yearOverline) {
        when (view) {
            CalendarView.Day -> ViewPivot(
                pageCount = DayPageCount,
                pageFor = { day -> (day - DayAnchorEpochDay).toInt() },
                epochDayFor = { page -> DayAnchorEpochDay + page.toLong() },
                overlineFor = { page -> CalendarLogic.dateHeaderLabel(DayAnchorEpochDay + page.toLong(), locale = locale) },
                titleFor = { page -> CalendarLogic.dayNameLower(DayAnchorEpochDay + page.toLong(), locale = locale) },
            )

            CalendarView.Week -> {
                val todayPage = Math.floorDiv((today - weekAnchor).toInt(), 7)
                ViewPivot(
                    pageCount = WeekPageCount,
                    pageFor = { day -> Math.floorDiv((day - weekAnchor).toInt(), 7) },
                    epochDayFor = { page -> weekAnchor + page.toLong() * 7L },
                    overlineFor = { page ->
                        val date = LocalDate.ofEpochDay(weekAnchor + page.toLong() * 7L)
                        "${date.month.getDisplayName(java.time.format.TextStyle.FULL, locale).uppercase(locale)} ${date.year}"
                    },
                    titleFor = { page ->
                        when (page - todayPage) {
                            0 -> "this week"
                            1 -> "next week"
                            -1 -> "last week"
                            else -> "week of " +
                                LocalDate.ofEpochDay(weekAnchor + page.toLong() * 7L).format(monthShort)
                        }
                    },
                )
            }

            CalendarView.Month -> ViewPivot(
                pageCount = MonthRangeMonths,
                pageFor = { day ->
                    val date = LocalDate.ofEpochDay(day)
                    (date.year - MonthAnchorYear) * 12 + (date.monthValue - 1)
                },
                epochDayFor = { page -> MonthAnchor.plusMonths(page.toLong()).toEpochDay() },
                overlineFor = { page -> MonthAnchor.plusMonths(page.toLong()).year.toString() },
                titleFor = { page ->
                    MonthAnchor.plusMonths(page.toLong())
                        .month.getDisplayName(java.time.format.TextStyle.FULL, locale).lowercase(locale)
                },
            )

            CalendarView.Year -> ViewPivot(
                pageCount = YearRangeYears,
                pageFor = { day -> (LocalDate.ofEpochDay(day).year - YearAnchor) },
                epochDayFor = { page -> LocalDate.of(page + YearAnchor, 1, 1).toEpochDay() },
                overlineFor = { yearOverline },
                titleFor = { page -> (page + YearAnchor).toString() },
            )
        }
    }

    val pagerState = rememberPagerState(
        initialPage = spec.pageFor(state.selectedEpochDay),
        pageCount = { spec.pageCount },
    )
    val scope = rememberCoroutineScope()

    LaunchedEffect(pagerState, view) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            state.selectDateFromPager(mergeIntoUnit(view, spec.epochDayFor(page), state.selectedEpochDay))
        }
    }
    LaunchedEffect(state.selectedEpochDay, view) {
        val target = spec.pageFor(state.selectedEpochDay)
        if (pagerState.currentPage != target) pagerState.scrollToPage(target)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        val currentPage = pagerState.currentPage
        MetroText(
            text = spec.overlineFor(currentPage),
            style = MetroTextStyle.SectionHeader,
            color = MetroTheme.colors.secondaryText,
            maxLines = 1,
            modifier = Modifier.padding(
                start = MetroDimens.ScreenHorizontalMargin,
                top = 6.dp,
            ),
        )
        MetroPivotTitleWindow(
            pageCount = spec.pageCount,
            selectedPage = currentPage,
            titleFor = spec.titleFor,
            onSelect = { page -> scope.launch { pagerState.animateScrollToPage(page) } },
            modifier = Modifier.padding(vertical = 6.dp),
        )
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            beyondViewportPageCount = 1,
            verticalAlignment = Alignment.Top,
        ) { page ->
            PivotPage(state = state, view = view, epochDay = spec.epochDayFor(page))
        }
    }
}

@Composable
private fun PivotPage(
    state: CalendarState,
    view: CalendarView,
    epochDay: Long,
) {
    when (view) {
        CalendarView.Day -> DayScreen(
            allDayEvents = state.allDayForDay(epochDay),
            timeline = state.timeline(epochDay),
            weather = if (epochDay == CalendarLogic.todayEpochDay()) state.weather else null,
            loadFailed = state.loadFailed,
            use24Hour = state.use24Hour(),
            scrollToNow = epochDay == CalendarLogic.todayEpochDay(),
            scrollRequestId = state.scrollToNowRequestId,
            onEventClick = state::openEventDetail,
            onEventLongClick = state::openEventMenu,
            onQuickEvent = { startMinute, title -> state.quickCreateEvent(epochDay, startMinute, title) },
        )

        CalendarView.Week -> WeekScreen(
            week = state.week,
            miniMonth = state.weekMiniMonth(epochDay),
            weather = if (epochDay <= CalendarLogic.todayEpochDay() && CalendarLogic.todayEpochDay() < epochDay + 7) state.weather else null,
            paneEpochDay = state.detailPaneEpochDay,
            use24Hour = state.use24Hour(),
            onSelectDay = state::selectDay,
            onOpenDay = state::openDay,
            onEventClick = state::openEventDetail,
            onEventLongClick = state::openEventMenu,
            eventTime = state::eventTimeLabel,
        )

        CalendarView.Month -> MonthScreen(
            monthEpochDay = epochDay,
            grid = state.monthGrid(epochDay),
            weekdayLabels = state.monthWeekdayLabels,
            paneEpochDay = state.detailPaneEpochDay,
            paneEvents = state.detailPaneEpochDay?.let { state.eventsForDay(it) }.orEmpty(),
            paneOverline = state.detailPaneEpochDay?.let { state.dateOverline(it) },
            use24Hour = state.use24Hour(),
            onSelectDay = state::selectDay,
            onOpenDay = state::openDay,
            onEventClick = state::openEventDetail,
            onEventLongClick = state::openEventMenu,
            eventTime = state::eventTimeLabel,
            eventDuration = state::eventDurationLabel,
        )

        CalendarView.Year -> YearScreen(
            months = state.yearMonths(epochDay),
            onSelectMonth = state::openMonth,
        )
    }
}

@Composable
private fun OnboardingEmptyState(
    onAllowDevice: () -> Unit,
    onSubscribe: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(top = 24.dp)) {
        MetroText(
            text = stringResource(R.string.app_name).uppercase(),
            style = MetroTextStyle.AppTitle,
            color = MetroTheme.colors.secondaryText,
            modifier = Modifier.padding(start = MetroDimens.ScreenHorizontalMargin),
        )
        MetroText(
            text = stringResource(R.string.no_calendars_yet),
            style = MetroTextStyle.ListItemTitle,
            color = MetroTheme.colors.primaryText,
            modifier = Modifier.padding(
                start = MetroDimens.ScreenHorizontalMargin,
                top = 16.dp,
                bottom = 8.dp,
            ),
        )
        MetroListItem(
            title = stringResource(R.string.allow_device_calendar),
            onClick = onAllowDevice,
        )
        MetroListItem(
            title = stringResource(R.string.subscribe_to_calendar),
            onClick = onSubscribe,
        )
    }
}

/** WP8.1 View selector popup: day / week / month / year. */
@Composable
private fun ViewSelector(state: CalendarState) {
    if (!state.viewMenuOpen) return
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .metroClickable { state.viewMenuOpen = false },
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MetroColors.DarkSecondarySurface)
                .metroNavBarPadding()
                .padding(vertical = 8.dp),
        ) {
            CalendarView.entries.forEach { candidate ->
                val active = candidate == state.view
                MetroText(
                    text = stringResource(labelOf(candidate)),
                    style = MetroTextStyle.ListItemTitle,
                    color = if (active) MetroTheme.colors.accent else MetroTheme.colors.primaryText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .metroClickable { state.selectView(candidate) }
                        .padding(start = MetroDimens.ScreenHorizontalMargin).padding(vertical = 14.dp),
                )
            }
        }
    }
}

/** WP8.1 long-press menu on an editable event: edit / delete. */
@Composable
private fun EventLongPressMenu(state: CalendarState) {
    val target = state.eventMenuTarget ?: return
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .metroClickable { state.closeEventMenu() },
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MetroColors.DarkSecondarySurface)
                .metroNavBarPadding()
                .padding(vertical = 8.dp),
        ) {
            MetroText(
                text = stringResource(R.string.edit_event),
                style = MetroTextStyle.ListItemTitle,
                color = MetroTheme.colors.primaryText,
                modifier = Modifier
                    .fillMaxWidth()
                    .metroClickable {
                        state.closeEventMenu()
                        state.openEventDetail(target)
                        state.openEditEvent()
                    }
                    .padding(start = MetroDimens.ScreenHorizontalMargin).padding(vertical = 14.dp),
            )
            MetroText(
                text = stringResource(R.string.delete_event),
                style = MetroTextStyle.ListItemTitle,
                color = MetroTheme.colors.primaryText,
                modifier = Modifier
                    .fillMaxWidth()
                    .metroClickable {
                        state.closeEventMenu()
                        state.deleteEvent(target.id)
                    }
                    .padding(start = MetroDimens.ScreenHorizontalMargin).padding(vertical = 14.dp),
            )
        }
    }
}

/** Inline Metro status line (replaces Android Toasts). */
@Composable
private fun InlineStatus(state: CalendarState) {
    val message = state.statusMessage ?: return
    LaunchedEffect(message) {
        kotlinx.coroutines.delay(4000)
        state.clearStatus()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 96.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        MetroText(
            text = message,
            style = MetroTextStyle.Body,
            color = MetroTheme.colors.primaryText,
            modifier = Modifier
                .background(MetroColors.DarkSecondarySurface)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun TodayDateBubbleIcon(color: Color) {
    val today = LocalDate.now()
    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMM", Locale.getDefault()) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        BasicText(
            text = today.dayOfMonth.toString(),
            style = TextStyle(
                fontFamily = MetroTheme.fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                lineHeight = 13.sp,
                color = color,
                textAlign = TextAlign.Center,
            ),
        )
        BasicText(
            text = monthFormatter.format(today).uppercase(Locale.getDefault()).take(3),
            style = TextStyle(
                fontFamily = MetroTheme.fontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 8.sp,
                lineHeight = 9.sp,
                color = color,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

private fun mergeIntoUnit(view: CalendarView, unitEpochDay: Long, currentEpochDay: Long): Long {
    val unit = LocalDate.ofEpochDay(unitEpochDay)
    val current = LocalDate.ofEpochDay(currentEpochDay)
    return when (view) {
        CalendarView.Day -> unitEpochDay
        CalendarView.Week -> {
            val offset = (current.dayOfWeek.value - unit.dayOfWeek.value + 7) % 7
            unitEpochDay + offset
        }
        CalendarView.Month -> {
            val day = current.dayOfMonth.coerceAtMost(unit.lengthOfMonth())
            unit.withDayOfMonth(day).toEpochDay()
        }
        CalendarView.Year -> {
            val day = current.dayOfMonth.coerceAtMost(LocalDate.of(unit.year, current.monthValue, 1).lengthOfMonth())
            LocalDate.of(unit.year, current.monthValue, day).toEpochDay()
        }
    }
}

private fun labelOf(view: CalendarView): Int = when (view) {
    CalendarView.Day -> R.string.view_day
    CalendarView.Week -> R.string.view_week
    CalendarView.Month -> R.string.view_month
    CalendarView.Year -> R.string.view_year
}
