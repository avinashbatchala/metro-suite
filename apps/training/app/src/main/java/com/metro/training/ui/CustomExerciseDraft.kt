package com.metro.training.ui

import com.metro.training.domain.exercises.EquipmentType
import com.metro.training.domain.exercises.ExerciseDefinition
import com.metro.training.domain.exercises.ExerciseMechanic
import com.metro.training.domain.exercises.LoadSemantics
import com.metro.training.domain.exercises.LoadUnit
import com.metro.training.domain.exercises.MuscleGroup
import com.metro.training.domain.exercises.ProgressionDirection
import com.metro.training.domain.exercises.ResistanceModel
import java.util.UUID

/**
 * Progressive custom-exercise form model. Only fields relevant to the chosen resistance model are
 * shown; validation mirrors [ExerciseDefinition.autoProgressBlocker].
 */
data class CustomExerciseDraft(
    val id: String = "custom_${UUID.randomUUID()}",
    val name: String = "",
    val mechanic: ExerciseMechanic = ExerciseMechanic.COMPOUND,
    val equipment: EquipmentType = EquipmentType.BARBELL,
    val resistanceModel: ResistanceModel = ResistanceModel.EXTERNAL_LOAD,
    val loadSemantics: LoadSemantics = LoadSemantics.TOTAL_WEIGHT,
    val progressionDirection: ProgressionDirection = ProgressionDirection.MORE_LOAD_IS_HARDER,
    val incrementKgText: String = "",
    val primaryMuscles: Set<MuscleGroup> = emptySet(),
    val secondaryMuscles: Set<MuscleGroup> = emptySet(),
    val unilateral: Boolean = false,
) {
    val loadBased: Boolean
        get() = resistanceModel == ResistanceModel.EXTERNAL_LOAD ||
            resistanceModel == ResistanceModel.BODYWEIGHT_PLUS_LOAD ||
            resistanceModel == ResistanceModel.ASSISTED_BODYWEIGHT

    fun toDefinition(): ExerciseDefinition? {
        if (name.isBlank()) return null
        if (primaryMuscles.isEmpty()) return null
        val increment = incrementKgText.trim().toDoubleOrNull()
        if (loadBased && (increment == null || increment <= 0.0)) return null
        val direction = when {
            resistanceModel == ResistanceModel.REPS_ONLY -> ProgressionDirection.REPS_ONLY
            resistanceModel == ResistanceModel.ASSISTED_BODYWEIGHT -> ProgressionDirection.LESS_LOAD_IS_HARDER
            else -> progressionDirection
        }
        return ExerciseDefinition(
            id = id,
            name = name.trim(),
            mechanic = mechanic,
            equipment = equipment,
            resistanceModel = resistanceModel,
            loadSemantics = if (resistanceModel == ResistanceModel.REPS_ONLY) LoadSemantics.NONE else loadSemantics,
            progressionDirection = direction,
            defaultIncrement = if (loadBased) increment else null,
            incrementUnit = if (loadBased) LoadUnit.KG else null,
            primaryMuscles = primaryMuscles,
            secondaryMuscles = secondaryMuscles - primaryMuscles,
            unilateral = unilateral,
            builtIn = false,
        )
    }

    fun validationMessage(): String = when {
        name.isBlank() -> "Enter a name"
        primaryMuscles.isEmpty() -> "Pick at least one primary muscle"
        loadBased && (incrementKgText.trim().toDoubleOrNull() ?: 0.0) <= 0.0 ->
            "Auto Progress needs a weight increment"
        else -> "Check the form"
    }

    fun validationSummary(): String = toDefinition()?.let {
        if (it.autoProgressReady) "Auto Progress: available" else "Auto Progress: ${it.autoProgressBlocker()}"
    } ?: "Auto Progress: needs more information"
}
