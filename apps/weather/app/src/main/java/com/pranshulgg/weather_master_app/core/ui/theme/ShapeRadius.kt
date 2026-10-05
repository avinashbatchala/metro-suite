package com.pranshulgg.weather_master_app.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Windows Metro geometry is square. All card/control radii collapse to zero;
 * only explicit circles (e.g. icon buttons, radio marks) keep a full radius.
 */
object ShapeRadius {
    val None = 0.dp
    val ExtraSmall = 0.dp
    val Small = 0.dp
    val Medium = 0.dp
    val Large = 0.dp
    val ExtraLarge = 0.dp
    val Full = 999.dp
}

val MetroShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp)
)
