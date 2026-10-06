/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.metro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.metro.ui.MetroAppPickerDefaults
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroEmptyState
import com.metro.ui.MetroListItem
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.music.vivi.playback.PlayerConnection

@Composable
internal fun QueueScreen(playerConnection: PlayerConnection?) {
    if (playerConnection == null) {
        MetroEmptyState("Queue is empty.")
        return
    }
    val windows by playerConnection.queueWindows.collectAsState()
    val currentIndex by playerConnection.currentWindowIndex.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MetroTheme.colors.secondarySurface),
    ) {
        MetroAppTitle("queue")
        if (windows.isEmpty()) {
            MetroEmptyState("Queue is empty.")
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(windows, key = { index, _ -> index }) { index, window ->
                    val selected = index == currentIndex
                    MetroListItem(
                        title = window.mediaItem.mediaMetadata.title?.toString().orEmpty(),
                        titleStyle = MetroTextStyle.ListItemTitle,
                        singleLine = true,
                        oneLineMinHeight = MetroAppPickerDefaults.RowMinHeight,
                        verticalPadding = MetroAppPickerDefaults.RowVerticalPadding,
                        onClick = {
                            runCatching {
                                playerConnection.player.seekToDefaultPosition(index)
                                playerConnection.player.playWhenReady = true
                            }
                        },
                        titleColor = if (selected) {
                            MetroTheme.colors.accent
                        } else {
                            MetroTheme.colors.primaryText
                        },
                    )
                }
            }
        }
    }
}
