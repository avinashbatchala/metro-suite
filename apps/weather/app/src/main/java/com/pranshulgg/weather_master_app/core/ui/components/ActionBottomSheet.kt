package com.pranshulgg.weather_master_app.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroTheme
import kotlinx.coroutines.launch

/**
 * Windows-style square bottom panel. Replaces the rounded Material sheet: no drag
 * handle, square black surface and flat Metro buttons.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionBottomSheet(
    sheetState: SheetState,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    confirmText: String = "Save",
    cancelText: String = "Cancel",
    showActions: Boolean = true,
    confirmBtnMaxWidth: Boolean = false,
    isConfirmDisabled: Boolean = false,
    enableHandle: Boolean = false,
    hideConfirmBtn: Boolean = false,
    removeBottomInset: Boolean = false,
    showActionsBorder: Boolean = false,
    content: @Composable (hide: () -> Unit) -> Unit
) {
    val scope = rememberCoroutineScope()

    fun hide() {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) onCancel()
        }
    }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onCancel,
        shape = RectangleShape,
        containerColor = MetroTheme.colors.background,
        scrimColor = Color.Black.copy(alpha = 0.6f),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 680.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) {
                content { hide() }
            }

            if (showActions) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 16.dp, start = 16.dp, bottom = 16.dp),
                    horizontalArrangement = if (hideConfirmBtn) Arrangement.End else Arrangement.SpaceBetween
                ) {
                    MetroBorderButton(text = cancelText, onClick = { hide() })
                    if (!hideConfirmBtn) {
                        if (confirmBtnMaxWidth) {
                            Spacer(Modifier.width(8.dp))
                        }
                        MetroBorderButton(
                            text = confirmText,
                            onClick = {
                                onConfirm()
                                hide()
                            },
                            modifier = if (confirmBtnMaxWidth) Modifier.fillMaxWidth() else Modifier,
                            enabled = !isConfirmDisabled
                        )
                    }
                }
            }
        }
    }
}
