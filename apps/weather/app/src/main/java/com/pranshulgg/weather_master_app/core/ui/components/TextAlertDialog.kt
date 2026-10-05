package com.pranshulgg.weather_master_app.core.ui.components

import androidx.compose.runtime.Composable
import com.metro.ui.MetroMessageDialog

@Composable
fun TextAlertDialog(
    show: Boolean,
    title: String,
    message: String,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!show) return
    MetroMessageDialog(
        title = title,
        onDismissRequest = onDismiss,
        body = message,
        confirmLabel = confirmText,
        onConfirm = onConfirm,
        dismissLabel = dismissText,
        onDismiss = onDismiss
    )
}
