package com.metro.training.data.backup

import com.metro.training.domain.exercises.LoadSemantics
import com.metro.training.domain.exercises.ProgressionDirection
import com.metro.training.domain.workout.ExposureSnapshot
import com.metro.training.domain.workout.SetQuality
import com.metro.training.domain.workout.SetType
import com.metro.training.domain.workout.Workout
import com.metro.training.domain.workout.WorkoutExercise
import com.metro.training.domain.workout.WorkoutSet
import com.metro.training.domain.workout.WorkoutStatus
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Workout backup codec: JSON (lossless, round-trip) and a flat CSV export. No cloud, no account.
 */
object TrainingBackup {

    fun exportJson(workouts: List<Workout>): String {
        val array = JSONArray()
        workouts.forEach { workout ->
            array.put(
                JSONObject().apply {
                    put("routineName", workout.routineName)
                    put("startedAt", workout.startedAt)
                    put("finishedAt", workout.finishedAt ?: JSONObject.NULL)
                    put("note", workout.note)
                    put("exercises", JSONArray().apply {
                        workout.exercises.forEach { exercise ->
                            put(
                                JSONObject().apply {
                                    put("exerciseId", exercise.exerciseId)
                                    put("exerciseName", exercise.exerciseName)
                                    put("repMin", exercise.snapshot.repMin)
                                    put("repMax", exercise.snapshot.repMax)
                                    put("workSetCount", exercise.snapshot.workSetCount)
                                    put("targetRirMin", exercise.snapshot.targetRirMin ?: JSONObject.NULL)
                                    put("targetRirMax", exercise.snapshot.targetRirMax ?: JSONObject.NULL)
                                    put("loadSemantics", exercise.snapshot.loadSemantics.name)
                                    put("progressionDirection", exercise.snapshot.progressionDirection.name)
                                    put("incrementKg", exercise.snapshot.incrementKg ?: JSONObject.NULL)
                                    put("restSeconds", exercise.restSeconds ?: JSONObject.NULL)
                                    put("note", exercise.note)
                                    put("sets", JSONArray().apply {
                                        exercise.sets.forEach { set -> put(set.toJson()) }
                                    })
                                },
                            )
                        }
                    })
                },
            )
        }
        return array.toString(2)
    }

    fun parseJson(text: String, idFactory: () -> String = { UUID.randomUUID().toString() }): List<Workout> {
        val array = JSONArray(text)
        val workouts = mutableListOf<Workout>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val workoutId = idFactory()
            val exercises = mutableListOf<WorkoutExercise>()
            val exerciseArray = obj.optJSONArray("exercises") ?: JSONArray()
            for (j in 0 until exerciseArray.length()) {
                val we = exerciseArray.optJSONObject(j) ?: continue
                val weId = idFactory()
                val setArray = we.optJSONArray("sets") ?: JSONArray()
                val sets = mutableListOf<WorkoutSet>()
                for (k in 0 until setArray.length()) {
                    val so = setArray.optJSONObject(k) ?: continue
                    sets.add(so.toSet(weId))
                }
                exercises.add(
                    WorkoutExercise(
                        id = weId,
                        workoutId = workoutId,
                        exerciseId = we.optString("exerciseId"),
                        exerciseName = we.optString("exerciseName"),
                        order = j,
                        prescriptionId = idFactory(),
                        snapshot = ExposureSnapshot(
                            repMin = we.optInt("repMin"),
                            repMax = we.optInt("repMax"),
                            workSetCount = we.optInt("workSetCount"),
                            targetRirMin = we.optIntOrNull("targetRirMin"),
                            targetRirMax = we.optIntOrNull("targetRirMax"),
                            loadSemantics = runCatching { LoadSemantics.valueOf(we.optString("loadSemantics")) }.getOrDefault(LoadSemantics.NONE),
                            progressionDirection = runCatching { ProgressionDirection.valueOf(we.optString("progressionDirection")) }.getOrDefault(ProgressionDirection.REPS_ONLY),
                            incrementKg = we.optDoubleOrNull("incrementKg"),
                        ),
                        restSeconds = we.optIntOrNull("restSeconds"),
                        autoProgressEnabled = false,
                        note = we.optString("note"),
                        supersetTag = null,
                        sets = sets,
                    ),
                )
            }
            val started = obj.optLong("startedAt")
            workouts.add(
                Workout(
                    id = workoutId,
                    routineId = null,
                    routineName = obj.optString("routineName").ifBlank { "Workout" },
                    startedAt = started,
                    finishedAt = obj.optLongOrNull("finishedAt") ?: started,
                    status = WorkoutStatus.COMPLETED,
                    restDeadlineMillis = null,
                    note = obj.optString("note"),
                    exercises = exercises,
                ),
            )
        }
        return workouts
    }

    /** Flat CSV, Hevy-compatible column names. */
    fun exportCsv(workouts: List<Workout>): String {
        val sb = StringBuilder()
        sb.append("title,start_time,end_time,exercise_title,set_index,set_type,weight_kg,reps,rir,notes\n")
        workouts.forEach { workout ->
            workout.exercises.forEach { exercise ->
                exercise.sets.forEach { set ->
                    sb.append(csv(workout.routineName)).append(',')
                    sb.append(workout.startedAt).append(',')
                    sb.append(workout.finishedAt ?: "").append(',')
                    sb.append(csv(exercise.exerciseName)).append(',')
                    sb.append(set.setIndex).append(',')
                    sb.append(set.setType.name.lowercase()).append(',')
                    sb.append(set.load?.toString() ?: "").append(',')
                    sb.append(set.repsCompleted).append(',')
                    sb.append(set.rir?.toString() ?: "").append(',')
                    sb.append(csv(set.note)).append('\n')
                }
            }
        }
        return sb.toString()
    }

    private fun csv(value: String): String =
        if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }

    private fun WorkoutSet.toJson() = JSONObject().apply {
        put("setIndex", setIndex)
        put("setType", setType.name)
        put("load", load ?: JSONObject.NULL)
        put("repsCompleted", repsCompleted)
        put("rir", rir ?: JSONObject.NULL)
        put("quality", quality.name)
        put("completed", completed)
        put("prescribed", prescribed)
        put("repsLeft", repsLeft ?: JSONObject.NULL)
        put("repsRight", repsRight ?: JSONObject.NULL)
        put("partialReps", partialReps ?: JSONObject.NULL)
        put("note", note)
        put("timestamp", timestamp)
    }

    private fun JSONObject.toSet(workoutExerciseId: String) = WorkoutSet(
        id = UUID.randomUUID().toString(),
        exerciseSessionId = workoutExerciseId,
        setIndex = optInt("setIndex"),
        setType = runCatching { SetType.valueOf(optString("setType")) }.getOrDefault(SetType.WORK),
        load = optDoubleOrNull("load"),
        repsCompleted = optInt("repsCompleted"),
        rir = optIntOrNull("rir"),
        quality = runCatching { SetQuality.valueOf(optString("quality")) }.getOrDefault(SetQuality.NORMAL),
        completed = optBoolean("completed"),
        prescribed = optBoolean("prescribed", true),
        repsLeft = optIntOrNull("repsLeft"),
        repsRight = optIntOrNull("repsRight"),
        partialReps = optIntOrNull("partialReps"),
        note = optString("note"),
        timestamp = optLong("timestamp"),
    )

    private fun JSONObject.optIntOrNull(key: String): Int? =
        if (isNull(key)) null else optInt(key)

    private fun JSONObject.optDoubleOrNull(key: String): Double? =
        if (isNull(key)) null else optDouble(key)

    private fun JSONObject.optLongOrNull(key: String): Long? =
        if (isNull(key)) null else optLong(key)
}
