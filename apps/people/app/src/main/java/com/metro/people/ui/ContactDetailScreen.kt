package com.metro.people.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import com.metro.ui.metroClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metro.people.R
import com.metro.people.data.PersonDetail
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

@Composable
fun ContactDetailScreen(
    detail: PersonDetail,
    onBack: () -> Unit,
    onCall: () -> Unit,
    onText: () -> Unit,
    onWhatsAppCall: () -> Unit,
    onWhatsAppText: () -> Unit,
    onEmail: (String) -> Unit,
    onPin: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val summary = detail.summary
    val whatsApp = detail.whatsApp
    BackHandler(onBack = onBack)

    androidx.compose.foundation.layout.Box(modifier = modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState()),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(top = 4.dp, bottom = 8.dp),
        ) {
            MetroText(
                text = summary.displayName.uppercase(),
                style = MetroTextStyle.SectionHeader,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            MetroText(
                text = summary.sourceLabel,
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.secondaryText,
            )
        }

        MetroText(
            text = "profile",
            style = MetroTextStyle.HubTitle,
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .padding(bottom = 8.dp),
        )

        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
            Row(modifier = Modifier.padding(bottom = 24.dp)) {
                ContactAvatar(
                    contactId = summary.id,
                    modifier = Modifier.size(200.dp),
                )
            }

            ActionRow(
                label = stringResource(R.string.call_mobile),
                value = summary.defaultPhone ?: "—",
                enabled = summary.defaultPhone != null,
                onClick = onCall,
            )
            ActionRow(
                label = stringResource(R.string.text),
                value = "SMS",
                enabled = summary.defaultPhone != null,
                onClick = onText,
            )
            if (whatsApp?.canVoiceCall == true) {
                ActionRow(
                    label = stringResource(R.string.call_whatsapp),
                    value = stringResource(R.string.whatsapp_label),
                    enabled = true,
                    onClick = onWhatsAppCall,
                )
            }
            if (whatsApp?.canMessage == true) {
                ActionRow(
                    label = stringResource(R.string.text_whatsapp),
                    value = stringResource(R.string.whatsapp_label),
                    enabled = true,
                    onClick = onWhatsAppText,
                )
            }
            summary.defaultEmail?.let { email ->
                ActionRow(
                    label = stringResource(R.string.send_email),
                    value = email,
                    enabled = true,
                    onClick = { onEmail(email) },
                )
            }
        }

        Spacer(modifier = Modifier.height(96.dp))
    }
    com.metro.ui.MetroAppBar(
        icons = listOf(
            com.metro.ui.MetroAppBarIcon(
                type = com.metro.ui.MetroSystemIconType.Pin,
                label = stringResource(R.string.pin_to_start),
                contentDescription = stringResource(R.string.pin_to_start),
                onClick = onPin,
            ),
        ),
        modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter),
    )
    }
}

@Composable
private fun ActionRow(
    label: String,
    value: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.metroClickable(onClick = onClick) else Modifier)
            .padding(vertical = 12.dp),
    ) {
        MetroText(
            text = label,
            style = MetroTextStyle.ListItemTitle,
        )
        MetroText(
            text = value,
            style = MetroTextStyle.ListItemSubtitle,
            color = if (enabled) MetroTheme.colors.accent else MetroTheme.colors.secondaryText,
        )
    }
}
