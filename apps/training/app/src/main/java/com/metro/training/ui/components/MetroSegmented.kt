package com.metro.training.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroClickable

/**
 * WP8.1-friendly segmented selector: a row of lowercase labels where the active item is accent with
 * a 2dp accent underline. No Material tabs/chips.
 */
@Composable
fun <T> MetroSegmentedRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        options.forEach { option ->
            val active = option == selected
            Column(
                modifier = Modifier
                    .width(IntrinsicSize.Min)
                    .metroClickable { onSelect(option) },
            ) {
                MetroText(
                    text = label(option),
                    style = MetroTextStyle.ListItemSubtitle,
                    color = if (active) MetroTheme.colors.primaryText else MetroTheme.colors.secondaryText,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(if (active) MetroTheme.colors.accent else Color.Transparent),
                )
            }
        }
    }
}
