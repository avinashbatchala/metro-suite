package com.metro.training.domain.routines

enum class ProgressionMethod { DOUBLE_PROGRESSION }

/**
 * Tunable, deterministic progression policy. Defaults encode the product's conservative rules
 * (not physiological laws). See `references/research.md`.
 */
data class ProgressionPolicy(
    val method: ProgressionMethod = ProgressionMethod.DOUBLE_PROGRESSION,
    /** Without RIR, require a second consecutive top-range exposure before adding load. */
    val requireTwoTopRangeSessionsWithoutRir: Boolean = true,
    /** Consecutive comparable underperformances before reducing load. */
    val underperformanceSessionsBeforeDecrease: Int = 2,
    /** Gaps longer than this reduce confidence and hold load. */
    val longGapDays: Int = 28,
)

/**
 * Per-routine prescription for one exercise. A fixed rep range lives here, never on the
 * [com.metro.training.domain.exercises.ExerciseDefinition].
 */
data class RoutineExercisePrescription(
    val id: String,
    val exerciseId: String,
    val workSetCount: Int,
    val repMin: Int,
    val repMax: Int,
    val targetRirMin: Int?,
    val targetRirMax: Int?,
    val restSeconds: Int?,
    val autoProgressEnabled: Boolean,
    val progressionPolicy: ProgressionPolicy = ProgressionPolicy(),
    /** Per-prescription override of the exercise's default increment (canonical kg). */
    val incrementOverrideKg: Double? = null,
    val note: String = "",
) {
    val rirConfigured: Boolean
        get() = targetRirMin != null && targetRirMax != null

    fun validate(): List<String> = buildList {
        if (workSetCount < 1) add("work set count must be at least 1")
        if (repMin < 1) add("rep minimum must be at least 1")
        if (repMax < repMin) add("rep maximum must be >= rep minimum")
        if (rirConfigured && (targetRirMin!! < 0 || targetRirMax!! < targetRirMin!!)) {
            add("target RIR range is invalid")
        }
    }
}

data class RoutineExercise(
    val prescription: RoutineExercisePrescription,
    val order: Int,
)

data class Routine(
    val id: String,
    val name: String,
    val exercises: List<RoutineExercise> = emptyList(),
    val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val workSetTotal: Int get() = exercises.sumOf { it.prescription.workSetCount }
}
