package com.metro.training.domain.plan

import com.metro.training.domain.exercises.MuscleGroup

/** Training emphasis. Affects rep/RIR defaults and exercise ordering — never a hidden multiplier. */
enum class TrainingGoal {
    STRENGTH,
    HYPERTROPHY,
    GENERAL,
    ;

    val label: String
        get() = when (this) {
            STRENGTH -> "strength"
            HYPERTROPHY -> "hypertrophy"
            GENERAL -> "general fitness"
        }
}

/** One training day's focus: a name and the muscle groups it targets. */
data class DayFocus(
    val name: String,
    val muscles: Set<MuscleGroup>,
)

data class PlanRequest(
    val days: Int,
    val goal: TrainingGoal,
    val focusPerDay: List<DayFocus>,
    val rirEnabled: Boolean,
)
