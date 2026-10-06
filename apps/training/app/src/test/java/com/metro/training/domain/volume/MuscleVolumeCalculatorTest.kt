package com.metro.training.domain.volume

import com.metro.training.domain.exercises.BuiltInExercises
import com.metro.training.domain.exercises.MuscleGroup
import com.metro.training.domain.workout.CompletedExerciseExposure
import com.metro.training.domain.workout.ExposureSnapshot
import com.metro.training.domain.workout.SetQuality
import com.metro.training.domain.workout.SetType
import com.metro.training.domain.workout.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Test

class MuscleVolumeCalculatorTest {

    private val bench = BuiltInExercises.byId.getValue("barbell_bench_press")

    @Test
    fun primaryGetsFullCreditSecondaryHalf() {
        val volume = MuscleVolumeCalculator.estimate(
            listOf(MuscleVolumeCalculator.Contribution(bench, 3)),
        )
        assertEquals(3.0, volume[MuscleGroup.CHEST]!!, 1e-9)
        assertEquals(1.5, volume[MuscleGroup.TRICEPS]!!, 1e-9)
        assertEquals(1.5, volume[MuscleGroup.FRONT_DELTS]!!, 1e-9)
    }

    @Test
    fun warmupSetsAreExcluded() {
        val exposure = CompletedExerciseExposure(
            id = "e",
            exerciseId = bench.id,
            prescriptionId = "p",
            performedAt = 0L,
            snapshot = ExposureSnapshot(
                repMin = 6, repMax = 10, workSetCount = 3,
                targetRirMin = 1, targetRirMax = 3,
                loadSemantics = bench.loadSemantics,
                progressionDirection = bench.progressionDirection,
                incrementKg = 2.5,
            ),
            sets = listOf(
                set("w1", 1, SetType.WARMUP, 10),
                set("w2", 2, SetType.WARMUP, 5),
                set("s1", 3, SetType.WORK, 8),
                set("s2", 4, SetType.WORK, 8),
                set("s3", 5, SetType.WORK, 8),
            ),
        )
        val volume = MuscleVolumeCalculator.estimate(listOf(exposure), mapOf(bench.id to bench))
        assertEquals(3.0, volume[MuscleGroup.CHEST]!!, 1e-9)
    }

    private fun set(id: String, index: Int, type: SetType, reps: Int) = WorkoutSet(
        id = id,
        exerciseSessionId = "e",
        setIndex = index,
        setType = type,
        load = 80.0,
        repsCompleted = reps,
        rir = null,
        quality = SetQuality.NORMAL,
        completed = true,
        prescribed = true,
    )
}
