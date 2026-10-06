/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.metro

import android.content.Context
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.metro.ui.MetroContextMenuClearOnDismiss
import com.metro.ui.MetroContextMenuItem
import com.metro.ui.MetroContextMenuPopup
import com.metro.ui.MetroSystemIcon
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.music.vivi.LocalDownloadUtil
import com.music.vivi.db.entities.Song
import com.music.vivi.extensions.toMediaItem
import com.music.vivi.playback.ExoDownloadService
import com.music.vivi.playback.PlayerConnection
import kotlin.math.roundToInt

/**
 * Queue a Media3 download for [id] (id == YouTube videoId == cache key).
 */
internal fun startSongDownload(context: Context, id: String, title: String) {
    val request = DownloadRequest.Builder(id, id.toUri())
        .setCustomCacheKey(id)
        .setData(title.toByteArray())
        .build()
    DownloadService.sendAddDownload(context, ExoDownloadService::class.java, request, false)
}

internal fun startSongDownload(context: Context, song: Song) =
    startSongDownload(context, song.id, song.title)

internal fun removeSongDownload(context: Context, id: String) {
    DownloadService.sendRemoveDownload(context, ExoDownloadService::class.java, id, false)
}

/** Trailing affordance for a song row: percent while in flight, save glyph when on device. */
@Composable
internal fun SongDownloadIndicator(
    download: Download?,
    downloaded: Boolean,
    modifier: Modifier = Modifier,
) {
    val downloading = download?.state == Download.STATE_DOWNLOADING ||
        download?.state == Download.STATE_QUEUED
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when {
            downloading -> {
                val percent = download.percentDownloaded.roundToInt().coerceIn(0, 100)
                MetroText(
                    text = "$percent%",
                    style = MetroTextStyle.ListItemSubtitle,
                    color = MetroTheme.colors.secondaryText,
                )
            }
            downloaded || download?.state == Download.STATE_COMPLETED -> {
                MetroSystemIcon(
                    type = MetroSystemIconType.Save,
                    iconSize = 32.dp,
                    color = MetroTheme.colors.accent,
                    showCircle = false,
                )
            }
        }
    }
}

/**
 * Long-press context menu for a song row. Create with [rememberSongContextMenuState], host the
 * screen content in [SongContextMenuHost], and call [SongContextMenuState.open] from the row's
 * onLongClick with its window bounds.
 */
internal class SongContextMenuState internal constructor() {
    var song by mutableStateOf<Song?>(null)
        private set
    var anchorBounds by mutableStateOf(Rect.Zero)
        private set
    var rootBounds by mutableStateOf(Rect.Zero)
        private set
    val visibility = MutableTransitionState(false)

    internal fun open(target: Song, anchor: Rect) {
        anchorBounds = anchor
        song = target
        visibility.targetState = true
    }

    internal fun dismiss() {
        visibility.targetState = false
    }

    internal fun updateRootBounds(bounds: Rect) {
        rootBounds = bounds
    }

    internal fun clear() {
        song = null
    }
}

@Composable
internal fun rememberSongContextMenuState(): SongContextMenuState =
    remember { SongContextMenuState() }

@Composable
internal fun SongContextMenuHost(
    state: SongContextMenuState,
    playerConnection: PlayerConnection?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val downloadUtil = LocalDownloadUtil.current
    val target = state.song

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { state.updateRootBounds(it.boundsInWindow()) },
    ) {
        content()
    }

    MetroContextMenuClearOnDismiss(state.visibility) { state.clear() }

    if (target != null) {
        val download by remember(target.id) { downloadUtil.getDownload(target.id) }
            .collectAsState(initial = null)
        val downloaded = target.song.isDownloaded || download?.state == Download.STATE_COMPLETED
        MetroContextMenuPopup(
            visibleState = state.visibility,
            anchorBounds = state.anchorBounds,
            rootBounds = state.rootBounds,
            items = listOf(
                MetroContextMenuItem(label = "play next") {
                    playerConnection?.playNext(listOf(target.toMediaItem()))
                    state.dismiss()
                },
                MetroContextMenuItem(label = "add to queue") {
                    playerConnection?.addToQueue(listOf(target.toMediaItem()))
                    state.dismiss()
                },
                MetroContextMenuItem(
                    label = if (downloaded) "remove download" else "download",
                ) {
                    if (downloaded) {
                        removeSongDownload(context, target.id)
                    } else {
                        startSongDownload(context, target)
                    }
                    state.dismiss()
                },
                MetroContextMenuItem(
                    label = if (target.song.liked) "unlike" else "like",
                ) {
                    playerConnection?.toggleLike()
                    state.dismiss()
                },
            ),
            onDismissRequest = { state.dismiss() },
        )
    }
}

/** Song row with download indicator and optional long-press context menu. */
@Composable
internal fun SongListRow(
    song: Song,
    onClick: () -> Unit,
    onLongClick: ((Rect) -> Unit)? = null,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val downloadUtil = LocalDownloadUtil.current
    val download by remember(song.id) { downloadUtil.getDownload(song.id) }
        .collectAsState(initial = null)
    var bounds by remember { mutableStateOf(Rect.Zero) }
    MusicListRow(
        title = song.title,
        subtitle = song.artists.joinToString { it.name }.ifBlank { null },
        onClick = onClick,
        onLongClick = onLongClick?.let { callback -> { callback(bounds) } },
        modifier = Modifier.onGloballyPositioned { bounds = it.boundsInWindow() },
        leading = leading,
        trailing = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                trailing?.invoke()
                SongDownloadIndicator(download = download, downloaded = song.song.isDownloaded)
            }
        },
    )
}
