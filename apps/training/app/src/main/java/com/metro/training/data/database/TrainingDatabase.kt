package com.metro.training.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ExerciseEntity::class,
        RoutineEntity::class,
        RoutineExerciseEntity::class,
        WorkoutEntity::class,
        WorkoutExerciseEntity::class,
        WorkoutSetEntity::class,
        ProgressionRecommendationEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class TrainingDatabase : RoomDatabase() {
    abstract fun dao(): TrainingDao

    companion object {
        @Volatile
        private var instance: TrainingDatabase? = null

        /** v1 → v2: add the history-listing index. Training history must never be dropped. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_workouts_status_startedAt " +
                        "ON workouts(status, startedAt)",
                )
            }
        }

        /** v2 → v3: per-side reps, partial reps, set notes and superset tags. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_sets ADD COLUMN repsLeft INTEGER")
                db.execSQL("ALTER TABLE workout_sets ADD COLUMN repsRight INTEGER")
                db.execSQL("ALTER TABLE workout_sets ADD COLUMN partialReps INTEGER")
                db.execSQL("ALTER TABLE workout_sets ADD COLUMN note TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE workout_exercises ADD COLUMN supersetTag TEXT")
                db.execSQL("ALTER TABLE routine_exercises ADD COLUMN supersetTag TEXT")
            }
        }

        fun get(context: Context): TrainingDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TrainingDatabase::class.java,
                    "training.db",
                )
                    // Training history is valuable; never silently drop it on schema changes.
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }
    }
}
