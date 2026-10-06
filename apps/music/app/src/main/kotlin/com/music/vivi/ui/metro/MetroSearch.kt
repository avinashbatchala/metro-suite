/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.metro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.metro.ui.MetroDimens
import com.metro.ui.MetroTextBox
import com.metro.ui.MetroTheme
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import com.music.vivi.db.entities.Song
import com.music.vivi.extensions.toMediaItem
import com.music.vivi.models.toMediaMetadata
import com.music.vivi.playback.PlayerConnection
import com.music.vivi.playback.queues.ListQueue
import com.music.vivi.playback.queues.YouTubeQueue
import com.music.vivi.viewmodels.LocalFilter
import com.music.vivi.viewmodels.LocalSearchViewModel
import kotlinx.coroutines.delay

@Composable
internal fun SearchScreen(playerConnection: PlayerConnection?) {
    val localViewModel: LocalSearchViewModel = hiltViewModel()
    var query by remember { mutableStateOf("") }
    val localResult by localViewModel.result.collectAsState()
    var online by remember { mutableStateOf<List<SongItem>>(emptyList()) }

    LaunchedEffect(query) {
        localViewModel.query.value = query
        if (query.isBlank()) {
            online = emptyList()
            return@LaunchedEffect
        }
        delay(400)
        online = YouTube
            .searchSummary(query)
            .getOrNull()
            ?.summaries
            ?.flatMap { it.items }
            ?.filterIsInstance<SongItem>()
            ?: emptyList()
    }

    val localSongs = localResult.map[LocalFilter.SONG].orEmpty().filterIsInstance<Song>()
    val songMenu = rememberSongContextMenuState()

    SongContextMenuHost(state = songMenu, playerConnection = playerConnection) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MetroTheme.colors.background),
        ) {
            MetroPageTitle("search")
            MetroTextBox(
                value = query,
                onValueChange = { query = it },
                placeholder = "search",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MetroDimens.ScreenHorizontalMargin, vertical = 8.dp),
            )
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(localSongs, key = { "local-${it.id}" }) { song ->
                    SongListRow(
                        song = song,
                        onClick = {
                            playerConnection?.playQueue(
                                ListQueue(
                                    title = "search",
                                    items = localSongs.map { it.toMediaItem() },
                                    startIndex = localSongs.indexOf(song).coerceAtLeast(0),
                                ),
                            )
                        },
                        onLongClick = { rect -> songMenu.open(song, rect) },
                    )
                }
                items(online, key = { "online-${it.id}" }) { item ->
                    MusicListRow(
                        title = item.title,
                        subtitle = item.artists.joinToString { it.name },
                        onClick = {
                            playerConnection?.playQueue(
                                YouTubeQueue(
                                    WatchEndpoint(videoId = item.id),
                                    item.toMediaMetadata(),
                                ),
                            )
                        },
                    )
                }
            }
        }
    }
}
