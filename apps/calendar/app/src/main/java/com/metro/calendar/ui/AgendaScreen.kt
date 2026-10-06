package com.metro.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

/**
 * WP8.1 (later update) agenda: a concise chronological list for the **selected day or week**,
 * grouped by date. Reached from the ellipsis menu of the Day/Week views.
 */
@Composable
fun AgendaScreen(
    buckets: List<DayBucket>,
    loadFailed: Boolean,
    scrollRequestId: Int,
    targetEpochDay: Long,
    onEventClick: (CalendarEvent) -> Unit,
    onEventLongClick: (CalendarEvent) -> Unit,
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
    LaunchedEffect(scrollRequestId, targetEpochDay, buckets) {
        if (scrollRequestId == 0) return@LaunchedEffect
        var index = 0
        var target: Int? = null
        buckets.forEach { bucket ->
            if (target == null && bucket.epochDay >= targetEpochDay) target = index
            index += 1 + bucket.events.size
        }
        listState.animateScrollToItem(target ?: 0)
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = MetroDimens.ScreenHorizontalMargin),
    ) {
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
                EventSummaryRow(
                    event = event,
                    time = CalendarLogic.formatEventTime(event),
                    duration = CalendarLogic.formatEventDuration(event),
                    onEventClick = onEventClick,
                    onEventLongClick = onEventLongClick,
                )
            }
        }
        item { Spacer(modifier = Modifier.height(96.dp)) }
    }
}
