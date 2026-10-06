package com.metro.training.data.backup

import com.metro.training.domain.exercises.ExerciseDefinition
import com.metro.training.domain.exercises.LoadSemantics
import com.metro.training.domain.exercises.ProgressionDirection
import com.metro.training.domain.workout.ExposureSnapshot
import com.metro.training.domain.workout.SetQuality
import com.metro.training.domain.workout.SetType
import com.metro.training.domain.workout.Workout
import com.metro.training.domain.workout.WorkoutExercise
import com.metro.training.domain.workout.WorkoutSet
import com.metro.training.domain.workout.WorkoutStatus
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Best-effort Hevy CSV import. Matches rows to built-in exercises by name and rebuilds workouts
 * grouped by (workout title, start time, exercise). Unmatched exercises are skipped.
 */
object HevyCsvImport {

    fun parse(text: String, exercises: List<ExerciseDefinition>): List<Workout> {
        val lines = text.lineSequence().filter { it.isNotBlank() }.toList()
        if (lines.size < 2) return emptyList()
        val header = splitCsv(lines.first()).mapIndexed { i, name -> name.trim().lowercase() to i }.toMap()

        fun col(row: List<String>, vararg names: String): String? {
            names.forEach { n ->
                header[n]?.let { idx -> row.getOrNull(idx)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it } }
            }
            return null
        }

        val byName = exercises.associateBy { it.name.lowercase() }
        data class Key(val title: String, val start: String)
        data class ExKey(val workout: Key, val exercise: String)
        val workouts = LinkedHashMap<Key, MutableList<Pair<String, MutableList<WorkoutSet>>>>()
        val fresh = LinkedHashMap<ExKey, MutableList<WorkoutSet>>()

        lines.drop(1).forEach { line ->
            val row = splitCsv(line)
            if (row.size < 3) return@forEach
            val title = col(row, "title", "workout_title") ?: "Workout"
            val start = col(row, "start_time", "date") ?: ""
            val exerciseName = col(row, "exercise_title", "exercise_name") ?: return@forEach
            val definition = byName[exerciseName.lowercase()] ?: return@forEach
            val setType = when (col(row, "set_type")?.lowercase()) {
                "warmup", "warm_up", "warm-up" -> SetType.WARMUP
                "failure" -> SetType.FAILURE
                "dropset", "drop", "drop_set" -> SetType.DROP
                "backoff" -> SetType.BACKOFF
                "myo", "myorep", "myo_rep" -> SetType.MYOREP
                else -> SetType.WORK
            }
            val load = col(row, "weight_kg")?.toDoubleOrNull()
            val reps = col(row, "reps")?.toIntOrNull() ?: 0
            val rir = col(row, "rir")?.toIntOrNull()
                ?: col(row, "rpe")?.toDoubleOrNull()?.let { (10.0 - it).toInt().coerceAtLeast(0) }
            val set = WorkoutSet(
                id = UUID.randomUUID().toString(),
                exerciseSessionId = UUID.randomUUID().toString(),
                setIndex = col(row, "set_index")?.toIntOrNull() ?: 1,
                setType = setType,
                load = load,
                repsCompleted = reps,
                rir = rir,
                quality = SetQuality.NORMAL,
                completed = true,
                prescribed = setType == SetType.WORK,
                note = col(row, "set_notes", "notes") ?: "",
            )
            val key = Key(title, start)
            val exKey = ExKey(key, definition.id)
            fresh.getOrPut(exKey) { mutableListOf() }
            // attach the set to a fresh exercise-session id per exercise
            val sessionId = fresh[exKey]!!.firstOrNull()?.exerciseSessionId ?: set.exerciseSessionId
            fresh[exKey]!!.add(set.copy(exerciseSessionId = sessionId))
        }

        // Build workouts from the per-exercise groups.
        val grouped = LinkedHashMap<Key, MutableList<ExKey>>()
        fresh.keys.forEach { exKey -> grouped.getOrPut(exKey.workout) { mutableListOf() }.add(exKey) }

        return grouped.map { (key, exKeys) ->
            val workoutId = UUID.randomUUID().toString()
            val started = parseTimestamp(key.start)
            val exercisesForWorkout = exKeys.mapIndexed { index, exKey ->
                val definition = byName[exKey.exercise.lowercase()] ?: exercises.first()
                val sets = fresh.getValue(exKey).sortedBy { it.setIndex }
                WorkoutExercise(
                    id = UUID.randomUUID().toString(),
                    workoutId = workoutId,
                    exerciseId = definition.id,
                    exerciseName = definition.name,
                    order = index,
                    prescriptionId = UUID.randomUUID().toString(),
                    snapshot = ExposureSnapshot(
                        repMin = sets.minOfOrNull { it.repsCompleted } ?: 1,
                        repMax = sets.maxOfOrNull { it.repsCompleted } ?: 1,
                        workSetCount = sets.count { it.setType == SetType.WORK },
                        targetRirMin = null,
                        targetRirMax = null,
                        loadSemantics = definition.loadSemantics,
                        progressionDirection = definition.progressionDirection,
                        incrementKg = definition.defaultIncrement,
                    ),
                    restSeconds = null,
                    autoProgressEnabled = false,
                    note = "",
                    supersetTag = null,
                    sets = sets,
                )
            }
            Workout(
                id = workoutId,
                routineId = null,
                routineName = key.title,
                startedAt = started,
                finishedAt = started,
                status = WorkoutStatus.COMPLETED,
                restDeadlineMillis = null,
                note = "imported",
                exercises = exercisesForWorkout,
            )
        }
    }

    private fun parseTimestamp(value: String): Long {
        if (value.isBlank()) return System.currentTimeMillis()
        return runCatching { Instant.parse(value).toEpochMilli() }
            .recoverCatching { OffsetDateTime.parse(value).toInstant().toEpochMilli() }
            .getOrDefault(System.currentTimeMillis())
    }

    /** Minimal CSV splitter supporting quoted fields. */
    private fun splitCsv(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var quoted = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (quoted && i + 1 < line.length && line[i + 1] == '"') {
                        sb.append('"'); i++
                    } else {
                        quoted = !quoted
                    }
                }
                c == ',' && !quoted -> {
                    result.add(sb.toString()); sb.clear()
                }
                else -> sb.append(c)
            }
            i++
        }
        result.add(sb.toString())
        return result
    }
}
