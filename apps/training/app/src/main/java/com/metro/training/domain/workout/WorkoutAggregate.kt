package com.metro.training.domain.workout

import com.metro.training.domain.exercises.LoadSemantics
import com.metro.training.domain.exercises.ProgressionDirection

enum class WorkoutStatus { IN_PROGRESS, COMPLETED }

/** One exercise inside a workout session, carrying its historical snapshot. */
data class WorkoutExercise(
    val id: String,
    val workoutId: String,
    val exerciseId: String,
    val exerciseName: String,
    val order: Int,
    val prescriptionId: String,
    val snapshot: ExposureSnapshot,
    val restSeconds: Int?,
    val autoProgressEnabled: Boolean = true,
    val note: String,
    val sets: List<WorkoutSet>,
) {
    fun toExposure(performedAt: Long): CompletedExerciseExposure =
        CompletedExerciseExposure(
            id = id,
            exerciseId = exerciseId,
            prescriptionId = prescriptionId,
            performedAt = performedAt,
            snapshot = snapshot,
            sets = sets,
        )

    val workSets: List<WorkoutSet>
        get() = sets.filter { it.setType == SetType.WORK && it.prescribed }
}

/** A workout session aggregate. */data class Workout(
    val id: String,
    val routineId: String?,
    val routineName: String,
    val startedAt: Long,
    val finishedAt: Long?,
    val status: WorkoutStatus,
    val restDeadlineMillis: Long?,
    val note: String,
    val exercises: List<WorkoutExercise>,
) {
    val isInProgress: Boolean get() = status == WorkoutStatus.IN_PROGRESS
    val completedExposureCount: Int
        get() = exercises.count { exercise -> exercise.sets.any { it.countsTowardProgression } }
    val durationMillis: Long
        get() = (finishedAt ?: System.currentTimeMillis()) - startedAt
}

/** A lightweight row for the history list — no exercises or sets loaded. */
data class WorkoutSummary(
    val id: String,
    val routineId: String?,
    val routineName: String,
    val startedAt: Long,
    val finishedAt: Long?,
    val exerciseCount: Int,
    val workSetCount: Int,
) {
    val durationMillis: Long
        get() = (finishedAt ?: startedAt) - startedAt
}

/** Helper to build a snapshot from a prescription + exercise metadata. */
fun snapshotOf(
    repMin: Int,
    repMax: Int,
    workSetCount: Int,
    targetRirMin: Int?,
    targetRirMax: Int?,
    loadSemantics: LoadSemantics,
    progressionDirection: ProgressionDirection,
    incrementKg: Double?,
): ExposureSnapshot = ExposureSnapshot(
    repMin = repMin,
    repMax = repMax,
    workSetCount = workSetCount,
    targetRirMin = targetRirMin,
    targetRirMax = targetRirMax,
    loadSemantics = loadSemantics,
    progressionDirection = progressionDirection,
    incrementKg = incrementKg,
)
