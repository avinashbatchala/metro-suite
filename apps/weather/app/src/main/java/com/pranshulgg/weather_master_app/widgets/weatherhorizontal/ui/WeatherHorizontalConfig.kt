package com.pranshulgg.weather_master_app.widgets.weatherhorizontal.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pranshulgg.weather_master_app.R
import com.pranshulgg.weather_master_app.core.ui.components.Gap
import com.pranshulgg.weather_master_app.core.ui.components.SettingSection
import com.pranshulgg.weather_master_app.core.ui.components.SettingTile
import com.pranshulgg.weather_master_app.core.ui.components.SettingsTileIcon
import com.pranshulgg.weather_master_app.core.ui.components.WeatherIconBox
import com.pranshulgg.weather_master_app.core.ui.components.tiles.DialogOption
import com.pranshulgg.weather_master_app.widgets.config.WidgetConfig
import com.pranshulgg.weather_master_app.widgets.model.WidgetVariant
import com.pranshulgg.weather_master_app.widgets.ui.colors.WidgetTextTheme
import com.pranshulgg.weather_master_app.widgets.ui.colors.WidgetTheme
import kotlin.math.round
import kotlin.math.roundToInt
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.graphics.RectangleShape
import com.metro.ui.MetroTheme
import com.metro.ui.MetroDimens
import com.metro.ui.MetroListPicker
import com.metro.ui.MetroListPickerOption

@Composable
fun WeatherHorizontalConfig(onDone: (WidgetConfig) -> Unit = {}) {

    var selectedVariant by remember { mutableStateOf(WidgetVariant.LARGE) }
    var selectedFontSize by remember { mutableFloatStateOf(1f) }
    var selectedIconSize by remember { mutableFloatStateOf(1f) }
    var widgetTheme by remember { mutableStateOf(WidgetTheme.AUTO) }

    val variantFiltered = listOf(WidgetVariant.LARGE, WidgetVariant.COMPACT, WidgetVariant.SMALL)
    var widgetTextTheme by remember { mutableStateOf(WidgetTextTheme.AUTO) }


    val widgetThemeOptions =
        WidgetTheme.entries
            .map { DialogOption(it.toString(), stringResource(it.label)) }
    val widgetTextThemeOptions =
        WidgetTextTheme.entries.map { DialogOption(it.toString(), stringResource(it.label)) }

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
                    WidgetPreview(
                        selectedVariant,
                        selectedFontSize,
                        selectedIconSize,
                        widgetTextTheme,
                        widgetTheme
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


            SettingSection(
                title = stringResource(R.string.setting_appearance),
                tiles = listOf(
                    SettingTile.DialogSliderTile(
                        title = stringResource(R.string.settings_widget_font_size),
                        dialogTitle = stringResource(R.string.settings_widget_font_size),
                        leading = { SettingsTileIcon(R.drawable.format_size_24px) },
                        description = "${round(selectedFontSize * 10) / 10}",
                        isDescriptionAsValue = true,
                        valueRange = 0.1f..2f,
                        initialValue = selectedFontSize,
                        labelFormatter = { "${round(it * 10) / 10}" },
                        steps = 18,
                        onValueSubmitted = {
                            selectedFontSize = it
                        },
                    ),
                    SettingTile.DialogSliderTile(
                        title = stringResource(R.string.settings_widget_icon_size),
                        dialogTitle = stringResource(R.string.settings_widget_icon_size),
                        leading = { SettingsTileIcon(R.drawable.photo_size_select_large_24px) },
                        description = "${round(selectedIconSize * 10) / 10}",
                        isDescriptionAsValue = true,
                        valueRange = 0.1f..2f,
                        initialValue = selectedIconSize,
                        labelFormatter = { "${round(it * 10) / 10}" },
                        steps = 18,
                        onValueSubmitted = {
                            selectedIconSize = it
                        },
                    ),
                    SettingTile.DialogOptionTile(
                        leading = { SettingsTileIcon(R.drawable.palette_24px) },
                        title = stringResource(R.string.settings_widget_background),
                        options = widgetThemeOptions,
                        selectedOption = widgetTheme.toString(),
                        onOptionSelected = {
                            val selected = when (it) {
                                "AUTO" -> WidgetTheme.AUTO
                                "DARK" -> WidgetTheme.DARK
                                "LIGHT" -> WidgetTheme.LIGHT
                                "TRANSPARENT" -> WidgetTheme.TRANSPARENT
                                else -> WidgetTheme.AUTO
                            }

                            widgetTheme = selected
                        }
                    ),
                    SettingTile.DialogOptionTile(
                        leading = { SettingsTileIcon(R.drawable.format_paint_24px) },
                        title = stringResource(R.string.settings_widget_text_color),
                        options = widgetTextThemeOptions,
                        selectedOption = widgetTextTheme.toString(),
                        onOptionSelected = {
                            val selected = when (it) {
                                "AUTO" -> WidgetTextTheme.AUTO
                                "WHITE" -> WidgetTextTheme.WHITE
                                "BLACK" -> WidgetTextTheme.BLACK
                                else -> WidgetTextTheme.AUTO
                            }

                            widgetTextTheme = selected
                        }
                    )
                )
            )

            Spacer(Modifier.weight(1f))
            com.metro.ui.MetroBorderButton(
                text = stringResource(R.string.action_create_widget),
                onClick = {
                    onDone(
                        WidgetConfig(
                            variant = selectedVariant,
                            fontSize = selectedFontSize,
                            iconSize = selectedIconSize,
                            widgetTheme = widgetTheme,
                            widgetTextTheme = widgetTextTheme
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
private fun WidgetPreview(
    variant: WidgetVariant,
    fontSize: Float,
    iconSize: Float,
    textTheme: WidgetTextTheme,
    widgetTheme: WidgetTheme,
) {
    when (variant) {
        WidgetVariant.LARGE -> LargeWidgetPreview(fontSize, iconSize, textTheme, widgetTheme)
        WidgetVariant.COMPACT -> WidgetCompactPreview(fontSize, iconSize, textTheme, widgetTheme)
        else -> WidgetSmallPreview(fontSize, iconSize, textTheme, widgetTheme)
    }

}


@Composable
private fun LargeWidgetPreview(
    fontSize: Float, iconSize: Float, textTheme: WidgetTextTheme,
    widgetTheme: WidgetTheme
) {

    val size = 18 * fontSize
    val fontSizeLocation = 16 * fontSize
    val tempSize = 40 * fontSize
    val iconSize = 48 * iconSize

    val textColor = when (textTheme) {
        WidgetTextTheme.AUTO -> if (widgetTheme == WidgetTheme.DARK)
            Color.White else if (widgetTheme == WidgetTheme.LIGHT)
            Color.Black else if (widgetTheme == WidgetTheme.TRANSPARENT) Color.White else MetroTheme.colors.primaryText

        WidgetTextTheme.BLACK -> Color.Black
        WidgetTextTheme.WHITE -> Color.White
    }


    val widgetColor = when (widgetTheme) {
        WidgetTheme.AUTO -> MetroTheme.colors.secondarySurface
        WidgetTheme.DARK -> Color.Black
        WidgetTheme.LIGHT -> Color.White
        WidgetTheme.TRANSPARENT -> Color.Transparent
    }



    Column(
        Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .height(90.dp)
            .background(
                widgetColor,
                RectangleShape
            ),
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            Modifier
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WeatherIconBox(R.drawable.weather_clear_day, size = iconSize.dp)
            Gap(horizontal = 6.dp)
            Column() {
                PreviewText(
                    "Mountain View",
                    fontSize = fontSizeLocation.sp,
                    color = textColor.copy(alpha = 0.8f)
                )
                PreviewText("Clear Sky", fontSize = size.sp, color = textColor)
            }

            Spacer(Modifier.weight(1f))

            Row(verticalAlignment = Alignment.CenterVertically) {
                PreviewText(
                    "28°",
                    fontWeight = FontWeight.Bold,
                    fontSize = tempSize.sp,
                    color = textColor
                )
                Gap(horizontal = 6.dp)
                Column() {
                    PreviewText(
                        "26°",
                        color = textColor,
                        fontWeight = FontWeight.Medium,
                        fontSize = size.sp
                    )
                    Gap(horizontal = 8.dp)
                    PreviewText(
                        "14°",
                        color = textColor.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium,
                        fontSize = size.sp
                    )
                }
            }
        }
    }
}


@Composable
private fun WidgetCompactPreview(
    fontSize: Float, iconSize: Float, textTheme: WidgetTextTheme,
    widgetTheme: WidgetTheme
) {
    val size = 18 * fontSize
    val tempSize = 24 * fontSize
    val iconSize = 48 * iconSize

    val textColor = when (textTheme) {
        WidgetTextTheme.AUTO -> if (widgetTheme == WidgetTheme.DARK)
            Color.White else if (widgetTheme == WidgetTheme.LIGHT)
            Color.Black else if (widgetTheme == WidgetTheme.TRANSPARENT) Color.White else MetroTheme.colors.primaryText

        WidgetTextTheme.BLACK -> Color.Black
        WidgetTextTheme.WHITE -> Color.White
    }


    val widgetColor = when (widgetTheme) {
        WidgetTheme.AUTO -> MetroTheme.colors.secondarySurface
        WidgetTheme.DARK -> Color.Black
        WidgetTheme.LIGHT -> Color.White
        WidgetTheme.TRANSPARENT -> Color.Transparent
    }

    Column(
        Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .height(90.dp)
            .background(
                widgetColor,
                RectangleShape
            ),
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            Modifier
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WeatherIconBox(R.drawable.weather_clear_day, size = iconSize.dp)
            Gap(horizontal = 6.dp)
            Column() {
                PreviewText("29°", fontSize = tempSize.sp, color = textColor)
                PreviewText("Clear Sky", fontSize = size.sp, color = textColor)
            }
        }
    }
}

@Composable
private fun WidgetSmallPreview(
    fontSize: Float, iconSize: Float, textTheme: WidgetTextTheme,
    widgetTheme: WidgetTheme
) {
    val tempSize = 40 * fontSize
    val iconSize = 48 * iconSize

    val textColor = when (textTheme) {
        WidgetTextTheme.AUTO -> if (widgetTheme == WidgetTheme.DARK)
            Color.White else if (widgetTheme == WidgetTheme.LIGHT)
            Color.Black else if (widgetTheme == WidgetTheme.TRANSPARENT) Color.White else MetroTheme.colors.primaryText

        WidgetTextTheme.BLACK -> Color.Black
        WidgetTextTheme.WHITE -> Color.White
    }


    val widgetColor = when (widgetTheme) {
        WidgetTheme.AUTO -> MetroTheme.colors.secondarySurface
        WidgetTheme.DARK -> Color.Black
        WidgetTheme.LIGHT -> Color.White
        WidgetTheme.TRANSPARENT -> Color.Transparent
    }

    Column(
        Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .height(90.dp)
            .background(
                widgetColor,
                RectangleShape
            ),
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            Modifier
                .padding(18.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            WeatherIconBox(R.drawable.weather_clear_day, size = iconSize.dp)
            Gap(horizontal = 6.dp)

            PreviewText("29°", fontSize = tempSize.sp, color = textColor)

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
