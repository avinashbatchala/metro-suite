package com.metro.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * WP8.1 content-press feedback — a short down-left nudge, never Material ripple or a
 * square darkened selection wash (METRO-UX-LANGUAGE §5.4).
 *
 * Press snaps to the nudged pose; release eases back quickly. [onClick] fires
 * immediately on tap (no deferred handler).
 */
val MetroPressNudge: Dp = 6.dp
const val MetroPressNudgeInMs: Int = 50
/** @deprecated Hold is no longer used — onClick fires immediately on tap. */
const val MetroPressNudgeHoldMs: Int = 0
/** Ease-back duration on release — keep snappy so the row does not look stuck. */
const val MetroPressNudgeOutMs: Int = 90

private class JobHolder(var job: Job? = null)

/**
 * Translates this node down-left while [interactionSource] is pressed.
 * Prefer [Modifier.metroClickable] for text-only rows.
 */
fun Modifier.metroPressNudge(
    interactionSource: MutableInteractionSource,
): Modifier = composed {
    val press = remember { Animatable(0f) }
    val nudgePx = with(LocalDensity.current) { MetroPressNudge.toPx() }
    LaunchedEffect(interactionSource) {
        var cycle: Job? = null
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    cycle?.cancel()
                    cycle = launch { press.snapTo(1f) }
                }
                is PressInteraction.Release, is PressInteraction.Cancel -> {
                    cycle?.cancel()
                    cycle = launch {
                        if (press.value > 0f) {
                            press.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(
                                    durationMillis = MetroPressNudgeOutMs,
                                    easing = MetroTransitions.PageEasing,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
    graphicsLayer {
        val amount = press.value
        translationX = -nudgePx * amount
        translationY = nudgePx * amount
    }
}

/**
 * Text-row tap with press-nudge and **no** Compose indication.
 *
 * Use for **text-only rows** (list items, showing labels, filter options, hub links).
 * Do **not** use on tiles — those keep `indication = null` with no shift.
 *
 * Snap down-left on press → ease back on release → [onClick] runs immediately.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.metroClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = Role.Button,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val press = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val latestOnClick by rememberUpdatedState(onClick)
    val latestOnLongClick by rememberUpdatedState(onLongClick)
    val animJob = remember { JobHolder() }
    val nudgePx = with(LocalDensity.current) { MetroPressNudge.toPx() }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    animJob.job?.cancel()
                    animJob.job = scope.launch { press.snapTo(1f) }
                }
                is PressInteraction.Release, is PressInteraction.Cancel -> {
                    animJob.job?.cancel()
                    animJob.job = scope.launch {
                        if (press.value > 0f) {
                            press.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(
                                    durationMillis = MetroPressNudgeOutMs,
                                    easing = MetroTransitions.PageEasing,
                                ),
                            )
                        } else {
                            press.snapTo(0f)
                        }
                    }
                }
            }
        }
    }

    graphicsLayer {
        val amount = press.value
        translationX = -nudgePx * amount
        translationY = nudgePx * amount
    }.then(
        if (onLongClick != null) {
            Modifier.combinedClickable(
                enabled = enabled,
                onClickLabel = onClickLabel,
                role = role,
                onLongClick = { latestOnLongClick?.invoke() },
                interactionSource = interactionSource,
                indication = null,
                onClick = { latestOnClick() },
            )
        } else {
            Modifier.clickable(
                enabled = enabled,
                onClickLabel = onClickLabel,
                role = role,
                interactionSource = interactionSource,
                indication = null,
                onClick = { latestOnClick() },
            )
        },
    )
}
