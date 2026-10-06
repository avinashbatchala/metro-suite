package com.metro.training.domain.volume

import com.metro.training.domain.exercises.ExerciseDefinition
import com.metro.training.domain.exercises.MuscleGroup
import com.metro.training.domain.workout.CompletedExerciseExposure
import com.metro.training.domain.workout.SetType

/**
 * Estimated weekly working-set attribution. This is an estimate, not biological truth, and it never
 * alters progression. Primary muscle = 1.0 set, secondary = 0.5 set.
 */
object MuscleVolumeCalculator {
    const val PRIMARY_CREDIT = 1.0
    const val SECONDARY_CREDIT = 0.5

    data class Contribution(val exercise: ExerciseDefinition, val workSets: Int)

    fun estimate(contributions: List<Contribution>): Map<MuscleGroup, Double> {
        val totals = linkedMapOf<MuscleGroup, Double>()
        contributions.forEach { contribution ->
            if (contribution.workSets <= 0) return@forEach
            contribution.exercise.primaryMuscles.forEach { muscle ->
                totals[muscle] = (totals[muscle] ?: 0.0) + PRIMARY_CREDIT * contribution.workSets
            }
            contribution.exercise.secondaryMuscles.forEach { muscle ->
                if (muscle in contribution.exercise.primaryMuscles) return@forEach
                totals[muscle] = (totals[muscle] ?: 0.0) + SECONDARY_CREDIT * contribution.workSets
            }
        }
        return totals
    }

    /** Work sets from a set of completed exposures (warm-ups and drop/backoff excluded). */
    fun estimate(
        exposures: List<CompletedExerciseExposure>,
        exercisesById: Map<String, ExerciseDefinition>,
    ): Map<MuscleGroup, Double> = estimate(
        exposures.mapNotNull { exposure ->
            val exercise = exercisesById[exposure.exerciseId] ?: return@mapNotNull null
            val workSets = exposure.sets.count { it.setType == SetType.WORK && it.completed }
            Contribution(exercise, workSets)
        },
    )
}
