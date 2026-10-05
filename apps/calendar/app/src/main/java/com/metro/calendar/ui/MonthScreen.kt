package com.metro.calendar.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.calendar.R
import com.metro.calendar.data.CalendarLogic
import com.metro.calendar.data.MonthGridCell
import com.metro.ui.MetroCircleIconButton
import com.metro.ui.MetroDimens
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroClickable
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val Weekdays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@Composable
fun MonthScreen(
    epochDay: Long,
    grid: List<MonthGridCell>,
    usingDemoData: Boolean,
    loadFailed: Boolean,
    onSelectDay: (Long) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        MonthHeader(
            epochDay = epochDay,
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth,
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
            item(key = "weekday-row") {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Weekdays.forEach { label ->
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
}

@Composable
private fun MonthHeader(
    epochDay: Long,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    val month = LocalDate.ofEpochDay(epochDay).month
    val nextMonth = month.plus(1).getDisplayName(TextStyle.SHORT, Locale.getDefault()).lowercase()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = MetroDimens.ScreenHorizontalMargin - 8.dp,
                end = MetroDimens.ScreenHorizontalMargin - 8.dp,
                top = 4.dp,
                bottom = 4.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MetroCircleIconButton(
            type = MetroSystemIconType.ChevronLeft,
            onClick = onPreviousMonth,
            contentDescription = stringResource(R.string.previous_month),
        )
        Column(modifier = Modifier.weight(1f)) {
            CalendarLineText(
                text = CalendarLogic.yearLabel(epochDay),
                style = MetroTextStyle.SectionHeader,
                color = MetroTheme.colors.secondaryText,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                MetroText(
                    text = CalendarLogic.monthNameLower(epochDay),
                    style = MetroTextStyle.HubTitle,
                    color = MetroTheme.colors.primaryText,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.width(12.dp))
                MetroText(
                    text = nextMonth,
                    style = MetroTextStyle.ListItemSubtitle,
                    color = MetroTheme.colors.secondaryText,
                    maxLines = 1,
                )
            }
        }
        MetroCircleIconButton(
            type = MetroSystemIconType.ChevronRight,
            onClick = onNextMonth,
            contentDescription = stringResource(R.string.next_month),
        )
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
            .border(0.5.dp, MetroTheme.colors.secondaryText.copy(alpha = 0.3f))
            .metroClickable(onClick = onSelect)
            .padding(4.dp),
    ) {
        MetroText(
            text = cell.dayOfMonth.toString(),
            style = MetroTextStyle.ListItemSubtitle,
            color = textColor,
            modifier = Modifier.align(Alignment.TopStart),
        )
        if (cell.isToday) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 18.dp)
                    .width(16.dp)
                    .height(2.dp)
                    .background(accent),
            )
        }
        // Event indicators (up to three) bottom-right; selection triangle top-right so the two
        // never overlap in the same corner.
        Row(
            modifier = Modifier.align(Alignment.BottomEnd),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            repeat(cell.eventIndicatorCount) {
                Box(
                    modifier = Modifier
                        .size(width = 8.dp, height = 3.dp)
                        .background(accent),
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
