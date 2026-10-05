package com.pranshulgg.weather_master_app.feature.settings.appearance.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.metro.ui.MetroTheme
import com.pranshulgg.weather_master_app.R
import com.pranshulgg.weather_master_app.core.prefs.LocalAppPrefs
import com.pranshulgg.weather_master_app.core.ui.components.ActionBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerBtn() {

    val prefs = LocalAppPrefs.current
    var selectedColor = prefs.customThemeColor
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Expanded, SheetValue.Hidden)
    )

    var isSheetOpen by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .width(24.dp)
            .height(36.dp)
            .border(1.dp, MetroTheme.colors.secondaryText)
            .background(
                color = Color(prefs.customThemeColor.toColorInt())
            )
            .clickable(
                onClick = {
                    isSheetOpen = true
                }
            ),
    ) {
    }

    if (isSheetOpen)
        ActionBottomSheet(
            sheetState = sheetState,
            cancelText = stringResource(R.string.action_cancel),
            confirmText = stringResource(R.string.action_save),
            onCancel = {
                isSheetOpen = false
            },
            onConfirm = {
                prefs.setCustomThemeColor(selectedColor)
            },
        ) {
            SelectableThemeColors(onThemeColorChanged = { color ->
                selectedColor = color
            })
        }
}