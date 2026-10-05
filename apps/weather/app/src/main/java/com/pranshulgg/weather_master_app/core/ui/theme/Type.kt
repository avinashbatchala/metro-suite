package com.pranshulgg.weather_master_app.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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

fun getAppTypography(useGoogleSans: Boolean): Typography {
    val family = MetroFontFamily
    return Typography().run {
        copy(
            displayLarge = displayLarge.copy(fontFamily = family, fontWeight = FontWeight.Light, fontSize = 57.sp, lineHeight = 62.sp),
            displayMedium = displayMedium.copy(fontFamily = family, fontWeight = FontWeight.Light, fontSize = 45.sp, lineHeight = 50.sp),
            displaySmall = displaySmall.copy(fontFamily = family, fontWeight = FontWeight.Light, fontSize = 36.sp, lineHeight = 42.sp),
            headlineLarge = headlineLarge.copy(fontFamily = family, fontWeight = FontWeight.Light, fontSize = 32.sp, lineHeight = 38.sp),
            headlineMedium = headlineMedium.copy(fontFamily = family, fontWeight = FontWeight.Light, fontSize = 28.sp, lineHeight = 34.sp),
            headlineSmall = headlineSmall.copy(fontFamily = family, fontWeight = FontWeight.Light, fontSize = 24.sp, lineHeight = 30.sp),
            titleLarge = titleLarge.copy(fontFamily = family, fontWeight = FontWeight.Light, fontSize = 22.sp, lineHeight = 28.sp),
            titleMedium = titleMedium.copy(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
            titleSmall = titleSmall.copy(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
            bodyLarge = bodyLarge.copy(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
            bodyMedium = bodyMedium.copy(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
            bodySmall = bodySmall.copy(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
            labelLarge = labelLarge.copy(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
            labelMedium = labelMedium.copy(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
            labelSmall = labelSmall.copy(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 16.sp)
        )
    }
}
