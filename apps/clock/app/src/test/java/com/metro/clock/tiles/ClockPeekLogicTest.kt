package com.metro.clock.tiles

import com.metro.clock.data.AlarmEntity
import com.metro.clock.data.StopwatchEntity
import com.metro.clock.data.TimerEntity
import com.metro.clock.timers.TimerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockPeekLogicTest {

    private val now = 1_000_000L

    private fun runningTimer(id: Long, remaining: Long) = TimerEntity(
        id = id,
        label = "T$id",
        durationMillis = remaining,
        state = TimerState.RUNNING.name,
        remainingWhenPaused = remaining,
        targetElapsedRealtime = now + remaining,
        targetEpochMillis = 0L,
        createdOrder = id,
    )

    private fun pausedTimer(id: Long, remaining: Long) = TimerEntity(
        id = id,
        label = "P$id",
        durationMillis = remaining,
        state = TimerState.PAUSED.name,
        remainingWhenPaused = remaining,
        targetElapsedRealtime = 0L,
        targetEpochMillis = 0L,
        createdOrder = id,
    )

    private fun finishedTimer(id: Long) = TimerEntity(
        id = id,
        label = "F$id",
        durationMillis = 0L,
        state = TimerState.FINISHED.name,
        remainingWhenPaused = 0L,
        targetElapsedRealtime = 0L,
        targetEpochMillis = 0L,
        createdOrder = id,
    )

    private fun stopwatch(id: Long, running: Boolean, accumulated: Long) = StopwatchEntity(
        id = id,
        name = "S$id",
        running = running,
        accumulatedElapsedMillis = accumulated,
        runningSinceElapsedRealtime = if (running) now else 0L,
        runningSinceEpochMillis = 0L,
        createdOrder = id,
    )

    private fun alarm(id: Long) = AlarmEntity(id = id, hour = 7, minute = 0)

    @Test
    fun deterministicOrder_matchesPriority() {
        val ordered = ClockPeekLogic.order(
            timers = listOf(finishedTimer(9), runningTimer(1, 120_000L), runningTimer(2, 900_000L), pausedTimer(3, 300_000L)),
            stopwatches = listOf(stopwatch(4, running = true, accumulated = 0L), stopwatch(5, running = false, accumulated = 5_000L)),
            nextAlarm = alarm(7) to 2_000_000L,
            nowElapsedRealtime = now,
        )
        assertEquals(listOf(9L, 1L, 2L, 3L, 4L, 5L, 7L), ordered.map { it.stableOrder })
        assertTrue(ordered[0] is ClockPeekLogic.FinishedTimer)
        assertTrue(ordered[1] is ClockPeekLogic.RunningTimer)
        assertTrue(ordered[4] is ClockPeekLogic.RunningStopwatch)
        assertTrue(ordered[5] is ClockPeekLogic.PausedStopwatch)
        assertTrue(ordered[6] is ClockPeekLogic.NextAlarm)
    }

    @Test
    fun runningTimersSortedBySoonestCompletion() {
        val ordered = ClockPeekLogic.order(
            timers = listOf(runningTimer(1, 900_000L), runningTimer(2, 120_000L)),
            stopwatches = emptyList(),
            nextAlarm = null,
            nowElapsedRealtime = now,
        )
        assertEquals(listOf(2L, 1L), ordered.map { it.stableOrder })
    }

    @Test
    fun overflowBeyondSix() {
        val timers = (1L..9L).map { runningTimer(it, it * 1000L) }
        val ordered = ClockPeekLogic.order(timers, emptyList(), null, now)
        assertEquals(9, ordered.size)
        assertEquals(3, ClockPeekLogic.overflowCount(ordered.size))
        assertEquals(0, ClockPeekLogic.overflowCount(4))
    }

    @Test
    fun hasActiveItems() {
        assertFalse(ClockPeekLogic.hasActiveItems(emptyList(), emptyList()))
        assertTrue(ClockPeekLogic.hasActiveItems(listOf(runningTimer(1, 1000L)), emptyList()))
        assertTrue(ClockPeekLogic.hasActiveItems(emptyList(), listOf(stopwatch(1, running = true, accumulated = 0L))))
        assertFalse(ClockPeekLogic.hasActiveItems(emptyList(), listOf(stopwatch(1, running = false, accumulated = 0L))))
    }
}
