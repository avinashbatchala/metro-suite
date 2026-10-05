package com.pranshulgg.weather_master_app.widgets.weather.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.layout.height
import androidx.glance.layout.width
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
import androidx.compose.ui.text.style.TextAlign


@Composable
fun WeatherWidgetConfig(onDone: (WidgetConfig) -> Unit = {}) {

    var selectedHourlyCount by remember { mutableFloatStateOf(6f) }
    var selectedVariant by remember { mutableStateOf(WidgetVariant.LARGE) }
    var selectedFontSize by remember { mutableFloatStateOf(1f) }
    var widgetTheme by remember { mutableStateOf(WidgetTheme.AUTO) }
    var widgetTextTheme by remember { mutableStateOf(WidgetTextTheme.AUTO) }
    var selectedIconSize by remember { mutableFloatStateOf(1f) }

    var showPrecipitationProbability by remember { mutableStateOf(false) }

    val widgetThemeOptions =
        WidgetTheme.entries.filter { it != WidgetTheme.TRANSPARENT }
            .map { DialogOption(it.toString(), stringResource(it.label)) }
    val widgetTextThemeOptions =
        WidgetTextTheme.entries.map { DialogOption(it.toString(), stringResource(it.label)) }

    val variantFiltered = listOf(WidgetVariant.LARGE, WidgetVariant.COMPACT, WidgetVariant.SMALL)

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
                        selectedHourlyCount,
                        selectedFontSize,
                        selectedIconSize,
                        widgetTextTheme,
                        widgetTheme,
                        showPrecipitationProbability
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
                        title = stringResource(R.string.settings_widget_hourly_forecast_count),
                        dialogTitle = stringResource(R.string.settings_widget_hourly_forecast_count),
                        leading = { SettingsTileIcon(R.drawable.date_range_24px) },
                        valueRange = 2f..12f,
                        description = stringResource(
                            R.string.time_hours,
                            "${selectedHourlyCount.roundToInt()}"
                        ),
                        isDescriptionAsValue = true,
                        initialValue = selectedHourlyCount,
                        labelFormatter = { "${it.roundToInt()}" },
                        steps = 9,
                        onValueSubmitted = {
                            selectedHourlyCount = it
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
                                else -> WidgetTheme.AUTO
                            }

                            widgetTheme = selected
                        }
                    ),
                    SettingTile.SwitchTile(
                        leading = { SettingsTileIcon(R.drawable.rainy_light_24px) },
                        title = stringResource(R.string.settings_widget_show_precip_probability),
                        description = stringResource(R.string.settings_widget_show_precip_probability_secondary),
                        checked = showPrecipitationProbability,
                        onCheckedChange = {
                            showPrecipitationProbability = it
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
                            hourlyCount = selectedHourlyCount.roundToInt(),
                            variant = selectedVariant,
                            fontSize = selectedFontSize,
                            iconSize = selectedIconSize,
                            widgetTheme = widgetTheme,
                            widgetTextTheme = widgetTextTheme,
                            showPrecipitationProbability = showPrecipitationProbability
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
    hourlyCount: Float,
    fontSize: Float,
    iconSize: Float,
    textTheme: WidgetTextTheme,
    widgetTheme: WidgetTheme,
    showPrecipitationProbability: Boolean
) {

    val textColor = when (textTheme) {
        WidgetTextTheme.AUTO -> if (widgetTheme == WidgetTheme.DARK)
            Color.White else if (widgetTheme == WidgetTheme.LIGHT)
            Color.Black else MetroTheme.colors.primaryText

        WidgetTextTheme.BLACK -> Color.Black
        WidgetTextTheme.WHITE -> Color.White
    }


    val textColorSecondary = when (textTheme) {
        WidgetTextTheme.AUTO -> if (widgetTheme == WidgetTheme.DARK)
            Color(0xB3FFFFFF) else if (widgetTheme == WidgetTheme.LIGHT)
            Color(0x99000000) else MetroTheme.colors.secondaryText

        WidgetTextTheme.BLACK -> Color(0x99000000)
        WidgetTextTheme.WHITE -> Color(0xB3FFFFFF)
    }


    val widgetColor = when (widgetTheme) {
        WidgetTheme.AUTO -> MetroTheme.colors.secondarySurface
        WidgetTheme.DARK -> Color.Black
        WidgetTheme.LIGHT -> Color.White
        else -> MetroTheme.colors.secondarySurface
    }

    val hourlyContainerColor = when (widgetTheme) {
        WidgetTheme.AUTO -> MetroTheme.colors.secondarySurface
        WidgetTheme.DARK -> Color(0xFF333333)
        WidgetTheme.LIGHT -> Color(0xFFe6e6e6)
        else -> MetroTheme.colors.secondarySurface
    }

    when (variant) {
        WidgetVariant.LARGE -> LargeWidgetPreview(
            hourlyCount,
            fontSize,
            iconSize,
            widgetColor,
            textColor,
            textColorSecondary,
            hourlyContainerColor,
            showPrecipitationProbability
        )

        WidgetVariant.COMPACT -> CompactWidgetPreview(
            fontSize, iconSize,
            widgetColor,
            textColor,
            textColorSecondary
        )

        else -> SmallWidgetPreview(fontSize, iconSize, widgetColor, textColor)
    }
}

@Composable
private fun LargeWidgetPreview(
    hourlyCount: Float,
    fontSize: Float,
    iconSize: Float,
    widgetColor: Color,
    textColor: Color,
    textColorSecondary: Color,
    hourlyContainerColor: Color,
    showPrecipitationProbability: Boolean
) {


    val times = listOf(
        "7PM",
        "8PM",
        "9PM",
        "10PM",
        "11PM",
        "12AM",
        "1AM",
        "2AM",
        "3AM",
        "4AM",
        "5AM",
        "6AM",
        "7AM"
    )
    val temps = listOf(15, 17, 18, 19, 20, 21, 22, 24, 26, 26, 27, 27, 28)
    val icons = listOf(
        R.drawable.weather_clear_night,
        R.drawable.weather_partly_cloudy_night,
        R.drawable.weather_partly_cloudy_night,
        R.drawable.weather_partly_cloudy_night,
        R.drawable.weather_clear_night,
        R.drawable.weather_clear_night,
        R.drawable.weather_clear_night,
        R.drawable.weather_mostly_clear_night,
        R.drawable.weather_mostly_clear_night,
        R.drawable.weather_mostly_clear_night,
        R.drawable.weather_clear_night,
        R.drawable.weather_clear_night,
        R.drawable.weather_clear_night
    )

    val hourlyIconSize = 22 * iconSize
    val hourlyTextSize = 14 * fontSize

    val hourlyItem: @Composable (String, Int, Int) -> Unit = { time, temp, icon ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(horizontal = 5.dp, vertical = 5.dp)
        ) {
            PreviewText(
                "${temp}°",
                fontSize = hourlyTextSize.sp,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
            if (showPrecipitationProbability) {
                PreviewText(
                    "${temp * 2}%",
                    fontSize = (hourlyTextSize * 0.9).sp,
                    color = textColorSecondary
                )
            }
            Gap(3.dp)
            WeatherIconBox(icon, hourlyIconSize.dp)
            Gap(3.dp)
            PreviewText(
                time,
                fontSize = hourlyTextSize.sp,
                fontWeight = FontWeight.Medium,
                color = textColorSecondary
            )
        }
    }

    val mainIconSize = 32 * iconSize
    val textFontSize = 18 * fontSize
    val locationFontSize = 16 * fontSize
    val tempFontSize = 42 * fontSize

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .height(230.dp)
            .background(
                widgetColor,
                RectangleShape
            )
    ) {

        Column(
            Modifier.padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()

            ) {
                Column() {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WeatherIconBox(R.drawable.weather_clear_night, mainIconSize.dp)
                        Gap(horizontal = 6.dp)
                        PreviewText("Clear sky", color = textColor, fontSize = textFontSize.sp)
                    }
                    Gap(6.dp)
                    Row() {
                        PreviewText(
                            "26°",
                            color = textColor,
                            fontWeight = FontWeight.Medium,
                            fontSize = textFontSize.sp
                        )
                        Gap(horizontal = 8.dp)
                        PreviewText(
                            "14°",
                            color = textColorSecondary,
                            fontWeight = FontWeight.Medium,
                            fontSize = textFontSize.sp
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    PreviewText(
                        "Mountain View",
                        color = textColorSecondary,
                        fontSize = locationFontSize.sp,
                        fontWeight = FontWeight.Medium
                    )
                    PreviewText(
                        "16°",
                        color = textColor,
                        fontSize = tempFontSize.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier.background(hourlyContainerColor)
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    times.take(hourlyCount.roundToInt()).forEachIndexed { index, string ->

                        hourlyItem(
                            string,
                            temps[index],
                            icons[index]
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactWidgetPreview(
    fontSize: Float, iconSize: Float,
    widgetColor: Color,
    textColor: Color,
    textColorSecondary: Color,
) {

    val mainIconSize = 28 * iconSize
    val textFontSize = 18 * fontSize
    val tempFontSize = 54 * fontSize

    Column(
        modifier = Modifier
            .padding(16.dp)
            .width(180.dp)
            .height(180.dp)
            .background(
                widgetColor,
                RectangleShape
            )
    ) {

        Column(
            Modifier.padding(18.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WeatherIconBox(R.drawable.weather_clear_night, mainIconSize.dp)
                Spacer(Modifier.weight(1f))
                PreviewText("Clear sky", color = textColor, fontSize = textFontSize.sp)
            }
            Spacer(Modifier.weight(1f))

            PreviewText(
                "16°",
                color = textColor,
                fontSize = tempFontSize.sp,
                fontWeight = FontWeight.Bold
            )
            Gap(horizontal = 6.dp)
            Row() {
                PreviewText(
                    "26°",
                    color = textColor,
                    fontWeight = FontWeight.Medium,
                    fontSize = textFontSize.sp
                )
                Gap(horizontal = 8.dp)
                PreviewText(
                    "14°",
                    color = textColorSecondary,
                    fontWeight = FontWeight.Medium,
                    fontSize = textFontSize.sp
                )
            }
        }
    }
}

@Composable
private fun SmallWidgetPreview(
    fontSize: Float, iconSize: Float,
    widgetColor: Color,
    textColor: Color
) {

    val fontSize = 54 * fontSize
    val iconSize = 48 * iconSize

    Column(
        modifier = Modifier
            .padding(16.dp)
            .width(150.dp)
            .height(180.dp)
            .background(
                widgetColor,
                RectangleShape
            )
    ) {

        Column(
            Modifier
                .padding(24.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            WeatherIconBox(R.drawable.weather_clear_night, iconSize.dp)

            Gap(8.dp)
            PreviewText(
                "16°",
                color = textColor,
                fontSize = fontSize.sp,
                fontWeight = FontWeight.Bold
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
