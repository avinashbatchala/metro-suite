package com.metro.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metro.calendar.data.CalendarEvent
import com.metro.calendar.data.CalendarWeek
import com.metro.calendar.data.CalendarWeather
import com.metro.calendar.data.MiniMonth
import com.metro.calendar.data.WeekDayTile
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroClickable

/**
 * WP8.1 week view: a 4×2 grid of eight tiles — seven day tiles plus a mini-month tile in the
 * bottom-right corner. Each day tile shows `MON 7` and its event titles; tapping a title opens
 * the event, tapping the tile drills into the day.
 */
@Composable
fun WeekScreen(
    week: CalendarWeek,
    miniMonth: MiniMonth,
    weather: CalendarWeather?,
    onSelectDay: (Long) -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val miniMonthEpochDay = miniMonth.days.firstOrNull { it.inCurrentMonth }?.epochDay
        ?: miniMonth.days.first().epochDay

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (rowIndex in 0 until 2) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                for (columnIndex in 0 until 4) {
                    val tileIndex = rowIndex * 4 + columnIndex
                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        if (tileIndex < week.days.size) {
                            val day = week.days[tileIndex]
                            WeekDayTileContent(
                                tile = day,
                                weather = weather,
                                onClick = { onSelectDay(day.epochDay) },
                                onEventClick = onEventClick,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            MiniMonthTile(
                                miniMonth = miniMonth,
                                onClick = { onSelectDay(miniMonthEpochDay) },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekDayTileContent(
    tile: WeekDayTile,
    weather: CalendarWeather?,
    onClick: () -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MetroTheme.colors.accent
    val borderColor = if (tile.isToday) accent else MetroTheme.colors.secondaryText.copy(alpha = 0.35f)
    Column(
        modifier = modifier
            .background(
                if (tile.isWeekend) {
                    MetroTheme.colors.secondaryText.copy(alpha = 0.06f)
                } else {
                    Color.Transparent
                },
            )
            .border(if (tile.isToday) 1.5.dp else 0.5.dp, borderColor, RectangleShape)
            .metroClickable(onClick = onClick)
            .padding(6.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            MetroText(
                text = tile.weekdayShort,
                style = MetroTextStyle.ListItemSubtitle,
                color = if (tile.isToday) accent else MetroTheme.colors.secondaryText,
                maxLines = 1,
            )
            Box(modifier = Modifier.width(6.dp))
            MetroText(
                text = tile.dayNumber.toString(),
                style = MetroTextStyle.ListItemTitle,
                color = if (tile.isToday) accent else MetroTheme.colors.primaryText,
                maxLines = 1,
            )
            if (tile.isToday && weather != null) {
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                MetroText(
                    text = weather.temperature,
                    style = MetroTextStyle.ListItemSubtitle,
                    color = MetroTheme.colors.primaryText,
                    maxLines = 1,
                )
            }
        }
        tile.events.take(4).forEach { event ->
            MetroText(
                text = event.title,
                style = MetroTextStyle.ListItemSubtitle,
                color = eventAccent(event),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .metroClickable(onClick = { onEventClick(event) }),
            )
        }
    }
}

@Composable
private fun MiniMonthTile(
    miniMonth: MiniMonth,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .border(0.5.dp, MetroTheme.colors.secondaryText.copy(alpha = 0.35f), RectangleShape)
            .metroClickable(onClick = onClick)
            .padding(6.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            MetroText(
                text = "${miniMonth.label} ${miniMonth.year}",
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.secondaryText,
                maxLines = 1,
            )
            Box(modifier = Modifier.padding(top = 2.dp)) {
                MiniMonthGrid(
                    miniMonth = miniMonth,
                    showEventBars = false,
                    dayTextSize = 8,
                )
            }
        }
    }
}
