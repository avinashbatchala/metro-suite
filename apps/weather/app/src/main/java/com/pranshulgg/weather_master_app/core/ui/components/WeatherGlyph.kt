package com.pranshulgg.weather_master_app.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import com.metro.ui.MetroTheme
import com.pranshulgg.weather_master_app.core.model.domain.weather.WeatherDaily
import com.pranshulgg.weather_master_app.core.model.weather.WeatherCondition
import kotlin.math.cos
import kotlin.math.sin

/**
 * Monochrome Metro weather glyphs — flat, single-color, no gradients.
 *
 * Replaces the full-color raster [WeatherIconBox] on the home pivot so the weather
 * app matches the rest of the suite (calendar / music).
 */
enum class WeatherGlyphKind {
    CLEAR_DAY,
    CLEAR_NIGHT,
    PARTLY_CLOUDY,
    CLOUDY,
    FOG,
    RAIN,
    HEAVY_RAIN,
    SNOW,
    THUNDER,
}

/**
 * Maps every [WeatherCondition] to one glyph kind. Night variants are used for
 * clear / partly-cloudy conditions when [isDay] is false.
 */
fun WeatherCondition.toGlyphKind(isDay: Boolean): WeatherGlyphKind = when (this) {
    WeatherCondition.CLEAR_SKY,
    WeatherCondition.MOSTLY_CLEAR,
    WeatherCondition.VERY_HOT,
    WeatherCondition.VERY_COLD -> if (isDay) WeatherGlyphKind.CLEAR_DAY else WeatherGlyphKind.CLEAR_NIGHT

    WeatherCondition.PARTLY_CLOUDY ->
        if (isDay) WeatherGlyphKind.PARTLY_CLOUDY else WeatherGlyphKind.CLEAR_NIGHT

    WeatherCondition.OVERCAST -> WeatherGlyphKind.CLOUDY

    WeatherCondition.LIGHT_RAIN,
    WeatherCondition.RAIN,
    WeatherCondition.MIXED_PRECIPITATION,
    WeatherCondition.CLEAR_WITH_RAIN,
    WeatherCondition.CLEAR_THEN_RAIN,
    WeatherCondition.CLOUDY_WITH_RAIN,
    WeatherCondition.CLOUDY_THEN_RAIN,
    WeatherCondition.RAIN_WITH_CLEAR,
    WeatherCondition.RAIN_WITH_CLOUDY,
    WeatherCondition.RAIN_WITH_SNOW,
    WeatherCondition.RAIN_THEN_CLEAR,
    WeatherCondition.RAIN_THEN_CLOUDY,
    WeatherCondition.RAIN_THEN_SNOW -> WeatherGlyphKind.RAIN

    WeatherCondition.HEAVY_RAIN -> WeatherGlyphKind.HEAVY_RAIN

    WeatherCondition.LIGHT_SNOW,
    WeatherCondition.SNOW,
    WeatherCondition.HEAVY_SNOW,
    WeatherCondition.SLEET,
    WeatherCondition.HAIL,
    WeatherCondition.CLEAR_WITH_SNOW,
    WeatherCondition.CLEAR_THEN_SNOW,
    WeatherCondition.CLOUDY_WITH_SNOW,
    WeatherCondition.CLOUDY_THEN_SNOW,
    WeatherCondition.SNOW_WITH_CLEAR,
    WeatherCondition.SNOW_WITH_CLOUDY,
    WeatherCondition.SNOW_WITH_RAIN,
    WeatherCondition.SNOW_THEN_CLEAR,
    WeatherCondition.SNOW_THEN_CLOUDY,
    WeatherCondition.SNOW_THEN_RAIN -> WeatherGlyphKind.SNOW

    WeatherCondition.THUNDERSTORM -> WeatherGlyphKind.THUNDER

    WeatherCondition.FOG_HAZE -> WeatherGlyphKind.FOG

    WeatherCondition.CLEAR_WITH_CLOUDY,
    WeatherCondition.CLEAR_THEN_CLOUDY,
    WeatherCondition.CLOUDY_WITH_CLEAR,
    WeatherCondition.CLOUDY_THEN_CLEAR -> WeatherGlyphKind.PARTLY_CLOUDY

    WeatherCondition.NO_CONDITION_FOUND -> WeatherGlyphKind.CLOUDY
}

/**
 * Convenience overload that resolves day/night from [daily]'s sunrise/sunset window,
 * mirroring the logic style of [WeatherCondition.toIcon].
 */
fun WeatherCondition.toGlyphKind(daily: WeatherDaily?, targetTimeMilli: Long): WeatherGlyphKind {
    val isDay = if (daily != null && daily.sunrise != null && daily.sunset != null) {
        targetTimeMilli in daily.sunrise..daily.sunset
    } else {
        true
    }
    return toGlyphKind(isDay)
}

@Composable
fun WeatherGlyph(
    kind: WeatherGlyphKind,
    size: Dp,
    color: Color = MetroTheme.colors.primaryText,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.minDimension
        when (kind) {
            WeatherGlyphKind.CLEAR_DAY -> glyphSun(color, s, 0.5f * s, 0.5f * s, 0.20f * s, 0.28f * s, 0.42f * s)
            WeatherGlyphKind.CLEAR_NIGHT -> glyphCrescent(color, s)
            WeatherGlyphKind.PARTLY_CLOUDY -> {
                glyphSun(color, s, 0.34f * s, 0.34f * s, 0.13f * s, 0.19f * s, 0.27f * s)
                glyphCloud(color, s, 0.5f * s, 0.65f * s, 0.74f * s)
            }
            WeatherGlyphKind.CLOUDY -> glyphCloud(color, s, 0.5f * s, 0.5f * s, 0.82f * s)
            WeatherGlyphKind.FOG -> glyphFog(color, s)
            WeatherGlyphKind.RAIN -> {
                glyphCloud(color, s, 0.5f * s, 0.42f * s, 0.78f * s)
                glyphRain(color, s, drops = 3)
            }
            WeatherGlyphKind.HEAVY_RAIN -> {
                glyphCloud(color, s, 0.5f * s, 0.40f * s, 0.78f * s)
                glyphRain(color, s, drops = 4)
            }
            WeatherGlyphKind.SNOW -> {
                glyphCloud(color, s, 0.5f * s, 0.42f * s, 0.78f * s)
                glyphSnow(color, s)
            }
            WeatherGlyphKind.THUNDER -> {
                glyphCloud(color, s, 0.5f * s, 0.40f * s, 0.78f * s)
                glyphBolt(color, s)
            }
        }
    }
}

private fun DrawScope.glyphSun(
    color: Color,
    s: Float,
    cx: Float,
    cy: Float,
    radius: Float,
    rayInner: Float,
    rayOuter: Float,
) {
    drawCircle(color = color, radius = radius, center = Offset(cx, cy))
    val stroke = (s * 0.055f).coerceAtLeast(1f)
    for (i in 0 until 8) {
        val angle = Math.toRadians((i * 45).toDouble())
        val c = cos(angle).toFloat()
        val sn = sin(angle).toFloat()
        drawLine(
            color = color,
            start = Offset(cx + c * rayInner, cy + sn * rayInner),
            end = Offset(cx + c * rayOuter, cy + sn * rayOuter),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.glyphCrescent(color: Color, s: Float) {
    val outer = Rect(center = Offset(0.5f * s, 0.5f * s), radius = 0.30f * s)
    val inner = Rect(center = Offset(0.5f * s + 0.16f * s, 0.5f * s), radius = 0.30f * s)
    val path = Path().apply {
        fillType = PathFillType.EvenOdd
        addOval(outer)
        addOval(inner)
    }
    drawPath(path = path, color = color)
}

private fun DrawScope.glyphCloud(color: Color, s: Float, cx: Float, cy: Float, width: Float) {
    val h = width * 0.62f
    val left = cx - width / 2f
    val top = cy - h / 2f
    drawRect(
        color = color,
        topLeft = Offset(left, top + h * 0.52f),
        size = Size(width, h * 0.48f),
    )
    drawCircle(color = color, radius = h * 0.42f, center = Offset(left + width * 0.28f, top + h * 0.56f))
    drawCircle(color = color, radius = h * 0.52f, center = Offset(left + width * 0.50f, top + h * 0.38f))
    drawCircle(color = color, radius = h * 0.38f, center = Offset(left + width * 0.72f, top + h * 0.58f))
}

private fun DrawScope.glyphRain(color: Color, s: Float, drops: Int) {
    val stroke = (s * 0.055f).coerceAtLeast(1f)
    val top = 0.70f * s
    val bottom = 0.88f * s
    val spanX = 0.42f * s
    val startX = 0.5f * s - spanX / 2f
    val step = if (drops > 1) spanX / (drops - 1) else 0f
    for (i in 0 until drops) {
        val x = startX + step * i
        drawLine(
            color = color,
            start = Offset(x + s * 0.04f, top),
            end = Offset(x, bottom),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.glyphSnow(color: Color, s: Float) {
    val r = 0.05f * s
    val y = 0.80f * s
    listOf(0.34f, 0.5f, 0.66f).forEach { fx ->
        drawCircle(color = color, radius = r, center = Offset(fx * s, y))
    }
}

private fun DrawScope.glyphBolt(color: Color, s: Float) {
    val path = Path().apply {
        moveTo(0.54f * s, 0.64f * s)
        lineTo(0.40f * s, 0.82f * s)
        lineTo(0.50f * s, 0.82f * s)
        lineTo(0.44f * s, 0.96f * s)
        lineTo(0.62f * s, 0.76f * s)
        lineTo(0.51f * s, 0.76f * s)
        close()
    }
    drawPath(path = path, color = color)
}

private fun DrawScope.glyphFog(color: Color, s: Float) {
    val stroke = (s * 0.07f).coerceAtLeast(1f)
    val bars = listOf(
        0.18f to 0.82f,
        0.26f to 0.90f,
        0.16f to 0.74f,
    )
    bars.forEachIndexed { index, (start, end) ->
        val y = (0.34f + index * 0.17f) * s
        drawLine(
            color = color,
            start = Offset(start * s, y),
            end = Offset(end * s, y),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}
