package com.metro.clock.stopwatch

import com.metro.clock.data.StopwatchEntity
import com.metro.system.MetroTileTemporalRender

/** Pure stopwatch math — elapsed derives from monotonic timestamps. */
object StopwatchLogic {

    fun elapsedMillis(stopwatch: StopwatchEntity, nowElapsedRealtime: Long): Long {
        val base = stopwatch.accumulatedElapsedMillis.coerceAtLeast(0L)
        if (!stopwatch.running) return base
        val since = stopwatch.runningSinceElapsedRealtime
        return (base + (nowElapsedRealtime - since)).coerceAtLeast(0L)
    }

    fun displayPrecise(stopwatch: StopwatchEntity, nowElapsedRealtime: Long): String =
        MetroTileTemporalRender.formatStopwatchPrecise(elapsedMillis(stopwatch, nowElapsedRealtime))

    fun display(stopwatch: StopwatchEntity, nowElapsedRealtime: Long): String =
        MetroTileTemporalRender.formatDuration(elapsedMillis(stopwatch, nowElapsedRealtime), roundUp = false)
}
