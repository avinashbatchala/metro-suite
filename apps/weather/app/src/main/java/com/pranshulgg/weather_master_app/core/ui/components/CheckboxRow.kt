package com.pranshulgg.weather_master_app.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroCheckBox
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

/** Square Windows checkbox row. */
@Composable
fun CheckboxRow(
    label: String,
    checked: Boolean,
    hasPadding: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .clickable { onCheckedChange(!checked) }
            .fillMaxWidth()
            .padding(
                horizontal = if (hasPadding) 16.dp else 0.dp,
                vertical = 10.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MetroCheckBox(checked = checked, onCheckedChange = null)
        Spacer(modifier = Modifier.width(10.dp))
        MetroText(
            text = label,
            style = MetroTextStyle.Body,
            color = MetroTheme.colors.primaryText
        )
    }
}
