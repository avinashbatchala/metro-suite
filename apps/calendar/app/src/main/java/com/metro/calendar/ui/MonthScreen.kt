package com.metro.calendar.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import com.metro.calendar.data.MonthGridCell
import com.metro.ui.MetroDimens
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroClickable

/**
 * WP8.1 month view: region-first weekday columns, a six-week grid, and up to three
 * per-calendar colour bars **under** each date number. Selecting a day drills into the day view.
 */
@Composable
fun MonthScreen(
    grid: List<MonthGridCell>,
    weekdayLabels: List<String>,
    usingDemoData: Boolean,
    loadFailed: Boolean,
    onSelectDay: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
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
        item(key = "weekday-row") {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                weekdayLabels.forEach { label ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        MetroText(
                            text = label,
                            style = MetroTextStyle.ListItemSubtitle,
                            color = MetroTheme.colors.secondaryText,
                        )
                    }
                }
            }
        }

        item(key = "month-grid") {
            Column {
                grid.chunked(7).forEachIndexed { rowIndex, week ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        week.forEach { cell ->
                            MonthCell(
                                cell = cell,
                                onSelect = { onSelectDay(cell.epochDay) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    if (rowIndex < 5) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MetroTheme.colors.secondaryText.copy(alpha = 0.3f)),
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(96.dp)) }
    }
}

@Composable
private fun MonthCell(
    cell: MonthGridCell,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textColor = when {
        !cell.inCurrentMonth -> MetroTheme.colors.secondaryText.copy(alpha = 0.4f)
        else -> MetroTheme.colors.primaryText
    }
    val accent = MetroTheme.colors.accent

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .metroClickable(onClick = onSelect)
            .padding(6.dp),
    ) {
        MetroText(
            text = cell.dayOfMonth.toString(),
            style = MetroTextStyle.ListItemTitle,
            color = textColor,
            modifier = Modifier.align(Alignment.TopStart),
        )
        // Per-calendar colour bars directly under the date number.
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 22.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            cell.eventColors.forEach { hex ->
                Box(
                    modifier = Modifier
                        .size(width = 20.dp, height = 3.dp)
                        .background(
                            runCatching { Color(android.graphics.Color.parseColor(hex)) }
                                .getOrDefault(accent),
                        ),
                )
            }
        }
        if (cell.isSelected) {
            Canvas(modifier = Modifier.align(Alignment.TopEnd).size(10.dp)) {
                val path = Path().apply {
                    moveTo(size.width, 0f)
                    lineTo(size.width, size.height)
                    lineTo(0f, 0f)
                    close()
                }
                drawPath(path, accent)
                drawCircle(color = accent, radius = 0.5f, center = Offset(size.width, 0f))
            }
        }
    }
}
