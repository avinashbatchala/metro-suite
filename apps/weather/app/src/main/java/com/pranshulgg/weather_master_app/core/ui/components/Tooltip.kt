package com.pranshulgg.weather_master_app.core.ui.components


import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Tooltip(
    tooltipText: String,
    modifier: Modifier = Modifier,
    preferredPosition: TooltipAnchorPosition = TooltipAnchorPosition.Above,
    spacing: Dp = 26.dp,
    content: @Composable () -> Unit
) {
    TooltipBox(
        modifier = modifier,
        state = rememberTooltipState(),
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            positioning = preferredPosition,
            spacingBetweenTooltipAndAnchor = spacing
        ),
        tooltip = {
            PlainTooltip {
                MetroText(
                    text = tooltipText,
                    style = MetroTextStyle.Body,
                    color = MetroTheme.colors.primaryText
                )
            }
        }
    ) {
        content()
    }
}