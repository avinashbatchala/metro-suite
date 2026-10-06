package com.metro.system

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Shared digital / analog clock parts for Start widget faces and the Widgets catalog. */
data class MetroClockFaceParts(
    val hour: String,
    val minute: String,
    val period: String,
    /** Degrees clockwise from 12 for the hour hand (includes minute sweep). */
    val hourHandDegrees: Float,
    /** Degrees clockwise from 12 for the minute hand. */
    val minuteHandDegrees: Float,
)

object MetroClockFace {
    private val hourFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("h", Locale.US)
    private val hour24Formatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("HH", Locale.US)
    private val minuteFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("mm", Locale.US)
    private val periodFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("a", Locale.US)
    private val time12: DateTimeFormatter =
        DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    private val time24: DateTimeFormatter =
        DateTimeFormatter.ofPattern("HH:mm", Locale.US)

    /**
     * @param use24Hour honor the device/suite 24-hour preference. Existing callers default to
     * 12-hour so nothing regresses; [period] is empty in 24-hour mode.
     */
    fun parts(
        now: LocalDateTime = LocalDateTime.now(),
        use24Hour: Boolean = false,
    ): MetroClockFaceParts {
        val minute = now.minute
        val hour12 = now.hour % 12
        return MetroClockFaceParts(
            hour = if (use24Hour) now.format(hour24Formatter) else now.format(hourFormatter),
            minute = now.format(minuteFormatter),
            period = if (use24Hour) "" else now.format(periodFormatter).uppercase(Locale.US),
            hourHandDegrees = hour12 * 30f + minute * 0.5f,
            minuteHandDegrees = minute * 6f,
        )
    }

    /** Formatted wall-clock time for an instant in [zone], respecting 12/24-hour preference. */
    fun time(
        epochMillis: Long,
        zone: ZoneId,
        use24Hour: Boolean,
    ): String =
        Instant.ofEpochMilli(epochMillis)
            .atZone(zone)
            .format(if (use24Hour) time24 else time12)
            .uppercase(Locale.US)
}
