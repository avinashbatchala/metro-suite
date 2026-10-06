package com.metro.training.domain.workout

enum class SetType { WARMUP, WORK, DROP, BACKOFF }

/** Explicit per-set flags. No invented form score. */
enum class SetQuality {
    NORMAL,
    TECHNIQUE_LIMITED,
    FAILED_REP,
    PAIN,
    ;

    val isPain: Boolean get() = this == PAIN
    val isTechniqueLimited: Boolean get() = this == TECHNIQUE_LIMITED
    val isFailedRep: Boolean get() = this == FAILED_REP
}

/** One logged set. `load` is canonical kg; `repsCompleted` counts only completed reps. */
data class WorkoutSet(
    val id: String,
    val exerciseSessionId: String,
    val setIndex: Int,
    val setType: SetType,
    val load: Double?,
    val repsCompleted: Int,
    val rir: Int?,
    val quality: SetQuality,
    val completed: Boolean,
    /** False for sets added beyond the prescribed work-set block (extra volume). */
    val prescribed: Boolean = true,
    val timestamp: Long = System.currentTimeMillis(),
) {
    val countsTowardProgression: Boolean
        get() = setType == SetType.WORK && prescribed && completed
}

/** Snapshot of prescription parameters captured at workout time (historical integrity). */
data class ExposureSnapshot(
    val repMin: Int,
    val repMax: Int,
    val workSetCount: Int,
    val targetRirMin: Int?,
    val targetRirMax: Int?,
    val loadSemantics: com.metro.training.domain.exercises.LoadSemantics,
    val progressionDirection: com.metro.training.domain.exercises.ProgressionDirection,
    val incrementKg: Double?,
)

/**
 * A completed exercise exposure (one exercise within one workout). `sets` should contain the
 * work-set block; warm-ups and extra sets may be present but are excluded from progression.
 */
data class CompletedExerciseExposure(
    val id: String,
    val exerciseId: String,
    val prescriptionId: String,
    val performedAt: Long,
    val snapshot: ExposureSnapshot,
    val sets: List<WorkoutSet>,
) {
    val workSets: List<WorkoutSet>
        get() = sets.filter { it.setType == SetType.WORK && it.prescribed }
}
