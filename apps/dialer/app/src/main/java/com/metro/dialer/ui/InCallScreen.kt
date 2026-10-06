package com.metro.dialer.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metro.dialer.R
import com.metro.dialer.data.DialerCallLogic
import com.metro.dialer.telecom.MetroCallEndpoint
import com.metro.dialer.telecom.MetroCallSession
import com.metro.dialer.telecom.MetroCallSessionState
import com.metro.dialer.telecom.MetroCallState
import com.metro.dialer.telecom.MetroTelecomCall
import com.metro.ui.MetroColors
import com.metro.ui.MetroMessageDialog
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private val InCallSectionBackground = Color(0xFF141414)
private val InCallTileBackground = Color(0xFF252525)
private val InCallTileDisabled = Color(0xFF5A5A5A)
private val InCallTileGap = 6.dp
private val InCallTileHeight = 78.dp
private val InCallKeyHeight = 64.dp

@Composable
fun InCallScreen(
    session: MetroCallSessionState,
    onEndCall: (String) -> Unit,
    onHold: (String, Boolean) -> Unit,
    onSwap: () -> Unit,
    onMerge: () -> Unit,
    onSplit: (String) -> Unit,
    onAnswerCall: (String) -> Unit,
    onAddCall: () -> Unit,
    onSelectEndpoint: (MetroCallEndpoint) -> Unit,
    onToggleMute: () -> Unit,
    onMinimize: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val primary = session.primaryCall ?: return
    val active = session.activeCall
    val heldCalls = session.heldCalls
    val incomingWaiting = session.calls.firstOrNull {
        it.isIncomingRinging && it.id != primary.id
    }

    var showKeypad by remember(primary.id) { mutableStateOf(false) }
    var dtmfDigits by remember(primary.id) { mutableStateOf("") }
    var showEndpointChooser by remember { mutableStateOf(false) }
    var photo by remember(primary.id) { mutableStateOf<ImageBitmap?>(null) }

    val audio = session.audio

    // Back hides the keypad first, otherwise minimizes to the return-to-call notification.
    // It never ends the call.
    androidx.activity.compose.BackHandler(enabled = showKeypad) { showKeypad = false }
    androidx.activity.compose.BackHandler(enabled = !showKeypad) { onMinimize() }

    DisposableEffect(primary.id) {
        onDispose { }
    }

    LaunchedEffect(primary.photoUri, primary.phoneNumber) {
        val uri = primary.photoUri
        photo = withContext(Dispatchers.IO) {
            if (uri != null) {
                com.metro.dialer.data.ContactsLookup(context).loadContactPhotoByUri(uri)?.asImageBitmap()
            } else {
                null
            }
        }
    }

    val statusText = callStatusText(primary)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
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
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    MetroText(
                        text = statusText,
                        style = MetroTextStyle.ListItemSubtitle,
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    MetroText(
                        text = primary.primaryLabel,
                        style = MetroTextStyle.PivotTab,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val subtitle = buildString {
                        primary.phoneLabel?.let { append(it).append("  ") }
                        if (showKeypad && dtmfDigits.isNotEmpty()) {
                            append(dtmfDigits)
                        } else {
                            append(DialerCallLogic.formatDisplayNumber(primary.phoneNumber.orEmpty()))
                        }
                    }
                    if (subtitle.isNotBlank()) {
                        MetroText(
                            text = subtitle,
                            style = MetroTextStyle.ListItemTitle,
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }

            incomingWaiting?.let { waiting ->
                WaitingCallRow(
                    call = waiting,
                    onAnswer = {
                        active?.let { onHold(it.id, true) }
                        onAnswerCall(waiting.id)
                    },
                    onIgnore = { onEndCall(waiting.id) },
                )
            }

            heldCalls.firstOrNull()?.let { held ->
                HeldCallRow(
                    call = held,
                    canMerge = held.canMerge && active?.canMerge == true,
                    onSwap = onSwap,
                    onMerge = onMerge,
                )
            }

            if (session.conferenceCallId != null && session.conferenceChildren.isNotEmpty()) {
                ConferenceSection(
                    participants = session.conferenceChildren,
                    onSplit = onSplit,
                    onDisconnect = { childId -> onEndCall(childId) },
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(InCallSectionBackground)
                .navigationBarsPadding()
                .padding(InCallTileGap),
            verticalArrangement = Arrangement.spacedBy(InCallTileGap),
        ) {
            if (showKeypad) {
                InCallDtmfKeypad(
                    onDigit = { digit ->
                        dtmfDigits += digit
                        MetroCallSession.playDtmf(digit, primary.id)
                    },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(InCallTileGap),
                ) {
                    EndCallTile(
                        onEndCall = { onEndCall(primary.id) },
                        modifier = Modifier.weight(2f),
                    )
                    InCallControlTile(
                        label = stringResource(R.string.hide_keypad),
                        icon = InCallIcon.Keypad,
                        enabled = true,
                        active = true,
                        onClick = { showKeypad = false },
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                InCallControlRow(
                    controls = listOf(
                        InCallControl(
                            label = stringResource(R.string.speaker),
                            icon = InCallIcon.Speaker,
                            enabled = true,
                            active = audio.currentEndpoint?.type ==
                                com.metro.dialer.telecom.MetroCallEndpointType.SPEAKER,
                            onClick = {
                                val speaker = audio.availableEndpoints.firstOrNull {
                                    it.type == com.metro.dialer.telecom.MetroCallEndpointType.SPEAKER
                                }
                                val earpiece = audio.availableEndpoints.firstOrNull {
                                    it.type == com.metro.dialer.telecom.MetroCallEndpointType.EARPIECE
                                }
                                val isSpeaker = audio.currentEndpoint?.type ==
                                    com.metro.dialer.telecom.MetroCallEndpointType.SPEAKER
                                (if (isSpeaker) earpiece else speaker)?.let(onSelectEndpoint)
                            },
                        ),
                        InCallControl(
                            label = stringResource(if (audio.muted) R.string.unmute else R.string.mute),
                            icon = InCallIcon.Mute,
                            enabled = true,
                            active = audio.muted,
                            onClick = onToggleMute,
                        ),
                        InCallControl(
                            label = stringResource(R.string.audio),
                            icon = InCallIcon.Audio,
                            enabled = audio.hasMultipleEndpoints,
                            active = audio.hasMultipleEndpoints,
                            onClick = { showEndpointChooser = true },
                        ),
                    ),
                )
                InCallControlRow(
                    controls = listOf(
                        InCallControl(
                            label = stringResource(R.string.hold),
                            icon = InCallIcon.Hold,
                            enabled = primary.canHold && (active != null || primary.state == MetroCallState.HOLDING),
                            active = primary.state == MetroCallState.HOLDING,
                            onClick = { onHold(primary.id, primary.state != MetroCallState.HOLDING) },
                        ),
                        InCallControl(
                            label = stringResource(R.string.keypad),
                            icon = InCallIcon.Keypad,
                            enabled = true,
                            active = false,
                            onClick = { showKeypad = true },
                        ),
                        InCallControl(
                            label = stringResource(R.string.add_call),
                            icon = InCallIcon.AddCall,
                            enabled = session.canAddCall,
                            active = false,
                            onClick = onAddCall,
                        ),
                    ),
                )
                EndCallTile(
                    onEndCall = { onEndCall(primary.id) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (showEndpointChooser) {
        AudioEndpointChooser(
            endpoints = audio.availableEndpoints,
            currentId = audio.currentEndpoint?.id,
            onSelect = {
                showEndpointChooser = false
                onSelectEndpoint(it)
            },
            onDismiss = { showEndpointChooser = false },
        )
    }
}

@Composable
private fun callStatusText(call: MetroTelecomCall): String = when (call.state) {
    MetroCallState.ACTIVE -> {
        val connectTime = call.connectTimeMillis
        if (connectTime != null) {
            ElapsedTime(connectTime)
        } else {
            stringResource(R.string.active_call)
        }
    }
    MetroCallState.HOLDING -> stringResource(R.string.on_hold)
    MetroCallState.DIALING, MetroCallState.CONNECTING -> stringResource(R.string.dialling)
    MetroCallState.RINGING -> stringResource(R.string.ringing)
    MetroCallState.DISCONNECTING, MetroCallState.DISCONNECTED -> stringResource(R.string.call_ended)
    else -> stringResource(R.string.calling)
}

@Composable
private fun ElapsedTime(connectTimeMillis: Long): String {
    var elapsed by remember(connectTimeMillis) { mutableLongStateOf(0L) }
    LaunchedEffect(connectTimeMillis) {
        while (true) {
            elapsed = ((System.currentTimeMillis() - connectTimeMillis) / 1000L).coerceAtLeast(0L)
            delay(1000)
        }
    }
    return DialerCallLogic.formatDuration(elapsed.toInt())
}

@Composable
private fun WaitingCallRow(
    call: MetroTelecomCall,
    onAnswer: () -> Unit,
    onIgnore: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MetroTheme.colors.secondarySurface)
            .padding(12.dp),
    ) {
        MetroText(
            text = stringResource(R.string.incoming_call_label),
            style = MetroTextStyle.ListItemSubtitle,
            color = MetroTheme.colors.secondaryText,
        )
        MetroText(text = call.primaryLabel, style = MetroTextStyle.ListItemTitle)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(InCallTileGap),
        ) {
            InCallControlTile(
                label = stringResource(R.string.answer),
                icon = InCallIcon.Answer,
                enabled = true,
                active = false,
                onClick = onAnswer,
                modifier = Modifier.weight(1f),
            )
            InCallControlTile(
                label = stringResource(R.string.ignore),
                icon = InCallIcon.End,
                enabled = true,
                active = false,
                onClick = onIgnore,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HeldCallRow(
    call: MetroTelecomCall,
    canMerge: Boolean,
    onSwap: () -> Unit,
    onMerge: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MetroTheme.colors.secondarySurface)
            .padding(12.dp),
    ) {
        MetroText(
            text = stringResource(R.string.on_hold),
            style = MetroTextStyle.ListItemSubtitle,
            color = MetroTheme.colors.secondaryText,
        )
        MetroText(text = call.primaryLabel, style = MetroTextStyle.ListItemTitle)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(InCallTileGap),
        ) {
            InCallControlTile(
                label = stringResource(R.string.swap),
                icon = InCallIcon.Swap,
                enabled = true,
                active = false,
                onClick = onSwap,
                modifier = Modifier.weight(1f),
            )
            if (canMerge) {
                InCallControlTile(
                    label = stringResource(R.string.merge),
                    icon = InCallIcon.AddCall,
                    enabled = true,
                    active = false,
                    onClick = onMerge,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ConferenceSection(
    participants: List<MetroTelecomCall>,
    onSplit: (String) -> Unit,
    onDisconnect: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MetroTheme.colors.secondarySurface)
            .padding(12.dp),
    ) {
        MetroText(
            text = stringResource(R.string.conference),
            style = MetroTextStyle.ListItemSubtitle,
            color = MetroTheme.colors.secondaryText,
        )
        participants.forEach { participant ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MetroText(
                    text = participant.primaryLabel,
                    style = MetroTextStyle.ListItemTitle,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                MetroText(
                    text = stringResource(R.string.private_call),
                    style = MetroTextStyle.ListItemSubtitle,
                    color = MetroTheme.colors.accent,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSplit(participant.id) },
                        )
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                )
                MetroText(
                    text = stringResource(R.string.end_call),
                    style = MetroTextStyle.ListItemSubtitle,
                    color = MetroColors.AccentRed,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onDisconnect(participant.id) },
                        )
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun AudioEndpointChooser(    endpoints: List<MetroCallEndpoint>,
    currentId: String?,
    onSelect: (MetroCallEndpoint) -> Unit,
    onDismiss: () -> Unit,
) {
    MetroMessageDialog(
        title = stringResource(R.string.audio),
        onDismissRequest = onDismiss,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            endpoints.forEach { endpoint ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(endpoint) }
                        .padding(vertical = 14.dp),
                ) {
                    MetroText(
                        text = endpoint.label,
                        style = MetroTextStyle.ListItemTitle,
                        color = if (endpoint.id == currentId) {
                            MetroTheme.colors.accent
                        } else {
                            MetroTheme.colors.primaryText
                        },
                    )
                }
            }
        }
    }
}

private enum class InCallIcon {
    Speaker,
    Mute,
    Audio,
    Hold,
    Keypad,
    AddCall,
    Answer,
    End,
    Swap,
}

private data class InCallControl(
    val label: String,
    val icon: InCallIcon,
    val enabled: Boolean,
    val active: Boolean = false,
    val onClick: () -> Unit = {},
)

@Composable
private fun InCallControlRow(
    controls: List<InCallControl>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(InCallTileGap),
    ) {
        controls.forEach { control ->
            InCallControlTile(
                label = control.label,
                icon = control.icon,
                enabled = control.enabled,
                active = control.active,
                onClick = control.onClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun InCallControlTile(
    label: String,
    icon: InCallIcon,
    enabled: Boolean,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val background = when {
        !enabled -> InCallTileBackground
        active || isPressed -> MetroTheme.colors.accent
        else -> InCallTileBackground
    }
    val contentColor = if (!enabled) InCallTileDisabled else Color.White
    Box(
        modifier = modifier
            .height(InCallTileHeight)
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            InCallControlIcon(icon = icon, color = contentColor)
            MetroText(
                text = label,
                style = MetroTextStyle.ListItemSubtitle,
                color = contentColor,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun EndCallTile(
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val background = if (isPressed) {
        MetroTheme.colors.accent.copy(alpha = 0.75f)
    } else {
        MetroTheme.colors.accent
    }
    Box(
        modifier = modifier
            .height(InCallTileHeight)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onEndCall,
            )
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        MetroText(
            text = stringResource(R.string.end_call),
            style = MetroTextStyle.ListItemSubtitle,
            color = Color.White,
        )
    }
}

@Composable
private fun InCallDtmfKeypad(
    onDigit: (Char) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(InCallTileGap),
    ) {
        InCallDtmfRow(listOf('1', '2', '3'), onDigit)
        InCallDtmfRow(listOf('4', '5', '6'), onDigit)
        InCallDtmfRow(listOf('7', '8', '9'), onDigit)
        InCallDtmfRow(listOf('*', '0', '#'), onDigit)
    }
}

@Composable
private fun InCallDtmfRow(
    digits: List<Char>,
    onDigit: (Char) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(InCallTileGap),
    ) {
        digits.forEach { digit ->
            InCallDtmfKey(
                digit = digit,
                onClick = { onDigit(digit) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun InCallDtmfKey(
    digit: Char,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = rememberDialKeyPressed(interactionSource)
    val background = if (isPressed) MetroTheme.colors.accent else InCallTileBackground
    val contentColor = if (isPressed) Color.White else MetroTheme.colors.primaryText
    Box(
        modifier = modifier
            .height(InCallKeyHeight)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        MetroText(
            text = digit.toString(),
            style = MetroTextStyle.PivotTab,
            color = contentColor,
        )
    }
}

@Composable
private fun InCallControlIcon(
    icon: InCallIcon,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(width = 30.dp, height = 26.dp)) {
        val w = size.width
        val h = size.height
        val sw = h * 0.085f
        val stroke = Stroke(width = sw)
        when (icon) {
            InCallIcon.Speaker -> {
                val cone = Path().apply {
                    moveTo(0.06f * w, 0.38f * h)
                    lineTo(0.20f * w, 0.38f * h)
                    lineTo(0.40f * w, 0.16f * h)
                    lineTo(0.40f * w, 0.84f * h)
                    lineTo(0.20f * w, 0.62f * h)
                    lineTo(0.06f * w, 0.62f * h)
                    close()
                }
                drawPath(cone, color)
                drawArc(
                    color = color,
                    startAngle = -55f,
                    sweepAngle = 110f,
                    useCenter = false,
                    topLeft = Offset(0.40f * w, 0.22f * h),
                    size = Size(0.28f * w, 0.56f * h),
                    style = stroke,
                )
                drawArc(
                    color = color,
                    startAngle = -55f,
                    sweepAngle = 110f,
                    useCenter = false,
                    topLeft = Offset(0.46f * w, 0.10f * h),
                    size = Size(0.46f * w, 0.80f * h),
                    style = stroke,
                )
            }
            InCallIcon.Mute -> {
                val micLeft = 0.39f * w
                val micRight = 0.61f * w
                drawArc(
                    color = color,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(micLeft, 0.12f * h),
                    size = Size(micRight - micLeft, micRight - micLeft),
                    style = stroke,
                )
                drawLine(color, Offset(micLeft, 0.23f * h), Offset(micLeft, 0.46f * h), sw)
                drawLine(color, Offset(micRight, 0.23f * h), Offset(micRight, 0.46f * h), sw)
                drawArc(
                    color = color,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(0.30f * w, 0.34f * h),
                    size = Size(0.40f * w, 0.40f * h),
                    style = stroke,
                )
                drawLine(color, Offset(0.5f * w, 0.74f * h), Offset(0.5f * w, 0.88f * h), sw)
                drawLine(color, Offset(0.36f * w, 0.88f * h), Offset(0.64f * w, 0.88f * h), sw)
                drawLine(color, Offset(0.12f * w, 0.08f * h), Offset(0.88f * w, 0.92f * h), sw)
            }
            InCallIcon.Audio -> {
                val path = Path().apply {
                    moveTo(0.10f * w, 0.5f * h)
                    lineTo(0.34f * w, 0.5f * h)
                    lineTo(0.54f * w, 0.24f * h)
                    lineTo(0.54f * w, 0.76f * h)
                    lineTo(0.34f * w, 0.5f * h)
                    close()
                }
                drawPath(path, color)
                drawArc(
                    color = color,
                    startAngle = -55f,
                    sweepAngle = 110f,
                    useCenter = false,
                    topLeft = Offset(0.54f * w, 0.28f * h),
                    size = Size(0.30f * w, 0.44f * h),
                    style = stroke,
                )
                drawArc(
                    color = color,
                    startAngle = -55f,
                    sweepAngle = 110f,
                    useCenter = false,
                    topLeft = Offset(0.62f * w, 0.16f * h),
                    size = Size(0.42f * w, 0.68f * h),
                    style = stroke,
                )
            }
            InCallIcon.Hold -> {
                drawRect(
                    color = color,
                    topLeft = Offset(0.34f * w, 0.20f * h),
                    size = Size(0.12f * w, 0.60f * h),
                )
                drawRect(
                    color = color,
                    topLeft = Offset(0.54f * w, 0.20f * h),
                    size = Size(0.12f * w, 0.60f * h),
                )
            }
            InCallIcon.Keypad -> {
                val xs = listOf(0.28f, 0.5f, 0.72f)
                val ys = listOf(0.22f, 0.5f, 0.78f)
                val radius = h * 0.07f
                ys.forEach { fy ->
                    xs.forEach { fx ->
                        drawCircle(color, radius, Offset(fx * w, fy * h))
                    }
                }
            }
            InCallIcon.AddCall -> {
                val plus = sw * 1.4f
                drawLine(color, Offset(0.5f * w, 0.22f * h), Offset(0.5f * w, 0.78f * h), plus)
                drawLine(color, Offset(0.22f * w, 0.5f * h), Offset(0.78f * w, 0.5f * h), plus)
            }
            InCallIcon.Answer -> {
                val path = Path().apply {
                    moveTo(0.10f * w, 0.52f * h)
                    quadraticBezierTo(0.5f * w, 0.06f * h, 0.90f * w, 0.52f * h)
                    quadraticBezierTo(0.72f * w, 0.56f * h, 0.62f * w, 0.46f * h)
                    lineTo(0.62f * w, 0.30f * h)
                    lineTo(0.38f * w, 0.30f * h)
                    lineTo(0.38f * w, 0.46f * h)
                    quadraticBezierTo(0.28f * w, 0.56f * h, 0.10f * w, 0.52f * h)
                    close()
                }
                drawPath(path, color, style = stroke)
            }
            InCallIcon.End -> {
                val path = Path().apply {
                    moveTo(0.10f * w, 0.48f * h)
                    quadraticBezierTo(0.5f * w, 0.94f * h, 0.90f * w, 0.48f * h)
                    quadraticBezierTo(0.72f * w, 0.44f * h, 0.62f * w, 0.54f * h)
                    lineTo(0.62f * w, 0.70f * h)
                    lineTo(0.38f * w, 0.70f * h)
                    lineTo(0.38f * w, 0.54f * h)
                    quadraticBezierTo(0.28f * w, 0.44f * h, 0.10f * w, 0.48f * h)
                    close()
                }
                drawPath(path, color, style = stroke)
            }
            InCallIcon.Swap -> {
                drawLine(color, Offset(0.16f * w, 0.34f * h), Offset(0.84f * w, 0.34f * h), sw)
                drawLine(color, Offset(0.72f * w, 0.20f * h), Offset(0.86f * w, 0.34f * h), sw)
                drawLine(color, Offset(0.16f * w, 0.66f * h), Offset(0.84f * w, 0.66f * h), sw)
                drawLine(color, Offset(0.28f * w, 0.52f * h), Offset(0.14f * w, 0.66f * h), sw)
            }
        }
    }
}
