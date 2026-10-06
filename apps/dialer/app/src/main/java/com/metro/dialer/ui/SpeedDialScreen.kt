package com.metro.dialer.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.dialer.R
import com.metro.dialer.data.DialerCallLogic
import com.metro.dialer.data.SpeedDialEntry
import com.metro.ui.MetroContextMenuClearOnDismiss
import com.metro.ui.MetroContextMenuItem
import com.metro.ui.MetroContextMenuPopup
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private class SpeedDialRectRef {
    var value: Rect = Rect.Zero
}

@Composable
fun SpeedDialScreen(
    state: DialerViewModel,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    if (state.speedDialEntries.isEmpty()) {
        EmptyPane(
            message = stringResource(R.string.speed_dial_empty),
            modifier = modifier,
        )
        return
    }

    var menuEntry by remember { mutableStateOf<SpeedDialEntry?>(null) }
    var menuAnchor by remember { mutableStateOf(Rect.Zero) }
    var menuRoot by remember { mutableStateOf(Rect.Zero) }
    val rootRef = remember { SpeedDialRectRef() }
    val menuVisible = remember { androidx.compose.animation.core.MutableTransitionState(false) }

    val openMenu: (SpeedDialEntry, Rect) -> Unit = { entry, bounds ->
        menuAnchor = bounds
        menuRoot = rootRef.value
        menuEntry = entry
        menuVisible.targetState = true
    }
    val dismissMenu: () -> Unit = { menuVisible.targetState = false }
    MetroContextMenuClearOnDismiss(menuVisible) { menuEntry = null }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { rootRef.value = it.boundsInWindow() },
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(horizontal = 12.dp),
        ) {
            items(state.speedDialEntries, key = { it.id }) { entry ->
                SpeedDialRow(
                    entry = entry,
                    contact = state.contactFor(entry.phoneNumber),
                    onCall = {
                        if (entry.phoneNumber.isNotBlank()) {
                            state.placeCall(entry.phoneNumber, entry.displayName)
                        } else {
                            state.openContact(entry)
                        }
                    },
                    onLongPress = { bounds -> openMenu(entry, bounds) },
                )
            }
        }

        menuEntry?.let { entry ->
            MetroContextMenuPopup(
                visibleState = menuVisible,
                anchorBounds = menuAnchor,
                rootBounds = menuRoot,
                items = listOf(
                    MetroContextMenuItem(
                        label = stringResource(R.string.open_contact),
                        onClick = {
                            state.openContact(entry)
                            dismissMenu()
                        },
                    ),
                    MetroContextMenuItem(
                        label = stringResource(R.string.pin_to_start),
                        onClick = {
                            state.pinToStart(entry)
                            dismissMenu()
                        },
                    ),
                    MetroContextMenuItem(
                        label = stringResource(R.string.remove_from_speed_dial),
                        onClick = {
                            state.removeSpeedDial(entry)
                            dismissMenu()
                        },
                    ),
                ),
                onDismissRequest = dismissMenu,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SpeedDialRow(
    entry: SpeedDialEntry,
    contact: com.metro.dialer.data.ContactSuggestion?,
    onCall: () -> Unit,
    onLongPress: (Rect) -> Unit,
) {
    val rowBounds = remember(entry.id) { SpeedDialRectRef() }
    val displayName = contact?.displayName ?: entry.displayName
    val subtitle = entry.phoneNumber.ifBlank { stringResource(R.string.speed_dial_unavailable) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .onGloballyPositioned { rowBounds.value = it.boundsInWindow() }
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onCall,
                onLongClick = { onLongPress(rowBounds.value) },
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SpeedDialAvatar(
            name = displayName,
            photoUri = contact?.photoUri,
            number = entry.phoneNumber,
        )
        Column(modifier = Modifier.padding(start = 16.dp)) {
            MetroText(text = displayName, style = MetroTextStyle.ListItemTitle)
            MetroText(
                text = subtitle,
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.secondaryText,
            )
        }
    }
}

@Composable
private fun SpeedDialAvatar(
    name: String,
    photoUri: android.net.Uri?,
    number: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var photo by remember(photoUri, number) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(photoUri, number) {
        photo = withContext(Dispatchers.IO) {
            val lookup = com.metro.dialer.data.ContactsLookup(context)
            val bitmap = if (photoUri != null) {
                lookup.loadContactPhotoByUri(photoUri)
            } else {
                lookup.loadContactPhoto(number)
            }
            bitmap?.asImageBitmap()
        }
    }
    val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
    Box(
        modifier = modifier
            .size(48.dp)
            .background(MetroTheme.colors.accent),
        contentAlignment = Alignment.Center,
    ) {
        val current = photo
        if (current != null) {
            Image(
                bitmap = current,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            MetroText(
                text = initial,
                style = MetroTextStyle.ListItemTitle,
                color = Color.White,
            )
        }
    }
}
