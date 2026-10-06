package com.metro.clock.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroClickable

private val WheelItemHeight = 56.dp

/**
 * A WP8.1-style scrolling number wheel: three visible rows, centre row is the selection.
 * Scroll or tap a number to choose. No Material picker/chips.
 */
@Composable
fun MetroWheelColumn(
    items: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Dp = WheelItemHeight,
) {
    val state = rememberLazyListState()
    val safeIndex = selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))

    LaunchedEffect(items, safeIndex) {
        if (state.firstVisibleItemIndex != safeIndex) {
            state.scrollToItem(safeIndex)
        }
    }
    LaunchedEffect(state, items) {
        snapshotFlow { state.firstVisibleItemIndex to state.isScrollInProgress }
            .collect { (index, scrolling) ->
                if (!scrolling) {
                    index.coerceIn(0, (items.size - 1).coerceAtLeast(0))
                        .takeIf { it != selectedIndex }
                        ?.let(onSelected)
                }
            }
    }

    Box(modifier = modifier.height(itemHeight * 3)) {
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = itemHeight),
        ) {
            itemsIndexed(items) { index, label ->
                val selected = index == safeIndex
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .metroClickable { onSelected(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    MetroText(
                        text = label,
                        style = if (selected) MetroTextStyle.HubTitle else MetroTextStyle.ListItemTitle,
                        color = if (selected) MetroTheme.colors.primaryText else MetroTheme.colors.secondaryText,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        val lineColor = MetroTheme.colors.secondaryText.copy(alpha = 0.6f)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val band = itemHeight.toPx()
            drawLine(lineColor, Offset(0f, band), Offset(size.width, band), strokeWidth = 1f)
            drawLine(lineColor, Offset(0f, band * 2f), Offset(size.width, band * 2f), strokeWidth = 1f)
        }
    }
}

/** Two-wheel hour/minute picker. [use24Hour] controls the hour range and whether AM/PM shows. */
@Composable
fun MetroTimePicker(
    hour: Int,
    minute: Int,
    use24Hour: Boolean,
    onHour: (Int) -> Unit,
    onMinute: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hours = remember(use24Hour) {
        if (use24Hour) (0..23).map { String.format("%02d", it) }
        else (1..12).map { it.toString() }
    }
    val hourIndex = remember(hour, use24Hour) {
        if (use24Hour) hour.coerceIn(0, 23) else ((hour + 11) % 12)
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MetroWheelColumn(
            items = hours,
            selectedIndex = hourIndex,
            onSelected = { index ->
                if (use24Hour) {
                    onHour(index)
                } else {
                    val pm = hour >= 12
                    val h12 = index + 1
                    onHour(if (pm) (h12 % 12) + 12 else h12 % 12)
                }
            },
            modifier = Modifier.width(88.dp),
        )
        MetroText(text = ":", style = MetroTextStyle.PageTitle, modifier = Modifier.padding(horizontal = 4.dp))
        MetroWheelColumn(
            items = (0..59).map { String.format("%02d", it) },
            selectedIndex = minute.coerceIn(0, 59),
            onSelected = onMinute,
            modifier = Modifier.width(88.dp),
        )
        if (!use24Hour) {
            val period = if (hour >= 12) "PM" else "AM"
            Column(
                modifier = Modifier.padding(start = 8.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                PeriodOption("AM", period == "AM") { onHour(hour % 12) }
                PeriodOption("PM", period == "PM") { onHour((hour % 12) + 12) }
            }
        }
    }
}

@Composable
private fun PeriodOption(label: String, selected: Boolean, onClick: () -> Unit) {
    MetroText(
        text = label,
        style = MetroTextStyle.ListItemTitle,
        color = if (selected) MetroTheme.colors.accent else MetroTheme.colors.secondaryText,
        modifier = Modifier
            .metroClickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
    )
}

/** Hours / minutes / seconds wheel picker. */
@Composable
fun MetroDurationPicker(
    hours: Int,
    minutes: Int,
    seconds: Int,
    onHours: (Int) -> Unit,
    onMinutes: (Int) -> Unit,
    onSeconds: (Int) -> Unit,
    maxHours: Int = 23,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MetroWheelColumn(
            items = (0..maxHours).map { it.toString() },
            selectedIndex = hours.coerceIn(0, maxHours),
            onSelected = onHours,
            modifier = Modifier.width(80.dp),
        )
        ColonSeparator()
        MetroWheelColumn(
            items = (0..59).map { String.format("%02d", it) },
            selectedIndex = minutes.coerceIn(0, 59),
            onSelected = onMinutes,
            modifier = Modifier.width(80.dp),
        )
        ColonSeparator()
        MetroWheelColumn(
            items = (0..59).map { String.format("%02d", it) },
            selectedIndex = seconds.coerceIn(0, 59),
            onSelected = onSeconds,
            modifier = Modifier.width(80.dp),
        )
    }
}

@Composable
private fun ColonSeparator() {
    MetroText(text = ":", style = MetroTextStyle.HubTitle, modifier = Modifier.padding(horizontal = 2.dp))
}
