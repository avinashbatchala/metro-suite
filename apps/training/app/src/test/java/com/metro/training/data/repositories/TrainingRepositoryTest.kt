package com.metro.training.data.repositories

import androidx.room.Room
import com.metro.training.data.database.TrainingDatabase
import com.metro.training.domain.progression.ProgressionDecision
import com.metro.training.domain.routines.Routine
import com.metro.training.domain.routines.RoutineExercise
import com.metro.training.domain.routines.RoutineExercisePrescription
import com.metro.training.domain.workout.Workout
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * End-to-end repository + Room + progression engine test. Exercises the §151 acceptance sequence
 * through real persistence rather than only the pure engine.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TrainingRepositoryTest {

    private lateinit var db: TrainingDatabase
    private lateinit var repo: TrainingRepository

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, TrainingDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = TrainingRepository(context, db.dao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun seedsBuiltInLibrary() = runBlocking {
        repo.ensureSeeded()
        repo.ensureSeeded() // idempotent
        val exercises = repo.exercises()
        assertTrue(exercises.any { it.id == "barbell_bench_press" })
        assertTrue(exercises.any { it.id == "lateral_raise" })
        assertTrue(exercises.any { it.id == "assisted_pull_up" })
    }

    @Test
    fun doubleProgressionAcrossPersistedWorkouts() = runBlocking {
        repo.ensureSeeded()
        val routine = benchRoutine()
        repo.saveRoutine(routine)

        // Session 1: 8 / 8 / 7 @2 → ADD_REPS with total+1 target.
        val session1 = repo.startWorkout(repo.routine("r1")!!)
        logWorkout(session1, reps = listOf(8, 8, 7), rir = 2, load = 80.0)
        val recs1 = repo.finishWorkout(session1)
        assertEquals(ProgressionDecision.ADD_REPS, recs1.first().decision)
        assertEquals(24, recs1.first().repTarget!!.totalRepsGoal)

        // Session 2: 10 / 10 / 10 within RIR → ADD_LOAD 82.5.
        val session2 = repo.startWorkout(repo.routine("r1")!!)
        logWorkout(session2, reps = listOf(10, 10, 10), rir = 2, load = 80.0)
        val recs2 = repo.finishWorkout(session2)
        assertEquals(ProgressionDecision.ADD_LOAD, recs2.first().decision)
        assertEquals(82.5, recs2.first().nextLoad!!, 1e-9)
    }

    @Test
    fun recommendationsAreStoredAndQueryable() = runBlocking {
        repo.ensureSeeded()
        repo.saveRoutine(benchRoutine())
        val session = repo.startWorkout(repo.routine("r1")!!)
        logWorkout(session, reps = listOf(10, 10, 10), rir = 2, load = 80.0)
        repo.finishWorkout(session)
        val stored = repo.recommendationsForWorkout(session)
        assertEquals(1, stored.size)
        assertEquals(1, stored.first().algorithmVersion)
    }

    private fun benchRoutine() = Routine(
        id = "r1",
        name = "Push A",
        exercises = listOf(
            RoutineExercise(
                prescription = RoutineExercisePrescription(
                    id = "p1",
                    exerciseId = "barbell_bench_press",
                    workSetCount = 3,
                    repMin = 6,
                    repMax = 10,
                    targetRirMin = 1,
                    targetRirMax = 3,
                    restSeconds = 120,
                    autoProgressEnabled = true,
                ),
                order = 0,
            ),
        ),
    )

    private suspend fun logWorkout(
        workoutId: String,
        reps: List<Int>,
        rir: Int?,
        load: Double,
    ) {
        val workout: Workout = repo.workout(workoutId)!!
        val exercise = workout.exercises.first()
        val workSets = exercise.sets.filter { it.prescribed }
        workSets.forEachIndexed { index, set ->
            val updated = set.copy(
                load = load,
                repsCompleted = reps.getOrElse(index) { 0 },
                rir = rir,
                completed = true,
            )
            repo.saveSet(updated)
        }
    }
}
