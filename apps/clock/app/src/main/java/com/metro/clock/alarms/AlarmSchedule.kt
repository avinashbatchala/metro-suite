package com.metro.clock.alarms

import com.metro.clock.data.AlarmEntity
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/**
 * Pure alarm scheduling math — no Android framework. Repeating alarms resolve to their **next
 * concrete occurrence**, so DST and manual clock changes are handled by recomputation rather than a
 * drifting platform repeating alarm.
 *
 * Weekday mask: bit0 = Monday … bit6 = Sunday (0 = one-time).
 */
object AlarmSchedule {

    const val DAY_MON = 1 shl 0
    const val DAY_TUE = 1 shl 1
    const val DAY_WED = 1 shl 2
    const val DAY_THU = 1 shl 3
    const val DAY_FRI = 1 shl 4
    const val DAY_SAT = 1 shl 5
    const val DAY_SUN = 1 shl 6

    val WEEKDAYS = DAY_MON or DAY_TUE or DAY_WED or DAY_THU or DAY_FRI
    val WEEKENDS = DAY_SAT or DAY_SUN

    private val ORDERED_DAYS = listOf(
        DayOfWeek.MONDAY to DAY_MON,
        DayOfWeek.TUESDAY to DAY_TUE,
        DayOfWeek.WEDNESDAY to DAY_WED,
        DayOfWeek.THURSDAY to DAY_THU,
        DayOfWeek.FRIDAY to DAY_FRI,
        DayOfWeek.SATURDAY to DAY_SAT,
        DayOfWeek.SUNDAY to DAY_SUN,
    )

    fun bitFor(day: DayOfWeek): Int = 1 shl (day.value - 1)

    fun isRepeating(alarm: AlarmEntity): Boolean = alarm.repeatMask != 0

    fun repeatsOn(alarm: AlarmEntity, day: DayOfWeek): Boolean =
        alarm.repeatMask and bitFor(day) != 0

    /**
     * Next trigger strictly after [fromMillis], or null when disabled / no matching occurrence.
     */
    fun nextTrigger(alarm: AlarmEntity, fromMillis: Long, zone: ZoneId): Long? {
        if (!alarm.enabled) return null
        val from = Instant.ofEpochMilli(fromMillis).atZone(zone)
        if (alarm.repeatMask == 0) {
            val today = from.toLocalDate().atTime(alarm.hour, alarm.minute).atZone(zone)
            val todayMillis = today.toInstant().toEpochMilli()
            return if (todayMillis > fromMillis) todayMillis
            else today.plusDays(1).toInstant().toEpochMilli()
        }
        for (offset in 0..7) {
            val day = from.toLocalDate().plusDays(offset.toLong())
            if (!repeatsOn(alarm, day.dayOfWeek)) continue
            val dt = day.atTime(alarm.hour, alarm.minute).atZone(zone)
            val millis = dt.toInstant().toEpochMilli()
            if (millis > fromMillis) return millis
        }
        return null
    }

    /** Earliest next trigger across [alarms], or null when none are enabled. */
    fun nextAlarm(alarms: List<AlarmEntity>, fromMillis: Long, zone: ZoneId): Pair<AlarmEntity, Long>? =
        alarms.asSequence()
            .filter { it.enabled }
            .mapNotNull { alarm -> nextTrigger(alarm, fromMillis, zone)?.let { alarm to it } }
            .minByOrNull { it.second }

    fun repeatSummary(alarm: AlarmEntity, locale: Locale = Locale.getDefault()): String = when {
        alarm.repeatMask == 0 -> "once"
        alarm.repeatMask == WEEKDAYS -> "weekdays"
        alarm.repeatMask == WEEKENDS -> "weekends"
        alarm.repeatMask == (WEEKDAYS or WEEKENDS) -> "every day"
        else -> ORDERED_DAYS
            .filter { alarm.repeatMask and it.second != 0 }
            .joinToString(", ") { it.first.getDisplayName(TextStyle.SHORT, locale) }
    }
}
