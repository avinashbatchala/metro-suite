/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.metro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadService
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroEmptyState
import com.metro.ui.MetroSystemIcon
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroClickable
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.db.MusicDatabase
import com.music.vivi.extensions.toMediaItem
import com.music.vivi.playback.DownloadUtil
import com.music.vivi.playback.ExoDownloadService
import com.music.vivi.playback.queues.ListQueue

/**
 * Downloads subpage — lists songs stored on the device and allows per-row removal.
 */
@Composable
fun DownloadsScreen(
    database: MusicDatabase = LocalDatabase.current,
    downloaded: DownloadUtil? = null,
) {
    val songs by remember { database.downloadedSongsByCreateDateAsc() }
        .collectAsState(initial = emptyList())
    val playerConnection = LocalPlayerConnection.current
    val context = LocalContext.current
    val downloadUtil = downloaded ?: com.music.vivi.LocalDownloadUtil.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background),
    ) {
        MetroAppTitle("MUSIC")
        if (songs.isEmpty()) {
            MetroEmptyState("No downloads yet.")
            return@Column
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(songs, key = { it.id }) { song ->
                val download by remember(song.id) {
                    downloadUtil.getDownload(song.id)
                }.collectAsState(initial = null)
                MusicListRow(
                    title = song.title,
                    subtitle = song.artists.joinToString { it.name }.ifBlank { null },
                    onClick = {
                        playerConnection?.playQueue(
                            ListQueue(
                                title = "downloads",
                                items = songs.map { it.toMediaItem() },
                                startIndex = songs.indexOf(song).coerceAtLeast(0),
                            ),
                        )
                    },
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            download?.let {
                                MetroText(
                                    text = it.stateLabel(),
                                    style = MetroTextStyle.ListItemSubtitle,
                                    color = MetroTheme.colors.secondaryText,
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .padding(start = 12.dp)
                                    .metroClickable {
                                        DownloadService.sendRemoveDownload(
                                            context,
                                            ExoDownloadService::class.java,
                                            song.id,
                                            false,
                                        )
                                    },
                            ) {
                                MetroSystemIcon(
                                    type = MetroSystemIconType.Delete,
                                    iconSize = 36.dp,
                                    color = MetroTheme.colors.primaryText,
                                )
                            }
                        }
                    },
                )
            }
        }
    }
}

private fun Download.stateLabel(): String = when (state) {
    Download.STATE_QUEUED -> "queued"
    Download.STATE_DOWNLOADING -> "downloading"
    Download.STATE_STOPPED -> "paused"
    Download.STATE_COMPLETED -> "done"
    Download.STATE_FAILED -> "failed"
    Download.STATE_REMOVING -> "removing"
    else -> ""
}
