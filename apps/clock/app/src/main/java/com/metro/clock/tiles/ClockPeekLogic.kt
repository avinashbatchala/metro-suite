package com.metro.clock.tiles

import com.metro.clock.alarms.AlarmSchedule
import com.metro.clock.data.AlarmEntity
import com.metro.clock.data.StopwatchEntity
import com.metro.clock.data.TimerEntity
import com.metro.clock.timers.TimerLogic
import com.metro.clock.timers.TimerState

/**
 * Deterministic peek ordering for the primary Clock tile. Stable across recompositions:
 *
 * 1. finished timers awaiting attention
 * 2. running timers, soonest completion first
 * 3. paused timers, soonest remaining first
 * 4. running stopwatches
 * 5. paused stopwatches
 * 6. next enabled alarm (final informational peek)
 */
object ClockPeekLogic {

    const val MAX_PEEKS = 6

    sealed interface Source {
        val stableOrder: Long
    }

    data class FinishedTimer(val timer: TimerEntity) : Source {
        override val stableOrder: Long get() = timer.id
    }

    data class RunningTimer(val timer: TimerEntity, val remaining: Long) : Source {
        override val stableOrder: Long get() = timer.id
    }

    data class PausedTimer(val timer: TimerEntity, val remaining: Long) : Source {
        override val stableOrder: Long get() = timer.id
    }

    data class RunningStopwatch(val stopwatch: StopwatchEntity) : Source {
        override val stableOrder: Long get() = stopwatch.id
    }

    data class PausedStopwatch(val stopwatch: StopwatchEntity) : Source {
        override val stableOrder: Long get() = stopwatch.id
    }

    data class NextAlarm(val alarm: AlarmEntity, val triggerMillis: Long) : Source {
        override val stableOrder: Long get() = alarm.id
    }

    fun order(
        timers: List<TimerEntity>,
        stopwatches: List<StopwatchEntity>,
        nextAlarm: Pair<AlarmEntity, Long>?,
        nowElapsedRealtime: Long,
    ): List<Source> {
        val result = ArrayList<Source>()

        timers.filter { TimerState.fromName(it.state) == TimerState.FINISHED }
            .sortedBy { it.id }
            .forEach { result.add(FinishedTimer(it)) }

        timers.filter { TimerState.fromName(it.state) == TimerState.RUNNING }
            .map { RunningTimer(it, TimerLogic.remainingMillis(it, nowElapsedRealtime)) }
            .sortedWith(compareBy({ it.remaining }, { it.timer.id }))
            .forEach { result.add(it) }

        timers.filter { TimerState.fromName(it.state) == TimerState.PAUSED }
            .map { PausedTimer(it, TimerLogic.remainingMillis(it, nowElapsedRealtime)) }
            .sortedWith(compareBy({ it.remaining }, { it.timer.id }))
            .forEach { result.add(it) }

        stopwatches.filter { it.running }
            .sortedBy { it.createdOrder }
            .forEach { result.add(RunningStopwatch(it)) }

        stopwatches.filter { !it.running && it.accumulatedElapsedMillis > 0L }
            .sortedBy { it.createdOrder }
            .forEach { result.add(PausedStopwatch(it)) }

        nextAlarm?.let { result.add(NextAlarm(it.first, it.second)) }
        return result
    }

    fun overflowCount(total: Int, max: Int = MAX_PEEKS): Int = (total - max).coerceAtLeast(0)

    /** True when any timer or stopwatch is active (finished/running/paused with content). */
    fun hasActiveItems(timers: List<TimerEntity>, stopwatches: List<StopwatchEntity>): Boolean =
        timers.any { TimerState.fromName(it.state) != TimerState.READY } ||
            stopwatches.any { it.running || it.accumulatedElapsedMillis > 0L }

    fun nextAlarm(
        alarms: List<AlarmEntity>,
        fromMillis: Long,
        zone: java.time.ZoneId,
    ): Pair<AlarmEntity, Long>? = AlarmSchedule.nextAlarm(alarms, fromMillis, zone)
}
