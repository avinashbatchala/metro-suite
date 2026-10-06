/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.metro

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroDimens
import com.metro.ui.MetroListItem
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.music.vivi.LocalDatabase
import com.music.vivi.db.entities.RecognitionHistory
import com.music.vivi.recognition.MusicRecognitionService
import com.music.shazamkit.models.RecognitionStatus
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/**
 * Music recognition subpage — records a clip via the Shazam pipeline and lists history.
 */
@Composable
fun RecognitionScreen() {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val scope = rememberCoroutineScope()

    val status by MusicRecognitionService.recognitionStatus.collectAsState()
    val history by remember { database.recognitionHistory() }
        .collectAsState(initial = emptyList())

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            scope.launch { MusicRecognitionService.recognize(context) }
        }
    }

    fun listen() {
        if (MusicRecognitionService.hasRecordPermission(context)) {
            scope.launch { MusicRecognitionService.recognize(context) }
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background),
    ) {
        MetroPageTitle("recognition")
        MetroBorderButton(
            text = "listen",
            onClick = { listen() },
            modifier = Modifier.padding(start = MetroDimens.ScreenHorizontalMargin),
        )
        Spacer(modifier = Modifier.height(12.dp))
        MetroText(
            text = status.describe(),
            style = MetroTextStyle.Body,
            color = MetroTheme.colors.secondaryText,
            modifier = Modifier.padding(horizontal = MetroDimens.ScreenHorizontalMargin),
        )
        Spacer(modifier = Modifier.height(12.dp))
        when (val current = status) {
            is RecognitionStatus.Success -> {
                val result = current.result
                MetroText(
                    text = result.title,
                    style = MetroTextStyle.ListItemTitle,
                    modifier = Modifier.padding(horizontal = MetroDimens.ScreenHorizontalMargin),
                )
                MetroText(
                    text = result.artist,
                    style = MetroTextStyle.ListItemSubtitle,
                    color = MetroTheme.colors.secondaryText,
                    modifier = Modifier.padding(horizontal = MetroDimens.ScreenHorizontalMargin),
                )
                LaunchedEffect(result.trackId) {
                    database.query {
                        insert(
                            RecognitionHistory(
                                trackId = result.trackId,
                                title = result.title,
                                artist = result.artist,
                                album = result.album,
                                coverArtUrl = result.coverArtUrl,
                                coverArtHqUrl = result.coverArtHqUrl,
                                genre = result.genre,
                                releaseDate = result.releaseDate,
                                label = result.label,
                                shazamUrl = result.shazamUrl,
                                appleMusicUrl = result.appleMusicUrl,
                                spotifyUrl = result.spotifyUrl,
                                isrc = result.isrc,
                                youtubeVideoId = result.youtubeVideoId,
                                recognizedAt = LocalDateTime.now(),
                            )
                        )
                    }
                }
            }
            else -> Unit
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(history, key = { it.id }) { entry ->
                MetroListItem(
                    title = entry.title,
                    subtitle = entry.artist,
                    singleLine = true,
                )
            }
        }
    }
}

private fun RecognitionStatus.describe(): String = when (this) {
    is RecognitionStatus.Ready -> "Tap listen to identify a song."
    is RecognitionStatus.Listening -> "Listening…"
    is RecognitionStatus.Processing -> "Processing…"
    is RecognitionStatus.Success -> "Match found."
    is RecognitionStatus.NoMatch -> message
    is RecognitionStatus.Error -> message
}
