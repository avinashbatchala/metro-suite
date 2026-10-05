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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.calendar.R
import com.metro.calendar.data.CalendarEvent
import com.metro.calendar.data.CalendarLogic
import com.metro.calendar.data.HourSlot
import com.metro.ui.MetroCircleIconButton
import com.metro.ui.MetroDimens
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroClickable

@Composable
fun DayScreen(
    epochDay: Long,
    allDayEvents: List<CalendarEvent>,
    hourSlots: List<HourSlot>,
    usingDemoData: Boolean,
    loadFailed: Boolean,
    onEventClick: (CalendarEvent) -> Unit,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        DayHeader(
            epochDay = epochDay,
            onPreviousDay = onPreviousDay,
            onNextDay = onNextDay,
        )

        LazyColumn(
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
private fun DayHeader(
    epochDay: Long,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = MetroDimens.ScreenHorizontalMargin - 8.dp,
                end = MetroDimens.ScreenHorizontalMargin - 8.dp,
                top = 4.dp,
                bottom = 8.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MetroCircleIconButton(
            type = MetroSystemIconType.ChevronLeft,
            onClick = onPreviousDay,
            contentDescription = stringResource(R.string.previous_day),
        )
        Column(modifier = Modifier.weight(1f)) {
            CalendarLineText(
                text = CalendarLogic.dateHeaderLabel(epochDay),
                style = MetroTextStyle.SectionHeader,
                color = MetroTheme.colors.secondaryText,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                MetroText(
                    text = CalendarLogic.dayNameLower(epochDay),
                    style = MetroTextStyle.HubTitle,
                    color = MetroTheme.colors.primaryText,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.width(12.dp))
                MetroText(
                    text = CalendarLogic.dayNameShort(epochDay + 1),
                    style = MetroTextStyle.ListItemSubtitle,
                    color = MetroTheme.colors.secondaryText,
                    maxLines = 1,
                )
            }
        }
        MetroCircleIconButton(
            type = MetroSystemIconType.ChevronRight,
            onClick = onNextDay,
            contentDescription = stringResource(R.string.next_day),
        )
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
            color = MetroTheme.colors.secondaryText,
            modifier = Modifier.width(56.dp).padding(top = 2.dp),
        )
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(48.dp)
                .background(MetroTheme.colors.secondaryText.copy(alpha = 0.4f)),
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
                        text = "${CalendarLogic.formatEventTime(event)}  ${event.title}",
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
