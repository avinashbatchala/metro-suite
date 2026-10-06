package com.metro.training.ui.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.metro.training.domain.analytics.ExercisePoint
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

private val ChartHeight = 160.dp

/**
 * WP8.1-styled line chart: hairline baseline + grid, accent polyline with dots, min/max labels.
 * No third-party chart library and no Material styling.
 */
@Composable
fun MetroLineChart(
    points: List<ExercisePoint>,
    valueText: (Double) -> String,
    modifier: Modifier = Modifier,
    lineColor: Color = MetroTheme.colors.accent,
) {
    if (points.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth().height(ChartHeight), contentAlignment = Alignment.Center) {
            MetroText(
                text = "no data in range",
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.secondaryText,
            )
        }
        return
    }

    val min = points.minOf { it.value }
    val max = points.maxOf { it.value }
    val span = (max - min).takeIf { it > 0.0 } ?: 1.0
    val gridColor = MetroTheme.colors.secondaryText.copy(alpha = 0.25f)

    Box(modifier = modifier.fillMaxWidth().height(ChartHeight)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val left = 4f
            val right = size.width - 4f
            val top = 8f
            val bottom = size.height - 8f
            // grid lines (top, middle, baseline)
            listOf(0f, 0.5f, 1f).forEach { f ->
                val y = top + (bottom - top) * f
                drawLine(gridColor, Offset(left, y), Offset(right, y), strokeWidth = 1f)
            }
            if (points.size == 1) {
                val y = bottom - ((points[0].value - min) / span * (bottom - top)).toFloat()
                drawCircle(lineColor, radius = 6f, center = Offset((left + right) / 2f, y))
                return@Canvas
            }
            val stepX = (right - left) / (points.size - 1)
            val path = Path()
            points.forEachIndexed { index, point ->
                val x = left + stepX * index
                val y = bottom - ((point.value - min) / span * (bottom - top)).toFloat()
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, lineColor, style = Stroke(width = 3f))
            points.forEachIndexed { index, point ->
                val x = left + stepX * index
                val y = bottom - ((point.value - min) / span * (bottom - top)).toFloat()
                drawCircle(lineColor, radius = 4f, center = Offset(x, y))
            }
        }
        MetroText(
            text = valueText(max),
            style = MetroTextStyle.ListItemSubtitle,
            color = MetroTheme.colors.secondaryText,
            modifier = Modifier.align(Alignment.TopStart).padding(start = 8.dp, top = 2.dp),
        )
        MetroText(
            text = valueText(min),
            style = MetroTextStyle.ListItemSubtitle,
            color = MetroTheme.colors.secondaryText,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 8.dp, bottom = 2.dp),
        )
    }
}

data class MetroBar(val label: String, val value: Double, val comparison: Double? = null)

/**
 * Horizontal Metro bar list (e.g. estimated weekly sets per muscle). Optional [MetroBar.comparison]
 * draws a thin secondary marker for the previous period.
 */
@Composable
fun MetroBarChart(
    bars: List<MetroBar>,
    valueText: (Double) -> String,
    modifier: Modifier = Modifier,
) {
    val max = (bars.maxOfOrNull { maxOf(it.value, it.comparison ?: 0.0) } ?: 1.0).coerceAtLeast(1.0)
    Column(modifier = modifier.fillMaxWidth()) {
        bars.forEach { bar ->
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    MetroText(text = bar.label, style = MetroTextStyle.ListItemTitle)
                    MetroText(
                        text = valueText(bar.value) +
                            (bar.comparison?.let { "  ·  ${valueText(it)}" } ?: ""),
                        style = MetroTextStyle.ListItemSubtitle,
                        color = MetroTheme.colors.secondaryText,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier.fillMaxWidth().height(10.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((bar.value / max).toFloat().coerceIn(0f, 1f))
                            .height(10.dp)
                            .background(MetroTheme.colors.accent),
                    )
                    bar.comparison?.let { previous ->
                        val fraction = (previous / max).toFloat().coerceIn(0f, 1f)
                        Box(modifier = Modifier.fillMaxWidth(fraction).height(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .width(2.dp)
                                    .height(10.dp)
                                    .background(MetroTheme.colors.secondaryText),
                            )
                        }
                    }
                }
            }
        }
    }
}
