package com.pranshulgg.weather_master_app.core.ui.components.tiles
import com.metro.ui.MetroListItem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.metro.ui.MetroColors
import com.metro.ui.MetroTheme

@Composable
fun ActionTile(
    headline: String,
    description: String? = null,
    leading: @Composable (() -> Unit)? = null,
    shapes: RoundedCornerShape,
    onClick: () -> Unit,
    colorDesc: Color = MetroTheme.colors.secondaryText,
    danger: Boolean = false,
    itemBgColor: Color = Color.Unspecified,
    selected: Boolean = false,
    trailing: @Composable (() -> Unit)? = null,
    overline: @Composable (() -> Unit)? = null,
) {
    MetroListItem(
        title = headline,
        subtitle = description,
        leading = leading,
        trailing = trailing,
        onClick = onClick,
        titleColor = when {
            danger -> MetroColors.AccentRed
            selected -> MetroTheme.colors.accent
            else -> null
        }
    )
}
