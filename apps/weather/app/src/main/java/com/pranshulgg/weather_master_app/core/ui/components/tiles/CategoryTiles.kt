package com.pranshulgg.weather_master_app.core.ui.components.tiles
import com.metro.ui.MetroListItem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pranshulgg.weather_master_app.core.ui.components.Symbol

@Composable
fun CategoryTile(
    headline: String,
    description: String? = null,
    leading: Int,
    shapes: RoundedCornerShape,
    color: Color,
    iconColor: Color,
    onClick: () -> Unit,
    itemBgColor: Color = Color.Unspecified
) {
    MetroListItem(
        title = headline,
        subtitle = description,
        leading = { IconContainer(color, icon = leading, iconColor = iconColor) },
        onClick = onClick
    )
}

/** Windows square icon chip (no circle). */
@Composable
fun IconContainer(color: Color, icon: Int, iconColor: Color) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Symbol(icon, color = iconColor, size = 22.dp)
    }
}
