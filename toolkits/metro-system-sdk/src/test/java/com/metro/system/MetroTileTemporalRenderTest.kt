package com.metro.system

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId

class MetroTileTemporalRenderTest {

    private val deviceZone = ZoneId.of("UTC")

    @Test
    fun timerRunning_showsRemaining() {
        val state = MetroTileTemporalState(
            kind = MetroTemporalKind.TIMER,
            running = true,
            targetElapsedRealtimeMillis = 100_000L,
        )
        assertEquals(42_000L, MetroTileTemporalRender.timerRemainingMillis(state, 58_000L))
        assertEquals("00:42", MetroTileTemporalRender.render(state, 58_000L, 0L, deviceZone = deviceZone).primary)
        assertEquals("remaining", MetroTileTemporalRender.render(state, 58_000L, 0L, deviceZone = deviceZone).secondary)
    }

    @Test
    fun timerPaused_showsPausedRemaining() {
        val state = MetroTileTemporalState(
            kind = MetroTemporalKind.TIMER,
            running = false,
            pausedRemainingMillis = 252_000L,
        )
        assertEquals("04:12", MetroTileTemporalRender.render(state, 9_999L, 0L, deviceZone = deviceZone).primary)
        assertEquals("paused", MetroTileTemporalRender.render(state, 9_999L, 0L, deviceZone = deviceZone).secondary)
    }

    @Test
    fun timerFinished_clampsToZeroNeverNegative() {
        val state = MetroTileTemporalState(kind = MetroTemporalKind.TIMER, running = true, targetElapsedRealtimeMillis = 1_000L)
        assertEquals(0L, MetroTileTemporalRender.timerRemainingMillis(state, 500_000L))
        val rendered = MetroTileTemporalRender.render(state, 500_000L, 0L, deviceZone = deviceZone)
        assertEquals("00:00", rendered.primary)
        assertEquals("finished", rendered.secondary)
    }

    @Test
    fun timerHoursFormatting() {
        val state = MetroTileTemporalState(
            kind = MetroTemporalKind.TIMER,
            running = false,
            pausedRemainingMillis = 3_723_000L,
        )
        assertEquals("1:02:03", MetroTileTemporalRender.render(state, 0L, 0L, deviceZone = deviceZone).primary)
    }

    @Test
    fun stopwatchRunning() {
        val state = MetroTileTemporalState(
            kind = MetroTemporalKind.STOPWATCH,
            running = true,
            accumulatedElapsedMillis = 100_000L,
            runningSinceElapsedRealtimeMillis = 1_000_000L,
        )
        assertEquals(102_000L, MetroTileTemporalRender.stopwatchElapsedMillis(state, 1_002_000L))
        assertEquals("01:42", MetroTileTemporalRender.render(state, 1_002_000L, 0L, deviceZone = deviceZone).primary)
        assertEquals("running", MetroTileTemporalRender.render(state, 1_002_000L, 0L, deviceZone = deviceZone).secondary)
    }

    @Test
    fun stopwatchPaused_showsAccumulated() {
        val state = MetroTileTemporalState(
            kind = MetroTemporalKind.STOPWATCH,
            running = false,
            accumulatedElapsedMillis = 3_723_450L,
        )
        val rendered = MetroTileTemporalRender.render(state, 500_000L, 0L, deviceZone = deviceZone)
        assertEquals("1:02:03", rendered.primary)
        assertEquals("paused", rendered.secondary)
    }

    @Test
    fun worldClockTwelveAndTwentyFourHour() {
        val epoch = 1_700_000_000_000L
        val london = MetroTileTemporalState(kind = MetroTemporalKind.WORLD_CLOCK, zoneId = "Europe/London")
        val twelve = MetroTileTemporalRender.render(london, 0L, epoch, use24Hour = false, deviceZone = deviceZone)
        val twentyFour = MetroTileTemporalRender.render(london, 0L, epoch, use24Hour = true, deviceZone = deviceZone)
        // 22:13 UTC on 2023-11-14 in winter (London = UTC).
        assertEquals("10:13 PM", twelve.primary)
        assertEquals("22:13", twentyFour.primary)
    }

    @Test
    fun worldClockInvalidZoneFallsBackToDevice() {
        val state = MetroTileTemporalState(kind = MetroTemporalKind.WORLD_CLOCK, zoneId = "Not/AZone")
        val rendered = MetroTileTemporalRender.render(state, 0L, 1_700_000_000_000L, use24Hour = true, deviceZone = deviceZone)
        assertEquals("22:13", rendered.primary)
    }

    @Test
    fun worldClockDayOffset() {
        val tokyo = MetroTileTemporalState(kind = MetroTemporalKind.WORLD_CLOCK, zoneId = "Asia/Tokyo")
        // 22:13 UTC → next day in Tokyo.
        val rendered = MetroTileTemporalRender.render(tokyo, 0L, 1_700_000_000_000L, use24Hour = true, deviceZone = deviceZone)
        assertEquals("tomorrow", rendered.secondary)
    }

    @Test
    fun dayOffsetLabelBoundaries() {
        assertNull(MetroTileTemporalRender.dayOffsetLabel(java.time.LocalDate.of(2024, 1, 1), java.time.LocalDate.of(2024, 1, 1)))
        assertEquals("tomorrow", MetroTileTemporalRender.dayOffsetLabel(java.time.LocalDate.of(2024, 1, 2), java.time.LocalDate.of(2024, 1, 1)))
        assertEquals("yesterday", MetroTileTemporalRender.dayOffsetLabel(java.time.LocalDate.of(2023, 12, 31), java.time.LocalDate.of(2024, 1, 1)))
    }

    @Test
    fun stopwatchPreciseFormatting() {
        assertEquals("01:42.18", MetroTileTemporalRender.formatStopwatchPrecise(102_180L))
        assertEquals("1:00:00.00", MetroTileTemporalRender.formatStopwatchPrecise(3_600_000L))
    }
}
