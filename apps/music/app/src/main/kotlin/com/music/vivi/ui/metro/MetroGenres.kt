/**
 * Music (MetroSuite port of the Vivi Music engine)
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.metro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroDimens
import com.metro.ui.MetroEmptyState
import com.metro.ui.MetroPageHeader
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroClickable
import com.music.vivi.viewmodels.MoodAndGenresViewModel

/**
 * Collection → genres. WP8.1 exposed genres in the Collection; this port lists the catalogue's
 * moods & genres (each opens a station) — a period-compatible extension over the streaming backend.
 */
@Composable
internal fun GenresScreen(
    onBack: () -> Unit,
    onOpenStation: (browseId: String, title: String) -> Unit,
    onSearch: (() -> Unit)?,
) {
    val viewModel: MoodAndGenresViewModel = hiltViewModel()
    val groups by viewModel.moodAndGenres.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(MetroTheme.colors.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            MetroText(
                text = "\u2039",
                style = MetroTextStyle.ListItemTitle,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier
                    .metroClickable(onClick = onBack)
                    .padding(start = MetroDimens.ScreenHorizontalMargin, top = 8.dp),
            )
            MetroPageHeader(title = "genres")

            val current = groups
            if (current == null) {
                MetroEmptyState("Loading genres.")
            } else if (current.isEmpty()) {
                MetroEmptyState("No genres available.")
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    current.forEach { group ->
                        item(key = "g:${group.title}") {
                            MetroText(
                                text = group.title,
                                style = MetroTextStyle.SectionHeader,
                                color = MetroTheme.colors.accent,
                                modifier = Modifier.padding(
                                    start = MetroDimens.ScreenHorizontalMargin,
                                    top = 12.dp,
                                    bottom = 4.dp,
                                ),
                            )
                        }
                        items(group.items, key = { "gi:${it.title}" }) { genre ->
                            MusicListRow(
                                title = genre.title,
                                onClick = { onOpenStation(genre.endpoint.browseId, genre.title) },
                            )
                        }
                    }
                }
            }
        }

        if (onSearch != null) {
            MetroAppBar(
                icons = listOf(
                    MetroAppBarIcon(type = MetroSystemIconType.Search, label = "search", onClick = onSearch),
                ),
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
