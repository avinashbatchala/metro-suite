package com.metro.training.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Resolves the bundled free-exercise-db illustration for an exercise (two frames), or null for
 * custom exercises / missing assets. Cached per (id, frame).
 */
@Composable
fun exerciseImageRes(exerciseId: String, frame: Int): Int? {
    val context = LocalContext.current
    return remember(exerciseId, frame) {
        val name = "training_ex_${exerciseId}_${frame.coerceIn(0, 1)}"
        val id = context.resources.getIdentifier(name, "drawable", context.packageName)
        id.takeIf { it != 0 }
    }
}
