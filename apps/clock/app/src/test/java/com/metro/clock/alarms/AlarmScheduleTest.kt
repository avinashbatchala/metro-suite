package com.metro.clock.alarms

import com.metro.clock.data.AlarmEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class AlarmScheduleTest {

    private val utc = ZoneId.of("UTC")

    private fun millis(date: String, time: String): Long =
        ZonedDateTime.of(LocalDate.parse(date), java.time.LocalTime.parse(time), utc)
            .toInstant().toEpochMilli()

    private fun alarm(hour: Int, minute: Int, mask: Int = 0, enabled: Boolean = true) =
        AlarmEntity(hour = hour, minute = minute, repeatMask = mask, enabled = enabled)

    @Test
    fun oneTime_laterToday() {
        val trigger = AlarmSchedule.nextTrigger(
            alarm(7, 0),
            fromMillis = millis("2024-01-01", "06:00"),
            zone = utc,
        )
        assertEquals(millis("2024-01-01", "07:00"), trigger)
    }

    @Test
    fun oneTime_alreadyPassed_tomorrow() {
        val trigger = AlarmSchedule.nextTrigger(
            alarm(7, 0),
            fromMillis = millis("2024-01-01", "08:00"),
            zone = utc,
        )
        assertEquals(millis("2024-01-02", "07:00"), trigger)
    }

    @Test
    fun daily_nextDay() {
        val daily = AlarmSchedule.WEEKDAYS or AlarmSchedule.WEEKENDS
        // 2024-01-01 is a Monday.
        val trigger = AlarmSchedule.nextTrigger(alarm(7, 0, daily), millis("2024-01-01", "08:00"), utc)
        assertEquals(millis("2024-01-02", "07:00"), trigger)
    }

    @Test
    fun weekdays_fromSaturday_jumpsToMonday() {
        // 2024-01-06 is a Saturday.
        val trigger = AlarmSchedule.nextTrigger(alarm(7, 0, AlarmSchedule.WEEKDAYS), millis("2024-01-06", "12:00"), utc)
        assertEquals(millis("2024-01-08", "07:00"), trigger)
    }

    @Test
    fun weekends_fromMonday_jumpsToSaturday() {
        val trigger = AlarmSchedule.nextTrigger(alarm(9, 30, AlarmSchedule.WEEKENDS), millis("2024-01-01", "06:00"), utc)
        assertEquals(millis("2024-01-06", "09:30"), trigger)
    }

    @Test
    fun specificWeekday_onlyWednesday() {
        // From Monday, only Wednesday (bit2).
        val trigger = AlarmSchedule.nextTrigger(alarm(6, 0, AlarmSchedule.DAY_WED), millis("2024-01-01", "06:00"), utc)
        assertEquals(millis("2024-01-03", "06:00"), trigger)
    }

    @Test
    fun disabled_returnsNull() {
        assertNull(AlarmSchedule.nextTrigger(alarm(7, 0, enabled = false), millis("2024-01-01", "06:00"), utc))
    }

    @Test
    fun nextAlarm_picksEarliestEnabled() {
        val alarms = listOf(
            alarm(9, 0),
            alarm(7, 0),
            alarm(6, 0, enabled = false),
        )
        val next = AlarmSchedule.nextAlarm(alarms, millis("2024-01-01", "05:00"), utc)
        assertEquals(7, next?.first?.hour)
    }

    @Test
    fun dstSpringForward_stillResolvesNextDay() {
        val newYork = ZoneId.of("America/New_York")
        // 2024-03-10 02:00→03:00 spring forward; a 02:30 alarm has no valid wall time.
        val from = ZonedDateTime.of(2024, 3, 9, 12, 0, 0, 0, newYork).toInstant().toEpochMilli()
        val trigger = AlarmSchedule.nextTrigger(
            alarm(2, 30, AlarmSchedule.WEEKDAYS or AlarmSchedule.WEEKENDS),
            from,
            newYork,
        )
        val local = Instant.ofEpochMilli(trigger!!).atZone(newYork).toLocalDate()
        assertEquals(LocalDate.of(2024, 3, 10), local)
    }

    @Test
    fun dstAutumnFallBack_resolves() {
        val newYork = ZoneId.of("America/New_York")
        val from = ZonedDateTime.of(2024, 11, 2, 12, 0, 0, 0, newYork).toInstant().toEpochMilli()
        val trigger = AlarmSchedule.nextTrigger(
            alarm(1, 30, AlarmSchedule.WEEKDAYS or AlarmSchedule.WEEKENDS),
            from,
            newYork,
        )
        val local = Instant.ofEpochMilli(trigger!!).atZone(newYork).toLocalDate()
        assertEquals(LocalDate.of(2024, 11, 3), local)
    }

    @Test
    fun repeatSummary_presets() {
        assertEquals("once", AlarmSchedule.repeatSummary(alarm(7, 0, 0)))
        assertEquals("every day", AlarmSchedule.repeatSummary(alarm(7, 0, AlarmSchedule.WEEKDAYS or AlarmSchedule.WEEKENDS)))
        assertEquals("weekdays", AlarmSchedule.repeatSummary(alarm(7, 0, AlarmSchedule.WEEKDAYS)))
        assertEquals("weekends", AlarmSchedule.repeatSummary(alarm(7, 0, AlarmSchedule.WEEKENDS)))
        assertTrue(AlarmSchedule.repeatSummary(alarm(7, 0, AlarmSchedule.DAY_WED)).isNotEmpty())
    }
}
