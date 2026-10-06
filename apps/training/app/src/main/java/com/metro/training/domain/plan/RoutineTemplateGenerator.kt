package com.metro.training.domain.plan

import com.metro.training.domain.exercises.ExerciseDefaults
import com.metro.training.domain.exercises.ExerciseDefinition
import com.metro.training.domain.exercises.ExerciseMechanic
import com.metro.training.domain.exercises.MuscleGroup
import com.metro.training.domain.routines.Routine
import com.metro.training.domain.routines.RoutineExercise
import com.metro.training.domain.routines.RoutineExercisePrescription
import java.util.UUID

/**
 * Deterministic plan builder. Given a weekly frequency, a goal and the per-day muscle focuses, it
 * produces concrete routines from the built-in library using evidence-informed exercise priority
 * (compound-first, cover every target muscle, add sensible isolation).
 *
 * Pure Kotlin — no Android dependencies. The generated routines are fully editable afterwards.
 */
object RoutineTemplateGenerator {

    /** Target movements per day (capped by available candidates for narrow focuses). */
    private const val TARGET_EXERCISES_PER_DAY = 6

    /** Canonical ordering so the largest / most demanding muscles get their compound first. */
    private val muscleOrder: List<MuscleGroup> = listOf(
        MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES,
        MuscleGroup.CHEST, MuscleGroup.LATS, MuscleGroup.UPPER_BACK,
        MuscleGroup.FRONT_DELTS, MuscleGroup.SIDE_DELTS, MuscleGroup.REAR_DELTS, MuscleGroup.TRAPS,
        MuscleGroup.BICEPS, MuscleGroup.TRICEPS, MuscleGroup.FOREARMS,
        MuscleGroup.CALVES, MuscleGroup.ABS, MuscleGroup.OBLIQUES,
        MuscleGroup.LOWER_BACK, MuscleGroup.ADDUCTORS,
    )

    fun generate(request: PlanRequest, builtIns: List<ExerciseDefinition>): List<Routine> {
        val byId = builtIns.associateBy { it.id }
        return request.focusPerDay.map { day -> buildDay(day, request, byId) }
    }

    private fun buildDay(
        day: DayFocus,
        request: PlanRequest,
        byId: Map<String, ExerciseDefinition>,
    ): Routine {
        val orderedMuscles = day.muscles.sortedBy { muscleOrder.indexOf(it).let { i -> if (i < 0) 99 else i } }
        val selected = LinkedHashSet<String>()

        // 1. Guarantee each target muscle has at least its top movement.
        orderedMuscles.forEach { muscle ->
            ExercisePriority.candidates(muscle).firstOrNull { it in byId }?.let { selected.add(it) }
        }

        // 2. Fill toward the target with the next-best movements (still compound-first per muscle).
        outer@ for (muscle in orderedMuscles) {
            for (id in ExercisePriority.candidates(muscle)) {
                if (selected.size >= TARGET_EXERCISES_PER_DAY) break@outer
                if (id in byId) selected.add(id)
            }
        }

        val exercises = selected.mapNotNull { byId[it] }
            .sortedWith(compareBy({ it.mechanic != ExerciseMechanic.COMPOUND }, { it.name }))

        val routineExercises = exercises.mapIndexed { index, exercise ->
            RoutineExercise(
                prescription = prescriptionFor(exercise, request),
                order = index,
            )
        }

        return Routine(
            id = UUID.randomUUID().toString(),
            name = day.name,
            exercises = routineExercises,
            createdAt = System.currentTimeMillis(),
        )
    }

    private fun prescriptionFor(
        exercise: ExerciseDefinition,
        request: PlanRequest,
    ): RoutineExercisePrescription {
        val base = when {
            request.goal == TrainingGoal.STRENGTH && exercise.mechanic == ExerciseMechanic.COMPOUND ->
                ExerciseDefaults.forGoal(ExerciseDefaults.Goal.STRENGTH)
            request.goal == TrainingGoal.HYPERTROPHY && exercise.mechanic == ExerciseMechanic.ISOLATION ->
                ExerciseDefaults.forGoal(ExerciseDefaults.Goal.HIGH_REP_ISOLATION)
            else -> ExerciseDefaults.forExercise(exercise)
        }
        return RoutineExercisePrescription(
            id = UUID.randomUUID().toString(),
            exerciseId = exercise.id,
            workSetCount = base.workSetCount,
            repMin = base.repMin,
            repMax = base.repMax,
            targetRirMin = if (request.rirEnabled) base.targetRirMin else null,
            targetRirMax = if (request.rirEnabled) base.targetRirMax else null,
            restSeconds = base.restSeconds,
            autoProgressEnabled = exercise.autoProgressReady,
        )
    }
}
