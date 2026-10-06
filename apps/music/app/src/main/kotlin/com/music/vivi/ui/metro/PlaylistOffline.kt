/**
 * Music (MetroSuite port of the Vivi Music engine)
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.metro

import android.content.Context

/**
 * Per-playlist "keep playlist offline" preference. Stored app-locally (not in the Room schema) so
 * no database migration is required; the Media3 download engine does the actual work.
 */
internal object PlaylistOffline {
    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences("metro_music_playlists", Context.MODE_PRIVATE)

    fun get(context: Context, playlistId: String): Boolean =
        prefs(context).getBoolean("offline_$playlistId", false)

    fun set(context: Context, playlistId: String, value: Boolean) {
        prefs(context).edit().putBoolean("offline_$playlistId", value).apply()
    }
}
