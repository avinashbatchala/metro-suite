package com.metro.training.domain.equipment

import com.metro.training.domain.progression.WeightMath

/** A suggested warm-up set: percentage of the working weight, reps, and the rounded load. */
data class WarmupSet(val percent: Int, val reps: Int, val loadKg: Double)

/**
 * Conservative warm-up calculator: a few ramped sets as a percentage of the working weight, rounded
 * to the available increment and never below the empty bar. Warm-ups are suggestions only.
 */
object WarmupCalculator {
    private val ladder = listOf(40 to 5, 60 to 3, 80 to 2, 90 to 1)

    fun suggest(workingKg: Double, barKg: Double, incrementKg: Double): List<WarmupSet> {
        if (workingKg <= barKg) return emptyList()
        val step = if (incrementKg > 0) incrementKg else 2.5
        return ladder.mapNotNull { (percent, reps) ->
            val target = workingKg * percent / 100.0
            val rounded = WeightMath.snapToIncrement(target, step)
                .coerceAtLeast(barKg)
                .coerceAtMost(workingKg)
            WarmupSet(percent, reps, rounded)
        }
            .filter { it.loadKg < workingKg - 1e-6 }
            .distinctBy { it.loadKg }
            .sortedBy { it.loadKg }
    }
}
