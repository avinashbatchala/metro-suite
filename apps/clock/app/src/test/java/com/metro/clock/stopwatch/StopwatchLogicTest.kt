package com.metro.clock.stopwatch

import com.metro.clock.data.StopwatchEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class StopwatchLogicTest {

    private fun stopwatch(
        running: Boolean,
        accumulated: Long = 0L,
        since: Long = 0L,
    ) = StopwatchEntity(
        id = 1L,
        name = "Run",
        running = running,
        accumulatedElapsedMillis = accumulated,
        runningSinceElapsedRealtime = since,
        runningSinceEpochMillis = 0L,
        createdOrder = 1L,
    )

    @Test
    fun running_addsTimeSinceStart() {
        val s = stopwatch(running = true, accumulated = 100_000L, since = 1_000_000L)
        assertEquals(102_000L, StopwatchLogic.elapsedMillis(s, 1_002_000L))
    }

    @Test
    fun paused_usesAccumulatedOnly() {
        val s = stopwatch(running = false, accumulated = 3_723_450L)
        assertEquals(3_723_450L, StopwatchLogic.elapsedMillis(s, 9_999_999L))
    }

    @Test
    fun largeDurations() {
        val s = stopwatch(running = true, accumulated = 0L, since = 0L)
        // 100 hours
        assertEquals(360_000_000L, StopwatchLogic.elapsedMillis(s, 360_000_000L))
        assertEquals("100:00:00.00", StopwatchLogic.displayPrecise(s, 360_000_000L))
    }

    @Test
    fun preciseFormatting() {
        val s = stopwatch(running = true, accumulated = 0L, since = 0L)
        assertEquals("01:42.18", StopwatchLogic.displayPrecise(s, 102_180L))
    }
}
