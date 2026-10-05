package com.pranshulgg.weather_master_app.widgets.froggy.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pranshulgg.weather_master_app.R
import com.pranshulgg.weather_master_app.core.model.weather.toFroggy
import com.pranshulgg.weather_master_app.core.ui.components.Gap
import com.pranshulgg.weather_master_app.core.ui.components.SettingSection
import com.pranshulgg.weather_master_app.core.ui.components.SettingTile
import com.pranshulgg.weather_master_app.core.ui.components.SettingsTileIcon
import com.pranshulgg.weather_master_app.core.ui.components.WeatherIconBox
import com.pranshulgg.weather_master_app.core.ui.components.tiles.DialogOption
import com.pranshulgg.weather_master_app.feature.main.components.FroggyContainer
import com.pranshulgg.weather_master_app.widgets.config.WidgetConfig
import com.pranshulgg.weather_master_app.widgets.ui.colors.WidgetTextTheme
import com.pranshulgg.weather_master_app.widgets.ui.colors.WidgetTheme
import kotlin.math.round
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.graphics.RectangleShape
import com.metro.ui.MetroTheme
import com.metro.ui.MetroDimens
import androidx.compose.ui.text.style.TextAlign


@Composable
fun FroggyWidgetConfig(onDone: (WidgetConfig) -> Unit = {}) {
    var widgetTheme by remember { mutableStateOf(WidgetTheme.AUTO) }
    var widgetTextTheme by remember { mutableStateOf(WidgetTextTheme.AUTO) }
    var selectedFontSize by remember { mutableFloatStateOf(1f) }
    var selectedIconSize by remember { mutableFloatStateOf(1f) }

    val widgetThemeOptions =
        WidgetTheme.entries.map { DialogOption(it.toString(), stringResource(it.label)) }
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

                    FroggyWidgetPreview(
                        widgetTextTheme,
                        widgetTheme,
                        selectedFontSize,
                        selectedIconSize
                    )
                    Gap(16.dp)
                }
            }
            Gap(16.dp)
            SettingSection(
                title = stringResource(R.string.setting_appearance),
                tiles = listOf(
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
                    ),
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
                    )
                )
            )
            Spacer(Modifier.weight(1f))
            com.metro.ui.MetroBorderButton(
                text = stringResource(R.string.action_create_widget),
                onClick = {
                    onDone(
                        WidgetConfig(
                            widgetTheme = widgetTheme,
                            widgetTextTheme = widgetTextTheme,
                            fontSize = selectedFontSize,
                            iconSize = selectedIconSize
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
private fun FroggyWidgetPreview(
    textTheme: WidgetTextTheme,
    widgetTheme: WidgetTheme,
    fontSize: Float,
    iconSize: Float,
) {

    val textColor = when (textTheme) {
        WidgetTextTheme.AUTO -> if (widgetTheme == WidgetTheme.TRANSPARENT)
            Color.White else if (widgetTheme == WidgetTheme.DARK)
            Color.White else if (widgetTheme == WidgetTheme.LIGHT)
            Color.Black else MetroTheme.colors.primaryText

        WidgetTextTheme.BLACK -> Color.Black
        WidgetTextTheme.WHITE -> Color.White
    }

    val widgetColor = when (widgetTheme) {
        WidgetTheme.AUTO -> MetroTheme.colors.secondarySurface
        WidgetTheme.DARK -> Color.Black
        WidgetTheme.LIGHT -> Color.White
        WidgetTheme.TRANSPARENT -> Color(0xFF787878)
    }

    val textColorSecondary = when (textTheme) {
        WidgetTextTheme.AUTO -> if (widgetTheme == WidgetTheme.DARK)
            Color(0xB3FFFFFF) else if (widgetTheme == WidgetTheme.LIGHT)
            Color(0x99000000) else MetroTheme.colors.secondaryText

        WidgetTextTheme.BLACK -> Color(0x99000000)
        WidgetTextTheme.WHITE -> Color(0xB3FFFFFF)
    }


    val mainIconSize = 42 * iconSize
    val textFontSize = 18 * fontSize
    val locationFontSize = 14 * fontSize
    val tempFontSize = 30 * fontSize
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .height(180.dp)
            .clip(RectangleShape)
            .background(
                widgetColor,
                RectangleShape
            )
    ) {

        Column(

        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)

            ) {
                WeatherIconBox(R.drawable.weather_clear_night, size = mainIconSize.dp)
                Gap(horizontal = 8.dp)
                Column() {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PreviewText(
                            "26°",
                            color = textColor,
                            fontSize = tempFontSize.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.weight(1f))
                        PreviewText(
                            "Clear sky",
                            color = textColor,
                            fontSize = textFontSize.sp
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PreviewText(
                            "Mountain View",
                            color = textColorSecondary,
                            fontSize = locationFontSize.sp
                        )
                        Spacer(Modifier.weight(1f))
                        PreviewText(
                            "H 28° / L 24°",
                            color = textColor,
                            fontSize = locationFontSize.sp
                        )
                    }
                }

            }

            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.BottomCenter
            ) {
                Image(
                    painter = painterResource(R.drawable.froggy_clear_night),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = 14.dp)
                )
            }
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
