package com.pranshulgg.weather_master_app.feature.settings.appearance.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.pranshulgg.weather_master_app.core.prefs.LocalAppPrefs
import com.metro.ui.MetroListPicker
import com.metro.ui.MetroTheme
import com.pranshulgg.weather_master_app.core.ui.theme.ThemeVariantType

private val scheme = listOf(
    "#f44336", "#ff5252", "#e91e63", "#ff4081", "#9c27b0", "#e040fb", "#673ab7",
    "#7c4dff", "#3f51b5", "#536dfe", "#0078d7", "#448aff", "#03a9f4", "#40c4ff",
    "#00bcd4", "#18ffff", "#009688", "#64ffda", "#4caf50", "#69f0ae", "#8bc34a",
    "#b2ff59", "#cddc39", "#eeff41", "#ffeb3b", "#ffff00", "#ffc107", "#ffd740",
    "#ff9800", "#ffab40", "#ff5722", "#ff6e40", "#795548", "#607d8b", "#9e9e9e"
)

private val variants = listOf(
    "Tonal Spot" to ThemeVariantType.TONAL_SPOT,
    "Neutral" to ThemeVariantType.NEUTRAL,
    "Vibrant" to ThemeVariantType.VIBRANT,
    "Expressive" to ThemeVariantType.EXPRESSIVE
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelectableThemeColors(onThemeColorChanged: (String) -> Unit) {
    val prefs = LocalAppPrefs.current
    var currentSelectedColor by remember { mutableStateOf(prefs.customThemeColor) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        FlowRow(
            maxItemsInEachRow = 7,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            scheme.forEach { hex ->
                val isSelected = currentSelectedColor == hex
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(hex.toColorInt()), RectangleShape)
                        .then(
                            if (isSelected) {
                                Modifier.border(3.dp, MetroTheme.colors.primaryText, RectangleShape)
                            } else {
                                Modifier
                            }
                        )
                        .clickable {
                            currentSelectedColor = hex
                            onThemeColorChanged(hex)
                        }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        MetroListPicker(
            options = variants.map { it.first },
            selectedOptionIndex = variants.indexOfFirst { it.second == prefs.themeVariantType },
            onSelectOption = { index -> prefs.setThemeVariantType(variants[index].second) }
        )
    }
}
