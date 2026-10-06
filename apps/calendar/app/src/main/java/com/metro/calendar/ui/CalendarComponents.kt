package com.metro.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.metro.calendar.data.CalendarEvent
import com.metro.calendar.data.MiniMonth
import com.metro.ui.MetroListItem
import com.metro.ui.MetroTheme
import com.metro.ui.metroClickable

/** Per-calendar accent colour, falling back to the system accent. */
@Composable
internal fun eventAccent(event: CalendarEvent): Color =
    runCatching { Color(android.graphics.Color.parseColor(event.calendarColorHex)) }
        .getOrDefault(MetroTheme.colors.accent)

/** The week view's eighth tile: a full mini-month; tapping selects the month anchor. */
@Composable
internal fun MiniMonthCell(
    miniMonth: MiniMonth,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (selected) MetroTheme.colors.accent.copy(alpha = 0.12f)
                else androidx.compose.ui.graphics.Color.Transparent,
            )
            .metroClickable(onClick = onClick)
            .padding(6.dp),
    ) {
        MiniMonthGrid(
            miniMonth = miniMonth,
            showEventBars = false,
            dayTextSize = 8,
        )
    }
}

/** A tappable appointment row: time + title (accent) + duration subtitle. */
@Composable
internal fun EventSummaryRow(
    event: CalendarEvent,
    time: String,
    duration: String? = null,
    onEventClick: (CalendarEvent) -> Unit,
    onEventLongClick: (CalendarEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    MetroListItem(
        title = event.title,
        subtitle = if (duration.isNullOrBlank()) time else "$time  ·  $duration",
        titleColor = eventAccent(event),
        onClick = { onEventClick(event) },
        onLongClick = { onEventLongClick(event) },
        modifier = modifier,
    )
}
