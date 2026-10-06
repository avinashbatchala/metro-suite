package com.metro.settings.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.metro.settings.R

/** About Metro sounds — attribution / non-authenticity notice. */
@Composable
fun SoundAboutScreen(modifier: Modifier = Modifier) {
    SettingsDetailScaffold(
        pageTitle = stringResource(R.string.settings_sound_about),
        modifier = modifier,
    ) {
        SettingsBodyText(text = stringResource(R.string.settings_sound_about_body))
    }
}
