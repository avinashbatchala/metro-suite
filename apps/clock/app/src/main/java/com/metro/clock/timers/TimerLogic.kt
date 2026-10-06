package com.metro.clock.timers

import com.metro.clock.data.TimerEntity
import com.metro.system.MetroTileTemporalRender

enum class TimerState {
    READY,
    RUNNING,
    PAUSED,
    FINISHED,
    ;

    companion object {
        fun fromName(name: String?): TimerState =
            entries.firstOrNull { it.name == name } ?: READY
    }
}

/** Pure timer math — timers derive remaining time from monotonic timestamps, never a tick loop. */
object TimerLogic {

    fun remainingMillis(timer: TimerEntity, nowElapsedRealtime: Long): Long {
        val state = TimerState.fromName(timer.state)
        return when (state) {
            TimerState.RUNNING -> (timer.targetElapsedRealtime - nowElapsedRealtime).coerceAtLeast(0L)
            TimerState.PAUSED -> timer.remainingWhenPaused.coerceAtLeast(0L)
            TimerState.READY -> timer.durationMillis.coerceAtLeast(0L)
            TimerState.FINISHED -> 0L
        }
    }

    fun progressFraction(timer: TimerEntity, nowElapsedRealtime: Long): Float {
        if (timer.durationMillis <= 0L) return 0f
        val remaining = remainingMillis(timer, nowElapsedRealtime)
        return (1f - remaining.toFloat() / timer.durationMillis.toFloat()).coerceIn(0f, 1f)
    }

    /** True once the timer is running and its monotonic deadline has passed. */
    fun hasExpired(timer: TimerEntity, nowElapsedRealtime: Long): Boolean =
        TimerState.fromName(timer.state) == TimerState.RUNNING &&
            timer.targetElapsedRealtime <= nowElapsedRealtime

    fun display(timer: TimerEntity, nowElapsedRealtime: Long): String =
        MetroTileTemporalRender.formatDuration(remainingMillis(timer, nowElapsedRealtime), roundUp = true)
}
