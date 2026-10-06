package com.metro.clock.alarms

import com.metro.clock.data.AlarmEntity
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object AlarmFormat {
    private val twelve = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    private val twentyFour = DateTimeFormatter.ofPattern("HH:mm", Locale.US)

    fun time(alarm: AlarmEntity, use24Hour: Boolean = false): String =
        time(alarm.hour, alarm.minute, use24Hour)

    fun time(hour: Int, minute: Int, use24Hour: Boolean): String {
        val time = LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
        return time.format(if (use24Hour) twentyFour else twelve).uppercase(Locale.US)
    }
}
