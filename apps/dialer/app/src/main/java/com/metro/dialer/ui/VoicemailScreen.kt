package com.metro.dialer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.dialer.R
import com.metro.ui.MetroEmptyState
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

@Composable
fun VoicemailScreen(
    state: DialerViewModel,
    modifier: Modifier = Modifier,
) {
    val number = state.voicemailNumber
    if (!state.voicemailLoaded) {
        Box(modifier = modifier.fillMaxSize().background(Color.Black))
        return
    }
    if (number.isNullOrBlank()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            MetroEmptyState(
                message = stringResource(R.string.voicemail_unavailable_body),
                modifier = Modifier.fillMaxWidth(),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = state::openPhoneSettings,
                    )
                    .padding(12.dp),
                contentAlignment = Alignment.Center,
            ) {
                MetroText(
                    text = stringResource(R.string.settings),
                    style = MetroTextStyle.ListItemTitle,
                    color = MetroTheme.colors.accent,
                )
            }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(12.dp),
    ) {
        MetroText(
            text = stringResource(R.string.voicemail),
            style = MetroTextStyle.SectionHeader,
            color = MetroTheme.colors.secondaryText,
            modifier = Modifier.padding(vertical = 12.dp),
        )
        VoicemailCallTile(onClick = state::callVoicemail)
    }
}

@Composable
private fun VoicemailCallTile(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .background(MetroTheme.colors.accent),
        contentAlignment = Alignment.Center,
    ) {
        MetroText(
            text = stringResource(R.string.call_voicemail),
            style = MetroTextStyle.ListItemTitle,
            color = Color.White,
        )
    }
}
