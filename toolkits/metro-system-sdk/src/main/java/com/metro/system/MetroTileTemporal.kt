package com.metro.system

/**
 * Optional structured temporal payload for a [MetroTilePeek]. Lets the launcher render timers,
 * stopwatches and world clocks that tick **locally** instead of the source app broadcasting an
 * update every second.
 *
 * Values are exported as timestamps/state, never as pre-formatted ticking strings:
 * - Timers use monotonic [targetElapsedRealtimeMillis] while the device boot is unchanged, with a
 *   wall-clock [targetEpochMillis] as recovery aid after reboot/process death.
 * - Stopwatches use [accumulatedElapsedMillis] + [runningSinceElapsedRealtimeMillis], with
 *   [runningSinceEpochMillis] as the reboot fallback.
 * - World clocks carry only an IANA [zoneId]; the launcher reads wall time itself.
 *
 * Existing text-only peeks keep working: [MetroTilePeek.temporal] defaults to null.
 */
object MetroTemporalKind {
    const val TIMER = "timer"
    const val STOPWATCH = "stopwatch"
    const val WORLD_CLOCK = "world_clock"
    const val ALARM = "alarm"
}

data class MetroTileTemporalState(
    val kind: String,
    val itemId: String? = null,
    val label: String? = null,
    val running: Boolean = false,

    // Timer
    val targetElapsedRealtimeMillis: Long? = null,
    val targetEpochMillis: Long? = null,
    val pausedRemainingMillis: Long? = null,
    val finished: Boolean = false,

    // Stopwatch
    val accumulatedElapsedMillis: Long? = null,
    val runningSinceElapsedRealtimeMillis: Long? = null,
    val runningSinceEpochMillis: Long? = null,

    // World clock
    val zoneId: String? = null,
) {
    val hasContent: Boolean
        get() = kind.isNotBlank()
}
