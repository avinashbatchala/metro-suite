package com.metro.people.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.people.R
import com.metro.ui.LocalMetroSubpageExit
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroAppBarMenuItem
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroColors
import com.metro.ui.MetroEmptyState
import com.metro.ui.MetroJumpList
import com.metro.ui.MetroPanorama
import com.metro.ui.MetroSubpageHost
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroNavBarPadding
import kotlinx.coroutines.launch

private val SearchFieldRowHeight = 48.dp
private val SearchFieldBorderWidth = 3.dp
private val SearchFieldHorizontalPadding = 10.dp
private val SearchFieldBottomSpacing = 8.dp

/**
 * WP8.1 People Hub: the `contacts · what's new · rooms` sections over a horizontal panorama,
 * with the contact card / settings drill-ins.
 */
@Composable
fun PeopleShell(
    state: PeopleState,
    onImportContacts: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val generation = state.generation
    @Suppress("UNUSED_VARIABLE")
    val observeState = generation

    var scrollToLetter by remember { mutableStateOf<Char?>(null) }
    val searching = state.searchVisible
    val sections = listOf(
        stringResource(R.string.section_contacts),
        stringResource(R.string.section_whats_new),
        stringResource(R.string.section_rooms),
    )
    val pagerState = rememberPagerState(pageCount = { sections.size })
    val scope = rememberCoroutineScope()

    BackHandler(enabled = searching && state.route == PeopleRoute.Hub) {
        state.dismissSearch()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .metroNavBarPadding(),
    ) {
        MetroSubpageHost(
            route = state.route,
            isRoot = { it == PeopleRoute.Hub },
            parentOf = { PeopleRoute.Hub },
            loadKeyOf = { subpageLoadKey(it) },
            onGoBack = state::closeOverlay,
            modifier = Modifier.fillMaxSize(),
            rootContent = {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (searching) {
                        MetroAppTitle(title = stringResource(R.string.people_search))
                        ContactSearchBar(
                            query = state.searchQuery,
                            onQueryChange = state::updateSearchQuery,
                            modifier = Modifier.padding(
                                horizontal = 12.dp,
                                vertical = SearchFieldBottomSpacing,
                            ),
                        )
                        AllPane(
                            filterLabel = state.filterLabel,
                            grouped = emptyMap(),
                            flatContacts = state.visibleContacts,
                            searchActive = true,
                            onFilterClick = state::openFilter,
                            onJumpClick = {},
                            onOpenDetail = { state.openDetail(it.id) },
                            onCall = state::callContact,
                            onAddToSpeedDial = state::addToSpeedDial,
                            onPinToStart = state::pinToStart,
                            scrollToLetter = null,
                            onScrollConsumed = {},
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        MetroPanorama(
                            titles = sections,
                            pagerState = pagerState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = MetroAppBarDefaults.BarHeight),
                            onTitleClick = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
                            pageContent = { page ->
                                when (page) {
                                    0 -> AllPane(
                                        filterLabel = state.filterLabel,
                                        grouped = state.groupedContacts,
                                        flatContacts = state.visibleContacts,
                                        searchActive = false,
                                        onFilterClick = state::openFilter,
                                        onJumpClick = state::toggleJumpList,
                                        onOpenDetail = { state.openDetail(it.id) },
                                        onCall = state::callContact,
                                        onAddToSpeedDial = state::addToSpeedDial,
                                        onPinToStart = state::pinToStart,
                                        scrollToLetter = scrollToLetter,
                                        onScrollConsumed = { scrollToLetter = null },
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                    1 -> WhatsNewPane()
                                    else -> RoomsPane()
                                }
                            },
                        )
                    }
                }
            },
            subpageContent = { route ->
                val requestExit = LocalMetroSubpageExit.current
                val onBack = { requestExit?.invoke() }
                when (route) {
                    PeopleRoute.Filter -> FilterScreen(
                        initial = state.filter,
                        accounts = state.knownAccounts(),
                        onSave = { filter ->
                            state.saveFilter(filter)
                            onBack()
                        },
                        onCancel = { onBack() },
                    )
                    PeopleRoute.Settings -> PeopleSettingsScreen(
                        state = state,
                        onBack = { onBack() },
                        onAddContacts = { state.openAddContacts() },
                        onImportContacts = onImportContacts,
                    )
                    PeopleRoute.AddContacts -> AddContactsScreen(
                        onBack = { onBack() },
                        onAddAccount = { state.addAccount() },
                        onImportContacts = onImportContacts,
                    )
                    is PeopleRoute.Detail -> {
                        state.selectedDetail?.let { detail ->
                            ContactDetailScreen(
                                detail = detail,
                                onBack = { onBack() },
                                onCall = { state.callContact(detail.summary) },
                                onText = { state.textContact(detail.summary) },
                                onWhatsAppCall = {
                                    detail.whatsApp?.let(state::whatsAppCall)
                                },
                                onWhatsAppText = {
                                    detail.whatsApp?.let(state::whatsAppText)
                                },
                                onEmail = state::emailContact,
                                onPin = { state.pinToStart(detail.summary) },
                            )
                        }
                    }
                    PeopleRoute.Import -> ImportScreen(
                        state = state,
                        onBack = { onBack() },
                        modifier = Modifier.fillMaxSize(),
                    )
                    PeopleRoute.Hub -> Unit
                }
            },
        )

        val appBarVisible = state.route == PeopleRoute.Hub && !searching
        MetroAppBar(
            visible = appBarVisible,
            icons = listOf(
                MetroAppBarIcon(
                    type = MetroSystemIconType.Add,
                    label = stringResource(R.string.new_contact),
                    onClick = state::newContact,
                    contentDescription = stringResource(R.string.new_contact),
                ),
                MetroAppBarIcon(
                    type = MetroSystemIconType.Search,
                    label = stringResource(R.string.people_search),
                    onClick = state::openSearch,
                    contentDescription = stringResource(R.string.search_contacts),
                ),
            ),
            menuItems = listOf(
                MetroAppBarMenuItem(
                    text = stringResource(R.string.settings),
                    onClick = state::openSettings,
                ),
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        if (state.jumpListVisible && !searching && state.route == PeopleRoute.Hub) {
            MetroJumpList(
                activeLetters = state.groupedContacts.keys,
                onLetterSelected = { scrollToLetter = it },
                onDismiss = state::dismissJumpList,
            )
        }

        state.statusMessage?.let { message ->
            LaunchedEffect(state.generation) {
                kotlinx.coroutines.delay(4000)
                state.clearStatus()
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = MetroAppBarDefaults.BarHeight)
                    .background(MetroColors.DarkSecondarySurface)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                MetroText(
                    text = message,
                    style = MetroTextStyle.Body,
                    color = MetroTheme.colors.primaryText,
                )
            }
        }
    }
}

@Composable
private fun WhatsNewPane() {
    MetroEmptyState(message = stringResource(R.string.whats_new_empty))
}

@Composable
private fun RoomsPane() {
    MetroEmptyState(message = stringResource(R.string.rooms_empty))
}

@Composable
private fun ContactSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val accent = MetroTheme.colors.accent

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    // WP8.1 messaging/people search: white fill, accent border, black text at top of page.
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .height(SearchFieldRowHeight)
            .background(MetroColors.LightBackground, RectangleShape)
            .border(SearchFieldBorderWidth, accent, RectangleShape)
            .focusRequester(focusRequester)
            .padding(horizontal = SearchFieldHorizontalPadding),
        textStyle = MetroTextStyle.ListItemTitle.toTextStyle().copy(
            color = MetroColors.LightPrimaryText,
        ),
        cursorBrush = SolidColor(accent),
        singleLine = true,
        decorationBox = { inner ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (query.isEmpty()) {
                    MetroText(
                        text = stringResource(R.string.search_contacts),
                        style = MetroTextStyle.ListItemSubtitle,
                        color = MetroColors.LightPrimaryText.copy(alpha = 0.5f),
                    )
                }
                inner()
            }
        },
    )
}

private fun subpageLoadKey(route: PeopleRoute): Any = when (route) {
    PeopleRoute.Filter -> "Filter"
    PeopleRoute.Settings -> "Settings"
    PeopleRoute.AddContacts -> "AddContacts"
    is PeopleRoute.Detail -> "Detail:${route.contactId}"
    PeopleRoute.Import -> "Import"
    PeopleRoute.Hub -> "Hub"
}
