package com.metro.training.domain.plan

import com.metro.training.domain.exercises.BuiltInExercises
import com.metro.training.domain.exercises.MuscleGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineTemplateGeneratorTest {

    private val builtIns = BuiltInExercises.all

    @Test
    fun dayFocuses_fillRequestedDaysWithSuffixes() {
        val focuses = SplitPreset.UPPER_LOWER.dayFocuses(4)
        assertEquals(4, focuses.size)
        assertEquals(listOf("Upper A", "Lower A", "Upper B", "Lower B"), focuses.map { it.name })
    }

    @Test
    fun suggest_picksSensiblePresetByFrequency() {
        assertEquals(SplitPreset.FULL_BODY, SplitPreset.suggest(1))
        assertEquals(SplitPreset.UPPER_LOWER, SplitPreset.suggest(2))
        assertEquals(SplitPreset.PUSH_PULL_LEGS, SplitPreset.suggest(3))
        assertEquals(SplitPreset.UPPER_LOWER, SplitPreset.suggest(4))
        assertEquals(SplitPreset.BRO, SplitPreset.suggest(5))
        assertEquals(SplitPreset.PUSH_PULL_LEGS, SplitPreset.suggest(6))
    }

    @Test
    fun generate_coversEveryTargetMuscleThatHasAnExercise() {
        val focuses = SplitPreset.PUSH_PULL_LEGS.dayFocuses(3)
        val routines = RoutineTemplateGenerator.generate(
            PlanRequest(days = 3, goal = TrainingGoal.HYPERTROPHY, focusPerDay = focuses, rirEnabled = true),
            builtIns,
        )
        assertEquals(3, routines.size)
        val byId = builtIns.associateBy { it.id }
        routines.forEachIndexed { index, routine ->
            assertTrue("routine ${routine.name} should have exercises", routine.exercises.isNotEmpty())
            val covered = routine.exercises
                .mapNotNull { byId[it.prescription.exerciseId] }
                .flatMap { it.primaryMuscles }
                .toSet()
            focuses[index].muscles.forEach { muscle ->
                val hasCandidate = ExercisePriority.candidates(muscle).any { id ->
                    byId[id]?.primaryMuscles?.contains(muscle) == true
                }
                if (hasCandidate) {
                    assertTrue("${routine.name} should cover $muscle", muscle in covered)
                }
            }
        }
    }

    @Test
    fun strengthGoal_prioritisesCompoundAndLowerReps() {
        val focuses = SplitPreset.PUSH_PULL_LEGS.dayFocuses(1) // Push
        val routines = RoutineTemplateGenerator.generate(
            PlanRequest(days = 1, goal = TrainingGoal.STRENGTH, focusPerDay = focuses, rirEnabled = true),
            builtIns,
        )
        val push = routines.first()
        val first = push.exercises.first().prescription
        assertEquals("barbell_bench_press", first.exerciseId)
        assertTrue("strength compound reps should be low", first.repMax <= 5)
        assertTrue(first.targetRirMin != null)
    }

    @Test
    fun hypertrophyGoal_usesHigherRepsForIsolation() {
        // Use an isolation-only day to check isolation rep range.
        val isolationDay = DayFocus("Isolation", setOf(MuscleGroup.SIDE_DELTS))
        val routines = RoutineTemplateGenerator.generate(
            PlanRequest(days = 1, goal = TrainingGoal.HYPERTROPHY, focusPerDay = listOf(isolationDay), rirEnabled = true),
            builtIns,
        )
        val lateral = routines.first().exercises.first().prescription
        assertEquals("lateral_raise", lateral.exerciseId)
        assertTrue("hypertrophy isolation should be higher reps", lateral.repMax >= 15)
    }

    @Test
    fun noRir_leavesTargetRirNull() {
        val focuses = SplitPreset.FULL_BODY.dayFocuses(1)
        val routines = RoutineTemplateGenerator.generate(
            PlanRequest(days = 1, goal = TrainingGoal.GENERAL, focusPerDay = focuses, rirEnabled = false),
            builtIns,
        )
        routines.first().exercises.forEach { record ->
            assertEquals(null, record.prescription.targetRirMin)
            assertEquals(null, record.prescription.targetRirMax)
        }
    }

    @Test
    fun generate_isDeterministic() {
        val focuses = SplitPreset.PUSH_PULL_LEGS.dayFocuses(3)
        val request = PlanRequest(3, TrainingGoal.HYPERTROPHY, focuses, rirEnabled = true)
        val first = RoutineTemplateGenerator.generate(request, builtIns)
        val second = RoutineTemplateGenerator.generate(request, builtIns)
        assertEquals(
            first.map { it.exercises.map { e -> e.prescription.exerciseId } },
            second.map { it.exercises.map { e -> e.prescription.exerciseId } },
        )
        assertEquals(
            first.map { it.exercises.map { e -> e.prescription.repMin to e.prescription.repMax } },
            second.map { it.exercises.map { e -> e.prescription.repMin to e.prescription.repMax } },
        )
    }
}
