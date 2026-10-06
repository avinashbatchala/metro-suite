package com.metro.dialer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.dialer.R
import com.metro.dialer.data.CallGroup
import com.metro.dialer.data.CallType
import com.metro.dialer.data.DialerCallLogic
import com.metro.ui.MetroColors
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroContextMenuActiveShift
import com.metro.ui.MetroContextMenuClearOnDismiss
import com.metro.ui.MetroContextMenuDimmedAlpha
import com.metro.ui.MetroContextMenuItem
import com.metro.ui.MetroContextMenuPopup
import com.metro.ui.MetroMultiSelectRow
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

private class HistoryRectRef {
    var value: Rect = Rect.Zero
}

@Composable
fun HistoryScreen(
    state: DialerViewModel,
    listState: LazyListState,
    onRequestPermissions: (Array<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.selectionMode) {
        HistorySelectionList(state = state, modifier = modifier)
        return
    }
    if (!state.hasCallLogPermission) {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            EmptyPane(message = stringResource(R.string.history_permission_denied))
            MetroBorderButton(
                text = stringResource(R.string.allow_access),
                onClick = { onRequestPermissions(arrayOf(android.Manifest.permission.READ_CALL_LOG)) },
                modifier = Modifier.padding(top = 16.dp),
            )
        }
        return
    }
    if (state.callGroups.isEmpty()) {
        EmptyPane(message = stringResource(R.string.history_empty), modifier = modifier)
        return
    }

    var contextMenuGroup by remember { mutableStateOf<CallGroup?>(null) }
    var contextMenuAnchor by remember { mutableStateOf(Rect.Zero) }
    var contextMenuRoot by remember { mutableStateOf(Rect.Zero) }
    val popupRootBounds = remember { HistoryRectRef() }
    val menuVisible = remember { androidx.compose.animation.core.MutableTransitionState(false) }

    val openMenu: (CallGroup, Rect) -> Unit = { group, bounds ->
        contextMenuAnchor = bounds
        contextMenuRoot = popupRootBounds.value
        contextMenuGroup = group
        menuVisible.targetState = true
    }
    val dismissMenu: () -> Unit = { menuVisible.targetState = false }
    MetroContextMenuClearOnDismiss(menuVisible) { contextMenuGroup = null }

    val sections = state.filteredSections

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { popupRootBounds.value = it.boundsInWindow() },
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(horizontal = 12.dp),
        ) {
            if (state.searchVisible) {
                item(key = "search") {
                    HistorySearchField(
                        query = state.searchQuery,
                        onQueryChange = state::updateSearchQuery,
                    )
                }
            }
            sections.forEach { section ->
                item(key = "header-${section.dateBucket}") {
                    MetroText(
                        text = section.label,
                        style = MetroTextStyle.SectionHeader,
                        color = MetroTheme.colors.secondaryText,
                        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp),
                    )
                }
                items(section.groups, key = { it.id }) { group ->
                    HistoryRow(
                        group = group,
                        contextMenuTarget = contextMenuGroup?.id == group.id,
                        onCall = { state.placeCall(group.phoneNumber, group.displayName) },
                        onOpenContact = { state.openContact(group) },
                        onLongPress = { bounds -> openMenu(group, bounds) },
                    )
                }
            }
        }

        contextMenuGroup?.let { group ->
            MetroContextMenuPopup(
                visibleState = menuVisible,
                anchorBounds = contextMenuAnchor,
                rootBounds = contextMenuRoot,
                items = buildList {
                    add(
                        MetroContextMenuItem(
                            label = stringResource(R.string.details),
                            onClick = {
                                state.openCallDetail(group)
                                dismissMenu()
                            },
                        ),
                    )
                    add(
                        MetroContextMenuItem(
                            label = stringResource(R.string.delete),
                            onClick = {
                                state.deleteGroup(group)
                                dismissMenu()
                            },
                        ),
                    )
                    add(
                        MetroContextMenuItem(
                            label = stringResource(R.string.block_number),
                            onClick = {
                                state.blockNumber(group)
                                dismissMenu()
                            },
                        ),
                    )
                    if (group.hasCallableNumber) {
                        add(
                            MetroContextMenuItem(
                                label = stringResource(R.string.add_to_speed_dial),
                                onClick = {
                                    state.addToSpeedDial(group)
                                    dismissMenu()
                                },
                            ),
                        )
                    }
                },
                onDismissRequest = dismissMenu,
            )
        }
    }
}

@Composable
private fun HistorySearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(MetroTheme.colors.secondarySurface)
            .padding(12.dp),
    ) {
        androidx.compose.foundation.text.BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            textStyle = MetroTextStyle.ListItemTitle.toTextStyle().copy(
                color = MetroTheme.colors.primaryText,
            ),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    MetroText(
                        text = stringResource(R.string.search_history),
                        style = MetroTextStyle.ListItemSubtitle,
                        color = MetroTheme.colors.secondaryText,
                    )
                }
                inner()
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryRow(
    group: CallGroup,
    contextMenuTarget: Boolean,
    onCall: () -> Unit,
    onOpenContact: () -> Unit,
    onLongPress: (Rect) -> Unit,
) {
    val primaryColor = if (group.latestType.isMissedFamily) {
        MetroColors.AccentRed
    } else {
        MetroTheme.colors.primaryText
    }
    val subtitle = "${typeLabel(group.latestType)} · ${DialerCallLogic.relativeTime(group.latestTimestamp)}"
    val labelBounds = remember(group.id) { HistoryRectRef() }
    val density = LocalDensity.current
    val activeShiftPx = with(density) { MetroContextMenuActiveShift.toPx() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .graphicsLayer {
                translationX = if (contextMenuTarget) -activeShiftPx else 0f
            }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .onGloballyPositioned { labelBounds.value = it.boundsInWindow() }
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onCall,
                    onLongClick = { onLongPress(labelBounds.value) },
                ),
        ) {
            MetroText(
                text = DialerCallLogic.primaryLabel(group),
                style = MetroTextStyle.ListItemTitle,
                color = primaryColor,
                maxLines = 1,
            )
            MetroText(
                text = subtitle,
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.secondaryText,
            )
        }
        Box(
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onOpenContact,
                )
                .padding(start = 8.dp),
        ) {
            ContactInfoIcon(color = MetroTheme.colors.primaryText)
        }
    }
}

@Composable
private fun HistorySelectionList(
    state: DialerViewModel,
    modifier: Modifier = Modifier,
) {
    val rows = state.callGroups.flatMap { group ->
        group.calls.map { call -> group to call }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 72.dp),
        ) {
            items(rows, key = { it.second.id }) { (group, call) ->
                MetroMultiSelectRow(
                    title = "${group.displayName} · ${typeLabel(call.type)} · " +
                        DialerCallLogic.relativeTime(call.timestamp),
                    checked = call.id in state.selectedCallIds,
                    onClick = { state.toggleSelection(call.id) },
                )
            }
        }
    }
}

@Composable
private fun typeLabel(type: CallType): String = stringResource(
    when (type) {
        CallType.INCOMING -> R.string.incoming
        CallType.OUTGOING -> R.string.outgoing
        CallType.MISSED -> R.string.missed
        CallType.REJECTED -> R.string.rejected
        CallType.BLOCKED -> R.string.blocked
        CallType.VOICEMAIL -> R.string.voicemail
        CallType.ANSWERED_EXTERNALLY -> R.string.answered_elsewhere
        CallType.UNKNOWN -> R.string.unknown
    },
)

@Composable
internal fun ContactInfoIcon(color: Color) {
    com.metro.ui.MetroSystemIcon(
        type = com.metro.ui.MetroSystemIconType.People,
        iconSize = 40.dp,
        color = color,
        showCircle = false,
    )
}
