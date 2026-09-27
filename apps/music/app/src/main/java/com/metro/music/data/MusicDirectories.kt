package com.metro.music.data

import android.content.Context
import android.content.SharedPreferences
import java.io.File

/** A MediaStore folder that contributes local tracks. */
data class MusicDirectory(
    /** Stable id used for exclusion prefs (relative path when available). */
    val id: String,
    val title: String,
    val songCount: Int,
)

object MusicDirectoryLogic {
    /**
     * Prefer MediaStore [RELATIVE_PATH] (no leading slash, trailing slash stripped).
     * Fall back to the parent of [DATA] / absolute path.
     */
    fun directoryId(relativePath: String?, absolutePath: String?): String? {
        val relative = relativePath?.trim().orEmpty()
        if (relative.isNotEmpty()) {
            return relative.trimEnd('/')
        }
        val absolute = absolutePath?.trim().orEmpty()
        if (absolute.isEmpty()) return null
        val parent = File(absolute).parent?.trimEnd('/') ?: return null
        return parent.ifBlank { null }
    }

    fun displayTitle(id: String): String {
        val trimmed = id.trimEnd('/')
        if (trimmed.isEmpty()) return "/"
        // Absolute paths: show the last two segments when possible (e.g. 0/Music).
        if (trimmed.startsWith("/")) {
            val parts = trimmed.split('/').filter { it.isNotEmpty() }
            return when {
                parts.isEmpty() -> trimmed
                parts.size == 1 -> parts.last()
                else -> parts.takeLast(2).joinToString("/")
            }
        }
        return trimmed
    }

    fun aggregate(directoryIds: List<String>): List<MusicDirectory> {
        val counts = linkedMapOf<String, Int>()
        directoryIds.forEach { id ->
            counts[id] = (counts[id] ?: 0) + 1
        }
        return counts.map { (id, count) ->
            MusicDirectory(
                id = id,
                title = displayTitle(id),
                songCount = count,
            )
        }.sortedBy { it.title.lowercase() }
    }

    fun filterExcluded(directoryId: String?, excludedIds: Set<String>): Boolean {
        if (directoryId == null) return true
        return directoryId !in excludedIds
    }
}

/** Persists directories the user has unchecked (excluded from the local library). */
class MusicDirectoryStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun loadExcludedIds(): Set<String> {
        val raw = prefs.getString(KEY_EXCLUDED, null) ?: return emptySet()
        if (raw.isBlank()) return emptySet()
        return raw.split(SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    fun saveExcludedIds(ids: Set<String>) {
        prefs.edit()
            .putString(KEY_EXCLUDED, ids.sorted().joinToString(SEPARATOR))
            .apply()
    }

    companion object {
        private const val PREFS = "music_directories"
        private const val KEY_EXCLUDED = "excluded"
        private const val SEPARATOR = "\u0001"
    }
}
