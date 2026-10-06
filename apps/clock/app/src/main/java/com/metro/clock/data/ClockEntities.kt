package com.metro.clock.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persisted alarm. Repeating alarms store a weekday bitmask and are rescheduled to their **next
 * concrete occurrence** (never a platform repeating alarm).
 */
@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val hour: Int,
    val minute: Int,
    val label: String = "",
    val enabled: Boolean = true,
    /** bit0 = Monday … bit6 = Sunday; 0 = one-time. */
    val repeatMask: Int = 0,
    val vibrate: Boolean = true,
    val soundUri: String? = null,
    val snoozeMinutes: Int = 5,
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * Persisted timer. Duration is derived from timestamps, never decremented per second.
 * [targetElapsedRealtime] is monotonic (valid within a boot); [targetEpochMillis] is the
 * wall-clock recovery aid used after reboot/process death.
 */
@Entity(tableName = "timers")
data class TimerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val label: String,
    val durationMillis: Long,
    /** One of [com.metro.clock.timers.TimerState] names. */
    val state: String,
    val remainingWhenPaused: Long,
    val targetElapsedRealtime: Long,
    val targetEpochMillis: Long,
    val createdOrder: Long,
)

@Entity(tableName = "stopwatches")
data class StopwatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val running: Boolean,
    val accumulatedElapsedMillis: Long,
    val runningSinceElapsedRealtime: Long,
    val runningSinceEpochMillis: Long,
    val createdOrder: Long,
)

@Entity(
    tableName = "stopwatch_laps",
    foreignKeys = [
        ForeignKey(
            entity = StopwatchEntity::class,
            parentColumns = ["id"],
            childColumns = ["stopwatchId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("stopwatchId")],
)
data class StopwatchLapEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val stopwatchId: Long,
    val lapNumber: Int,
    val lapDurationMillis: Long,
    val totalDurationMillis: Long,
)

/** World-clock city selection — references [com.metro.system.MetroWorldClockCatalog] by stable id. */
@Entity(tableName = "world_clock_cities")
data class WorldClockCityEntity(
    @PrimaryKey val cityId: String,
    val sortOrder: Int,
)
