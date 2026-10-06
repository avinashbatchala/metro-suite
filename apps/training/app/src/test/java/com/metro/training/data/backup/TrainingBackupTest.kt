package com.metro.training.data.backup

import com.metro.training.domain.workout.ExposureSnapshot
import com.metro.training.domain.workout.SetQuality
import com.metro.training.domain.workout.SetType
import com.metro.training.domain.workout.Workout
import com.metro.training.domain.workout.WorkoutExercise
import com.metro.training.domain.workout.WorkoutSet
import com.metro.training.domain.workout.WorkoutStatus
import com.metro.training.domain.exercises.LoadSemantics
import com.metro.training.domain.exercises.ProgressionDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TrainingBackupTest {

    @Test
    fun jsonRoundTripPreservesWorkouts() {
        val workout = sampleWorkout()
        val json = TrainingBackup.exportJson(listOf(workout))
        val parsed = TrainingBackup.parseJson(json)
        assertEquals(1, parsed.size)
        val restored = parsed.first()
        assertEquals("Push", restored.routineName)
        assertEquals(1, restored.exercises.size)
        assertEquals(2, restored.exercises.first().sets.size)
        assertEquals(80.0, restored.exercises.first().sets.first().load!!, 1e-9)
        assertEquals(10, restored.exercises.first().sets.first().repsCompleted)
        assertEquals(SetType.DROP, restored.exercises.first().sets[1].setType)
    }

    @Test
    fun csvExportHasHevyHeaderAndRows() {
        val csv = TrainingBackup.exportCsv(listOf(sampleWorkout()))
        assertTrue(csv.startsWith("title,start_time,end_time,exercise_title"))
        assertEquals(3, csv.trim().lines().size) // header + 2 set rows
    }

    private fun sampleWorkout(): Workout {
        val workoutId = "w1"
        val exerciseId = "we1"
        return Workout(
            id = workoutId,
            routineId = null,
            routineName = "Push",
            startedAt = 1_700_000_000_000,
            finishedAt = 1_700_000_600_000,
            status = WorkoutStatus.COMPLETED,
            restDeadlineMillis = null,
            note = "",
            exercises = listOf(
                WorkoutExercise(
                    id = exerciseId,
                    workoutId = workoutId,
                    exerciseId = "barbell_bench_press",
                    exerciseName = "Barbell Bench Press",
                    order = 0,
                    prescriptionId = "p1",
                    snapshot = ExposureSnapshot(
                        repMin = 6, repMax = 10, workSetCount = 1,
                        targetRirMin = 1, targetRirMax = 3,
                        loadSemantics = LoadSemantics.TOTAL_WEIGHT,
                        progressionDirection = ProgressionDirection.MORE_LOAD_IS_HARDER,
                        incrementKg = 2.5,
                    ),
                    restSeconds = 120,
                    autoProgressEnabled = true,
                    note = "",
                    supersetTag = null,
                    sets = listOf(
                        WorkoutSet(
                            id = "s1", exerciseSessionId = exerciseId, setIndex = 1,
                            setType = SetType.WORK, load = 80.0, repsCompleted = 10, rir = 2,
                            quality = SetQuality.NORMAL, completed = true, prescribed = true,
                        ),
                        WorkoutSet(
                            id = "s2", exerciseSessionId = exerciseId, setIndex = 2,
                            setType = SetType.DROP, load = 60.0, repsCompleted = 8, rir = 0,
                            quality = SetQuality.NORMAL, completed = true, prescribed = false,
                        ),
                    ),
                ),
            ),
        )
    }
}
