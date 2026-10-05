package com.metro.calendar.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class CalendarLogicTest {
    private val zoneId = ZoneId.of("UTC")

    @Test
    fun formatEventTime_allDay() {
        val event = sampleEvent(allDay = true)
        assertEquals("All day", CalendarLogic.formatEventTime(event, zoneId))
    }

    @Test
    fun formatEventTime_timed() {
        val start = LocalDate.of(2026, 6, 26).atTime(14, 30).atZone(zoneId).toInstant().toEpochMilli()
        val event = sampleEvent(allDay = false, startMillis = start, endMillis = start + TimeUnit.HOURS.toMillis(1))
        assertEquals("14:30", CalendarLogic.formatEventTime(event, zoneId))
    }

    @Test
    fun groupIntoAgendaBuckets_groupsByDay() {
        val day = LocalDate.of(2026, 6, 26).toEpochDay()
        val start = LocalDate.ofEpochDay(day).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val events = listOf(
            sampleEvent(id = 1, startMillis = start, endMillis = start + 3_600_000),
            sampleEvent(id = 2, startMillis = start + 7_200_000, endMillis = start + 10_800_000),
        )
        val buckets = CalendarLogic.groupIntoAgendaBuckets(events, day, dayCount = 1, zoneId = zoneId)
        assertEquals(1, buckets.size)
        assertEquals(2, buckets.first().events.size)
    }

    @Test
    fun buildMonthGrid_has42Cells() {
        val day = LocalDate.of(2026, 6, 15).toEpochDay()
        val grid = CalendarLogic.buildMonthGrid(2026, 6, emptyList(), day, zoneId)
        assertEquals(42, grid.size)
    }

    @Test
    fun buildMonthGrid_marksSelectedDay() {
        val day = LocalDate.of(2026, 6, 15).toEpochDay()
        val grid = CalendarLogic.buildMonthGrid(2026, 6, emptyList(), day, zoneId)
        assertTrue(grid.any { it.isSelected && it.dayOfMonth == 15 })
    }

    @Test
    fun eventsForDay_filtersCorrectly() {
        val day = LocalDate.of(2026, 6, 26).toEpochDay()
        val start = LocalDate.ofEpochDay(day).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val nextDay = LocalDate.ofEpochDay(day + 1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val events = listOf(
            sampleEvent(id = 1, startMillis = start + 3_600_000, endMillis = start + 7_200_000),
            sampleEvent(id = 2, startMillis = nextDay, endMillis = nextDay + 3_600_000),
        )
        val filtered = CalendarLogic.eventsForDay(events, day, zoneId)
        assertEquals(1, filtered.size)
        assertEquals(1L, filtered.first().id)
    }

    @Test
    fun nextUpcomingEvent_returnsEarliestFuture() {
        val now = LocalDate.of(2026, 6, 26).atTime(12, 0).atZone(zoneId).toInstant().toEpochMilli()
        val past = now - TimeUnit.HOURS.toMillis(2)
        val future1 = now + TimeUnit.HOURS.toMillis(1)
        val future2 = now + TimeUnit.HOURS.toMillis(3)
        val events = listOf(
            sampleEvent(id = 1, startMillis = past, endMillis = past + 3_600_000),
            sampleEvent(id = 2, startMillis = future2, endMillis = future2 + 3_600_000),
            sampleEvent(id = 3, startMillis = future1, endMillis = future1 + 3_600_000),
        )
        val next = CalendarLogic.nextUpcomingEvent(events, now)
        assertEquals(3L, next?.id)
    }

    @Test
    fun formatEventDuration_allDaySingle() {
        val day = LocalDate.of(2026, 6, 26).toEpochDay()
        val start = LocalDate.ofEpochDay(day).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = LocalDate.ofEpochDay(day + 1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val event = sampleEvent(allDay = true, startMillis = start, endMillis = end)
        assertEquals("1 day", CalendarLogic.formatEventDuration(event))
    }

    @Test
    fun buildHourSlots_coversRequestedRange() {
        val day = LocalDate.of(2026, 6, 26).toEpochDay()
        val slots = CalendarLogic.buildHourSlots(emptyList(), day, startHour = 8, endHour = 12, zoneId = zoneId)
        assertEquals(5, slots.size)
        assertEquals("8 AM", slots.first().label)
        assertFalse(slots.any { it.events.isNotEmpty() })
    }

    // --- Timezone / all-day / recurrence-instance edge cases -------------------------------

    @Test
    fun allDayEvent_groupedUnderLocalDay_negativeOffset() {
        // Provider stores all-day BEGIN/END in UTC. In a negative-offset zone the naive local
        // mapping shifts a Monday all-day event to Sunday; eventStartEpochDay must not.
        val zone = ZoneId.of("-05:00")
        val event = sampleEvent(
            allDay = true,
            startMillis = isoMillis("2026-06-26T00:00:00Z"),
            endMillis = isoMillis("2026-06-27T00:00:00Z"),
        )
        val day = LocalDate.of(2026, 6, 26).toEpochDay()
        assertEquals(day, CalendarLogic.eventStartEpochDay(event, zone))
        val buckets = CalendarLogic.groupIntoAgendaBuckets(listOf(event), day, dayCount = 1, zoneId = zone)
        assertEquals(day, buckets.single().epochDay)
        assertEquals(1, buckets.single().events.size)
    }

    @Test
    fun allDayEvent_mappedToCorrectDay_positiveOffset() {
        val zone = ZoneId.of("+05:00")
        val event = allDayEvent("2026-06-26")
        val day = LocalDate.of(2026, 6, 26).toEpochDay()
        assertEquals(day, CalendarLogic.eventStartEpochDay(event, zone))
        assertEquals(1, CalendarLogic.allDayEventsForDay(listOf(event), day, zone).size)
    }

    @Test
    fun multiDayAllDay_appearsOnEachCoveredDay() {
        val zone = ZoneId.of("UTC")
        val event = sampleEvent(
            allDay = true,
            startMillis = isoMillis("2026-06-26T00:00:00Z"),
            endMillis = isoMillis("2026-06-29T00:00:00Z"),
        )
        val days = listOf("2026-06-26", "2026-06-27", "2026-06-28")
        days.forEach { d ->
            assertEquals("expected event on $d", 1, CalendarLogic.eventsForDay(listOf(event), epochDayOf(d), zone).size)
        }
        assertTrue(CalendarLogic.eventsForDay(listOf(event), epochDayOf("2026-06-29"), zone).isEmpty())
    }

    @Test
    fun timedEventCrossingMidnight_appearsOnBothDays() {
        val zone = ZoneId.of("UTC")
        val event = sampleEvent(
            startMillis = isoMillis("2026-06-26T23:00:00Z"),
            endMillis = isoMillis("2026-06-27T01:00:00Z"),
        )
        assertEquals(1, CalendarLogic.eventsForDay(listOf(event), epochDayOf("2026-06-26"), zone).size)
        assertEquals(1, CalendarLogic.eventsForDay(listOf(event), epochDayOf("2026-06-27"), zone).size)
    }

    @Test
    fun allDayEvent_onDstTransition_keepsItsDay() {
        val zone = ZoneId.of("America/New_York")
        val event = allDayEvent("2026-03-08") // US spring-forward day
        val day = epochDayOf("2026-03-08")
        assertEquals(day, CalendarLogic.eventStartEpochDay(event, zone))
        assertEquals(1, CalendarLogic.eventsForDay(listOf(event), day, zone).size)
    }

    @Test
    fun agenda_keepsMultiDayEventThatStartedBeforeWindow() {
        val zone = ZoneId.of("UTC")
        val event = sampleEvent(
            allDay = true,
            startMillis = isoMillis("2026-06-21T00:00:00Z"),
            endMillis = isoMillis("2026-06-23T00:00:00Z"), // covers 21 + 22
        )
        val windowStart = epochDayOf("2026-06-22")
        val buckets = CalendarLogic.groupIntoAgendaBuckets(listOf(event), windowStart, dayCount = 2, zoneId = zone)
        assertEquals(1, buckets.size)
        assertEquals(windowStart, buckets.single().epochDay)
    }

    @Test
    fun agenda_dropsEventEntirelyOutsideWindow() {
        val zone = ZoneId.of("UTC")
        val event = allDayEvent("2026-06-20")
        val buckets = CalendarLogic.groupIntoAgendaBuckets(listOf(event), epochDayOf("2026-06-25"), dayCount = 3, zoneId = zone)
        assertTrue(buckets.isEmpty())
    }

    @Test
    fun tileEventLines_prefixesWeekdayForNonToday() {
        val zone = ZoneId.of("UTC")
        val today = epochDayOf("2026-06-26")
        val tomorrow = allDayEvent("2026-06-27")
        val lines = CalendarLogic.tileEventLines(tomorrow, today, zone)
        assertEquals("Sat: All day", lines.last())

        val todayEvent = allDayEvent("2026-06-26")
        assertEquals("All day", CalendarLogic.tileEventLines(todayEvent, today, zone).last())
    }

    private fun isoMillis(iso: String): Long = Instant.parse(iso).toEpochMilli()

    private fun epochDayOf(iso: String): Long = LocalDate.parse(iso).toEpochDay()

    private fun allDayEvent(isoDate: String): CalendarEvent = sampleEvent(
        allDay = true,
        startMillis = isoMillis("${isoDate}T00:00:00Z"),
        endMillis = isoMillis("${LocalDate.parse(isoDate).plusDays(1)}T00:00:00Z"),
    )

    private fun sampleEvent(
        id: Long = 1L,
        allDay: Boolean = false,
        startMillis: Long = 0L,
        endMillis: Long = startMillis + 3_600_000,
    ) = CalendarEvent(
        id = id,
        title = "Test event",
        startMillis = startMillis,
        endMillis = endMillis,
        allDay = allDay,
        calendarColorHex = "#1BA1E2",
        calendarName = "Test",
        location = null,
    )
}
