package com.pranshulgg.weather_master_app.core.ui.components.tiles
import com.metro.ui.MetroListItem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.metro.ui.MetroSlider
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

@Composable
fun DialogSliderTile(
    headline: String,
    description: String? = null,
    initialValue: Float = 0.5f,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueSubmitted: (Float) -> Unit,
    leading: @Composable (() -> Unit)? = null,
    shapes: RoundedCornerShape,
    labelFormatter: (Float) -> String = { it.toString() },
    dialogTitle: String,
    isDescriptionAsValue: Boolean = false,
    itemBgColor: Color = Color.Unspecified
) {
    var showDialog by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableStateOf(initialValue) }

    MetroListItem(
        title = headline,
        subtitle = description ?: labelFormatter(sliderValue),
        leading = leading,
        onClick = { showDialog = true }
    )

    if (showDialog) {
        MetroMessageDialog(title = "", onDismissRequest = { showDialog = false }, content = {
            MetroText(
                text = dialogTitle,
                style = MetroTextStyle.DialogTitle,
                color = MetroTheme.colors.primaryText
            )
            Spacer(modifier = Modifier.height(10.dp))
            MetroText(
                text = labelFormatter(sliderValue),
                style = MetroTextStyle.ListItemTitle,
                color = MetroTheme.colors.accent
            )
            Spacer(modifier = Modifier.height(6.dp))
            LabeledSlider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                valueRange = valueRange,
                steps = steps,
                labelFormatter = labelFormatter
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
            ) {
                MetroBorderButton(text = "Cancel", onClick = { showDialog = false })
                MetroBorderButton(
                    text = "Save",
                    onClick = {
                        onValueSubmitted(sliderValue)
                        showDialog = false
                    }
                )
            }
        })
    }
}

/** Thin Windows slider bound to a value range (no Material thumb/label bubble). */
@Composable
fun LabeledSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    labelFormatter: (Float) -> String = { it.toString() }
) {
    val span = (valueRange.endInclusive - valueRange.start).takeIf { it != 0f } ?: 1f
    val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)
    Column(modifier = Modifier.fillMaxWidth()) {
        MetroSlider(
            value = fraction,
            onValueChange = { onValueChange(valueRange.start + it * span) }
        )
    }
}
