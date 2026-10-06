package com.metro.training.domain.plan

import com.metro.training.domain.exercises.MuscleGroup

/**
 * Split presets keyed off the number of training days. Each preset is an ordered list of day
 * templates cycled to fill the requested number of days (e.g. 2 days of an Upper/Lower preset, or
 * 6 days of Push/Pull/Legs become PPL ×2).
 */
enum class SplitPreset(val label: String) {
    FULL_BODY("full body"),
    UPPER_LOWER("upper / lower"),
    PUSH_PULL_LEGS("push / pull / legs"),
    BRO("body-part split"),
    ;

    fun templates(): List<DayTemplate> = when (this) {
        FULL_BODY -> listOf(
            DayTemplate("Full Body", setOf(
                MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES,
                MuscleGroup.CHEST, MuscleGroup.LATS, MuscleGroup.UPPER_BACK,
                MuscleGroup.SIDE_DELTS, MuscleGroup.BICEPS, MuscleGroup.TRICEPS, MuscleGroup.ABS,
            )),
        )
        UPPER_LOWER -> listOf(
            DayTemplate("Upper", setOf(
                MuscleGroup.CHEST, MuscleGroup.UPPER_BACK, MuscleGroup.LATS,
                MuscleGroup.FRONT_DELTS, MuscleGroup.SIDE_DELTS, MuscleGroup.BICEPS, MuscleGroup.TRICEPS,
            )),
            DayTemplate("Lower", setOf(
                MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES,
                MuscleGroup.CALVES, MuscleGroup.ABS,
            )),
        )
        PUSH_PULL_LEGS -> listOf(
            DayTemplate("Push", setOf(
                MuscleGroup.CHEST, MuscleGroup.FRONT_DELTS, MuscleGroup.SIDE_DELTS, MuscleGroup.TRICEPS,
            )),
            DayTemplate("Pull", setOf(
                MuscleGroup.LATS, MuscleGroup.UPPER_BACK, MuscleGroup.REAR_DELTS, MuscleGroup.BICEPS,
            )),
            DayTemplate("Legs", setOf(
                MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES, MuscleGroup.ABS,
            )),
        )
        BRO -> listOf(
            DayTemplate("Chest", setOf(MuscleGroup.CHEST, MuscleGroup.TRICEPS)),
            DayTemplate("Back", setOf(MuscleGroup.LATS, MuscleGroup.UPPER_BACK, MuscleGroup.REAR_DELTS, MuscleGroup.BICEPS)),
            DayTemplate("Shoulders", setOf(MuscleGroup.FRONT_DELTS, MuscleGroup.SIDE_DELTS, MuscleGroup.REAR_DELTS, MuscleGroup.TRAPS)),
            DayTemplate("Arms", setOf(MuscleGroup.BICEPS, MuscleGroup.TRICEPS, MuscleGroup.FOREARMS)),
            DayTemplate("Legs", setOf(MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES, MuscleGroup.ABS)),
        )
    }

    companion object {
        /** Sensible preset for a given weekly frequency. */
        fun suggest(days: Int): SplitPreset = when {
            days <= 1 -> FULL_BODY
            days == 2 -> UPPER_LOWER
            days == 3 -> PUSH_PULL_LEGS
            days == 4 -> UPPER_LOWER
            days == 5 -> BRO
            else -> PUSH_PULL_LEGS
        }
    }
}

data class DayTemplate(val name: String, val muscles: Set<MuscleGroup>)

/** Expand a preset to exactly [days] day focuses, cycling with A/B suffixes when repeated. */
fun SplitPreset.dayFocuses(days: Int): List<DayFocus> {
    val templates = templates()
    if (templates.isEmpty()) return emptyList()
    val result = ArrayList<DayFocus>(days)
    val counts = HashMap<String, Int>()
    for (index in 0 until days) {
        val template = templates[index % templates.size]
        val seen = (counts[template.name] ?: 0) + 1
        counts[template.name] = seen
        val suffix = if (days > templates.size) " " + ('A' + (seen - 1)) else ""
        result += DayFocus(name = template.name + suffix, muscles = template.muscles)
    }
    return result
}
