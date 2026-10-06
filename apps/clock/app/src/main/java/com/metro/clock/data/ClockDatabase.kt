package com.metro.clock.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        AlarmEntity::class,
        TimerEntity::class,
        StopwatchEntity::class,
        StopwatchLapEntity::class,
        WorldClockCityEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class ClockDatabase : RoomDatabase() {
    abstract fun dao(): ClockDao

    companion object {
        @Volatile
        private var instance: ClockDatabase? = null

        fun get(context: Context): ClockDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ClockDatabase::class.java,
                    "metro_clock.db",
                ).build().also { instance = it }
            }
    }
}
