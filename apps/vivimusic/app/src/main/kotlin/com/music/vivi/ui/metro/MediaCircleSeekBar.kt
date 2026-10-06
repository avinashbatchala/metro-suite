/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.metro

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroLoadingDots
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

private val SeekRowHeight = 40.dp
private val SeekLabelGap = 12.dp
private val SeekTrackThickness = 2.dp
private val SeekThumbDiameter = 14.dp
private val SeekThumbStroke = 3.dp

internal fun formatElapsed(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

internal fun formatRemaining(positionMs: Long, durationMs: Long): String {
    if (durationMs <= 0L) return "-0:00"
    val remaining = (durationMs - positionMs).coerceAtLeast(0L)
    return "-${formatElapsed(remaining)}"
}

/**
 * Xbox Music now-playing scrubber: elapsed time, a hairline track carrying a hollow circular
 * thumb, then time remaining.
 */
@Composable
fun MediaCircleSeekBar(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
) {
    val seekable = !loading && durationMs > 0L
    val currentOnSeek by rememberUpdatedState(onSeek)
    var scrubFraction by remember { mutableStateOf<Float?>(null) }
    val playedFraction = if (seekable) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val fraction = scrubFraction ?: playedFraction
    val labelPositionMs = when {
        loading -> 0L
        else -> scrubFraction?.let { (it * durationMs).toLong() } ?: positionMs
    }

    val foreground = MetroTheme.colors.primaryText
    val trackColor = foreground.copy(alpha = 0.2f)
    val thumbRadiusPx = with(LocalDensity.current) { SeekThumbDiameter.toPx() / 2f }

    fun fractionAt(x: Float, width: Int): Float {
        val travel = (width - thumbRadiusPx * 2f).coerceAtLeast(1f)
        return ((x - thumbRadiusPx) / travel).coerceIn(0f, 1f)
    }

    Row(
        modifier = modifier.height(SeekRowHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MetroText(
            text = formatElapsed(labelPositionMs),
            style = MetroTextStyle.Body,
            color = if (loading) MetroTheme.colors.secondaryText else foreground,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = SeekLabelGap)
                .clipToBounds()
                .pointerInput(seekable, durationMs) {
                    if (!seekable) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        try {
                            scrubFraction = fractionAt(down.position.x, size.width)
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                change.consume()
                                if (change.changedToUpIgnoreConsumed()) break
                                scrubFraction = fractionAt(change.position.x, size.width)
                            }
                            scrubFraction?.let { currentOnSeek((it * durationMs).toLong()) }
                        } finally {
                            scrubFraction = null
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(SeekRowHeight)) {
                val centerY = size.height / 2f
                drawLine(
                    color = trackColor,
                    start = Offset(0f, centerY),
                    end = Offset(size.width, centerY),
                    strokeWidth = SeekTrackThickness.toPx(),
                    cap = StrokeCap.Butt,
                )
                if (!loading) {
                    val travel = (size.width - thumbRadiusPx * 2f).coerceAtLeast(0f)
                    val thumbX = thumbRadiusPx + fraction * travel
                    val strokePx = SeekThumbStroke.toPx()
                    val holeRadiusPx = thumbRadiusPx - strokePx

                    val playedEnd = thumbX - holeRadiusPx
                    if (playedEnd > 0f) {
                        drawLine(
                            color = foreground,
                            start = Offset(0f, centerY),
                            end = Offset(playedEnd, centerY),
                            strokeWidth = SeekTrackThickness.toPx(),
                            cap = StrokeCap.Butt,
                        )
                    }
                    drawCircle(
                        color = foreground,
                        radius = thumbRadiusPx - strokePx / 2f,
                        center = Offset(thumbX, centerY),
                        style = Stroke(width = strokePx),
                    )
                }
            }
            if (loading) {
                MetroLoadingDots()
            }
        }
        MetroText(
            text = if (loading) {
                formatRemaining(0L, 0L)
            } else {
                formatRemaining(labelPositionMs, durationMs)
            },
            style = MetroTextStyle.Body,
            color = if (loading) MetroTheme.colors.secondaryText else foreground,
        )
    }
}
