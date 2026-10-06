package com.metro.system

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Pure, framework-free temporal rendering shared by the launcher (tile faces) and tests. Given a
 * [MetroTileTemporalState] and the current monotonic / wall timestamps, produces the display
 * strings — so tiles tick locally with no per-second broadcasts.
 */
object MetroTileTemporalRender {

    data class Rendered(
        val primary: String?,
        val secondary: String?,
    )

    private val time12: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    private val time24: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)

    fun render(
        state: MetroTileTemporalState,
        nowElapsedRealtimeMillis: Long,
        nowEpochMillis: Long,
        use24Hour: Boolean = false,
        deviceZone: ZoneId = ZoneId.systemDefault(),
    ): Rendered = when (state.kind) {
        MetroTemporalKind.TIMER -> renderTimer(state, nowElapsedRealtimeMillis)
        MetroTemporalKind.STOPWATCH -> renderStopwatch(state, nowElapsedRealtimeMillis)
        MetroTemporalKind.WORLD_CLOCK -> renderWorldClock(state, nowEpochMillis, use24Hour, deviceZone)
        else -> Rendered(state.label, null)
    }

    /** Remaining milliseconds, clamped at zero. Never negative. */
    fun timerRemainingMillis(state: MetroTileTemporalState, nowElapsedRealtimeMillis: Long): Long {
        if (!state.running) return (state.pausedRemainingMillis ?: 0L).coerceAtLeast(0L)
        val target = state.targetElapsedRealtimeMillis
        val derived = if (target != null) {
            target
        } else {
            nowElapsedRealtimeMillis + (state.pausedRemainingMillis ?: 0L)
        }
        return (derived - nowElapsedRealtimeMillis).coerceAtLeast(0L)
    }

    /** Elapsed milliseconds for a stopwatch, never negative. */
    fun stopwatchElapsedMillis(state: MetroTileTemporalState, nowElapsedRealtimeMillis: Long): Long {
        val base = (state.accumulatedElapsedMillis ?: 0L).coerceAtLeast(0L)
        if (!state.running) return base
        val since = state.runningSinceElapsedRealtimeMillis ?: nowElapsedRealtimeMillis
        return (base + (nowElapsedRealtimeMillis - since)).coerceAtLeast(0L)
    }

    private fun renderTimer(state: MetroTileTemporalState, nowElapsed: Long): Rendered {
        val remaining = timerRemainingMillis(state, nowElapsed)
        val secondary = when {
            state.finished || remaining == 0L -> "finished"
            state.running -> "remaining"
            else -> "paused"
        }
        return Rendered(formatDuration(remaining, roundUp = true), secondary)
    }

    private fun renderStopwatch(state: MetroTileTemporalState, nowElapsed: Long): Rendered {
        val elapsed = stopwatchElapsedMillis(state, nowElapsed)
        return Rendered(formatDuration(elapsed, roundUp = false), if (state.running) "running" else "paused")
    }

    private fun renderWorldClock(
        state: MetroTileTemporalState,
        nowEpochMillis: Long,
        use24Hour: Boolean,
        deviceZone: ZoneId,
    ): Rendered {
        val zone = runCatching { ZoneId.of(state.zoneId ?: "") }.getOrDefault(deviceZone)
        val instant = Instant.ofEpochMilli(nowEpochMillis)
        val cityDateTime = instant.atZone(zone)
        val time = cityDateTime.format(if (use24Hour) time24 else time12).uppercase(Locale.US)
        val offset = dayOffsetLabel(cityDateTime.toLocalDate(), instant.atZone(deviceZone).toLocalDate())
        return Rendered(time, offset)
    }

    fun dayOffsetLabel(cityDate: LocalDate, deviceDate: LocalDate): String? = when {
        cityDate.isAfter(deviceDate) -> "tomorrow"
        cityDate.isBefore(deviceDate) -> "yesterday"
        else -> null
    }

    /** `H:MM:SS` when ≥ 1 hour, otherwise `MM:SS`. [roundUp] keeps countdowns from hitting 0 early. */
    fun formatDuration(millis: Long, roundUp: Boolean): String {
        val clamped = millis.coerceAtLeast(0L)
        val totalSeconds = if (roundUp) (clamped + 999L) / 1000L else clamped / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0L) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    /** `H:MM:SS.hh` — hundredths used by the stopwatch detail UI. */
    fun formatStopwatchPrecise(millis: Long): String {
        val clamped = millis.coerceAtLeast(0L)
        val hours = clamped / 3_600_000L
        val minutes = (clamped % 3_600_000L) / 60_000L
        val seconds = (clamped % 60_000L) / 1000L
        val hundredths = (clamped % 1000L) / 10L
        return if (hours > 0L) {
            String.format(Locale.US, "%d:%02d:%02d.%02d", hours, minutes, seconds, hundredths)
        } else {
            String.format(Locale.US, "%02d:%02d.%02d", minutes, seconds, hundredths)
        }
    }
}
