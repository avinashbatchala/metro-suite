/**
 * vivimusic Project (C) 2026
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.metro.ui.MetroDimens
import com.metro.ui.MetroListItem
import com.metro.ui.MetroSystemIcon
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroClickable
import com.music.vivi.ui.screens.equalizer.EQViewModel

/**
 * Equalizer subpage — lists saved EQ profiles and applies/removes them.
 */
@Composable
fun EqScreen(
    viewModel: EQViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background),
    ) {
        MetroPageTitle("equalizer")
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item(key = "disable") {
                MetroListItem(
                    title = "disable",
                    subtitle = if (state.activeProfileId == null) "active" else null,
                    singleLine = true,
                    onClick = { viewModel.selectProfile(null) },
                )
            }
            if (state.profiles.isEmpty()) {
                item(key = "empty") {
                    MetroText(
                        text = "No EQ profiles.",
                        style = MetroTextStyle.ListItemTitle,
                        color = MetroTheme.colors.secondaryText,
                        modifier = Modifier.padding(MetroDimens.ScreenHorizontalMargin),
                    )
                }
            } else {
                items(state.profiles, key = { it.id }) { profile ->
                    MetroListItem(
                        title = profile.name,
                        subtitle = profile.deviceModel.ifBlank { null },
                        singleLine = true,
                        onClick = { viewModel.selectProfile(profile.id) },
                        trailing = {
                            Box(
                                modifier = Modifier
                                    .padding(start = 12.dp)
                                    .metroClickable { viewModel.deleteProfile(profile.id) },
                            ) {
                                MetroSystemIcon(
                                    type = MetroSystemIconType.Delete,
                                    iconSize = 36.dp,
                                    color = MetroTheme.colors.primaryText,
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}
