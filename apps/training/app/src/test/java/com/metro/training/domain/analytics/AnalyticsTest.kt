package com.metro.training.domain.analytics

import com.metro.training.domain.exercises.BuiltInExercises
import com.metro.training.domain.workout.CompletedExerciseExposure
import com.metro.training.domain.workout.ExposureSnapshot
import com.metro.training.domain.workout.SetQuality
import com.metro.training.domain.workout.SetType
import com.metro.training.domain.workout.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.concurrent.TimeUnit

class AnalyticsTest {

    private val bench = BuiltInExercises.byId.getValue("barbell_bench_press")
    private val now = 1_700_000_000_000L

    @Test
    fun epley_onlyWithinSensibleRepRange() {
        assertEquals(103.33, OneRepMax.epley(100.0, 1)!!, 0.01)
        assertNull(OneRepMax.epley(100.0, 13))
        assertNull(OneRepMax.epley(0.0, 5))
        assertNull(OneRepMax.epley(100.0, 0))
    }

    @Test
    fun series_computesEachMetric() {
        val exposure = exposure("e1", now - 1_000, listOf(80.0 to 10, 80.0 to 8, 75.0 to 6))
        assertEquals(106.67, ExerciseAnalytics.valueOf(exposure, ProgressMetric.ESTIMATED_1RM)!!, 0.01)
        assertEquals(80.0, ExerciseAnalytics.valueOf(exposure, ProgressMetric.TOP_SET_LOAD)!!, 1e-9)
        assertEquals(24.0, ExerciseAnalytics.valueOf(exposure, ProgressMetric.TOTAL_REPS)!!, 1e-9)
        // 80*10 + 80*8 + 75*6 = 1890
        assertEquals(1890.0, ExerciseAnalytics.valueOf(exposure, ProgressMetric.VOLUME_LOAD)!!, 1e-9)
    }

    @Test
    fun series_rangeFilterRecentOnly() {
        val recent = exposure("recent", now - TimeUnit.DAYS.toMillis(2), listOf(80.0 to 10))
        val old = exposure("old", now - TimeUnit.DAYS.toMillis(200), listOf(90.0 to 8))
        val week = ExerciseAnalytics.series(listOf(recent, old), ProgressMetric.TOP_SET_LOAD, ProgressRange.WEEK, now)
        assertEquals(1, week.size)
        assertEquals(80.0, week.first().value, 1e-9)
        val all = ExerciseAnalytics.series(listOf(recent, old), ProgressMetric.TOP_SET_LOAD, ProgressRange.ALL, now)
        assertEquals(2, all.size)
    }

    @Test
    fun personalBest_takesMaximum() {
        val a = exposure("a", now - 1000, listOf(80.0 to 10))
        val b = exposure("b", now - 2000, listOf(90.0 to 8))
        assertEquals(
            90.0,
            ExerciseAnalytics.personalBest(listOf(a, b), ProgressMetric.TOP_SET_LOAD)!!,
            1e-9,
        )
    }

    private fun exposure(
        id: String,
        performedAt: Long,
        sets: List<Pair<Double, Int>>,
    ) = CompletedExerciseExposure(
        id = id,
        exerciseId = bench.id,
        prescriptionId = "p",
        performedAt = performedAt,
        snapshot = ExposureSnapshot(
            repMin = 6, repMax = 10, workSetCount = sets.size,
            targetRirMin = 1, targetRirMax = 3,
            loadSemantics = bench.loadSemantics,
            progressionDirection = bench.progressionDirection,
            incrementKg = 2.5,
        ),
        sets = sets.mapIndexed { index, (load, reps) ->
            WorkoutSet(
                id = "$id-$index",
                exerciseSessionId = id,
                setIndex = index + 1,
                setType = SetType.WORK,
                load = load,
                repsCompleted = reps,
                rir = 2,
                quality = SetQuality.NORMAL,
                completed = true,
                prescribed = true,
            )
        },
    )
}
