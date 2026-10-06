package com.pranshulgg.weather_master_app.core.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.pranshulgg.weather_master_app.R

/**
 * Windows 10 Mobile "Segoe-like" typography, approximated with Open Sans.
 * Light display weights and compact labels create the Metro hierarchy.
 */
val MetroFontFamily = FontFamily(
    Font(R.font.open_sans, FontWeight.Light),
    Font(R.font.open_sans, FontWeight.Normal),
    Font(R.font.open_sans, FontWeight.Medium),
    Font(R.font.open_sans, FontWeight.SemiBold),
    Font(R.font.open_sans, FontWeight.Bold)
)

// Kept for compatibility with existing screens that reference these directly.
val weatherMasterTitleFont = FontFamily(Font(R.font.open_sans, FontWeight.SemiBold))
val googleSansFlex = FontFamily(Font(R.font.open_sans, FontWeight.Normal))

