package com.pranshulgg.weather_master_app.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Windows Phone 10 / Windows 10 Mobile Metro palette.
 * Pure black (OLED) or pure white canvases, accent-coloured highlights and
 * square geometry — the visual language of the Windows 10 Mobile Weather app.
 */
object MetroColors {
    val BackgroundBlack = Color(0xFF000000)
    val BackgroundWhite = Color(0xFFFFFFFF)

    val SurfaceDark = Color(0xFF1F1F1F)
    val SurfaceLight = Color(0xFFF2F2F2)

    val TextWhite = Color(0xFFFFFFFF)
    val TextBlack = Color(0xFF000000)
    val TextDim = Color(0xFF999999)
    val TextSubtle = Color(0xFF666666)

    val DividerDark = Color(0x33FFFFFF)
    val DividerLight = Color(0x33000000)

    // Default Windows "Lumia Blue" accent.
    val Blue = Color(0xFF0078D7)
    val Cobalt = Color(0xFF004E8C)
    val Cyan = Color(0xFF00B7C3)
    val Teal = Color(0xFF008299)
    val Emerald = Color(0xFF107C10)
    val Green = Color(0xFF339933)
    val Lime = Color(0xFF84BD00)
    val Amber = Color(0xFFFFB900)
    val Orange = Color(0xFFFF8C00)
    val Crimson = Color(0xFFA80000)
    val Red = Color(0xFFE81123)
    val Magenta = Color(0xFFD80073)
    val Purple = Color(0xFF6B007B)
    val Violet = Color(0xFF744DA9)

    val WindowsAccents = listOf(
        Blue, Cobalt, Cyan, Teal, Emerald, Green, Lime, Amber, Orange, Red, Crimson, Magenta, Purple, Violet
    )
}
