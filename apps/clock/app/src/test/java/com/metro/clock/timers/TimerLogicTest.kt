package com.metro.clock.timers

import com.metro.clock.data.TimerEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerLogicTest {

    private fun timer(
        state: TimerState,
        duration: Long = 600_000L,
        remainingWhenPaused: Long = 0L,
        targetElapsed: Long = 0L,
    ) = TimerEntity(
        id = 1L,
        label = "Pasta",
        durationMillis = duration,
        state = state.name,
        remainingWhenPaused = remainingWhenPaused,
        targetElapsedRealtime = targetElapsed,
        targetEpochMillis = 0L,
        createdOrder = 1L,
    )

    @Test
    fun running_derivesRemainingFromMonotonicTarget() {
        val t = timer(TimerState.RUNNING, targetElapsed = 100_000L)
        assertEquals(40_000L, TimerLogic.remainingMillis(t, 60_000L))
    }

    @Test
    fun running_neverNegative() {
        val t = timer(TimerState.RUNNING, targetElapsed = 100_000L)
        assertEquals(0L, TimerLogic.remainingMillis(t, 500_000L))
    }

    @Test
    fun paused_usesPausedRemaining() {
        val t = timer(TimerState.PAUSED, remainingWhenPaused = 252_000L)
        assertEquals(252_000L, TimerLogic.remainingMillis(t, 999_999L))
    }

    @Test
    fun ready_usesFullDuration() {
        val t = timer(TimerState.READY, duration = 300_000L)
        assertEquals(300_000L, TimerLogic.remainingMillis(t, 123L))
    }

    @Test
    fun finished_isZero() {
        assertEquals(0L, TimerLogic.remainingMillis(timer(TimerState.FINISHED), 123L))
    }

    @Test
    fun expiryDetection() {
        val t = timer(TimerState.RUNNING, targetElapsed = 100_000L)
        assertFalse(TimerLogic.hasExpired(t, 99_999L))
        assertTrue(TimerLogic.hasExpired(t, 100_000L))
        assertTrue(TimerLogic.hasExpired(t, 100_001L))
    }

    @Test
    fun displayRoundsUpSoRunningTimerNeverShowsZeroEarly() {
        val t = timer(TimerState.RUNNING, targetElapsed = 60_500L)
        assertEquals("00:01", TimerLogic.display(t, 60_000L))
    }
}
