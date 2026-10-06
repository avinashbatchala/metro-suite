package com.metro.training.data

import com.metro.training.data.database.ExerciseEntity
import com.metro.training.data.database.ProgressionRecommendationEntity
import com.metro.training.data.database.RoutineEntity
import com.metro.training.data.database.RoutineExerciseEntity
import com.metro.training.data.database.WorkoutEntity
import com.metro.training.data.database.WorkoutExerciseEntity
import com.metro.training.data.database.WorkoutSetEntity
import com.metro.training.domain.exercises.EquipmentType
import com.metro.training.domain.exercises.ExerciseDefinition
import com.metro.training.domain.exercises.ExerciseMechanic
import com.metro.training.domain.exercises.LoadSemantics
import com.metro.training.domain.exercises.LoadUnit
import com.metro.training.domain.exercises.MuscleGroup
import com.metro.training.domain.exercises.ProgressionDirection
import com.metro.training.domain.exercises.ResistanceModel
import com.metro.training.domain.progression.ProgressionConfidence
import com.metro.training.domain.progression.ProgressionDecision
import com.metro.training.domain.progression.ProgressionReason
import com.metro.training.domain.progression.ProgressionRecommendation
import com.metro.training.domain.progression.ProgressionExplanation
import com.metro.training.domain.progression.RepTarget
import com.metro.training.domain.routines.ProgressionMethod
import com.metro.training.domain.routines.ProgressionPolicy
import com.metro.training.domain.routines.Routine
import com.metro.training.domain.routines.RoutineExercise
import com.metro.training.domain.routines.RoutineExercisePrescription
import com.metro.training.domain.workout.ExposureSnapshot
import com.metro.training.domain.workout.SetQuality
import com.metro.training.domain.workout.SetType
import com.metro.training.domain.workout.Workout
import com.metro.training.domain.workout.WorkoutExercise
import com.metro.training.domain.workout.WorkoutSet
import com.metro.training.domain.workout.WorkoutStatus

private const val SEP = ","

private fun Set<MuscleGroup>.encode(): String = joinToString(SEP) { it.name }

private fun String.decodeMuscles(): Set<MuscleGroup> =
    split(SEP).mapNotNull { token -> token.trim().takeIf { it.isNotEmpty() }?.let { runCatching { MuscleGroup.valueOf(it) }.getOrNull() } }.toSet()

fun ExerciseEntity.toDomain(): ExerciseDefinition = ExerciseDefinition(
    id = id,
    name = name,
    mechanic = runCatching { ExerciseMechanic.valueOf(mechanic) }.getOrDefault(ExerciseMechanic.OTHER),
    equipment = runCatching { EquipmentType.valueOf(equipment) }.getOrDefault(EquipmentType.OTHER),
    resistanceModel = runCatching { ResistanceModel.valueOf(resistanceModel) }.getOrDefault(ResistanceModel.REPS_ONLY),
    loadSemantics = runCatching { LoadSemantics.valueOf(loadSemantics) }.getOrDefault(LoadSemantics.NONE),
    progressionDirection = runCatching { ProgressionDirection.valueOf(progressionDirection) }.getOrDefault(ProgressionDirection.REPS_ONLY),
    defaultIncrement = defaultIncrement,
    incrementUnit = incrementUnit?.let { runCatching { LoadUnit.valueOf(it) }.getOrNull() },
    primaryMuscles = primaryMuscles.decodeMuscles(),
    secondaryMuscles = secondaryMuscles.decodeMuscles(),
    unilateral = unilateral,
    builtIn = builtIn,
    archived = archived,
)

fun ExerciseDefinition.toEntity(now: Long = System.currentTimeMillis()): ExerciseEntity = ExerciseEntity(
    id = id,
    name = name,
    mechanic = mechanic.name,
    equipment = equipment.name,
    resistanceModel = resistanceModel.name,
    loadSemantics = loadSemantics.name,
    progressionDirection = progressionDirection.name,
    defaultIncrement = defaultIncrement,
    incrementUnit = incrementUnit?.name,
    primaryMuscles = primaryMuscles.encode(),
    secondaryMuscles = secondaryMuscles.encode(),
    unilateral = unilateral,
    builtIn = builtIn,
    archived = archived,
    updatedAt = now,
)

fun RoutineExerciseEntity.toPrescription() = RoutineExercisePrescription(
    id = id,
    exerciseId = exerciseId,
    workSetCount = workSetCount,
    repMin = repMin,
    repMax = repMax,
    targetRirMin = targetRirMin,
    targetRirMax = targetRirMax,
    restSeconds = restSeconds,
    autoProgressEnabled = autoProgressEnabled,
    progressionPolicy = ProgressionPolicy(
        method = runCatching { ProgressionMethod.valueOf(policyMethod) }.getOrDefault(ProgressionMethod.DOUBLE_PROGRESSION),
        requireTwoTopRangeSessionsWithoutRir = requireTwoTopRangeWithoutRir,
        underperformanceSessionsBeforeDecrease = underperformanceBeforeDecrease,
        longGapDays = longGapDays,
    ),
    incrementOverrideKg = incrementOverrideKg,
    note = note,
)

fun RoutineExercisePrescription.toEntity(routineId: String, order: Int) = RoutineExerciseEntity(
    id = id,
    routineId = routineId,
    exerciseId = exerciseId,
    order = order,
    workSetCount = workSetCount,
    repMin = repMin,
    repMax = repMax,
    targetRirMin = targetRirMin,
    targetRirMax = targetRirMax,
    restSeconds = restSeconds,
    autoProgressEnabled = autoProgressEnabled,
    incrementOverrideKg = incrementOverrideKg,
    policyMethod = progressionPolicy.method.name,
    requireTwoTopRangeWithoutRir = progressionPolicy.requireTwoTopRangeSessionsWithoutRir,
    underperformanceBeforeDecrease = progressionPolicy.underperformanceSessionsBeforeDecrease,
    longGapDays = progressionPolicy.longGapDays,
    note = note,
)

fun RoutineEntity.toDomain(exercises: List<RoutineExerciseEntity>): Routine = Routine(
    id = id,
    name = name,
    exercises = exercises.sortedBy { it.order }
        .map { RoutineExercise(prescription = it.toPrescription(), order = it.order) },
    archived = archived,
    createdAt = createdAt,
)

fun WorkoutSetEntity.toDomain() = WorkoutSet(
    id = id,
    exerciseSessionId = workoutExerciseId,
    setIndex = setIndex,
    setType = runCatching { SetType.valueOf(setType) }.getOrDefault(SetType.WORK),
    load = load,
    repsCompleted = repsCompleted,
    rir = rir,
    quality = runCatching { SetQuality.valueOf(quality) }.getOrDefault(SetQuality.NORMAL),
    completed = completed,
    prescribed = prescribed,
    timestamp = timestamp,
)

fun WorkoutSet.toEntity(workoutExerciseId: String) = WorkoutSetEntity(
    id = id,
    workoutExerciseId = workoutExerciseId,
    setIndex = setIndex,
    setType = setType.name,
    load = load,
    repsCompleted = repsCompleted,
    rir = rir,
    quality = quality.name,
    completed = completed,
    prescribed = prescribed,
    timestamp = timestamp,
)

fun WorkoutExerciseEntity.toDomain(sets: List<WorkoutSetEntity>): WorkoutExercise = WorkoutExercise(
    id = id,
    workoutId = workoutId,
    exerciseId = exerciseId,
    exerciseName = exerciseName,
    order = order,
    prescriptionId = prescriptionId,
    snapshot = ExposureSnapshot(
        repMin = repMin,
        repMax = repMax,
        workSetCount = workSetCount,
        targetRirMin = targetRirMin,
        targetRirMax = targetRirMax,
        loadSemantics = runCatching { LoadSemantics.valueOf(loadSemantics) }.getOrDefault(LoadSemantics.NONE),
        progressionDirection = runCatching { ProgressionDirection.valueOf(progressionDirection) }.getOrDefault(ProgressionDirection.REPS_ONLY),
        incrementKg = incrementKg,
    ),
    restSeconds = restSeconds,
    autoProgressEnabled = autoProgressEnabled,
    note = note,
    sets = sets.sortedBy { it.setIndex }.map { it.toDomain() },
)

fun WorkoutExercise.toEntity() = WorkoutExerciseEntity(
    id = id,
    workoutId = workoutId,
    exerciseId = exerciseId,
    exerciseName = exerciseName,
    order = order,
    prescriptionId = prescriptionId,
    workSetCount = snapshot.workSetCount,
    repMin = snapshot.repMin,
    repMax = snapshot.repMax,
    targetRirMin = snapshot.targetRirMin,
    targetRirMax = snapshot.targetRirMax,
    loadSemantics = snapshot.loadSemantics.name,
    progressionDirection = snapshot.progressionDirection.name,
    incrementKg = snapshot.incrementKg,
    restSeconds = restSeconds,
    autoProgressEnabled = autoProgressEnabled,
    note = note,
)

fun WorkoutEntity.toDomain(exercises: List<WorkoutExerciseEntity>, setsByExercise: Map<String, List<WorkoutSetEntity>>): Workout =
    Workout(
        id = id,
        routineId = routineId,
        routineName = routineName,
        startedAt = startedAt,
        finishedAt = finishedAt,
        status = runCatching { WorkoutStatus.valueOf(status) }.getOrDefault(WorkoutStatus.IN_PROGRESS),
        restDeadlineMillis = restDeadlineMillis,
        note = note,
        exercises = exercises.sortedBy { it.order }
            .map { it.toDomain(setsByExercise[it.id].orEmpty()) },
    )

fun ProgressionRecommendationEntity.toDomain() = ProgressionRecommendation(
    decision = runCatching { ProgressionDecision.valueOf(decision) }.getOrDefault(ProgressionDecision.HOLD),
    currentLoad = currentLoad,
    nextLoad = nextLoad,
    repTarget = repTargetGoal?.let { RepTarget(it, repMin ?: 1, repMax ?: 1) },
    confidence = runCatching { ProgressionConfidence.valueOf(confidence) }.getOrDefault(ProgressionConfidence.LOW),
    reasons = reasonsCsv.split(SEP).mapNotNull { runCatching { ProgressionReason.valueOf(it) }.getOrNull() },
    explanation = ProgressionExplanation(headline, detailsCsv.split("\u001F").filter { it.isNotBlank() }),
    algorithmVersion = algorithmVersion,
)
