package com.metro.dialer.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import com.metro.dialer.R
import com.metro.dialer.data.DialerCallLogic
import com.metro.dialer.telecom.MetroTelecomCall
import com.metro.ui.MetroMessageDialog
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val IncomingSectionBackground = Color(0xFF141414)
private val IncomingSecondaryTileBackground = Color(0xFF252525)
private val IncomingTileGap = 6.dp
private val IncomingTileHeight = 78.dp

@Composable
fun IncomingCallScreen(
    call: MetroTelecomCall,
    textReplies: List<String>,
    textReplyEnabled: Boolean,
    lockedReveal: Boolean,
    onAnswer: () -> Unit,
    onIgnore: () -> Unit,
    onTextReply: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var photo by remember(call.id) { mutableStateOf<ImageBitmap?>(null) }
    var showReplyChooser by remember { mutableStateOf(false) }
    var revealed by remember(call.id) { mutableStateOf(!lockedReveal) }

    // Do not let an accidental Back dismiss a ringing call — answer/ignore/text-reply are explicit.
    androidx.activity.compose.BackHandler(enabled = true) { }

    LaunchedEffect(call.photoUri, call.phoneNumber) {
        val uri = call.photoUri
        photo = withContext(Dispatchers.IO) {
            if (uri != null) {
                com.metro.dialer.data.ContactsLookup(context).loadContactPhotoByUri(uri)?.asImageBitmap()
            } else {
                null
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .let { if (lockedReveal && !revealed) it.clickable { revealed = true } else it },
        ) {
            val currentPhoto = photo
            if (currentPhoto != null) {
                Image(
                    bitmap = currentPhoto,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(start = 12.dp, top = 16.dp, bottom = 16.dp),
            ) {
                call.carrierLabel?.let { carrier ->
                    MetroText(
                        text = carrier,
                        style = MetroTextStyle.ListItemSubtitle,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
                MetroText(
                    text = stringResource(R.string.incoming_call_label),
                    style = MetroTextStyle.SectionHeader,
                    color = Color.White,
                )
                MetroText(
                    text = call.primaryLabel,
                    style = MetroTextStyle.PivotTab,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 12.dp),
                )
                val subtitle = buildString {
                    call.phoneLabel?.let { append(it).append("  ") }
                    append(DialerCallLogic.formatDisplayNumber(call.phoneNumber.orEmpty()))
                }
                if (subtitle.isNotBlank()) {
                    MetroText(
                        text = subtitle,
                        style = MetroTextStyle.ListItemTitle,
                        color = Color.White.copy(alpha = 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                if (lockedReveal && !revealed) {
                    Spacer(modifier = Modifier.height(24.dp))
                    MetroText(
                        text = stringResource(R.string.slide_up_to_show),
                        style = MetroTextStyle.ListItemSubtitle,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(IncomingSectionBackground)
                .navigationBarsPadding()
                .padding(IncomingTileGap),
            verticalArrangement = Arrangement.spacedBy(IncomingTileGap),
        ) {
            if (!lockedReveal || revealed) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(IncomingTileGap),
                ) {
                    IncomingActionTile(
                        label = stringResource(R.string.answer),
                        background = MetroTheme.colors.accent,
                        onClick = onAnswer,
                        modifier = Modifier.weight(1f),
                    )
                    IncomingActionTile(
                        label = stringResource(R.string.ignore),
                        background = IncomingSecondaryTileBackground,
                        onClick = onIgnore,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (textReplyEnabled) {
                    IncomingActionTile(
                        label = stringResource(R.string.text_reply),
                        background = IncomingSecondaryTileBackground,
                        onClick = { showReplyChooser = true },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                IncomingActionTile(
                    label = stringResource(R.string.answer),
                    background = MetroTheme.colors.accent,
                    onClick = onAnswer,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (showReplyChooser) {
        TextReplyChooser(
            replies = textReplies,
            onSelect = {
                showReplyChooser = false
                onTextReply(it)
            },
            onDismiss = { showReplyChooser = false },
        )
    }
}

@Composable
private fun TextReplyChooser(
    replies: List<String>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var customMode by remember { mutableStateOf(false) }
    var customText by remember { mutableStateOf("") }

    if (customMode) {
        MetroMessageDialog(
            title = stringResource(R.string.text_reply),
            onDismissRequest = onDismiss,
            confirmLabel = stringResource(R.string.send),
            onConfirm = { if (customText.isNotBlank()) onSelect(customText) },
        ) {
            com.metro.ui.MetroTextBox(
                value = customText,
                onValueChange = { customText = it },
                placeholder = stringResource(R.string.type_message),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        return
    }

    MetroMessageDialog(
        title = stringResource(R.string.text_reply),
        onDismissRequest = onDismiss,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            replies.forEach { reply ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(reply) }
                        .padding(vertical = 14.dp),
                ) {
                    MetroText(text = reply, style = MetroTextStyle.ListItemTitle)
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { customMode = true }
                    .padding(vertical = 14.dp),
            ) {
                MetroText(
                    text = stringResource(R.string.type_message),
                    style = MetroTextStyle.ListItemTitle,
                    color = MetroTheme.colors.secondaryText,
                )
            }
        }
    }
}

@Composable
private fun IncomingActionTile(
    label: String,
    background: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val tileBackground = if (isPressed) background.copy(alpha = 0.75f) else background
    Box(
        modifier = modifier
            .height(IncomingTileHeight)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .background(tileBackground),
        contentAlignment = Alignment.Center,
    ) {
        MetroText(
            text = label,
            style = MetroTextStyle.ListItemSubtitle,
            color = Color.White,
        )
    }
}
