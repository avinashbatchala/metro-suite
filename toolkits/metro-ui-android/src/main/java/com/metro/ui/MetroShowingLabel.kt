package com.metro.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

/**
 * WP8.1 "showing …" filter chip — "showing" in primary text, remainder in accent.
 * Press uses [Modifier.metroClickable] (down-left nudge), never a square wash.
 */
@Composable
fun MetroShowingLabel(
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val showingPrefix = "showing "
    val remainder = if (label.startsWith(showingPrefix, ignoreCase = true)) {
        label.substring(showingPrefix.length)
    } else {
        label
    }
    val showPrefix = label.startsWith(showingPrefix, ignoreCase = true)

    val annotated = buildAnnotatedString {
        if (showPrefix) {
            withStyle(SpanStyle(color = MetroTheme.colors.primaryText)) {
                append(showingPrefix)
            }
            withStyle(SpanStyle(color = MetroTheme.colors.accent)) {
                append(remainder)
            }
        } else {
            withStyle(SpanStyle(color = MetroTheme.colors.accent)) {
                append(label)
            }
        }
    }

    val clickableModifier = if (onClick != null) {
        Modifier.metroClickable(onClick = onClick)
    } else {
        Modifier
    }

    MetroText(
        text = annotated,
        style = MetroTextStyle.Body,
        modifier = modifier.then(clickableModifier),
    )
}
