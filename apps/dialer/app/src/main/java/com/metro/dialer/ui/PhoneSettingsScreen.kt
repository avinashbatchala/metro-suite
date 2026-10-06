package com.metro.dialer.ui

import android.content.Intent
import android.os.Build
import android.telecom.TelecomManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.dialer.R
import com.metro.dialer.data.PhonePreferences
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroListItem
import com.metro.ui.MetroMessageDialog
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroToggleSwitch
import com.metro.ui.MetroTheme
import com.metro.ui.metroNavBarPadding
import androidx.compose.foundation.layout.statusBarsPadding

@Composable
fun PhoneSettingsScreen(
    preferences: PhonePreferences,
    onBack: () -> Unit,
    onEditReplies: () -> Unit,
    onVoicemail: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var textReply by remember { mutableStateOf(preferences.textReplyEnabled) }
    var smartDial by remember { mutableStateOf(preferences.smartDialEnabled) }
    var notice by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .metroNavBarPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 72.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            MetroAppTitle(title = stringResource(R.string.settings))
            MetroText(
                text = stringResource(R.string.phone),
                style = MetroTextStyle.SectionHeader,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )

            SettingToggle(
                label = stringResource(R.string.text_reply),
                checked = textReply,
                onCheckedChange = {
                    textReply = it
                    preferences.textReplyEnabled = it
                },
            )
            MetroListItem(
                title = stringResource(R.string.edit_replies),
                onClick = onEditReplies,
            )
            MetroListItem(
                title = stringResource(R.string.voicemail),
                onClick = onVoicemail,
            )
            SettingAction(
                title = stringResource(R.string.voicemail_settings),
                subtitle = stringResource(R.string.voicemail_settings_body),
                onClick = {
                    openSystemSetting(context, TelecomManager.ACTION_SHOW_CALL_SETTINGS) { notice = it }
                },
            )
            SettingAction(
                title = stringResource(R.string.call_forwarding),
                subtitle = stringResource(R.string.carrier_handoff_body),
                onClick = { openSystemSetting(context, TelecomManager.ACTION_SHOW_CALL_SETTINGS) { notice = it } },
            )
            SettingAction(
                title = stringResource(R.string.call_waiting),
                subtitle = stringResource(R.string.carrier_handoff_body),
                onClick = { openSystemSetting(context, TelecomManager.ACTION_SHOW_CALL_SETTINGS) { notice = it } },
            )

            MetroText(
                text = stringResource(R.string.metrosuite_extensions),
                style = MetroTextStyle.SectionHeader,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
            SettingToggle(
                label = stringResource(R.string.smart_dial),
                checked = smartDial,
                onCheckedChange = {
                    smartDial = it
                    preferences.smartDialEnabled = it
                },
            )
            MetroText(
                text = stringResource(R.string.smart_dial_body),
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }

        MetroAppBar(
            icons = listOf(
                MetroAppBarIcon(
                    type = MetroSystemIconType.Back,
                    label = stringResource(R.string.back),
                    onClick = onBack,
                ),
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    notice?.let { message ->
        MetroMessageDialog(
            title = message,
            confirmLabel = stringResource(R.string.ok),
            onConfirm = { notice = null },
            onDismissRequest = { notice = null },
        )
    }
}

@Composable
private fun SettingToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        MetroToggleSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            label = label,
        )
    }
}

@Composable
private fun SettingAction(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    MetroListItem(title = title, subtitle = subtitle, onClick = onClick)
}

private fun openSystemSetting(
    context: android.content.Context,
    action: String,
    notice: (String) -> Unit,
) {
    val intent = Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val launched = runCatching { context.startActivity(intent) }.isSuccess
    if (!launched) notice("not available on this device")
}

@Composable
internal fun ClickableSettingRow(title: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 16.dp),
    ) {
        MetroText(text = title, style = MetroTextStyle.ListItemTitle)
    }
}
