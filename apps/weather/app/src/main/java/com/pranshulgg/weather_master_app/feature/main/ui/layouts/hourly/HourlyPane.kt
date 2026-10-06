package com.pranshulgg.weather_master_app.feature.main.ui.layouts.hourly

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metro.ui.MetroDimens
import com.metro.ui.MetroEmptyState
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.pranshulgg.weather_master_app.core.model.domain.weather.WeatherDaily
import com.pranshulgg.weather_master_app.core.model.weather.TemperatureUnit
import com.pranshulgg.weather_master_app.core.model.weather.toLabel
import com.pranshulgg.weather_master_app.core.ui.components.WeatherGlyph
import com.pranshulgg.weather_master_app.core.ui.components.WeatherGlyphKind
import com.pranshulgg.weather_master_app.core.ui.components.toGlyphKind
import com.pranshulgg.weather_master_app.feature.main.data.HourlyForecast
import com.pranshulgg.weather_master_app.feature.main.data.formatHourlyTime
import com.pranshulgg.weather_master_app.feature.main.data.formatPrecipitation
import com.pranshulgg.weather_master_app.feature.main.data.formatTemperature
import com.pranshulgg.weather_master_app.feature.main.data.hourlyContentDescription

private val HourTimeWidth = 76.dp
private val HourArtWidth = 48.dp
private val HourArtSize = 40.dp
private val HourPrecipWidth = 72.dp
private val HourRowHeight = 72.dp

/** WP8.1 separator between hourly rows — flat 1dp hairline, never a Material divider. */
@Composable
private fun HourlySeparator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.16f)),
    )
}

/**
 * Bing Weather hourly pane — a vertical, breathable list of [HourlyForecastRow]s over the
 * flat Bing blue surface. The panorama owns horizontal paging; this pane only scrolls
 * vertically. [scrollState] is hoisted by the caller so it survives pane switches.
 */
@Composable
fun HourlyPane(
    forecasts: List<HourlyForecast>,
    timezone: String,
    is24Hour: Boolean,
    temperatureUnit: TemperatureUnit,
    today: WeatherDaily?,
    context: Context,
    scrollState: androidx.compose.foundation.ScrollState = rememberScrollState(),
    modifier: Modifier = Modifier,
) {
    if (forecasts.isEmpty()) {
        MetroEmptyState(
            message = "no hourly data",
            modifier = modifier,
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = MetroDimens.ScreenHorizontalMargin),
    ) {
        forecasts.forEachIndexed { index, forecast ->
            val timeLabel = if (index == 0) {
                "now"
            } else {
                formatHourlyTime(
                    timestampMillis = forecast.timestamp,
                    timezoneId = timezone,
                    is24Hour = is24Hour,
                )
            }
            val glyphKind = forecast.condition.toGlyphKind(
                daily = today,
                targetTimeMilli = forecast.timestamp,
            )

            HourlyForecastRow(
                timeLabel = timeLabel,
                conditionLabel = forecast.condition.toLabel(context),
                temperatureText = formatTemperature(forecast.temperature, temperatureUnit),
                precipitationText = formatPrecipitation(forecast.precipitationProbability),
                glyphKind = glyphKind,
            )

            if (index != forecasts.lastIndex) {
                HourlySeparator()
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * Stable four-column hourly row: `TIME | WEATHER ART | TEMPERATURE | PRECIPITATION`.
 *
 * The row is a single accessibility node ([clearAndSetSemantics]); the artwork and separators
 * never surface as separate nodes.
 */
@Composable
fun HourlyForecastRow(
    timeLabel: String,
    conditionLabel: String,
    temperatureText: String,
    precipitationText: String,
    glyphKind: WeatherGlyphKind,
    modifier: Modifier = Modifier,
) {
    val description = hourlyContentDescription(
        time = timeLabel,
        conditionLabel = conditionLabel.lowercase(),
        temperature = temperatureText,
        precipitation = precipitationText,
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(HourRowHeight)
            .clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MetroText(
            text = timeLabel,
            style = MetroTextStyle.ListItemTitle,
            color = MetroTheme.colors.primaryText,
            maxLines = 1,
            modifier = Modifier.width(HourTimeWidth),
        )
        Box(
            modifier = Modifier.width(HourArtWidth),
            contentAlignment = Alignment.Center,
        ) {
            WeatherGlyph(
                kind = glyphKind,
                size = HourArtSize,
                color = MetroTheme.colors.primaryText,
            )
        }
        TemperatureText(
            value = temperatureText,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        )
        PrecipitationIndicator(
            percentText = precipitationText,
            modifier = Modifier.width(HourPrecipWidth),
        )
    }
}

/** The hourly temperature — large and light-weight, the strongest value in the row. */
@Composable
fun TemperatureText(value: String, modifier: Modifier = Modifier) {
    BasicText(
        text = value,
        modifier = modifier,
        style = MetroTextStyle.ListItemTitle
            .toTextStyle(MetroTheme.fontFamily)
            .copy(
                fontSize = 34.sp,
                fontWeight = FontWeight.Light,
                color = MetroTheme.colors.primaryText,
            ),
        maxLines = 1,
    )
}

/** Right-aligned precipitation chance — a small monochrome droplet followed by the percent. */
@Composable
fun PrecipitationIndicator(percentText: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DropletGlyph(color = MetroTheme.colors.secondaryText)
        Spacer(modifier = Modifier.width(4.dp))
        MetroText(
            text = percentText,
            style = MetroTextStyle.ListItemSubtitle,
            color = MetroTheme.colors.secondaryText,
            maxLines = 1,
        )
    }
}

/** Minimal monochrome droplet drawn with a [Canvas] — no container, no pill. */
@Composable
private fun DropletGlyph(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(14.dp)) {
        val s = size.minDimension
        val head = Path().apply {
            moveTo(0.5f * s, 0.08f * s)
            lineTo(0.20f * s, 0.60f * s)
            lineTo(0.80f * s, 0.60f * s)
            close()
        }
        drawPath(path = head, color = color)
        drawCircle(
            color = color,
            radius = 0.30f * s,
            center = Offset(0.5f * s, 0.60f * s),
        )
    }
}
