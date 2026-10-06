package com.metro.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.metro.calendar.R
import com.metro.calendar.data.CalendarEvent
import com.metro.calendar.data.MonthGridCell
import com.metro.ui.MetroDimens
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroClickable

/**
 * WP8.1 month view: a dense edge-to-edge seven-column grid with hairline separators, plus a
 * selected-day pane below. Selecting a day does **not** leave Month.
 */
@Composable
fun MonthScreen(
    monthEpochDay: Long,
    grid: List<MonthGridCell>,
    weekdayLabels: List<String>,
    paneEpochDay: Long?,
    paneEvents: List<CalendarEvent>,
    paneOverline: String?,
    use24Hour: Boolean,
    onSelectDay: (Long) -> Unit,
    onOpenDay: (Long) -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    onEventLongClick: (CalendarEvent) -> Unit,
    eventTime: (CalendarEvent) -> String,
    eventDuration: (CalendarEvent) -> String,
    modifier: Modifier = Modifier,
) {
    val line = MetroTheme.colors.secondaryText.copy(alpha = 0.3f)

    Column(modifier = modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekdayLabels.forEach { label ->
                Box(
                    modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    MetroText(
                        text = label,
                        style = MetroTextStyle.ListItemSubtitle,
                        color = MetroTheme.colors.secondaryText,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(line))

        grid.chunked(7).forEachIndexed { rowIndex, week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEachIndexed { columnIndex, cell ->
                    Box(modifier = Modifier.weight(1f)) {
                        MonthDayCell(
                            cell = cell,
                            selected = cell.epochDay == paneEpochDay,
                            onClick = {
                                if (cell.epochDay == paneEpochDay) onOpenDay(cell.epochDay)
                                else onSelectDay(cell.epochDay)
                            },
                        )
                    }
                    if (columnIndex < 6) {
                        Box(Modifier.width(1.dp).height(56.dp).background(line))
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(line))
        }

        if (paneEpochDay != null) {
            Column(modifier = Modifier.fillMaxWidth()) {
                MetroText(
                    text = paneOverline.orEmpty(),
                    style = MetroTextStyle.SectionHeader,
                    color = MetroTheme.colors.secondaryText,
                    maxLines = 1,
                    modifier = Modifier.padding(
                        start = MetroDimens.ScreenHorizontalMargin,
                        top = 10.dp,
                        bottom = 4.dp,
                    ),
                )
                if (paneEvents.isEmpty()) {
                    MetroText(
                        text = stringResource(R.string.no_events_day),
                        style = MetroTextStyle.Body,
                        color = MetroTheme.colors.secondaryText,
                        modifier = Modifier.padding(start = MetroDimens.ScreenHorizontalMargin),
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(paneEvents, key = { "${it.id}-${it.startMillis}" }) { event ->
                            EventSummaryRow(
                                event = event,
                                time = eventTime(event),
                                duration = eventDuration(event),
                                onEventClick = onEventClick,
                                onEventLongClick = onEventLongClick,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthDayCell(
    cell: MonthGridCell,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val accent = MetroTheme.colors.accent
    val textColor = when {
        !cell.inCurrentMonth -> MetroTheme.colors.secondaryText.copy(alpha = 0.4f)
        cell.isToday -> accent
        else -> MetroTheme.colors.primaryText
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(if (selected) accent.copy(alpha = 0.14f) else Color.Transparent)
            .metroClickable(onClick = onClick)
            .padding(4.dp),
    ) {
        MetroText(
            text = cell.dayOfMonth.toString(),
            style = MetroTextStyle.ListItemSubtitle,
            color = textColor,
            maxLines = 1,
        )
        cell.eventColors.forEach { hex ->
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(width = 18.dp, height = 3.dp)
                    .background(
                        runCatching { Color(android.graphics.Color.parseColor(hex)) }
                            .getOrDefault(accent),
                    ),
            )
        }
    }
}
