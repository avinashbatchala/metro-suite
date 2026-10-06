package com.metro.training.domain.analytics

import com.metro.training.domain.workout.CompletedExerciseExposure
import com.metro.training.domain.workout.SetType
import java.util.concurrent.TimeUnit

/** Which performance metric the progress chart shows. */
enum class ProgressMetric(val label: String) {
    ESTIMATED_1RM("estimated 1RM"),
    TOP_SET_LOAD("top-set load"),
    TOTAL_REPS("total reps"),
    VOLUME_LOAD("volume load"),
}

/** Time window for charts (explicit, user-selected). */
enum class ProgressRange(val label: String, val days: Int?) {
    WEEK("week", 7),
    MONTH("month", 30),
    THREE_MONTHS("3m", 90),
    SIX_MONTHS("6m", 180),
    YEAR("year", 365),
    ALL("all", null),
}

/** One plotted point (session end time → metric value). */
data class ExercisePoint(val timestamp: Long, val value: Double)

/**
 * Builds chart series from completed exposures. Pure and deterministic; the UI does not do maths.
 */
object ExerciseAnalytics {

    fun series(
        exposures: List<CompletedExerciseExposure>,
        metric: ProgressMetric,
        range: ProgressRange,
        now: Long = System.currentTimeMillis(),
    ): List<ExercisePoint> {
        val cutoff = range.days?.let { now - TimeUnit.DAYS.toMillis(it.toLong()) }
        return exposures
            .filter { cutoff == null || it.performedAt >= cutoff }
            .mapNotNull { exposure ->
                valueOf(exposure, metric)?.let { ExercisePoint(exposure.performedAt, it) }
            }
            .sortedBy { it.timestamp }
    }

    fun valueOf(exposure: CompletedExerciseExposure, metric: ProgressMetric): Double? {
        val work = exposure.sets.filter { it.setType == SetType.WORK && it.completed }
        if (work.isEmpty()) return null
        return when (metric) {
            ProgressMetric.ESTIMATED_1RM -> work
                .mapNotNull { set -> set.load?.let { OneRepMax.epley(it, set.repsCompleted) } }
                .maxOrNull()
            ProgressMetric.TOP_SET_LOAD -> work.mapNotNull { it.load }.maxOrNull()
            ProgressMetric.TOTAL_REPS -> work.sumOf { it.repsCompleted }.toDouble()
            ProgressMetric.VOLUME_LOAD -> work.sumOf { (it.load ?: 0.0) * it.repsCompleted }
        }
    }

    /** Best value ever recorded for [metric] across [exposures] (used for PR detection). */
    fun personalBest(
        exposures: List<CompletedExerciseExposure>,
        metric: ProgressMetric,
    ): Double? = exposures.mapNotNull { valueOf(it, metric) }.maxOrNull()
}
