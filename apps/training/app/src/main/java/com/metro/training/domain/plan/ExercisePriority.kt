package com.metro.training.domain.plan

import com.metro.training.domain.exercises.MuscleGroup

/**
 * Evidence-informed per-muscle exercise priority (compound/most-productive movements first).
 *
 * Rationale (see `references/research.md`):
 * - Multi-joint compound movements are prioritized for major muscles (efficiency, loading, strength
 *   carryover; ACSM progression models).
 * - Muscles with little compound involvement (side/rear delts, biceps, triceps, calves, core) lead
 *   with sensible isolation work.
 * - Stable, loadable movements are preferred so progressive overload can be applied cleanly.
 *
 * These are curated *defaults* for the plan builder; the user may freely edit any routine afterward.
 */
object ExercisePriority {

    private val byMuscle: Map<MuscleGroup, List<String>> = mapOf(
        MuscleGroup.CHEST to listOf(
            "barbell_bench_press", "incline_dumbbell_press", "machine_chest_press", "cable_fly",
        ),
        MuscleGroup.FRONT_DELTS to listOf(
            "overhead_press", "dumbbell_shoulder_press",
        ),
        MuscleGroup.SIDE_DELTS to listOf("lateral_raise"),
        MuscleGroup.REAR_DELTS to listOf("reverse_pec_deck"),
        MuscleGroup.LATS to listOf(
            "pull_up", "lat_pulldown", "barbell_row", "seated_cable_row",
        ),
        MuscleGroup.UPPER_BACK to listOf(
            "barbell_row", "seated_cable_row", "lat_pulldown", "pull_up",
        ),
        MuscleGroup.TRAPS to listOf("barbell_row"),
        MuscleGroup.BICEPS to listOf("dumbbell_curl", "preacher_curl", "cable_curl"),
        MuscleGroup.TRICEPS to listOf(
            "close_grip_bench_press", "cable_pushdown", "overhead_cable_extension",
        ),
        MuscleGroup.FOREARMS to listOf("dumbbell_curl"),
        MuscleGroup.QUADS to listOf(
            "back_squat", "leg_press", "bulgarian_split_squat", "leg_extension",
        ),
        MuscleGroup.HAMSTRINGS to listOf(
            "romanian_deadlift", "seated_leg_curl", "lying_leg_curl",
        ),
        MuscleGroup.GLUTES to listOf(
            "hip_thrust", "romanian_deadlift", "bulgarian_split_squat",
        ),
        MuscleGroup.ADDUCTORS to listOf("bulgarian_split_squat"),
        MuscleGroup.CALVES to listOf("standing_calf_raise", "seated_calf_raise"),
        MuscleGroup.ABS to listOf("cable_crunch", "hanging_leg_raise"),
        MuscleGroup.OBLIQUES to listOf("hanging_leg_raise"),
        MuscleGroup.LOWER_BACK to listOf("romanian_deadlift"),
    )

    fun candidates(muscle: MuscleGroup): List<String> = byMuscle[muscle].orEmpty()
}
