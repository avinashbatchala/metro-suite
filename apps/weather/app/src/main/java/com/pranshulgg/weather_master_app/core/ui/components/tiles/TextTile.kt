package com.pranshulgg.weather_master_app.core.ui.components.tiles
import com.metro.ui.MetroListItem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun TextTile(
    headline: String,
    description: String? = null,
    leading: @Composable (() -> Unit)? = null,
    shapes: RoundedCornerShape,
    itemBgColor: Color = Color.Unspecified,
    descriptionMaxLines: Int = Int.MAX_VALUE
) {
    MetroListItem(
        title = headline,
        subtitle = description,
        leading = leading
    )
}
