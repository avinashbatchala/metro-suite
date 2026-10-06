package com.metro.calendar.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metro.calendar.R
import com.metro.calendar.data.CalendarView
import com.metro.ui.MetroDimens
import com.metro.ui.MetroTheme
import com.metro.ui.metroClickable

/** The four zoom levels shown as tabs; agenda is a separate destination (app-bar list icon). */
internal val ScaleViews = listOf(
    CalendarView.Day,
    CalendarView.Week,
    CalendarView.Month,
    CalendarView.Year,
)

internal fun scaleIndex(view: CalendarView): Int = ScaleViews.indexOf(view)

/**
 * Compact view switcher: `day · week · month · year`. The active tab is accent + medium weight;
 * [previewView] (the pinch target, before release) is accent at reduced opacity; others are
 * secondary text. Agenda shows no active tab.
 */
@Composable
fun CalendarViewTabs(
    view: CalendarView,
    previewView: CalendarView?,
    onSelect: (CalendarView) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fontFamily = MetroTheme.fontFamily
    val base = remember(fontFamily) {
        TextStyle(
            fontFamily = fontFamily,
            fontSize = 20.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Normal,
        )
    }
    val accent = MetroTheme.colors.accent
    val secondary = MetroTheme.colors.secondaryText

    Row(
        modifier = modifier.padding(
            start = MetroDimens.ScreenHorizontalMargin,
            end = MetroDimens.ScreenHorizontalMargin,
            top = 6.dp,
            bottom = 2.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ScaleViews.forEach { candidate ->
            val active = candidate == view
            val preview = !active && candidate == previewView
            val color = when {
                active -> accent
                preview -> accent.copy(alpha = 0.55f)
                else -> secondary
            }
            BasicText(
                text = stringResource(labelOf(candidate)),
                style = base.copy(
                    color = color,
                    fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                ),
                modifier = Modifier.metroClickable { onSelect(candidate) },
            )
        }
    }
}

private fun labelOf(view: CalendarView): Int = when (view) {
    CalendarView.Day -> R.string.view_day
    CalendarView.Week -> R.string.view_week
    CalendarView.Month -> R.string.view_month
    CalendarView.Year -> R.string.view_year
    CalendarView.Agenda -> R.string.agenda
}
