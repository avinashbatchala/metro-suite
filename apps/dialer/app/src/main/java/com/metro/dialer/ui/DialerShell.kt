package com.metro.dialer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.dialer.R
import com.metro.dialer.data.PhonePreferences
import com.metro.ui.LocalMetroSubpageExit
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroAppBarMenuItem
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroHubTitleMode
import com.metro.ui.MetroHubTitleRow
import com.metro.ui.MetroMessageDialog
import com.metro.ui.MetroSubpageHost
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroNavBarPadding
import kotlinx.coroutines.launch

@Composable
fun DialerShell(
    state: DialerViewModel,
    onRequestPermissions: (Array<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })
    val historyListState = rememberLazyListState()
    val speedDialListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.pivot) {
        val target = when (state.pivot) {
            PhonePivot.History -> 0
            PhonePivot.SpeedDial -> 1
            PhonePivot.Voicemail -> 2
        }
        if (pagerState.currentPage != target) pagerState.scrollToPage(target)
    }
    LaunchedEffect(pagerState.currentPage) {
        state.setPivot(pagerState.currentPage)
    }

    MetroSubpageHost(
        route = state.route,
        isRoot = { it == DialerRoute.Main },
        parentOf = { DialerRoute.Main },
        loadKeyOf = { route ->
            when (route) {
                DialerRoute.CallDetail -> "CallDetail:${state.selectedGroup?.id.orEmpty()}"
                else -> route.name
            }
        },
        onGoBack = state::closeSubpage,
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        rootContent = {
            RootContent(
                state = state,
                pagerState = pagerState,
                historyListState = historyListState,
                speedDialListState = speedDialListState,
                onRequestPermissions = onRequestPermissions,
                onSelectPage = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
            )
        },
        subpageContent = { route ->
            val requestExit = LocalMetroSubpageExit.current
            when (route) {
                DialerRoute.DialPad -> DialPadSubpage(
                    state = state,
                    onBack = { requestExit?.invoke() },
                )
                DialerRoute.CallDetail -> state.selectedGroup?.let { group ->
                    CallDetailScreen(
                        group = group,
                        onBack = { requestExit?.invoke() },
                        onCall = { state.placeCall(group.phoneNumber, group.displayName) },
                        onMessage = { state.sendMessage(group.phoneNumber) },
                        onDelete = { state.deleteGroup(group) },
                    )
                }
                DialerRoute.SpeedDialAdd -> SpeedDialAddScreen(
                    state = state,
                    onRequestPermissions = onRequestPermissions,
                    onBack = { requestExit?.invoke() },
                )
                DialerRoute.PhoneSettings -> PhoneSettingsScreen(
                    preferences = state.phonePreferences,
                    onBack = { requestExit?.invoke() },
                    onEditReplies = state::openEditReplies,
                    onVoicemail = { state.setPivot(2); requestExit?.invoke() },
                )
                DialerRoute.EditReplies -> EditRepliesScreen(
                    preferences = state.phonePreferences,
                    onBack = { requestExit?.invoke() },
                )
                DialerRoute.SaveContact -> SaveContactScreen(
                    number = state.dialString,
                    onNewContact = { state.saveNumberAsNewContact(it) },
                    onAddToExisting = { state.addNumberToExistingContact(it) },
                    onBack = { requestExit?.invoke() },
                )
                DialerRoute.Main -> Unit
            }
        },
    )

    state.simChooser?.let { chooser ->
        SimChooserDialog(
            chooser = chooser,
            onSelect = state::confirmSimChoice,
            onDismiss = state::dismissSimChooser,
        )
    }

    state.status?.let { status ->
        MetroMessageDialog(
            title = status.message,
            confirmLabel = stringResource(R.string.ok),
            onConfirm = state::dismissStatus,
            onDismissRequest = state::dismissStatus,
        )
    }
}

@Composable
private fun RootContent(
    state: DialerViewModel,
    pagerState: PagerState,
    historyListState: androidx.compose.foundation.lazy.LazyListState,
    speedDialListState: androidx.compose.foundation.lazy.LazyListState,
    onRequestPermissions: (Array<String>) -> Unit,
    onSelectPage: (Int) -> Unit,
) {
    val page = pagerState.currentPage
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .metroNavBarPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            MetroAppTitle(title = stringResource(R.string.app_name))
            MetroHubTitleRow(
                titles = listOf(
                    stringResource(R.string.pivot_history),
                    stringResource(R.string.pivot_speed_dial),
                    stringResource(R.string.pivot_voicemail),
                ),
                selectedIndex = page,
                mode = MetroHubTitleMode.Pivot,
                onTitleClick = onSelectPage,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                beyondViewportPageCount = 1,
            ) { index ->
                when (index) {
                    0 -> HistoryScreen(
                        state = state,
                        listState = historyListState,
                        onRequestPermissions = onRequestPermissions,
                    )
                    1 -> SpeedDialScreen(
                        state = state,
                        listState = speedDialListState,
                    )
                    else -> VoicemailScreen(state = state)
                }
            }
        }

        RootAppBar(state = state, page = page)
    }
}

@Composable
private fun BoxScope.RootAppBar(state: DialerViewModel, page: Int) {
    if (state.selectionMode) {
        MetroAppBar(
            icons = listOf(
                MetroAppBarIcon(
                    type = MetroSystemIconType.Delete,
                    label = stringResource(R.string.delete),
                    onClick = state::deleteSelected,
                ),
                MetroAppBarIcon(
                    type = MetroSystemIconType.SelectAll,
                    label = stringResource(R.string.select_all),
                    onClick = state::selectAll,
                ),
                MetroAppBarIcon(
                    type = MetroSystemIconType.Close,
                    label = stringResource(R.string.cancel),
                    onClick = state::exitSelectionMode,
                ),
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        )
        return
    }

    val icons = when (page) {
        1 -> listOf(
            MetroAppBarIcon(
                type = MetroSystemIconType.Add,
                label = stringResource(R.string.add),
                onClick = state::openSpeedDialAdd,
            ),
            MetroAppBarIcon(
                type = MetroSystemIconType.People,
                label = stringResource(R.string.people),
                onClick = state::launchPeople,
            ),
        )
        else -> listOf(
            MetroAppBarIcon(
                type = MetroSystemIconType.DialPad,
                label = stringResource(R.string.dial_pad),
                onClick = state::openDialPad,
            ),
            MetroAppBarIcon(
                type = MetroSystemIconType.People,
                label = stringResource(R.string.people),
                onClick = state::launchPeople,
            ),
            MetroAppBarIcon(
                type = MetroSystemIconType.Search,
                label = stringResource(R.string.search),
                onClick = state::toggleSearch,
            ),
        )
    }

    val menuItems = buildList {
        add(
            MetroAppBarMenuItem(
                text = stringResource(R.string.select_calls),
                onClick = state::enterSelectionMode,
            ),
        )
        add(
            MetroAppBarMenuItem(
                text = stringResource(R.string.settings),
                onClick = state::openPhoneSettings,
            ),
        )
    }

    MetroAppBar(
        icons = icons,
        menuItems = menuItems,
        enterKey = page,
        modifier = Modifier.align(Alignment.BottomCenter),
    )
}

@Composable
private fun SimChooserDialog(
    chooser: SimChooserState,
    onSelect: (SimOption) -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)
    MetroMessageDialog(
        title = stringResource(R.string.call_using),
        onDismissRequest = onDismiss,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            chooser.options.forEach { option ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelect(option) },
                        )
                        .padding(vertical = 12.dp),
                ) {
                    Column {
                        MetroText(text = option.label, style = MetroTextStyle.ListItemTitle)
                        MetroText(
                            text = com.metro.dialer.data.DialerCallLogic.formatDisplayNumber(chooser.number),
                            style = MetroTextStyle.ListItemSubtitle,
                            color = MetroTheme.colors.secondaryText,
                        )
                    }
                }
            }
        }
    }
}

