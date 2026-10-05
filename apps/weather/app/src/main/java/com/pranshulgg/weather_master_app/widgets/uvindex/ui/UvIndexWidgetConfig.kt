package com.pranshulgg.weather_master_app.widgets.uvindex.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pranshulgg.weather_master_app.R
import com.pranshulgg.weather_master_app.core.model.weather.uv.UvIndex
import com.pranshulgg.weather_master_app.core.model.weather.uv.toColor
import com.pranshulgg.weather_master_app.core.model.weather.uv.toTextColor
import com.pranshulgg.weather_master_app.core.ui.components.Gap
import com.pranshulgg.weather_master_app.widgets.config.WidgetConfig
import com.pranshulgg.weather_master_app.widgets.model.WidgetVariant
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.graphics.RectangleShape
import com.metro.ui.MetroTheme
import com.metro.ui.MetroDimens
import com.metro.ui.MetroListPicker
import com.metro.ui.MetroListPickerOption
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import androidx.compose.ui.text.style.TextAlign


@Composable
fun UvIndexWidgetConfig(onDone: (WidgetConfig) -> Unit = {}) {

    var selectedVariant by remember { mutableStateOf(WidgetVariant.LARGE) }


    val variantFiltered = listOf(WidgetVariant.LARGE, WidgetVariant.COMPACT)

    Box(
        Modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background)
            .statusBarsPadding()
    ) {
        Column(
            Modifier
                .padding(bottom = 0.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF787878))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Gap(0.dp)
                    GlanceWidgetPreview(
                        selectedVariant,
                    )
                }
            }
            Gap(16.dp)
            MetroListPicker(
                selected = selectedVariant,
                options = variantFiltered.map { MetroListPickerOption(it, it.label) },
                onSelectedChange = { selectedVariant = it },
                label = stringResource(R.string.label_variant),
                modifier = Modifier.padding(horizontal = MetroDimens.ScreenHorizontalMargin)
            )


            Gap(12.dp)
            MetroText(
                text = "Make sure your currently selected source provides UV index, as some sources do not",
                style = MetroTextStyle.Body,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.padding(horizontal = MetroDimens.ScreenHorizontalMargin)
            )

            Spacer(Modifier.weight(1f))
            com.metro.ui.MetroBorderButton(
                text = stringResource(R.string.action_create_widget),
                onClick = {
                    onDone(
                        WidgetConfig(
                            variant = selectedVariant,
                        )
                    )
                },
                modifier = Modifier
                    .padding(horizontal = MetroDimens.ScreenHorizontalMargin)
            )
        }
    }

}


@Composable
private fun GlanceWidgetPreview(
    variant: WidgetVariant,
) {


    when (variant) {
        WidgetVariant.LARGE -> GlanceWidgetLargePreview(
        )

        else -> GlanceWidgetCompactPreview(
        )
    }
}

@Composable
private fun GlanceWidgetLargePreview(
) {


    Column(
        modifier = Modifier
            .padding(16.dp)
            .size(200.dp)
            .background(
                UvIndex.MODERATE.toColor(),
                RectangleShape
            )
    ) {
        Column(Modifier.padding(16.dp)) {
            PreviewText("Current", color = Color(0xFF4A3900), fontSize = 20.sp)
            PreviewText(
                "Moderate",
                color = UvIndex.MODERATE.toTextColor(),
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium
            )
            PreviewText(
                "4",
                color = UvIndex.MODERATE.toTextColor(),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            PreviewText("Today's max: 8", color = Color(0xFF4A3900), fontSize = 18.sp)
            PreviewText("At around 12:00PM", color = Color(0xFF4A3900), fontSize = 16.sp)
        }

    }
}

@Composable
private fun GlanceWidgetCompactPreview(
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .width(250.dp)
            .height(100.dp)
            .background(
                UvIndex.MODERATE.toColor(),
                RectangleShape
            ),
        verticalArrangement = Arrangement.Center,
    ) {
        PreviewText(
            "Current",
            color = Color(0xFF4A3900),
            fontSize = 20.sp,
            modifier = Modifier.padding(start = 16.dp)
        )
        Gap(3.dp)
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(start = 16.dp)) {
            PreviewText(
                "4",
                color = UvIndex.MODERATE.toTextColor(),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Gap(horizontal = 5.dp)
            PreviewText(
                "Moderate",
                color = UvIndex.MODERATE.toTextColor(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun PreviewText(
    text: String,
    fontSize: TextUnit = TextUnit.Unspecified,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    style: TextStyle = TextStyle(),
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.merge(
            TextStyle(
                fontFamily = MetroTheme.fontFamily,
                fontSize = fontSize,
                fontWeight = fontWeight,
                color = color,
                textAlign = textAlign ?: TextAlign.Unspecified,
            )
        ),
    )
}
