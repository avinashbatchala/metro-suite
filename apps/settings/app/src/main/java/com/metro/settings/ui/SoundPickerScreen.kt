package com.metro.settings.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.settings.R
import com.metro.settings.data.sounds.MetroRingtoneDefaults
import com.metro.settings.data.sounds.MetroSoundAsset
import com.metro.settings.data.sounds.MetroSoundPreview
import com.metro.settings.data.sounds.MetroSoundsController
import com.metro.system.MetroSoundRole
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroCheckBox
import com.metro.ui.MetroDimens
import com.metro.ui.MetroListItem
import com.metro.ui.MetroMessageDialog
import com.metro.ui.MetroSettingsHeader
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

/**
 * Sound picker for one semantic role. Tapping a sound selects and previews it; the current selection
 * shows a Metro checkmark. System-linked roles may require Modify System Settings.
 */
@Composable
fun SoundPickerScreen(
    state: SettingsState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val role = state.soundPickerRole
    val sounds = state.sounds
    var showWriteDialog by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose { MetroSoundPreview.stop() }
    }

    val roleTitle = pickerTitle(role)
    val systemLinked = MetroRingtoneDefaults.systemTypeFor(role) != null

    LazyColumn(modifier = modifier) {
        item { MetroSettingsHeader(pageTitle = roleTitle) }

        if (!sounds.isPackInstalled()) {
            item {
                MetroText(
                    text = stringResource(R.string.settings_sounds_not_installed_body),
                    style = MetroTextStyle.Body,
                    color = MetroTheme.colors.secondaryText,
                    modifier = Modifier.padding(
                        start = MetroDimens.ScreenHorizontalMargin,
                        end = MetroDimens.ScreenHorizontalMargin,
                        bottom = 8.dp,
                    ),
                )
            }
            item {
                MetroBorderButton(
                    text = stringResource(R.string.settings_sounds_install),
                    onClick = { sounds.installPack() },
                    modifier = Modifier.padding(
                        start = MetroDimens.ScreenHorizontalMargin,
                        end = MetroDimens.ScreenHorizontalMargin,
                        bottom = 12.dp,
                    ),
                )
            }
        }

        if (systemLinked && !state.hasWriteSettings()) {
            item {
                MetroText(
                    text = stringResource(R.string.settings_sounds_write_settings_help),
                    style = MetroTextStyle.ListItemSubtitle,
                    color = MetroTheme.colors.secondaryText,
                    modifier = Modifier.padding(
                        start = MetroDimens.ScreenHorizontalMargin,
                        end = MetroDimens.ScreenHorizontalMargin,
                        bottom = 8.dp,
                    ),
                )
            }
            item {
                MetroBorderButton(
                    text = stringResource(R.string.settings_sounds_grant_write),
                    onClick = { context.startActivity(sounds.writeSettingsIntent()) },
                    modifier = Modifier.padding(
                        start = MetroDimens.ScreenHorizontalMargin,
                        end = MetroDimens.ScreenHorizontalMargin,
                        bottom = 8.dp,
                    ),
                )
            }
        }

        item {
            MetroListItem(
                title = MetroSoundsController.SYSTEM_DEFAULT,
                trailing = {
                    MetroCheckBox(
                        checked = sounds.selectedSoundId(role) == null,
                        onCheckedChange = null,
                    )
                },
                onClick = {
                    sounds.selectSound(role, null)
                    MetroSoundPreview.stop()
                },
            )
        }

        val choices = sounds.choicesForRole(role)
        items(choices, key = { it.id }) { asset ->
            SoundChoiceRow(
                asset = asset,
                selected = sounds.selectedSoundId(role) == asset.id,
                onSelect = {
                    when (sounds.selectSound(role, asset.id)) {
                        MetroSoundsController.SelectOutcome.NOT_INSTALLED ->
                            message = context.getString(R.string.settings_sounds_not_installed_choice)
                        MetroSoundsController.SelectOutcome.NEEDS_WRITE_SETTINGS -> {
                            previewAsset(context, sounds, asset, role)
                            showWriteDialog = true
                        }
                        else -> previewAsset(context, sounds, asset, role)
                    }
                },
            )
        }

        message?.let { msg ->
            item {
                MetroText(
                    text = msg,
                    style = MetroTextStyle.ListItemSubtitle,
                    color = MetroTheme.colors.accent,
                    modifier = Modifier.padding(
                        start = MetroDimens.ScreenHorizontalMargin,
                        end = MetroDimens.ScreenHorizontalMargin,
                        top = 8.dp,
                    ),
                )
            }
        }
    }

    if (showWriteDialog) {
        MetroMessageDialog(
            title = stringResource(R.string.settings_sounds_write_title),
            body = stringResource(R.string.settings_sounds_write_body),
            confirmLabel = stringResource(R.string.settings_sounds_write_open),
            onConfirm = {
                showWriteDialog = false
                context.startActivity(sounds.writeSettingsIntent())
            },
            dismissLabel = stringResource(R.string.settings_sounds_write_later),
            onDismiss = { showWriteDialog = false },
            onDismissRequest = { showWriteDialog = false },
        )
    }
}

@Composable
private fun SoundChoiceRow(
    asset: MetroSoundAsset,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    MetroListItem(
        title = asset.title,
        trailing = {
            MetroCheckBox(checked = selected, onCheckedChange = null)
        },
        onClick = onSelect,
    )
}

private fun previewAsset(
    context: android.content.Context,
    sounds: MetroSoundsController,
    asset: MetroSoundAsset,
    role: MetroSoundRole,
) {
    val uri = sounds.resolveUri(asset.id) ?: return
    MetroSoundPreview.play(context, uri, role)
}

private fun pickerTitle(role: MetroSoundRole): String = when (role) {
    MetroSoundRole.PHONE_RINGTONE -> "ringtone"
    MetroSoundRole.MESSAGE -> "messages"
    MetroSoundRole.MAIL -> "mail"
    MetroSoundRole.CALENDAR -> "calendar"
    MetroSoundRole.REMINDER -> "reminders"
    MetroSoundRole.SYSTEM_NOTIFICATION -> "system notification"
    MetroSoundRole.ALARM -> "default alarm"
    MetroSoundRole.TIMER -> "timer"
}
