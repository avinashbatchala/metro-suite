package com.metro.training.domain.exercises

/** Movement classification. Influences defaults/analytics, not a separate progression algorithm. */
enum class ExerciseMechanic {
    COMPOUND,
    ISOLATION,
    CARRY,
    ISOMETRIC,
    CARDIO,
    OTHER,
}

enum class EquipmentType {
    BARBELL,
    DUMBBELL,
    MACHINE,
    CABLE,
    BODYWEIGHT,
    SMITH_MACHINE,
    KETTLEBELL,
    BAND,
    OTHER,
}

/**
 * How resistance is applied. Mandatory for Auto Progress. The first four are fully supported by
 * Auto Progress v1.
 */
enum class ResistanceModel {
    EXTERNAL_LOAD,
    BODYWEIGHT_PLUS_LOAD,
    ASSISTED_BODYWEIGHT,
    REPS_ONLY,
    TIME,
    DISTANCE,
    WEIGHTED_TIME,
}

/** What the recorded number means, removing ambiguous logs. */
enum class LoadSemantics {
    TOTAL_WEIGHT,
    PER_DUMBBELL,
    PER_SIDE,
    ADDED_WEIGHT,
    ASSISTANCE_WEIGHT,
    NONE,
}

/** Which direction of load change represents more difficulty. */
enum class ProgressionDirection {
    MORE_LOAD_IS_HARDER,
    LESS_LOAD_IS_HARDER,
    REPS_ONLY,
}

enum class LoadUnit { KG, LB }

/** Normalized muscle taxonomy (analytics + defaults only — never load increments). */
enum class MuscleGroup {
    CHEST,
    FRONT_DELTS,
    SIDE_DELTS,
    REAR_DELTS,
    LATS,
    UPPER_BACK,
    TRAPS,
    BICEPS,
    TRICEPS,
    FOREARMS,
    QUADS,
    HAMSTRINGS,
    GLUTES,
    ADDUCTORS,
    CALVES,
    ABS,
    OBLIQUES,
    LOWER_BACK,
}

/**
 * Exercise identity + progression-relevant metadata. Built-in and custom exercises share this
 * exact model — there is no custom-exercise special case.
 *
 * A fixed rep range is deliberately NOT stored here; it belongs to
 * [com.metro.training.domain.routines.RoutineExercisePrescription].
 */
data class ExerciseDefinition(
    val id: String,
    val name: String,
    val mechanic: ExerciseMechanic,
    val equipment: EquipmentType,
    val resistanceModel: ResistanceModel,
    val loadSemantics: LoadSemantics,
    val progressionDirection: ProgressionDirection,
    val defaultIncrement: Double?,
    val incrementUnit: LoadUnit?,
    val primaryMuscles: Set<MuscleGroup>,
    val secondaryMuscles: Set<MuscleGroup>,
    val unilateral: Boolean = false,
    val builtIn: Boolean = false,
    val archived: Boolean = false,
) {
    /** Auto Progress needs resistance semantics, a direction and (for load) a valid increment. */
    val loadBased: Boolean
        get() = resistanceModel == ResistanceModel.EXTERNAL_LOAD ||
            resistanceModel == ResistanceModel.BODYWEIGHT_PLUS_LOAD ||
            resistanceModel == ResistanceModel.ASSISTED_BODYWEIGHT

    val incrementValid: Boolean
        get() = !loadBased || (defaultIncrement != null && defaultIncrement > 0.0)

    /** Whether all exercise-level metadata required by Auto Progress is present. */
    val autoProgressReady: Boolean
        get() = when {
            resistanceModel == ResistanceModel.REPS_ONLY ->
                progressionDirection == ProgressionDirection.REPS_ONLY
            loadBased ->
                incrementValid && progressionDirection != ProgressionDirection.REPS_ONLY
            else -> false
        }

    /** Human-readable reason Auto Progress is unavailable, or null when it is available. */
    fun autoProgressBlocker(): String? = when {
        autoProgressReady -> null
        resistanceModel == ResistanceModel.REPS_ONLY &&
            progressionDirection != ProgressionDirection.REPS_ONLY ->
            "Reps-only exercises must use reps-only progression."
        loadBased && !incrementValid -> "needs weight increment"
        loadBased && progressionDirection == ProgressionDirection.REPS_ONLY ->
            "load exercises need a load direction"
        else -> "resistance model not supported by Auto Progress"
    }
}
