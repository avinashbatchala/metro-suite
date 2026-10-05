package com.metro.calendar.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.calendar.R
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

/** Single-line calendar chrome — clips at the screen edge, never wraps. */
@Composable
internal fun CalendarLineText(
    text: String,
    style: MetroTextStyle,
    modifier: Modifier = Modifier,
    color: Color = MetroTheme.colors.primaryText,
) {
    MetroText(
        text = text,
        style = style,
        color = color,
        maxLines = 1,
        softWrap = false,
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * Consistent status line shown at the top of every pivot when the schedule is not the user's
 * real one (demo fallback) or when the provider query failed.
 */
@Composable
internal fun CalendarStatusBanner(
    usingDemoData: Boolean,
    loadFailed: Boolean,
    modifier: Modifier = Modifier,
) {
    val message = when {
        loadFailed -> stringResource(R.string.load_error)
        usingDemoData -> stringResource(R.string.demo_data_banner)
        else -> return
    }
    MetroText(
        text = message,
        style = MetroTextStyle.ListItemSubtitle,
        color = MetroTheme.colors.secondaryText,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp, top = 4.dp),
    )
}
