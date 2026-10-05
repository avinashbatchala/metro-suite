package com.metro.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.calendar.R
import com.metro.calendar.data.CalendarEvent
import com.metro.calendar.data.CalendarLogic
import com.metro.calendar.data.DayBucket
import com.metro.ui.MetroDimens
import com.metro.ui.MetroEmptyState
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroClickable

@Composable
fun AgendaScreen(
    buckets: List<DayBucket>,
    usingDemoData: Boolean,
    loadFailed: Boolean,
    scrollRequestId: Int,
    targetEpochDay: Long,
    onEventClick: (CalendarEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (buckets.isEmpty()) {
        MetroEmptyState(
            message = stringResource(if (loadFailed) R.string.load_error else R.string.no_events),
            modifier = modifier,
        )
        return
    }

    val listState = rememberLazyListState()

    // Today action (Blueprint): scroll to today's / the selected day's first event.
    val bannerOffset = if (usingDemoData || loadFailed) 1 else 0
    LaunchedEffect(scrollRequestId, targetEpochDay, buckets) {
        if (scrollRequestId == 0) return@LaunchedEffect
        var index = bannerOffset
        var target: Int? = null
        buckets.forEach { bucket ->
            if (target == null && bucket.epochDay >= targetEpochDay) target = index
            index += 1 + bucket.events.size
        }
        listState.animateScrollToItem(target ?: bannerOffset)
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = MetroDimens.ScreenHorizontalMargin),
    ) {
        if (usingDemoData || loadFailed) {
            item(key = "status-banner") {
                CalendarStatusBanner(usingDemoData = usingDemoData, loadFailed = loadFailed)
            }
        }
        buckets.forEach { bucket ->
            item(key = "header-${bucket.epochDay}") {
                CalendarLineText(
                    text = bucket.headerLabel,
                    style = MetroTextStyle.SectionHeader,
                    color = MetroTheme.colors.secondaryText,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                )
            }
            items(bucket.events, key = { "${bucket.epochDay}-${it.id}-${it.startMillis}" }) { event ->
                AgendaEventRow(event = event, onClick = { onEventClick(event) })
            }
        }
        item { Spacer(modifier = Modifier.height(96.dp)) }
    }
}

@Composable
private fun AgendaEventRow(
    event: CalendarEvent,
    onClick: () -> Unit,
) {
    val eventColor = eventAccent(event)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .metroClickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CalendarLineText(
            text = CalendarLogic.formatEventTime(event),
            style = MetroTextStyle.ListItemTitle,
            modifier = Modifier.width(80.dp),
        )
        Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
            CalendarLineText(
                text = event.title,
                style = MetroTextStyle.ListItemTitle,
                color = eventColor,
            )
            CalendarLineText(
                text = CalendarLogic.formatEventDuration(event),
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.secondaryText,
            )
        }
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(48.dp)
                .background(eventColor),
        )
    }
}

/** Per-calendar accent colour, falling back to the system accent. */
@Composable
internal fun eventAccent(event: CalendarEvent): Color =
    runCatching { Color(android.graphics.Color.parseColor(event.calendarColorHex)) }
        .getOrDefault(MetroTheme.colors.accent)
