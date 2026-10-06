package com.metro.training.domain.analytics

/**
 * Estimated one-rep max. Uses a single documented equation (Epley) and only reports for a sensible
 * rep range. This is an *estimate*, never presented as a measured 1RM.
 */
object OneRepMax {
    const val MAX_REPS_FOR_ESTIMATE = 12

    /** Epley: 1RM ≈ load × (1 + reps / 30). Null outside a sensible range or for zero load. */
    fun epley(loadKg: Double, reps: Int): Double? =
        if (loadKg > 0.0 && reps in 1..MAX_REPS_FOR_ESTIMATE) {
            loadKg * (1.0 + reps / 30.0)
        } else {
            null
        }
}
