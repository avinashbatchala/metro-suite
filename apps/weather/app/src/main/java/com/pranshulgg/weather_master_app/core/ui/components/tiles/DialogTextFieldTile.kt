package com.pranshulgg.weather_master_app.core.ui.components.tiles
import com.metro.ui.MetroListItem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroMessageDialog
import com.metro.ui.MetroTextBox
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

@Composable
fun DialogTextFieldTile(
    headline: String,
    description: String? = null,
    initialText: String = "",
    onTextSubmitted: (String) -> Unit,
    leading: @Composable (() -> Unit)? = null,
    placeholder: String,
    placeholderTextField: String,
    shapes: RoundedCornerShape,
    itemBgColor: Color = Color.Unspecified,
    trailing: (@Composable (() -> Unit))? = null,
    placeholderAsValue: Boolean = false,
    overline: @Composable (() -> Unit)? = null
) {
    var showDialog by remember { mutableStateOf(false) }
    var textFieldValue by remember(initialText) { mutableStateOf(initialText) }

    val descriptionText = when {
        description != null -> description
        textFieldValue.isNotBlank() -> textFieldValue
        else -> placeholder
    }

    MetroListItem(
        title = headline,
        subtitle = descriptionText,
        leading = leading,
        trailing = trailing,
        onClick = { showDialog = true }
    )

    if (showDialog) {
        MetroMessageDialog(title = "", onDismissRequest = { showDialog = false }, content = {
            MetroText(
                text = headline,
                style = MetroTextStyle.DialogTitle,
                color = MetroTheme.colors.primaryText
            )
            Spacer(modifier = Modifier.height(12.dp))
            MetroTextBox(
                value = textFieldValue,
                onValueChange = { textFieldValue = it },
                placeholder = placeholderTextField
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
            ) {
                MetroBorderButton(text = "Cancel", onClick = { showDialog = false })
                MetroBorderButton(
                    text = "Save",
                    onClick = {
                        onTextSubmitted(textFieldValue)
                        showDialog = false
                    }
                )
            }
        })
    }
}
