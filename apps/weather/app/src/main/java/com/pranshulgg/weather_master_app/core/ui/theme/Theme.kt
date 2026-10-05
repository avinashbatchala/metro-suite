package com.pranshulgg.weather_master_app.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.metro.ui.MetroSystemTheme
import com.pranshulgg.weather_master_app.core.prefs.LocalAppPrefs

/**
 * Kept for secondary surfaces (widget config). Delegates to the suite-wide
 * [MetroSystemTheme] so accent / typeface / dark mode follow MetroSuite Settings.
 */
@Composable
fun WeatherMasterTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    @Suppress("UNUSED_PARAMETER") seedColor: Color = MetroColors.Blue,
    @Suppress("UNUSED_PARAMETER") dynamicTheme: Boolean = false,
    @Suppress("UNUSED_PARAMETER") themeVariantType: ThemeVariantType,
    @Suppress("UNUSED_PARAMETER") applySystemUi: Boolean = true,
    content: @Composable () -> Unit
) {
    MetroSystemTheme(content = content)
}

@Composable
fun isThemeDark(): Boolean {
    val prefs = LocalAppPrefs.current

    return when (prefs.appTheme) {
        "Dark" -> true
        "Light" -> false
        else -> isSystemInDarkTheme()
    }
}
