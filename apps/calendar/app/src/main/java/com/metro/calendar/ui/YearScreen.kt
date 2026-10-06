package com.metro.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.metro.calendar.data.MiniMonth
import com.metro.ui.metroClickable

/**
 * WP8.1 year view: twelve mini-months in a 3×4 grid. Each mini-month shows event bars and boxes
 * today; tapping one drills into its month (via the day view at the first of the month).
 */
@Composable
fun YearScreen(
    months: List<MiniMonth>,
    onSelectMonth: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        months.chunked(3).forEach { rowMonths ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowMonths.forEach { month ->
                    Box(modifier = Modifier.weight(1f)) {
                        MiniMonthGrid(
                            miniMonth = month,
                            title = month.label,
                            showEventBars = true,
                            dayTextSize = 9,
                            modifier = Modifier.metroClickable(
                                onClick = {
                                    month.days.firstOrNull { it.inCurrentMonth }
                                        ?.let { onSelectMonth(it.epochDay) }
                                },
                            ),
                        )
                    }
                }
                // Keep the last row aligned to 3 columns when fewer than three months remain.
                repeat(3 - rowMonths.size) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
