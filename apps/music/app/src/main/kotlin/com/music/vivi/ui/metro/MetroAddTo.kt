/**
 * Music (MetroSuite port of the Vivi Music engine)
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.metro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroColors
import com.metro.ui.MetroDimens
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroClickable
import com.metro.ui.metroNavBarPadding
import com.music.vivi.db.MusicDatabase
import com.music.vivi.db.entities.PlaylistEntity
import java.time.LocalDateTime

/**
 * What can be added via the WP8.1 `add to…` chooser. [persist] ensures the underlying
 * song/album/artist rows exist before membership/playlist writes (needed for online-only items).
 */
internal class AddToTarget(
    val title: String,
    val songIds: List<String>,
    val persist: (MusicDatabase) -> Unit = {},
)

internal class AddToState {
    var target by mutableStateOf<AddToTarget?>(null)
        private set

    fun open(value: AddToTarget) {
        target = value
    }

    fun dismiss() {
        target = null
    }
}

@Composable
internal fun rememberAddToState(): AddToState = remember { AddToState() }

/**
 * WP8.1 `add to…` chooser: `collection`, every editable playlist, and `new playlist`.
 * Rendered as a flat Metro overlay (no Material dialog).
 */
@Composable
internal fun AddToHost(
    state: AddToState,
    database: MusicDatabase,
) {
    val target = state.target ?: return
    val playlists by remember { database.editablePlaylistsByNameAsc() }
        .collectAsState(initial = emptyList())

    fun addToCollection() {
        target.persist(database)
        database.query {
            val now = LocalDateTime.now()
            target.songIds.forEach { inLibrary(it, now) }
        }
        state.dismiss()
    }

    fun addToPlaylist(playlist: com.music.vivi.db.entities.Playlist) {
        target.persist(database)
        database.query { addSongToPlaylist(playlist, target.songIds) }
        state.dismiss()
    }

    fun createAndAdd() {
        target.persist(database)
        val entity = PlaylistEntity(name = "new playlist", isLocal = true)
        database.query {
            insert(entity)
            addSongToPlaylist(
                com.music.vivi.db.entities.Playlist(
                    playlist = entity,
                    songCount = 0,
                    songThumbnails = emptyList(),
                ),
                target.songIds,
            )
        }
        state.dismiss()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .metroClickable { state.dismiss() },
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MetroColors.DarkSecondarySurface)
                .metroNavBarPadding()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
        ) {
            MetroText(
                text = "add to",
                style = MetroTextStyle.SectionHeader,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.padding(
                    start = MetroDimens.ScreenHorizontalMargin,
                    top = 4.dp,
                    bottom = 4.dp,
                ),
            )
            AddToRow(label = "collection", onClick = { addToCollection() })
            playlists.forEach { playlist ->
                AddToRow(label = playlist.title, onClick = { addToPlaylist(playlist) })
            }
            AddToRow(label = "new playlist", onClick = { createAndAdd() })
        }
    }
}

@Composable
private fun AddToRow(label: String, onClick: () -> Unit) {
    MetroText(
        text = label,
        style = MetroTextStyle.ListItemTitle,
        color = MetroTheme.colors.primaryText,
        modifier = Modifier
            .fillMaxWidth()
            .metroClickable(onClick = onClick)
            .padding(
                start = MetroDimens.ScreenHorizontalMargin,
                end = MetroDimens.ScreenHorizontalMargin,
                top = 14.dp,
                bottom = 14.dp,
            ),
    )
}
