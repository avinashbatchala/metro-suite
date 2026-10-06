package com.metro.training.data.repositories

import android.content.Context
import com.metro.training.data.database.ProgressionRecommendationEntity
import com.metro.training.data.database.TrainingDao
import com.metro.training.data.database.TrainingDatabase
import com.metro.training.data.database.WorkoutEntity
import com.metro.training.data.database.WorkoutExerciseEntity
import com.metro.training.data.database.WorkoutSetEntity
import com.metro.training.data.database.WorkoutSummaryRow
import com.metro.training.data.toDomain
import com.metro.training.data.toEntity
import com.metro.training.domain.exercises.BuiltInExercises
import com.metro.training.domain.exercises.ExerciseDefinition
import com.metro.training.domain.progression.DoubleProgressionEngine
import com.metro.training.domain.progression.ProgressionRecommendation
import com.metro.training.domain.routines.Routine
import com.metro.training.domain.routines.RoutineExercisePrescription
import com.metro.training.domain.workout.CompletedExerciseExposure
import com.metro.training.domain.workout.ExposureSnapshot
import com.metro.training.domain.workout.SetType
import com.metro.training.domain.workout.Workout
import com.metro.training.domain.workout.WorkoutExercise
import com.metro.training.domain.workout.WorkoutSet
import com.metro.training.domain.workout.WorkoutStatus
import com.metro.training.domain.workout.WorkoutSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.TimeUnit

class TrainingRepository(
    private val context: Context,
    private val dao: TrainingDao = TrainingDatabase.get(context).dao(),
    private val engine: DoubleProgressionEngine = DoubleProgressionEngine(),
) {
    private val io = Dispatchers.IO

    // ---- seeding ---------------------------------------------------------

    suspend fun ensureSeeded() = withContext(io) {
        val existing = dao.allExercisesIncludingArchived().map { it.id }.toSet()
        val missing = BuiltInExercises.all.filterNot { it.id in existing }
        if (missing.isNotEmpty()) {
            dao.upsertExercises(missing.map { it.toEntity() })
        }
    }

    // ---- exercises -------------------------------------------------------

    fun observeExercises(): Flow<List<ExerciseDefinition>> =
        dao.observeExercises().map { list -> list.map { it.toDomain() } }

    suspend fun exercises(): List<ExerciseDefinition> = withContext(io) {
        dao.exercises().map { it.toDomain() }
    }

    suspend fun customExerciseCount(): Int = withContext(io) { dao.customExerciseCount() }

    suspend fun saveExercise(exercise: ExerciseDefinition) = withContext(io) {
        dao.upsertExercise(exercise.toEntity())
    }

    suspend fun archiveExercise(id: String) = withContext(io) {
        dao.archiveExercise(id, System.currentTimeMillis())
    }

    // ---- routines --------------------------------------------------------

    fun observeRoutines(): Flow<List<Routine>> =
        dao.observeRoutines().map { list ->
            // Single query for all routine exercises instead of one query per routine.
            val byRoutine = withContext(io) { dao.allRoutineExercises() }.groupBy { it.routineId }
            list.map { entity -> entity.toDomain(byRoutine[entity.id].orEmpty()) }
        }

    suspend fun routine(id: String): Routine? = withContext(io) {
        dao.routine(id)?.toDomain(dao.routineExercises(id))
    }

    suspend fun saveRoutine(routine: Routine) = withContext(io) {
        val now = System.currentTimeMillis()
        dao.upsertRoutine(
            com.metro.training.data.database.RoutineEntity(
                id = routine.id,
                name = routine.name,
                archived = routine.archived,
                createdAt = routine.createdAt,
                updatedAt = now,
            ),
        )
        dao.deleteRoutineExercises(routine.id)
        dao.upsertRoutineExercises(
            routine.exercises.mapIndexed { index, exercise ->
                exercise.prescription.toEntity(routine.id, index, exercise.supersetTag)
            },
        )
    }

    suspend fun archiveRoutine(id: String) = withContext(io) {
        dao.archiveRoutine(id, System.currentTimeMillis())
    }

    /** Duplicate a routine (new ids, same prescriptions/superset tags). Returns the new id. */
    suspend fun duplicateRoutine(id: String): String? = withContext(io) {
        val source = routine(id) ?: return@withContext null
        val copy = source.copy(
            id = UUID.randomUUID().toString(),
            name = "${source.name} copy".trim(),
            createdAt = System.currentTimeMillis(),
            exercises = source.exercises.map { it.copy(prescription = it.prescription.copy(id = UUID.randomUUID().toString())) },
        )
        saveRoutine(copy)
        copy.id
    }

    // ---- workouts --------------------------------------------------------

    /** One-shot recovery load of the in-progress workout. Not observed during editing. */
    suspend fun activeWorkout(): Workout? = withContext(io) {
        dao.inProgressWorkout()?.let { loadWorkout(it.id) }
    }

    /** History list summaries (aggregated in SQL, limited). */
    fun observeCompletedWorkoutSummaries(limit: Int = HISTORY_LIMIT): Flow<List<WorkoutSummary>> =
        dao.observeCompletedWorkoutSummaries(limit).map { rows -> rows.map { it.toSummary() } }

    suspend fun completedWorkoutSummaries(limit: Int = HISTORY_LIMIT): List<WorkoutSummary> =
        withContext(io) { dao.completedWorkoutSummaries(limit).map { it.toSummary() } }

    suspend fun allCompletedWorkouts(): List<Workout> = withContext(io) {
        dao.completedWorkoutSummaries(100_000).mapNotNull { loadWorkout(it.id) }
    }

    /** Insert imported workouts (JSON backup). Ids are expected to be pre-assigned unique ids. */
    suspend fun importWorkouts(workouts: List<Workout>) = withContext(io) {
        workouts.forEach { workout ->
            dao.upsertWorkout(
                WorkoutEntity(
                    id = workout.id,
                    routineId = null,
                    routineName = workout.routineName,
                    startedAt = workout.startedAt,
                    finishedAt = workout.finishedAt,
                    status = WorkoutStatus.COMPLETED.name,
                    restDeadlineMillis = null,
                    note = workout.note,
                ),
            )
            dao.upsertWorkoutExercises(workout.exercises.map { it.toEntity() })
            workout.exercises.forEach { exercise ->
                exercise.sets.forEach { dao.upsertSet(it.toEntity(exercise.id)) }
            }
        }
    }

    suspend fun workout(id: String): Workout? = withContext(io) { loadWorkout(id) }

    suspend fun startWorkout(routine: Routine): String = withContext(io) {
        val now = System.currentTimeMillis()
        val workoutId = UUID.randomUUID().toString()
        dao.upsertWorkout(
            WorkoutEntity(
                id = workoutId,
                routineId = routine.id,
                routineName = routine.name,
                startedAt = now,
                finishedAt = null,
                status = WorkoutStatus.IN_PROGRESS.name,
                restDeadlineMillis = null,
                note = "",
            ),
        )
        val exercises = exercisesById()
        val workoutExercises = mutableListOf<WorkoutExerciseEntity>()
        val sets = mutableListOf<WorkoutSetEntity>()
        routine.exercises.forEachIndexed { index, routineExercise ->
            val prescription = routineExercise.prescription
            val definition = exercises[prescription.exerciseId] ?: return@forEachIndexed
            val increment = prescription.incrementOverrideKg ?: definition.defaultIncrement
            val workoutExerciseId = UUID.randomUUID().toString()
            workoutExercises += WorkoutExerciseEntity(
                id = workoutExerciseId,
                workoutId = workoutId,
                exerciseId = definition.id,
                exerciseName = definition.name,
                order = index,
                prescriptionId = prescription.id,
                workSetCount = prescription.workSetCount,
                repMin = prescription.repMin,
                repMax = prescription.repMax,
                targetRirMin = prescription.targetRirMin,
                targetRirMax = prescription.targetRirMax,
                loadSemantics = definition.loadSemantics.name,
                progressionDirection = definition.progressionDirection.name,
                incrementKg = increment,
                restSeconds = prescription.restSeconds,
                autoProgressEnabled = prescription.autoProgressEnabled,
                note = prescription.note,
                supersetTag = routineExercise.supersetTag,
            )
            val prefillLoad = lastUsedLoad(definition.id, prescription)
            repeat(prescription.workSetCount) { index2 ->
                sets += WorkoutSetEntity(
                    id = UUID.randomUUID().toString(),
                    workoutExerciseId = workoutExerciseId,
                    setIndex = index2 + 1,
                    setType = SetType.WORK.name,
                    load = prefillLoad,
                    repsCompleted = 0,
                    rir = null,
                    quality = com.metro.training.domain.workout.SetQuality.NORMAL.name,
                    completed = false,
                    prescribed = true,
                    timestamp = now,
                )
            }
        }
        dao.upsertWorkoutExercises(workoutExercises)
        sets.forEach { dao.upsertSet(it) }
        workoutId
    }

    suspend fun addExerciseToWorkout(
        workoutId: String,
        exerciseId: String,
        prescription: RoutineExercisePrescription,
    ) = withContext(io) {
        val definition = dao.exercise(exerciseId)?.toDomain() ?: return@withContext
        val existing = dao.workoutExercises(workoutId)
        val order = existing.size
        val workoutExerciseId = UUID.randomUUID().toString()
        val increment = prescription.incrementOverrideKg ?: definition.defaultIncrement
        val entity = WorkoutExerciseEntity(
            id = workoutExerciseId,
            workoutId = workoutId,
            exerciseId = definition.id,
            exerciseName = definition.name,
            order = order,
            prescriptionId = prescription.id,
            workSetCount = prescription.workSetCount,
            repMin = prescription.repMin,
            repMax = prescription.repMax,
            targetRirMin = prescription.targetRirMin,
            targetRirMax = prescription.targetRirMax,
            loadSemantics = definition.loadSemantics.name,
            progressionDirection = definition.progressionDirection.name,
            incrementKg = increment,
            restSeconds = prescription.restSeconds,
            autoProgressEnabled = prescription.autoProgressEnabled,
            note = prescription.note,
            supersetTag = null,
        )
        dao.upsertWorkoutExercises(listOf(entity))
        val now = System.currentTimeMillis()
        repeat(prescription.workSetCount) { index ->
            dao.upsertSet(
                WorkoutSetEntity(
                    id = UUID.randomUUID().toString(),
                    workoutExerciseId = workoutExerciseId,
                    setIndex = index + 1,
                    setType = SetType.WORK.name,
                    load = null,
                    repsCompleted = 0,
                    rir = null,
                    quality = com.metro.training.domain.workout.SetQuality.NORMAL.name,
                    completed = false,
                    prescribed = true,
                    timestamp = now,
                ),
            )
        }
    }

    suspend fun addExtraSet(workoutExerciseId: String, setType: SetType) = withContext(io) {
        val existing = dao.sets(workoutExerciseId)
        val now = System.currentTimeMillis()
        val previous = existing.maxByOrNull { it.setIndex }
        dao.upsertSet(
            WorkoutSetEntity(
                id = UUID.randomUUID().toString(),
                workoutExerciseId = workoutExerciseId,
                setIndex = (existing.maxOfOrNull { it.setIndex } ?: 0) + 1,
                setType = setType.name,
                // Copy the previous set's values (Hevy-style "add set").
                load = previous?.load,
                repsCompleted = previous?.repsCompleted ?: 0,
                rir = previous?.rir,
                quality = com.metro.training.domain.workout.SetQuality.NORMAL.name,
                completed = false,
                prescribed = false,
                timestamp = now,
            ),
        )
    }

    suspend fun saveSet(set: WorkoutSet) = withContext(io) {
        dao.upsertSet(set.toEntity(set.exerciseSessionId))
    }

    /** Insert a warm-up set with an explicit load (from the warm-up calculator). */
    suspend fun addWarmupSet(workoutExerciseId: String, loadKg: Double, reps: Int) = withContext(io) {
        val existing = dao.sets(workoutExerciseId)
        dao.upsertSet(
            WorkoutSetEntity(
                id = UUID.randomUUID().toString(),
                workoutExerciseId = workoutExerciseId,
                setIndex = (existing.maxOfOrNull { it.setIndex } ?: 0) + 1,
                setType = SetType.WARMUP.name,
                load = loadKg,
                repsCompleted = reps,
                rir = null,
                quality = com.metro.training.domain.workout.SetQuality.NORMAL.name,
                completed = false,
                prescribed = false,
                timestamp = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun deleteSet(id: String) = withContext(io) { dao.deleteSet(id) }

    /** Start a workout with no routine ("start empty workout"). */
    suspend fun startEmptyWorkout(): String = withContext(io) {
        val workoutId = UUID.randomUUID().toString()
        dao.upsertWorkout(
            WorkoutEntity(
                id = workoutId,
                routineId = null,
                routineName = "Workout",
                startedAt = System.currentTimeMillis(),
                finishedAt = null,
                status = WorkoutStatus.IN_PROGRESS.name,
                restDeadlineMillis = null,
                note = "",
            ),
        )
        workoutId
    }

    suspend fun updateWorkoutNote(workoutId: String, note: String) = withContext(io) {
        val workout = dao.workout(workoutId) ?: return@withContext
        dao.upsertWorkout(workout.copy(note = note))
    }

    suspend fun updateWorkoutExerciseNote(workoutExerciseId: String, note: String) = withContext(io) {
        val entity = dao.workoutExercise(workoutExerciseId) ?: return@withContext
        dao.upsertWorkoutExercises(listOf(entity.copy(note = note)))
    }

    suspend fun setSupersetTag(workoutExerciseId: String, tag: String?) = withContext(io) {
        val entity = dao.workoutExercise(workoutExerciseId) ?: return@withContext
        dao.upsertWorkoutExercises(listOf(entity.copy(supersetTag = tag)))
    }

    suspend fun moveWorkoutExercise(workoutExerciseId: String, delta: Int) = withContext(io) {
        val workoutId = dao.workoutExercise(workoutExerciseId)?.workoutId ?: return@withContext
        val ordered = dao.workoutExercises(workoutId).sortedBy { it.order }.toMutableList()
        val index = ordered.indexOfFirst { it.id == workoutExerciseId }
        if (index < 0) return@withContext
        val target = (index + delta).coerceIn(0, ordered.lastIndex)
        if (target == index) return@withContext
        val item = ordered.removeAt(index)
        ordered.add(target, item)
        dao.upsertWorkoutExercises(ordered.mapIndexed { i, e -> e.copy(order = i) })
    }

    suspend fun removeWorkoutExercise(workoutExerciseId: String) = withContext(io) {
        val workoutId = dao.workoutExercise(workoutExerciseId)?.workoutId
        dao.deleteWorkoutExercise(workoutExerciseId)
        if (workoutId != null) {
            val ordered = dao.workoutExercises(workoutId).sortedBy { it.order }
            dao.upsertWorkoutExercises(ordered.mapIndexed { i, e -> e.copy(order = i) })
        }
    }

    /** Replace an exercise in a live workout (smart swap), keeping rep range/work-set count. */
    suspend fun replaceWorkoutExercise(workoutExerciseId: String, newExerciseId: String) = withContext(io) {
        val existing = dao.workoutExercise(workoutExerciseId) ?: return@withContext
        val definition = dao.exercise(newExerciseId)?.toDomain() ?: return@withContext
        dao.sets(workoutExerciseId).forEach { dao.deleteSet(it.id) }
        val replacement = existing.copy(
            exerciseId = definition.id,
            exerciseName = definition.name,
            loadSemantics = definition.loadSemantics.name,
            progressionDirection = definition.progressionDirection.name,
            incrementKg = definition.defaultIncrement,
        )
        dao.upsertWorkoutExercises(listOf(replacement))
        val prescription = com.metro.training.domain.routines.RoutineExercisePrescription(
            id = existing.prescriptionId,
            exerciseId = definition.id,
            workSetCount = existing.workSetCount,
            repMin = existing.repMin,
            repMax = existing.repMax,
            targetRirMin = existing.targetRirMin,
            targetRirMax = existing.targetRirMax,
            restSeconds = existing.restSeconds,
            autoProgressEnabled = existing.autoProgressEnabled,
            incrementOverrideKg = definition.defaultIncrement,
        )
        val prefill = lastUsedLoad(definition.id, prescription)
        val now = System.currentTimeMillis()
        repeat(existing.workSetCount) { index ->
            dao.upsertSet(
                WorkoutSetEntity(
                    id = UUID.randomUUID().toString(),
                    workoutExerciseId = workoutExerciseId,
                    setIndex = index + 1,
                    setType = SetType.WORK.name,
                    load = prefill,
                    repsCompleted = 0,
                    rir = null,
                    quality = com.metro.training.domain.workout.SetQuality.NORMAL.name,
                    completed = false,
                    prescribed = true,
                    timestamp = now,
                ),
            )
        }
    }

    /** Previous comparable work-set values for prefill/ghost column: (load, reps, rir). */
    suspend fun previousWorkSets(
        exerciseId: String,
        repMin: Int,
        repMax: Int,
        workSetCount: Int,
    ): List<Triple<Double?, Int, Int?>> = withContext(io) {
        val exposure = recentExposures(exerciseId, limit = 8).firstOrNull {
            it.snapshot.repMin == repMin &&
                it.snapshot.repMax == repMax &&
                it.snapshot.workSetCount == workSetCount
        } ?: return@withContext emptyList()
        exposure.sets
            .filter { it.setType.feedsProgression && it.prescribed && it.completed }
            .sortedBy { it.setIndex }
            .map { Triple(it.load, it.repsCompleted, it.rir) }
    }

    suspend fun discardWorkout(workoutId: String) = withContext(io) {
        dao.deleteWorkout(workoutId)
    }

    /** Mark the workout complete and persist a recommendation per auto-progress exercise. */
    suspend fun finishWorkout(workoutId: String): List<ProgressionRecommendation> = withContext(io) {
        val workout = loadWorkout(workoutId) ?: return@withContext emptyList()
        val now = System.currentTimeMillis()
        val entity = dao.workout(workoutId) ?: return@withContext emptyList()
        dao.upsertWorkout(entity.copy(status = WorkoutStatus.COMPLETED.name, finishedAt = now, restDeadlineMillis = null))

        val definitions = exercisesById()
        val storedRecommendations = mutableListOf<ProgressionRecommendationEntity>()
        val results = mutableListOf<ProgressionRecommendation>()

        workout.exercises.forEach { exercise ->
            val definition = definitions[exercise.exerciseId] ?: return@forEach
            if (!exercise.autoProgressEnabled) return@forEach

            val exposure = exercise.toExposure(now)
            val history = recentExposures(exercise.exerciseId, limit = 6)
            val prescription = prescriptionFromSnapshot(exercise)
            val recommendation = engine.evaluate(
                exercise = definition,
                prescription = prescription,
                currentExposure = exposure,
                history = history,
                policy = prescription.progressionPolicy,
                nowMillis = now,
            )
            results += recommendation
            storedRecommendations += recommendation.toEntity(
                workoutId = workoutId,
                workoutExerciseId = exercise.id,
                exerciseId = exercise.exerciseId,
            )
        }
        if (storedRecommendations.isNotEmpty()) {
            dao.upsertRecommendations(storedRecommendations)
        }
        results
    }

    fun observeRecommendationsForWorkout(workoutId: String): Flow<List<ProgressionRecommendation>> =
        dao.observeRecommendationsForWorkout(workoutId).map { list -> list.map { it.toDomain() } }

    suspend fun recommendationsForWorkout(workoutId: String): List<ProgressionRecommendation> =
        withContext(io) { dao.recommendationsForWorkout(workoutId).map { it.toDomain() } }

    suspend fun acceptRecommendation(
        workoutId: String,
        workoutExerciseId: String,
        chosenLoad: Double?,
        chosenRepGoal: Int?,
    ) = withContext(io) {
        val entity = dao.recommendationsForWorkout(workoutId)
            .firstOrNull { it.workoutExerciseId == workoutExerciseId } ?: return@withContext
        dao.updateRecommendation(
            entity.copy(accepted = true, chosenLoad = chosenLoad, chosenRepGoal = chosenRepGoal),
        )
    }

    // ---- history ---------------------------------------------------------

    suspend fun recentExposures(exerciseId: String, limit: Int = 6): List<CompletedExerciseExposure> =
        withContext(io) {
            val rows = dao.recentExposuresForExercise(exerciseId, limit)
            rows.mapNotNull { row ->
                val workout = dao.workout(row.workoutId) ?: return@mapNotNull null
                val setEntities = dao.sets(row.id)
                val domainExercise = row.toDomain(setEntities)
                CompletedExerciseExposure(
                    id = row.id,
                    exerciseId = row.exerciseId,
                    prescriptionId = row.prescriptionId,
                    performedAt = workout.startedAt,
                    snapshot = domainExercise.snapshot,
                    sets = domainExercise.sets,
                )
            }
        }

    suspend fun completedExposuresSince(sinceMillis: Long): List<CompletedExerciseExposure> =
        withContext(io) {
            dao.completedExposuresSince(sinceMillis).mapNotNull { row ->
                val workout = dao.workout(row.workoutId) ?: return@mapNotNull null
                val setEntities = dao.sets(row.id)
                val domainExercise = row.toDomain(setEntities)
                CompletedExerciseExposure(
                    id = row.id,
                    exerciseId = row.exerciseId,
                    prescriptionId = row.prescriptionId,
                    performedAt = workout.startedAt,
                    snapshot = domainExercise.snapshot,
                    sets = domainExercise.sets,
                )
            }
        }

    suspend fun exercisesById(): Map<String, ExerciseDefinition> = withContext(io) {
        dao.allExercisesIncludingArchived().associate { it.id to it.toDomain() }
    }

    // ---- internals -------------------------------------------------------

    private suspend fun loadWorkout(id: String): Workout? {
        val entity = dao.workout(id) ?: return null
        val exercises = dao.workoutExercises(id)
        val setsByExercise = exercises.associate { it.id to dao.sets(it.id) }
        return entity.toDomain(exercises, setsByExercise)
    }

    private suspend fun lastUsedLoad(
        exerciseId: String,
        prescription: RoutineExercisePrescription,
    ): Double? {
        val history = recentExposures(exerciseId, limit = 6)
            .filter {
                it.snapshot.repMin == prescription.repMin &&
                    it.snapshot.repMax == prescription.repMax &&
                    it.snapshot.workSetCount == prescription.workSetCount
            }
        val sets = history.firstOrNull()?.workSets?.filter { it.completed }.orEmpty()
        val loads = sets.mapNotNull { it.load }
        if (loads.isEmpty()) return null
        return loads.groupingBy { it }.eachCount().entries.maxByOrNull { it.value }?.key
    }

    private fun prescriptionFromSnapshot(exercise: WorkoutExercise): RoutineExercisePrescription =
        RoutineExercisePrescription(
            id = exercise.prescriptionId,
            exerciseId = exercise.exerciseId,
            workSetCount = exercise.snapshot.workSetCount,
            repMin = exercise.snapshot.repMin,
            repMax = exercise.snapshot.repMax,
            targetRirMin = exercise.snapshot.targetRirMin,
            targetRirMax = exercise.snapshot.targetRirMax,
            restSeconds = exercise.restSeconds,
            autoProgressEnabled = exercise.autoProgressEnabled,
            incrementOverrideKg = exercise.snapshot.incrementKg,
        )

    private fun ProgressionRecommendation.toEntity(
        workoutId: String,
        workoutExerciseId: String,
        exerciseId: String,
    ) = ProgressionRecommendationEntity(
        id = UUID.randomUUID().toString(),
        workoutId = workoutId,
        workoutExerciseId = workoutExerciseId,
        exerciseId = exerciseId,
        decision = decision.name,
        currentLoad = currentLoad,
        nextLoad = nextLoad,
        confidence = confidence.name,
        headline = explanation.headline,
        reasonsCsv = reasons.joinToString(",") { it.name },
        detailsCsv = explanation.details.joinToString("\u001F"),
        repTargetGoal = repTarget?.totalRepsGoal,
        repMin = repTarget?.repMin,
        repMax = repTarget?.repMax,
        algorithmVersion = algorithmVersion,
        generatedAt = System.currentTimeMillis(),
        accepted = false,
        chosenLoad = null,
        chosenRepGoal = null,
    )

    companion object {
        const val HISTORY_LIMIT = 60

        @JvmStatic
        fun newId(): String = UUID.randomUUID().toString()

        fun daysToMillis(days: Int): Long = TimeUnit.DAYS.toMillis(days.toLong())

        private fun WorkoutSummaryRow.toSummary() = WorkoutSummary(
            id = id,
            routineId = routineId,
            routineName = routineName,
            startedAt = startedAt,
            finishedAt = finishedAt,
            exerciseCount = exerciseCount,
            workSetCount = workSetCount,
        )
    }
}
