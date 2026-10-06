package com.metro.training.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val mechanic: String,
    val equipment: String,
    val resistanceModel: String,
    val loadSemantics: String,
    val progressionDirection: String,
    val defaultIncrement: Double?,
    val incrementUnit: String?,
    val primaryMuscles: String,
    val secondaryMuscles: String,
    val unilateral: Boolean,
    val builtIn: Boolean,
    val archived: Boolean,
    val updatedAt: Long,
)

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey val id: String,
    val name: String,
    val archived: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "routine_exercises",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("routineId")],
)
data class RoutineExerciseEntity(
    @PrimaryKey val id: String,
    val routineId: String,
    val exerciseId: String,
    val order: Int,
    val workSetCount: Int,
    val repMin: Int,
    val repMax: Int,
    val targetRirMin: Int?,
    val targetRirMax: Int?,
    val restSeconds: Int?,
    val autoProgressEnabled: Boolean,
    val incrementOverrideKg: Double?,
    val policyMethod: String,
    val requireTwoTopRangeWithoutRir: Boolean,
    val underperformanceBeforeDecrease: Int,
    val longGapDays: Int,
    val note: String,
    val supersetTag: String? = null,
)

@Entity(
    tableName = "workouts",
    indices = [Index("status", "startedAt")],
)
data class WorkoutEntity(
    @PrimaryKey val id: String,
    val routineId: String?,
    val routineName: String,
    val startedAt: Long,
    val finishedAt: Long?,
    /** IN_PROGRESS or COMPLETED. */
    val status: String,
    val restDeadlineMillis: Long?,
    val note: String,
)

@Entity(
    tableName = "workout_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("workoutId")],
)
data class WorkoutExerciseEntity(
    @PrimaryKey val id: String,
    val workoutId: String,
    val exerciseId: String,
    val exerciseName: String,
    val order: Int,
    val prescriptionId: String,
    val workSetCount: Int,
    val repMin: Int,
    val repMax: Int,
    val targetRirMin: Int?,
    val targetRirMax: Int?,
    val loadSemantics: String,
    val progressionDirection: String,
    val incrementKg: Double?,
    val restSeconds: Int?,
    val autoProgressEnabled: Boolean,
    val note: String,
    val supersetTag: String?,
)

@Entity(
    tableName = "workout_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutExerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("workoutExerciseId")],
)
data class WorkoutSetEntity(
    @PrimaryKey val id: String,
    val workoutExerciseId: String,
    val setIndex: Int,
    val setType: String,
    val load: Double?,
    val repsCompleted: Int,
    val rir: Int?,
    val quality: String,
    val completed: Boolean,
    val prescribed: Boolean,
    val timestamp: Long,
    val repsLeft: Int? = null,
    val repsRight: Int? = null,
    val partialReps: Int? = null,
    val note: String = "",
)

@Entity(
    tableName = "progression_recommendations",
    indices = [Index("workoutId"), Index("exerciseId")],
)
data class ProgressionRecommendationEntity(
    @PrimaryKey val id: String,
    val workoutId: String,
    val workoutExerciseId: String,
    val exerciseId: String,
    val decision: String,
    val currentLoad: Double?,
    val nextLoad: Double?,
    val confidence: String,
    val headline: String,
    val reasonsCsv: String,
    val detailsCsv: String,
    val repTargetGoal: Int?,
    val repMin: Int?,
    val repMax: Int?,
    val algorithmVersion: Int,
    val generatedAt: Long,
    val accepted: Boolean,
    val chosenLoad: Double?,
    val chosenRepGoal: Int?,
)
