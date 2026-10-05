package com.pranshulgg.weather_master_app.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroMessageDialog
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

/** Square Windows dialog with optional confirm/dismiss actions. */
@Composable
fun DialogBasic(
    show: Boolean,
    title: String,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel",
    onConfirm: () -> Unit = {},
    onDismiss: () -> Unit,
    showOnlyDismissAction: Boolean = false,
    showDefaultActions: Boolean = true,
    confirmBtnDisabled: Boolean = false,
    content: @Composable () -> Unit,
) {
    if (!show) return

    MetroMessageDialog(title = "", onDismissRequest = onDismiss, content = {
        MetroText(
            text = title,
            style = MetroTextStyle.DialogTitle,
            color = MetroTheme.colors.primaryText
        )
        Spacer(modifier = Modifier.height(12.dp))
        Column { content() }

        if (showDefaultActions) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
            ) {
                MetroBorderButton(text = dismissText, onClick = onDismiss)
                if (!showOnlyDismissAction) {
                    MetroBorderButton(
                        text = confirmText,
                        onClick = {
                            onConfirm()
                            onDismiss()
                        },
                        enabled = !confirmBtnDisabled
                    )
                }
            }
        }
    })
}
