package com.metro.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.settings.R
import com.metro.system.MetroSoundRole
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroDimens
import com.metro.ui.MetroListItem
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.MetroToggleSwitch

/** WP8.1 Settings → ringtones + sounds. */
@Composable
fun RingtonesSoundsScreen(
    state: SettingsState,
    modifier: Modifier = Modifier,
) {
    @Suppress("UNUSED_VARIABLE")
    val generation = state.sounds.installedVersion

    SettingsDetailScaffold(
        pageTitle = stringResource(R.string.settings_ringtones_sounds),
        modifier = modifier,
    ) {
        if (!state.sounds.isPackInstalled()) {
            SettingsBodyText(text = stringResource(R.string.settings_sounds_not_installed_body))
            MetroBorderButton(
                text = stringResource(R.string.settings_sounds_install),
                onClick = { state.sounds.installPack() },
                modifier = Modifier.padding(
                    start = MetroDimens.ScreenHorizontalMargin,
                    end = MetroDimens.ScreenHorizontalMargin,
                    bottom = 16.dp,
                ),
            )
        } else {
            MetroText(
                text = stringResource(R.string.settings_sounds_installed),
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.padding(
                    start = MetroDimens.ScreenHorizontalMargin,
                    end = MetroDimens.ScreenHorizontalMargin,
                    bottom = 12.dp,
                ),
            )
        }

        SoundSectionHeader(stringResource(R.string.settings_sounds_section_ringer))
        SoundRow(
            title = stringResource(R.string.settings_sound_ringtone),
            value = state.sounds.selectionLabel(MetroSoundRole.PHONE_RINGTONE),
            onClick = { state.openSoundPicker(MetroSoundRole.PHONE_RINGTONE) },
        )
        MetroToggleSwitch(
            checked = state.sounds.vibrate,
            onCheckedChange = state.sounds::applyVibrate,
            label = stringResource(R.string.settings_sound_vibrate),
            modifier = Modifier.padding(horizontal = MetroDimens.ScreenHorizontalMargin, vertical = 12.dp),
        )

        SoundSectionHeader(stringResource(R.string.settings_sounds_section_notifications))
        SoundRow(
            title = stringResource(R.string.settings_sound_messages),
            value = state.sounds.selectionLabel(MetroSoundRole.MESSAGE),
            onClick = { state.openSoundPicker(MetroSoundRole.MESSAGE) },
        )
        SoundRow(
            title = stringResource(R.string.settings_sound_mail),
            value = state.sounds.selectionLabel(MetroSoundRole.MAIL),
            onClick = { state.openSoundPicker(MetroSoundRole.MAIL) },
        )
        SoundRow(
            title = stringResource(R.string.settings_sound_calendar),
            value = state.sounds.selectionLabel(MetroSoundRole.CALENDAR),
            onClick = { state.openSoundPicker(MetroSoundRole.CALENDAR) },
        )
        SoundRow(
            title = stringResource(R.string.settings_sound_reminders),
            value = state.sounds.selectionLabel(MetroSoundRole.REMINDER),
            onClick = { state.openSoundPicker(MetroSoundRole.REMINDER) },
        )
        SoundRow(
            title = stringResource(R.string.settings_sound_system),
            value = state.sounds.selectionLabel(MetroSoundRole.SYSTEM_NOTIFICATION),
            onClick = { state.openSoundPicker(MetroSoundRole.SYSTEM_NOTIFICATION) },
        )

        SoundSectionHeader(stringResource(R.string.settings_sounds_section_alarms))
        SoundRow(
            title = stringResource(R.string.settings_sound_default_alarm),
            value = state.sounds.selectionLabel(MetroSoundRole.ALARM),
            onClick = { state.openSoundPicker(MetroSoundRole.ALARM) },
        )
        SoundRow(
            title = stringResource(R.string.settings_sound_timer),
            value = state.sounds.selectionLabel(MetroSoundRole.TIMER),
            onClick = { state.openSoundPicker(MetroSoundRole.TIMER) },
        )

        SoundSectionHeader(stringResource(R.string.settings_sounds_section_about))
        SoundRow(
            title = stringResource(R.string.settings_sound_about),
            value = null,
            onClick = state::openSoundAbout,
        )
    }
}

@Composable
internal fun SoundSectionHeader(text: String) {
    MetroText(
        text = text,
        style = MetroTextStyle.SectionHeader,
        color = MetroTheme.colors.accent,
        modifier = Modifier.padding(
            start = MetroDimens.ScreenHorizontalMargin,
            end = MetroDimens.ScreenHorizontalMargin,
            top = 16.dp,
            bottom = 4.dp,
        ),
    )
}

@Composable
private fun SoundRow(
    title: String,
    value: String?,
    onClick: () -> Unit,
) {
    MetroListItem(
        title = title,
        subtitle = value,
        onClick = onClick,
    )
}
