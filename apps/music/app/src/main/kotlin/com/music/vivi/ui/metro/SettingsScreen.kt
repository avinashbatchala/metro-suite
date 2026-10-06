/**
 * Music (MetroSuite port of the Vivi Music engine)
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.metro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.metro.system.MetroIntents
import com.metro.ui.MetroListItem
import com.metro.ui.MetroToggleSwitch
import com.metro.ui.MetroTheme
import com.music.vivi.BuildConfig
import com.music.vivi.constants.EnableSponsorBlockKey
import com.music.vivi.constants.SponsorBlockSkipIntroOutroKey
import com.music.vivi.constants.SponsorBlockSkipSelfPromoKey
import com.music.vivi.constants.SponsorBlockSkipSponsorKey
import com.music.vivi.utils.rememberPreference

/**
 * Settings subpage — minimal Metro list of the app's surviving engine features.
 */
@Composable
fun SettingsScreen(
    onOpenDownloads: () -> Unit,
    onOpenRecognition: () -> Unit,
    onOpenEq: () -> Unit,
) {
    val (enableSponsorBlock, setEnableSponsorBlock) =
        rememberPreference(EnableSponsorBlockKey, defaultValue = true)
    val (skipSponsor, setSkipSponsor) =
        rememberPreference(SponsorBlockSkipSponsorKey, defaultValue = true)
    val (skipSelfPromo, setSkipSelfPromo) =
        rememberPreference(SponsorBlockSkipSelfPromoKey, defaultValue = true)
    val (skipIntroOutro, setSkipIntroOutro) =
        rememberPreference(SponsorBlockSkipIntroOutroKey, defaultValue = false)
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background),
    ) {
        MetroPageTitle("settings")
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item(key = "pin") {
                MetroListItem(
                    title = "pin to start",
                    subtitle = "add the now-playing live tile to Start",
                    onClick = {
                        MetroIntents.requestPinTile(context, context.packageName)
                    },
                )
            }
            item(key = "downloads") {
                MetroListItem(
                    title = "downloads",
                    subtitle = "manage downloaded songs",
                    onClick = onOpenDownloads,
                )
            }
            item(key = "recognition") {
                MetroListItem(
                    title = "recognition",
                    subtitle = "identify a song with the microphone",
                    onClick = onOpenRecognition,
                )
            }
            item(key = "equalizer") {
                MetroListItem(
                    title = "equalizer",
                    subtitle = "select an EQ profile",
                    onClick = onOpenEq,
                )
            }
            item(key = "about") {
                MetroListItem(
                    title = "about",
                    subtitle = "music ${BuildConfig.VERSION_NAME}",
                    singleLine = true,
                )
            }
            item(key = "sponsorblock_header") {
                Spacer(modifier = Modifier.height(16.dp))
                MetroListItem(
                    title = "sponsorblock",
                    subtitle = "skip non-music segments",
                    trailing = {
                        MetroToggleSwitch(
                            checked = enableSponsorBlock,
                            onCheckedChange = { setEnableSponsorBlock(it) },
                            showStatus = false,
                        )
                    },
                )
            }
            item(key = "sponsor") {
                MetroListItem(
                    title = "skip sponsor",
                    trailing = {
                        MetroToggleSwitch(
                            checked = skipSponsor,
                            onCheckedChange = { setSkipSponsor(it) },
                            showStatus = false,
                        )
                    },
                )
            }
            item(key = "selfpromo") {
                MetroListItem(
                    title = "skip self promotion",
                    trailing = {
                        MetroToggleSwitch(
                            checked = skipSelfPromo,
                            onCheckedChange = { setSkipSelfPromo(it) },
                            showStatus = false,
                        )
                    },
                )
            }
            item(key = "introoutro") {
                MetroListItem(
                    title = "skip intro / outro",
                    trailing = {
                        MetroToggleSwitch(
                            checked = skipIntroOutro,
                            onCheckedChange = { setSkipIntroOutro(it) },
                            showStatus = false,
                        )
                    },
                )
            }
            item(key = "footer") {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
