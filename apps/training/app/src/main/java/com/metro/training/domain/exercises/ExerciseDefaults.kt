package com.metro.training.domain.exercises

/**
 * Suggested prescription defaults derived from exercise metadata. These are conveniences the user
 * can always change — they never become hidden algorithm branches.
 */
object ExerciseDefaults {

    data class Prescription(
        val workSetCount: Int,
        val repMin: Int,
        val repMax: Int,
        val targetRirMin: Int?,
        val targetRirMax: Int?,
        val restSeconds: Int,
    )

    enum class Goal { STRENGTH, HYPERTROPHY, HIGH_REP_ISOLATION }

    fun forExercise(exercise: ExerciseDefinition): Prescription {
        val rest = when {
            exercise.mechanic == ExerciseMechanic.COMPOUND && exercise.equipment == EquipmentType.BARBELL -> 180
            exercise.mechanic == ExerciseMechanic.COMPOUND -> 120
            else -> 90
        }
        return when {
            exercise.resistanceModel == ResistanceModel.REPS_ONLY ->
                Prescription(3, 10, 20, null, null, rest)
            exercise.mechanic == ExerciseMechanic.COMPOUND &&
                exercise.equipment == EquipmentType.BARBELL ->
                Prescription(3, 4, 8, 2, 3, rest)
            exercise.mechanic == ExerciseMechanic.COMPOUND ->
                Prescription(3, 6, 10, 1, 3, rest)
            exercise.equipment == EquipmentType.DUMBBELL &&
                (exercise.defaultIncrement ?: 0.0) >= 2.0 ->
                Prescription(3, 8, 15, 1, 2, rest)
            else ->
                Prescription(3, 8, 12, 1, 2, rest)
        }
    }

    /** Routine-exercise presets (§126). Only prefill; never a hidden algorithm branch. */
    fun forGoal(goal: Goal): Prescription = when (goal) {
        Goal.STRENGTH -> Prescription(3, 3, 5, 2, 4, 180)
        Goal.HYPERTROPHY -> Prescription(3, 6, 10, 1, 3, 120)
        Goal.HIGH_REP_ISOLATION -> Prescription(3, 10, 20, 0, 2, 90)
    }
}
