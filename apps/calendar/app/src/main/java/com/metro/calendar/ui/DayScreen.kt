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
import com.metro.calendar.data.CalendarWeather
import com.metro.calendar.data.HourSlot
import com.metro.ui.MetroDimens
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroClickable

/**
 * WP8.1 day view: full 24-hour agenda grid. Date overline above, then hour rows; long-pressable
 * events open the detail page. Scrolls to the current hour when today.
 */
@Composable
fun DayScreen(
    dateOverline: String,
    allDayEvents: List<CalendarEvent>,
    hourSlots: List<HourSlot>,
    weather: CalendarWeather?,
    usingDemoData: Boolean,
    loadFailed: Boolean,
    onEventClick: (CalendarEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val bannerOffset = if (usingDemoData || loadFailed) 1 else 0
    val allDayOffset = if (allDayEvents.isNotEmpty()) 1 else 0

    LaunchedEffect(hourSlots) {
        if (hourSlots.none { it.isNow }) return@LaunchedEffect
        val target = hourSlots.indexOfFirst { it.isNow }
        if (target >= 0) {
            listState.scrollToItem((target + bannerOffset + allDayOffset).coerceAtLeast(0))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        CalendarLineText(
            text = dateOverline,
            style = MetroTextStyle.SectionHeader,
            color = MetroTheme.colors.secondaryText,
            modifier = Modifier.padding(
                start = MetroDimens.ScreenHorizontalMargin,
                end = MetroDimens.ScreenHorizontalMargin,
                top = 4.dp,
                bottom = 8.dp,
            ),
        )

        if (weather != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = MetroDimens.ScreenHorizontalMargin,
                        end = MetroDimens.ScreenHorizontalMargin,
                        bottom = 8.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MetroText(
                    text = weather.temperature,
                    style = MetroTextStyle.ListItemTitle,
                    color = MetroTheme.colors.primaryText,
                )
                weather.condition?.let { condition ->
                    Box(modifier = Modifier.width(8.dp))
                    MetroText(
                        text = condition,
                        style = MetroTextStyle.ListItemSubtitle,
                        color = MetroTheme.colors.secondaryText,
                    )
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = MetroDimens.ScreenHorizontalMargin),
        ) {
            if (usingDemoData || loadFailed) {
                item(key = "status-banner") {
                    CalendarStatusBanner(usingDemoData = usingDemoData, loadFailed = loadFailed)
                }
            }
            if (allDayEvents.isNotEmpty()) {
                items(allDayEvents, key = { "allday-${it.id}-${it.startMillis}" }) { event ->
                    CalendarLineText(
                        text = event.title,
                        style = MetroTextStyle.ListItemTitle,
                        color = eventAccent(event),
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .metroClickable(onClick = { onEventClick(event) }),
                    )
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            items(hourSlots, key = { "hour-${it.hour}" }) { slot ->
                HourRow(slot = slot, onEventClick = onEventClick)
            }
            item { Spacer(modifier = Modifier.height(96.dp)) }
        }
    }
}

@Composable
private fun HourRow(
    slot: HourSlot,
    onEventClick: (CalendarEvent) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(vertical = 4.dp),
    ) {
        MetroText(
            text = slot.label,
            style = MetroTextStyle.ListItemSubtitle,
            color = if (slot.isNow) MetroTheme.colors.accent else MetroTheme.colors.secondaryText,
            modifier = Modifier.width(64.dp).padding(top = 2.dp),
        )
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(48.dp)
                .background(
                    if (slot.isNow) {
                        MetroTheme.colors.accent
                    } else {
                        MetroTheme.colors.secondaryText.copy(alpha = 0.4f)
                    },
                ),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            if (slot.events.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MetroTheme.colors.secondaryText.copy(alpha = 0.25f)),
                )
            } else {
                slot.events.forEach { event ->
                    CalendarLineText(
                        text = event.title,
                        style = MetroTextStyle.ListItemTitle,
                        color = eventAccent(event),
                        modifier = Modifier
                            .padding(vertical = 2.dp)
                            .metroClickable(onClick = { onEventClick(event) }),
                    )
                }
            }
        }
    }
}
