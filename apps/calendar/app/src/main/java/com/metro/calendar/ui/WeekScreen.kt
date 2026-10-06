package com.metro.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metro.calendar.R
import com.metro.calendar.data.CalendarEvent
import com.metro.calendar.data.CalendarLogic
import com.metro.calendar.data.CalendarWeek
import com.metro.calendar.data.CalendarWeather
import com.metro.calendar.data.MiniMonth
import com.metro.calendar.data.WeekDayTile
import com.metro.ui.MetroDimens
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroClickable

/**
 * WP8.1 week view: a continuous 4×2 grid (seven day tiles + a mini-month tile) with hairline
 * separators, plus an expanded pane for the selected day. Selecting a day does **not** leave Week.
 */
@Composable
fun WeekScreen(
    week: CalendarWeek,
    miniMonth: MiniMonth,
    weather: CalendarWeather?,
    paneEpochDay: Long?,
    use24Hour: Boolean,
    onSelectDay: (Long) -> Unit,
    onOpenDay: (Long) -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    onEventLongClick: (CalendarEvent) -> Unit,
    eventTime: (CalendarEvent) -> String,
    modifier: Modifier = Modifier,
) {
    val line = MetroTheme.colors.secondaryText.copy(alpha = 0.35f)
    val miniMonthEpochDay = miniMonth.days.firstOrNull { it.inCurrentMonth }?.epochDay
        ?: miniMonth.days.first().epochDay

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
        ) {
            for (rowIndex in 0 until 2) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (columnIndex in 0 until 4) {
                        val tileIndex = rowIndex * 4 + columnIndex
                        Box(modifier = Modifier.weight(1f)) {
                            if (tileIndex < week.days.size) {
                                val day = week.days[tileIndex]
                                WeekDayCell(
                                    tile = day,
                                    weather = weather,
                                    selected = day.epochDay == paneEpochDay,
                                    eventTime = eventTime,
                                    onClick = {
                                        if (day.epochDay == paneEpochDay) onOpenDay(day.epochDay)
                                        else onSelectDay(day.epochDay)
                                    },
                                )
                            } else {
                                MiniMonthCell(
                                    miniMonth = miniMonth,
                                    selected = miniMonthEpochDay == paneEpochDay,
                                    onClick = { onSelectDay(miniMonthEpochDay) },
                                )
                            }
                        }
                        if (columnIndex < 3) {
                            Box(Modifier.width(1.dp).height(150.dp).background(line))
                        }
                    }
                }
                if (rowIndex < 1) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(line))
                }
            }
        }

        if (paneEpochDay != null) {
            WeekExpandedPane(
                epochDay = paneEpochDay,
                events = week.days.firstOrNull { it.epochDay == paneEpochDay }?.events.orEmpty(),
                use24Hour = use24Hour,
                onEventClick = onEventClick,
                onEventLongClick = onEventLongClick,
                eventTime = eventTime,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun WeekDayCell(
    tile: WeekDayTile,
    weather: CalendarWeather?,
    selected: Boolean,
    eventTime: (CalendarEvent) -> String,
    onClick: () -> Unit,
) {
    val accent = MetroTheme.colors.accent
    val headerColor = when {
        selected -> accent
        tile.isToday -> accent
        else -> MetroTheme.colors.primaryText
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .background(if (selected) accent.copy(alpha = 0.12f) else Color.Transparent)
            .metroClickable(onClick = onClick)
            .padding(6.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            MetroText(
                text = tile.weekdayShort,
                style = MetroTextStyle.ListItemSubtitle,
                color = if (tile.isToday || selected) accent else MetroTheme.colors.secondaryText,
                maxLines = 1,
            )
            Box(Modifier.width(6.dp))
            MetroText(
                text = tile.dayNumber.toString(),
                style = MetroTextStyle.ListItemTitle,
                color = headerColor,
                maxLines = 1,
            )
            if (tile.isToday && weather != null) {
                Spacer(Modifier.weight(1f))
                MetroText(
                    text = weather.temperature,
                    style = MetroTextStyle.ListItemSubtitle,
                    color = MetroTheme.colors.primaryText,
                    maxLines = 1,
                )
            }
        }
        tile.events.take(5).forEach { event ->
            MetroText(
                text = event.title,
                style = MetroTextStyle.ListItemSubtitle,
                color = eventAccent(event),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun WeekExpandedPane(
    epochDay: Long,
    events: List<CalendarEvent>,
    use24Hour: Boolean,
    onEventClick: (CalendarEvent) -> Unit,
    onEventLongClick: (CalendarEvent) -> Unit,
    eventTime: (CalendarEvent) -> String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        MetroText(
            text = CalendarLogic.dateHeaderLabel(epochDay),
            style = MetroTextStyle.SectionHeader,
            color = MetroTheme.colors.secondaryText,
            maxLines = 1,
            modifier = Modifier.padding(
                start = MetroDimens.ScreenHorizontalMargin,
                top = 10.dp,
                bottom = 4.dp,
            ),
        )
        if (events.isEmpty()) {
            MetroText(
                text = stringResource(R.string.no_events_day),
                style = MetroTextStyle.Body,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.padding(start = MetroDimens.ScreenHorizontalMargin),
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(events, key = { "${it.id}-${it.startMillis}" }) { event ->
                    EventSummaryRow(
                        event = event,
                        time = eventTime(event),
                        duration = CalendarLogic.formatEventDuration(event),
                        onEventClick = onEventClick,
                        onEventLongClick = onEventLongClick,
                    )
                }
            }
        }
    }
}
