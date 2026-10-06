package com.metro.calendar.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metro.calendar.R
import com.metro.calendar.data.CalendarLogic
import com.metro.calendar.data.CalendarView
import com.metro.ui.LocalMetroSubpageExit
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroAppBarMenuItem
import com.metro.ui.MetroPivotTitleWindow
import com.metro.ui.MetroSubpageHost
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroTheme
import com.metro.ui.MetroTransitions
import com.metro.ui.metroNavBarPadding
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.ln
import kotlin.math.round

private val DayAnchorEpochDay = LocalDate.of(1970, 1, 1).toEpochDay()
private const val DayPageCount = 200_000
private const val WeekPageCount = 30_000
private const val MonthRangeMonths = 3_000
private const val YearRangeYears = 400
private val MonthAnchor = LocalDate.of(1970, 1, 1)
private const val MonthAnchorYear = 1970
private const val YearAnchor = 1970

/** Net zoom required per pinch step. */
private const val PinchStepFactor = 1.25f

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
                CalendarRoute.EventDetail -> CalendarRoute.Root
                CalendarRoute.EventEdit -> CalendarRoute.EventDetail
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

                CalendarRoute.EventDetail -> {
                    val event = state.eventDetail
                    if (event != null) {
                        EventDetailScreen(
                            event = event,
                            onBack = { exit?.invoke() ?: state.routeBack() },
                            onEdit = { state.openEditEvent() },
                            onDelete = { state.deleteEvent(event.id) },
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        LaunchedEffect(Unit) { exit?.invoke() ?: state.routeBack() }
                    }
                }

                CalendarRoute.EventEdit -> EventEditScreen(
                    state = state,
                    onBack = { exit?.invoke() ?: state.routeBack() },
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
    // Target view previewed while a pinch is in flight (committed on release).
    var previewView by remember { mutableStateOf<CalendarView?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .metroNavBarPadding()
            .pinchToSwitchView(
                current = state.view,
                onPreview = { previewView = it },
                onCommit = { target ->
                    previewView = null
                    target?.let(state::selectView)
                },
            ),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CalendarViewTabs(
                view = state.view,
                previewView = previewView,
                onSelect = { state.selectView(it) },
            )

            AnimatedContent(
                targetState = state.view,
                transitionSpec = { viewTransition(initialState, targetState) },
                modifier = Modifier.fillMaxSize(),
                label = "calendarView",
            ) { view ->
                if (view == CalendarView.Agenda) {
                    AgendaScreen(
                        buckets = state.agendaBuckets,
                        usingDemoData = state.usingDemoData,
                        loadFailed = state.loadFailed,
                        scrollRequestId = state.agendaScrollRequestId,
                        targetEpochDay = state.selectedEpochDay,
                        onEventClick = state::openEventDetail,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    PivotViewHost(state = state, view = view)
                }
            }
        }

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
                    onClick = state::openNewEvent,
                ),
                MetroAppBarIcon(
                    type = MetroSystemIconType.List,
                    label = stringResource(R.string.agenda),
                    onClick = { state.selectView(CalendarView.Agenda) },
                    selected = state.view == CalendarView.Agenda,
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
    }
}

/**
 * View transition: the four zoom levels cross-fade + scale (coarser grows in, finer shrinks in)
 * to read as a zoom; agenda enters/exits with a short vertical slide (a distinct mode, not a zoom).
 */
private fun viewTransition(
    initial: CalendarView,
    target: CalendarView,
): androidx.compose.animation.ContentTransform {
    val initialIndex = scaleIndex(initial)
    val targetIndex = scaleIndex(target)
    val zoom = initialIndex >= 0 && targetIndex >= 0 && initial != target
    val spec = MetroTransitions.pivotTween<Float>()
    return if (zoom) {
        val coarser = targetIndex > initialIndex
        val enterScale = if (coarser) 0.92f else 1.08f
        val exitScale = if (coarser) 1.06f else 0.94f
        (fadeIn(spec) + scaleIn(spec, initialScale = enterScale)) togetherWith
            (fadeOut(spec) + scaleOut(spec, targetScale = exitScale))
    } else {
        val slideSpec = MetroTransitions.pivotTween<androidx.compose.ui.unit.IntOffset>()
        (fadeIn(spec) + slideInVertically(slideSpec) { height -> height / 14 }) togetherWith
            (fadeOut(spec) + slideOutVertically(slideSpec) { height -> -height / 14 })
    }
}

/** Per-view pager mapping between page index and a representative epoch day. */
private class ViewPivot(
    val pageCount: Int,
    val pageFor: (Long) -> Int,
    val epochDayFor: (Int) -> Long,
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
            .with(TemporalAdjusters.previousOrSame(CalendarLogic.firstDayOfWeek(locale)))
            .toEpochDay()
    }
    val monthShort = remember(locale) { DateTimeFormatter.ofPattern("MMM d", locale) }

    val spec = remember(view, locale, today, weekAnchor) {
        when (view) {
            CalendarView.Day -> ViewPivot(
                pageCount = DayPageCount,
                pageFor = { day -> (day - DayAnchorEpochDay).toInt() },
                epochDayFor = { page -> DayAnchorEpochDay + page.toLong() },
                titleFor = { page ->
                    CalendarLogic.dayNameLower(
                        DayAnchorEpochDay + page.toLong(),
                        locale = locale,
                    )
                },
            )

            CalendarView.Week -> {
                val todayPage = Math.floorDiv((today - weekAnchor).toInt(), 7)
                ViewPivot(
                    pageCount = WeekPageCount,
                    pageFor = { day -> Math.floorDiv((day - weekAnchor).toInt(), 7) },
                    epochDayFor = { page -> weekAnchor + page.toLong() * 7L },
                    titleFor = { page ->
                        when (page - todayPage) {
                            0 -> "this week"
                            1 -> "next week"
                            -1 -> "last week"
                            else -> "week of " +
                                LocalDate.ofEpochDay(weekAnchor + page.toLong() * 7L)
                                    .format(monthShort)
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
                epochDayFor = { page ->
                    MonthAnchor.plusMonths(page.toLong()).toEpochDay()
                },
                titleFor = { page ->
                    val date = MonthAnchor.plusMonths(page.toLong())
                    date.month.getDisplayName(java.time.format.TextStyle.FULL, locale)
                        .lowercase(locale)
                },
            )

            CalendarView.Year -> ViewPivot(
                pageCount = YearRangeYears,
                pageFor = { day -> (LocalDate.ofEpochDay(day).year - YearAnchor) },
                epochDayFor = { page -> LocalDate.of(page + YearAnchor, 1, 1).toEpochDay() },
                titleFor = { page -> (page + YearAnchor).toString() },
            )

            CalendarView.Agenda -> ViewPivot(
                pageCount = 1,
                pageFor = { 0 },
                epochDayFor = { today },
                titleFor = { "" },
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
            val unitDay = spec.epochDayFor(page)
            state.selectDateFromPager(mergeIntoUnit(view, unitDay, state.selectedEpochDay))
        }
    }
    LaunchedEffect(state.selectedEpochDay, view) {
        val target = spec.pageFor(state.selectedEpochDay)
        if (pagerState.currentPage != target) {
            pagerState.scrollToPage(target)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        MetroPivotTitleWindow(
            pageCount = spec.pageCount,
            selectedPage = pagerState.currentPage,
            titleFor = spec.titleFor,
            onSelect = { page -> scope.launch { pagerState.animateScrollToPage(page) } },
            modifier = Modifier.padding(vertical = 8.dp),
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
            dateOverline = state.dayDateOverline(epochDay),
            allDayEvents = state.dayAllDayEvents(epochDay),
            hourSlots = state.dayHourSlots(epochDay),
            weather = state.weather,
            usingDemoData = state.usingDemoData,
            loadFailed = state.loadFailed,
            onEventClick = state::openEventDetail,
        )

        CalendarView.Week -> WeekScreen(
            week = state.week(epochDay),
            miniMonth = state.weekMiniMonth(epochDay),
            weather = state.weather,
            onSelectDay = state::selectDay,
            onEventClick = state::openEventDetail,
        )

        CalendarView.Month -> MonthScreen(
            grid = state.monthGrid(epochDay),
            weekdayLabels = state.monthWeekdayLabels,
            usingDemoData = state.usingDemoData,
            loadFailed = state.loadFailed,
            onSelectDay = state::selectDay,
        )

        CalendarView.Year -> YearScreen(
            months = state.yearMonths(epochDay),
            onSelectMonth = state::selectDay,
        )

        CalendarView.Agenda -> Unit
    }
}

/**
 * WP8.1 Calendar "today" app-bar affordance: the current day number over a short month
 * abbreviation, inside the standard circular icon outline drawn by [MetroAppBarIcon].
 */
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

/**
 * Preserves the selected day-of-month / weekday when the pivot settles on a new unit, so
 * switching to month/year keeps the current day instead of collapsing to the first of the unit.
 */
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
        CalendarView.Agenda -> currentEpochDay
    }
}

/**
 * Two-finger pinch → view zoom. Accumulates net zoom during the gesture, reports the target view
 * for preview, and **commits on release** so the change animates instead of tearing down mid-pinch.
 * Single-finger drags are left untouched so the pivot pager still handles horizontal swipes.
 */
private fun Modifier.pinchToSwitchView(
    current: CalendarView,
    onPreview: (CalendarView?) -> Unit,
    onCommit: (CalendarView?) -> Unit,
): Modifier = pointerInput(current) {
    awaitEachGesture {
        var netZoom = 1f
        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()
            if (event.changes.size >= 2) {
                val zoom = event.calculateZoom()
                if (zoom != 1f) {
                    event.changes.forEach { it.consume() }
                    netZoom *= zoom
                    val target = pinchTarget(current, netZoom)
                    onPreview(target.takeIf { it != current })
                }
            }
        } while (event.changes.any { it.pressed })
        val target = pinchTarget(current, netZoom)
        onCommit(target.takeIf { it != current })
    }
}

/** Target view for the accumulated pinch [netZoom]; agenda is never a pinch target. */
private fun pinchTarget(current: CalendarView, netZoom: Float): CalendarView {
    val currentIndex = scaleIndex(current)
    if (currentIndex < 0 || netZoom <= 0f) return current
    val steps = round(ln(netZoom) / ln(PinchStepFactor)).toInt()
    val targetIndex = (currentIndex - steps).coerceIn(0, ScaleViews.lastIndex)
    return ScaleViews[targetIndex]
}
