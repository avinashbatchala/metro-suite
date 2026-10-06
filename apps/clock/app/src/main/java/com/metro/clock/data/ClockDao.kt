package com.metro.clock.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface ClockDao {
    // Alarms -----------------------------------------------------------------
    @Query("SELECT * FROM alarms ORDER BY hour ASC, minute ASC, id ASC")
    suspend fun alarms(): List<AlarmEntity>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun alarm(id: Long): AlarmEntity?

    @Insert
    suspend fun insertAlarm(alarm: AlarmEntity): Long

    @Update
    suspend fun updateAlarm(alarm: AlarmEntity)

    @Query("DELETE FROM alarms WHERE id = :id")
    suspend fun deleteAlarm(id: Long)

    // Timers -----------------------------------------------------------------
    @Query("SELECT * FROM timers ORDER BY createdOrder ASC")
    suspend fun timers(): List<TimerEntity>

    @Query("SELECT * FROM timers WHERE id = :id")
    suspend fun timer(id: Long): TimerEntity?

    @Insert
    suspend fun insertTimer(timer: TimerEntity): Long

    @Update
    suspend fun updateTimer(timer: TimerEntity)

    @Query("DELETE FROM timers WHERE id = :id")
    suspend fun deleteTimer(id: Long)

    // Stopwatches ------------------------------------------------------------
    @Query("SELECT * FROM stopwatches ORDER BY createdOrder ASC")
    suspend fun stopwatches(): List<StopwatchEntity>

    @Query("SELECT * FROM stopwatches WHERE id = :id")
    suspend fun stopwatch(id: Long): StopwatchEntity?

    @Insert
    suspend fun insertStopwatch(stopwatch: StopwatchEntity): Long

    @Update
    suspend fun updateStopwatch(stopwatch: StopwatchEntity)

    @Query("DELETE FROM stopwatches WHERE id = :id")
    suspend fun deleteStopwatch(id: Long)

    @Query("DELETE FROM stopwatch_laps WHERE stopwatchId = :stopwatchId")
    suspend fun deleteLaps(stopwatchId: Long)

    @Query("SELECT * FROM stopwatch_laps WHERE stopwatchId = :stopwatchId ORDER BY lapNumber DESC")
    suspend fun laps(stopwatchId: Long): List<StopwatchLapEntity>

    @Insert
    suspend fun insertLap(lap: StopwatchLapEntity): Long

    // World clock ------------------------------------------------------------
    @Query("SELECT * FROM world_clock_cities ORDER BY sortOrder ASC")
    suspend fun worldCities(): List<WorldClockCityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorldCity(city: WorldClockCityEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorldCities(cities: List<WorldClockCityEntity>)

    @Query("DELETE FROM world_clock_cities WHERE cityId = :cityId")
    suspend fun deleteWorldCity(cityId: String)

    @Query("SELECT COUNT(*) FROM world_clock_cities")
    suspend fun worldCityCount(): Int

    @Query("SELECT MAX(sortOrder) FROM world_clock_cities")
    suspend fun maxWorldCityOrder(): Int?
}
