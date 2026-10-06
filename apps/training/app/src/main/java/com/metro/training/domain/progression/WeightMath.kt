package com.metro.training.domain.progression

import com.metro.training.domain.exercises.ProgressionDirection
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Canonical weight maths. All values are kilograms internally. Results are snapped to a sane
 * decimal precision so `80 + 2.5` never becomes `82.50000000000001`.
 */
object WeightMath {
    const val KG_PER_LB = 0.45359237

    fun kgToLb(kg: Double): Double = clean(kg / KG_PER_LB)

    fun lbToKg(lb: Double): Double = clean(lb * KG_PER_LB)

    /** One increment harder (or lighter assistance). */
    fun increase(load: Double, increment: Double, direction: ProgressionDirection): Double? =
        when (direction) {
            ProgressionDirection.MORE_LOAD_IS_HARDER -> clean(load + increment)
            ProgressionDirection.LESS_LOAD_IS_HARDER -> clean((load - increment).coerceAtLeast(0.0))
            ProgressionDirection.REPS_ONLY -> null
        }

    /** One increment easier (or more assistance). */
    fun decrease(load: Double, increment: Double, direction: ProgressionDirection): Double? =
        when (direction) {
            ProgressionDirection.MORE_LOAD_IS_HARDER -> clean((load - increment).coerceAtLeast(0.0))
            ProgressionDirection.LESS_LOAD_IS_HARDER -> clean(load + increment)
            ProgressionDirection.REPS_ONLY -> null
        }

    /** Snap an arbitrary user/entered value to the nearest valid equipment step. */
    fun snapToIncrement(value: Double, increment: Double): Double {
        if (increment <= 0.0) return clean(value)
        val steps = Math.round(value / increment).toDouble()
        return clean(steps * increment)
    }

    /** Round to at most 3 decimals (gram precision) and drop trailing zeros. */
    fun clean(value: Double): Double =
        BigDecimal(value).setScale(3, RoundingMode.HALF_UP).stripTrailingZeros().toDouble()
}
