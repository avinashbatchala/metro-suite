/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.metro

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import com.metro.ui.MetroDimens
import com.metro.ui.MetroMediaGlyph
import com.metro.ui.MetroMediaGlyphButton
import com.metro.ui.MetroMediaTransportButton
import com.metro.ui.MetroMediaTransportButtonSize
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.music.vivi.playback.PlayerConnection
import kotlinx.coroutines.delay

private const val SkipDragThresholdPx = 80f

@Composable
internal fun NothingPlaying() {
    Column(modifier = Modifier.padding(horizontal = MetroDimens.ScreenHorizontalMargin, vertical = 16.dp)) {
        MetroText(
            text = "Nothing playing",
            style = MetroTextStyle.Body,
            color = MetroTheme.colors.secondaryText,
        )
        Spacer(modifier = Modifier.height(12.dp))
        MetroText(
            text = "Pick a song from collection or get music.",
            style = MetroTextStyle.Body,
            color = MetroTheme.colors.secondaryText,
        )
    }
}

/**
 * WP8.1 now playing pane: track title + `by Artist`, album art (swipe up/down to change track),
 * shuffle/repeat/queue adjacent to the art, scrubber, `Up next`, then transport. Secondary actions
 * (like / download / add to… / radio / share) live in the app-bar ellipsis.
 */
@Composable
internal fun NowPlayingPane(
    playerConnection: PlayerConnection?,
    onOpenQueue: () -> Unit,
) {
    if (playerConnection == null) {
        NothingPlaying()
        return
    }
    val song = playerConnection.mediaMetadata.collectAsState().value
    if (song == null) {
        NothingPlaying()
        return
    }
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val shuffle by playerConnection.shuffleModeEnabled.collectAsState()
    val repeat by playerConnection.repeatMode.collectAsState()
    val playbackState by playerConnection.playbackState.collectAsState()
    val windows by playerConnection.queueWindows.collectAsState()
    val currentIndex by playerConnection.currentWindowIndex.collectAsState()

    val upNext = windows.getOrNull(currentIndex + 1)
        ?.mediaItem
        ?.mediaMetadata
        ?.title
        ?.toString()
    val upNextArtist = windows.getOrNull(currentIndex + 1)
        ?.mediaItem
        ?.mediaMetadata
        ?.artist
        ?.toString()

    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(playerConnection, song.id) {
        while (true) {
            positionMs = runCatching { playerConnection.player.currentPosition }.getOrDefault(0L)
            val duration = runCatching { playerConnection.player.duration }.getOrDefault(0L)
            durationMs = if (duration > 0L) duration else 0L
            delay(500)
        }
    }

    var dragAccum by remember(song.id) { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .padding(horizontal = MetroDimens.ScreenHorizontalMargin)
            .padding(top = 12.dp, bottom = 8.dp),
    ) {
        MetroText(
            text = song.title,
            style = MetroTextStyle.ListItemTitle,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            modifier = Modifier.wrapContentWidth(align = Alignment.Start, unbounded = true),
        )
        MetroText(
            text = "by ${song.artists.joinToString { it.name }}",
            style = MetroTextStyle.ListItemSubtitle,
            color = MetroTheme.colors.secondaryText,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            modifier = Modifier.wrapContentWidth(align = Alignment.Start, unbounded = true),
        )
        MetroText(
            text = "Up next: " + (upNext?.let { title ->
                if (!upNextArtist.isNullOrBlank()) "$title — $upNextArtist" else title
            } ?: "—"),
            style = MetroTextStyle.ListItemSubtitle,
            color = MetroTheme.colors.secondaryText,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            modifier = Modifier.wrapContentWidth(align = Alignment.Start, unbounded = true),
        )
        Spacer(modifier = Modifier.height(12.dp))

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val artSize = maxWidth
            Box(
                modifier = Modifier
                    .size(artSize)
                    .background(MetroTheme.colors.secondarySurface)
                    .pointerInput(song.id) {
                        detectVerticalDragGestures(
                            onDragStart = { dragAccum = 0f },
                            onDragCancel = { dragAccum = 0f },
                            onDragEnd = {
                                val accumulated = dragAccum
                                dragAccum = 0f
                                when {
                                    accumulated < -SkipDragThresholdPx -> playerConnection.seekToNext()
                                    accumulated > SkipDragThresholdPx -> playerConnection.seekToPrevious()
                                }
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                dragAccum += dragAmount
                            },
                        )
                    },
            ) {
                AsyncImage(
                    model = song.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MetroMediaGlyphButton(
                glyph = MetroMediaGlyph.Shuffle,
                onClick = { runCatching { playerConnection.player.shuffleModeEnabled = !shuffle } },
                contentDescription = if (shuffle) "shuffle on" else "shuffle",
                color = if (shuffle) MetroTheme.colors.accent else MetroTheme.colors.secondaryText,
            )
            Spacer(modifier = Modifier.width(8.dp))
            MetroMediaGlyphButton(
                glyph = if (repeat == Player.REPEAT_MODE_ONE) MetroMediaGlyph.RepeatOne else MetroMediaGlyph.Repeat,
                onClick = {
                    runCatching {
                        playerConnection.player.repeatMode = when (repeat) {
                            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                            else -> Player.REPEAT_MODE_OFF
                        }
                    }
                },
                contentDescription = "repeat",
                color = if (repeat != Player.REPEAT_MODE_OFF) MetroTheme.colors.accent else MetroTheme.colors.secondaryText,
            )
            Spacer(modifier = Modifier.weight(1f))
            MetroMediaGlyphButton(
                glyph = MetroMediaGlyph.Queue,
                onClick = onOpenQueue,
                contentDescription = "queue",
            )
        }

        MediaCircleSeekBar(
            positionMs = positionMs,
            durationMs = durationMs,
            onSeek = { playerConnection.seekTo(it) },
            loading = durationMs <= 0L || playbackState == Player.STATE_BUFFERING,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(MetroMediaTransportButtonSize, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MetroMediaTransportButton(
                type = com.metro.ui.MetroSystemIconType.Previous,
                onClick = { playerConnection.seekToPrevious() },
                contentDescription = "previous",
            )
            MetroMediaTransportButton(
                type = if (isPlaying) com.metro.ui.MetroSystemIconType.Pause else com.metro.ui.MetroSystemIconType.Play,
                onClick = { playerConnection.togglePlayPause() },
                contentDescription = if (isPlaying) "pause" else "play",
            )
            MetroMediaTransportButton(
                type = com.metro.ui.MetroSystemIconType.Next,
                onClick = { playerConnection.seekToNext() },
                contentDescription = "next",
            )
        }
    }
}
