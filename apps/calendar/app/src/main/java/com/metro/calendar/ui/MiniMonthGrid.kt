package com.metro.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metro.calendar.data.MiniMonth
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import java.time.LocalDate
import java.time.format.TextStyle as JTextStyle
import java.util.Locale

/**
 * Compact month grid used by the year view (3×4) and the week view's bottom-right tile.
 * Renders single-letter weekday headers, day numbers, an optional per-calendar event bar,
 * and a box around today.
 */
@Composable
fun MiniMonthGrid(
    miniMonth: MiniMonth,
    modifier: Modifier = Modifier,
    title: String? = null,
    showWeekdayHeader: Boolean = true,
    showEventBars: Boolean = true,
    dayTextSize: Int = 10,
) {
    val locale = remember { Locale.getDefault() }
    val weekdayLetters = remember(miniMonth) {
        val first = LocalDate.ofEpochDay(miniMonth.days.first().epochDay).dayOfWeek
        (0..6).map { first.plus(it.toLong()).getDisplayName(JTextStyle.NARROW, locale).take(1) }
    }
    val fontFamily = MetroTheme.fontFamily
    val dayStyle = remember(fontFamily, dayTextSize) {
        MetroTextStyle.ListItemSubtitle.toTextStyle(fontFamily).copy(
            fontSize = dayTextSize.sp,
            lineHeight = (dayTextSize + 2).sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
        )
    }
    val headerStyle = remember(dayStyle) {
        dayStyle.copy(fontSize = (dayTextSize - 2).coerceAtLeast(7).sp)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (title != null) {
            MetroText(
                text = title,
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.primaryText,
                maxLines = 1,
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
        if (showWeekdayHeader) {
            Row(modifier = Modifier.fillMaxWidth()) {
                weekdayLetters.forEach { letter ->
                    BasicText(
                        text = letter,
                        style = headerStyle.copy(
                            color = MetroTheme.colors.secondaryText.copy(alpha = 0.7f),
                        ),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        miniMonth.days.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    MiniDayCell(
                        dayNumber = day.dayOfMonth,
                        inMonth = day.inCurrentMonth,
                        isToday = day.isToday,
                        colorHex = day.eventColors.firstOrNull(),
                        showEventBar = showEventBars,
                        dayStyle = dayStyle,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniDayCell(
    dayNumber: Int,
    inMonth: Boolean,
    isToday: Boolean,
    colorHex: String?,
    showEventBar: Boolean,
    dayStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    val accent = MetroTheme.colors.accent
    val textColor = if (inMonth) {
        MetroTheme.colors.primaryText
    } else {
        MetroTheme.colors.secondaryText.copy(alpha = 0.35f)
    }
    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        if (isToday) {
            Box(
                modifier = Modifier
                    .size((dayStyle.fontSize.value + 8).dp)
                    .background(MetroTheme.colors.primaryText.copy(alpha = 0.14f)),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BasicText(
                text = dayNumber.toString(),
                style = dayStyle.copy(color = if (isToday) accent else textColor),
            )
            if (showEventBar && colorHex != null) {
                Box(
                    modifier = Modifier
                        .padding(top = 1.dp)
                        .width(8.dp)
                        .height(2.dp)
                        .background(
                            runCatching { Color(android.graphics.Color.parseColor(colorHex)) }
                                .getOrDefault(accent),
                        ),
                )
            } else {
                Spacer(modifier = Modifier.height(3.dp))
            }
        }
    }
}
