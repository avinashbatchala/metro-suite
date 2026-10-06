package com.metro.training.tiles

import android.content.Context
import com.metro.system.MetroPreferences
import com.metro.system.MetroTileData
import com.metro.training.data.database.TrainingDatabase

/**
 * Exports the Training Live Tile. Updates on semantic events only (workout started/finished);
 * it never ticks per second.
 */
class TrainingTileDataSource(private val context: Context) {

    suspend fun buildTileData(tileId: String): MetroTileData {
        val dao = TrainingDatabase.get(context).dao()
        val accent = runCatching { MetroPreferences(context).accentColorHex }
            .getOrDefault("#0078D7")

        val active = dao.inProgressWorkout()
        val backFace = if (active != null) {
            val exercises = dao.workoutExercises(active.id)
            val current = exercises.firstOrNull { exercise ->
                dao.sets(exercise.id).none { it.completed }
            }?.exerciseName ?: exercises.lastOrNull()?.exerciseName
            if (current != null) "${active.routineName}\n$current" else "${active.routineName}\nin progress"
        } else {
            val next = dao.firstRoutine()
            if (next != null) "next\n${next.name}" else "open to train"
        }

        return MetroTileData(
            title = "Training",
            backgroundColorHex = accent,
            backFaceTitle = backFace,
        )
    }
}
