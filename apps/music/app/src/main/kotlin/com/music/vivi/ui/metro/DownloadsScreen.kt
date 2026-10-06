/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.metro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.metro.ui.MetroEmptyState
import com.metro.ui.MetroTheme
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.db.MusicDatabase
import com.music.vivi.extensions.toMediaItem
import com.music.vivi.playback.DownloadUtil
import com.music.vivi.playback.queues.ListQueue

/**
 * Downloads subpage — lists songs stored on the device; long-press for per-row actions.
 */
@Composable
fun DownloadsScreen(
    database: MusicDatabase = LocalDatabase.current,
    downloaded: DownloadUtil? = null,
) {
    val songs by remember { database.downloadedSongsByCreateDateAsc() }
        .collectAsState(initial = emptyList())
    val playerConnection = LocalPlayerConnection.current
    val songMenu = rememberSongContextMenuState()

    SongContextMenuHost(
        state = songMenu,
        playerConnection = playerConnection,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MetroTheme.colors.background),
        ) {
            MetroPageTitle("downloads")
            if (songs.isEmpty()) {
                MetroEmptyState("No downloads yet.")
                return@Column
            }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(songs, key = { it.id }) { song ->
                    SongListRow(
                        song = song,
                        onClick = {
                            playerConnection?.playQueue(
                                ListQueue(
                                    title = "downloads",
                                    items = songs.map { it.toMediaItem() },
                                    startIndex = songs.indexOf(song).coerceAtLeast(0),
                                ),
                            )
                        },
                        onLongClick = { rect -> songMenu.open(song, rect) },
                    )
                }
            }
        }
    }
}
