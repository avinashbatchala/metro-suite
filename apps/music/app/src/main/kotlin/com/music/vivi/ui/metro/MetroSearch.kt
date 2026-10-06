/**
 * Music (MetroSuite port of the Vivi Music engine)
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.metro

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroContextMenuClearOnDismiss
import com.metro.ui.MetroContextMenuItem
import com.metro.ui.MetroContextMenuPopup
import com.metro.ui.MetroDimens
import com.metro.ui.MetroEmptyState
import com.metro.ui.MetroLoadingDots
import com.metro.ui.MetroPivot
import com.metro.ui.MetroTextBox
import com.metro.ui.MetroTheme
import com.music.innertube.YouTube
import com.music.innertube.YouTube.SearchFilter.Companion.FILTER_ALBUM
import com.music.innertube.YouTube.SearchFilter.Companion.FILTER_ARTIST
import com.music.innertube.YouTube.SearchFilter.Companion.FILTER_COMMUNITY_PLAYLIST
import com.music.innertube.YouTube.SearchFilter.Companion.FILTER_FEATURED_PLAYLIST
import com.music.innertube.YouTube.SearchFilter.Companion.FILTER_SONG
import com.music.innertube.YouTube.SearchFilter.Companion.FILTER_VIDEO
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import com.music.innertube.models.YTItem
import com.music.vivi.extensions.toMediaItem
import com.music.vivi.models.toMediaMetadata
import com.music.vivi.playback.PlayerConnection
import com.music.vivi.playback.queues.YouTubeAlbumRadio
import com.music.vivi.playback.queues.YouTubePlaylistQueue
import com.music.vivi.playback.queues.YouTubeQueue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Search pivot definitions, mirroring the suite collection screen: one tab per result type.
 * "playlists" combines YouTube's featured + community playlist filters.
 */
private data class SearchTab(val title: String, val filters: List<YouTube.SearchFilter>)

private val SearchTabs = listOf(
    SearchTab("songs", listOf(FILTER_SONG)),
    SearchTab("albums", listOf(FILTER_ALBUM)),
    SearchTab("artists", listOf(FILTER_ARTIST)),
    SearchTab("playlists", listOf(FILTER_FEATURED_PLAYLIST, FILTER_COMMUNITY_PLAYLIST)),
    SearchTab("videos", listOf(FILTER_VIDEO)),
)

private const val SearchDebounceMs = 400L

/**
 * Comprehensive YouTube Music search: a search field on top and pivot tabs for
 * songs / albums / artists / playlists / videos.
 */
@Composable
internal fun SearchScreen(playerConnection: PlayerConnection?) {
    var query by remember { mutableStateOf("") }
    var debounced by remember { mutableStateOf("") }
    val pagerState = rememberPagerState(pageCount = { SearchTabs.size })
    val scope = rememberCoroutineScope()

    val results = remember { mutableStateMapOf<Int, List<YTItem>>() }
    val loading = remember { mutableStateMapOf<Int, Boolean>() }
    val loaded = remember { mutableStateMapOf<Int, Boolean>() }
    val continuation = remember { mutableStateMapOf<Int, String?>() }

    val menu = remember { OnlineMenuState() }

    suspend fun load(page: Int, q: String) {
        loading[page] = true
        val tab = SearchTabs[page]
        val collected = mutableListOf<YTItem>()
        var nextContinuation: String? = null
        for (filter in tab.filters) {
            YouTube.search(q, filter).onSuccess { result ->
                collected += result.items
                if (nextContinuation == null) nextContinuation = result.continuation
            }
        }
        if (q == debounced) {
            results[page] = collected.distinctBy { it.id }
            continuation[page] = nextContinuation
            loaded[page] = true
        }
        loading[page] = false
    }

    suspend fun loadMore(page: Int) {
        val token = continuation[page] ?: return
        if (loading[page] == true) return
        loading[page] = true
        val q = debounced
        YouTube.searchContinuation(token).onSuccess { result ->
            if (q == debounced && q.isNotEmpty()) {
                val merged = (results[page].orEmpty() + result.items).distinctBy { it.id }
                results[page] = merged
                continuation[page] = result.continuation
            }
        }
        loading[page] = false
    }

    LaunchedEffect(query) {
        delay(SearchDebounceMs)
        val trimmed = query.trim()
        if (trimmed == debounced) return@LaunchedEffect
        debounced = trimmed
        results.clear()
        loading.clear()
        loaded.clear()
        continuation.clear()
        if (trimmed.isNotEmpty()) {
            load(pagerState.currentPage, trimmed)
        }
    }

    LaunchedEffect(pagerState.currentPage, debounced) {
        val q = debounced
        if (q.isNotEmpty() && loaded[pagerState.currentPage] != true && loading[pagerState.currentPage] != true) {
            load(pagerState.currentPage, q)
        }
    }

    OnlineMenuHost(state = menu, playerConnection = playerConnection) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MetroTheme.colors.background),
        ) {
            MetroPageTitle("search")
            MetroTextBox(
                value = query,
                onValueChange = { query = it },
                placeholder = "search youtube music",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MetroDimens.ScreenHorizontalMargin, vertical = 8.dp),
            )
            if (debounced.isBlank()) {
                MetroEmptyState("Search YouTube Music for songs, albums, artists, playlists and videos.")
            } else {
                MetroPivot(
                    titles = SearchTabs.map { it.title },
                    pagerState = pagerState,
                    modifier = Modifier.weight(1f),
                    onTitleClick = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
                    pageContent = { page ->
                        SearchResultsPane(
                            items = results[page].orEmpty(),
                            isLoading = loading[page] == true,
                            hasMore = continuation[page] != null,
                            onLoadMore = { scope.launch { loadMore(page) } },
                            playerConnection = playerConnection,
                            onLongPressSong = { item, bounds -> menu.open(item, bounds) },
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun SearchResultsPane(
    items: List<YTItem>,
    isLoading: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    playerConnection: PlayerConnection?,
    onLongPressSong: (SongItem, Rect) -> Unit,
) {
    when {
        items.isEmpty() && isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                MetroLoadingDots()
            }
        }

        items.isEmpty() -> MetroEmptyState("No results.")

        else -> {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(items, key = { it.id }) { item ->
                    SearchRow(
                        item = item,
                        playerConnection = playerConnection,
                        onLongPressSong = onLongPressSong,
                    )
                }
                if (hasMore) {
                    item(key = "load-more") {
                        LaunchedEffect(items.size) { onLoadMore() }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = androidx.compose.ui.Alignment.Center,
                        ) {
                            MetroLoadingDots()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchRow(
    item: YTItem,
    playerConnection: PlayerConnection?,
    onLongPressSong: (SongItem, Rect) -> Unit,
) {
    var bounds by remember { mutableStateOf(Rect.Zero) }

    val subtitle: String? = when (item) {
        is SongItem -> buildList {
            add(item.artists.joinToString { it.name })
            item.album?.name?.let { add(it) }
        }.filter { it.isNotBlank() }.joinToString(" · ").ifBlank { null }

        is AlbumItem -> buildList {
            item.artists?.joinToString { it.name }?.let { add(it) }
            item.year?.let { add(it.toString()) }
        }.filter { !it.isNullOrBlank() }.joinToString(" · ").ifBlank { null }

        is ArtistItem -> item.subtext?.ifBlank { null } ?: "artist"

        is PlaylistItem -> buildList {
            item.author?.name?.let { add(it) }
            item.songCountText?.let { add(it) }
        }.filter { !it.isNullOrBlank() }.joinToString(" · ").ifBlank { null }
    }

    val onClick: () -> Unit = {
        when (item) {
            is SongItem -> playerConnection?.playQueue(
                YouTubeQueue(WatchEndpoint(videoId = item.id), item.toMediaMetadata()),
            )

            is AlbumItem -> playerConnection?.playQueue(YouTubeAlbumRadio(item.playlistId))

            is PlaylistItem -> playerConnection?.playQueue(
                YouTubePlaylistQueue(item.id, item.title),
            )

            is ArtistItem -> (item.playEndpoint ?: item.radioEndpoint)?.let { endpoint ->
                playerConnection?.playQueue(YouTubeQueue(endpoint))
            }
        }
    }

    MusicListRow(
        title = item.title,
        subtitle = subtitle,
        onClick = onClick,
        onLongClick = if (item is SongItem) ({ onLongPressSong(item, bounds) }) else null,
        modifier = Modifier.onGloballyPositioned { bounds = it.boundsInWindow() },
        leading = { AlbumArtThumbnail(model = item.thumbnail, contentDescription = null) },
    )
}

// ---------------------------------------------------------------------------------------------
// Long-press menu for an online song / video result
// ---------------------------------------------------------------------------------------------

internal class OnlineMenuState {
    var song by mutableStateOf<SongItem?>(null)
        private set
    var anchorBounds by mutableStateOf(Rect.Zero)
        private set
    var rootBounds by mutableStateOf(Rect.Zero)
        private set
    val visibility = MutableTransitionState(false)

    internal fun open(target: SongItem, anchor: Rect) {
        song = target
        anchorBounds = anchor
        visibility.targetState = true
    }

    internal fun dismiss() {
        visibility.targetState = false
    }

    internal fun updateRootBounds(bounds: Rect) {
        rootBounds = bounds
    }

    internal fun clear() {
        song = null
    }
}

@Composable
private fun OnlineMenuHost(
    state: OnlineMenuState,
    playerConnection: PlayerConnection?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { state.updateRootBounds(it.boundsInWindow()) },
    ) {
        content()
    }

    MetroContextMenuClearOnDismiss(state.visibility) { state.clear() }

    val target = state.song
    if (target != null) {
        val mediaItem = target.toMediaItem()
        MetroContextMenuPopup(
            visibleState = state.visibility,
            anchorBounds = state.anchorBounds,
            rootBounds = state.rootBounds,
            items = listOf(
                MetroContextMenuItem(label = "play next") {
                    playerConnection?.playNext(listOf(mediaItem))
                    state.dismiss()
                },
                MetroContextMenuItem(label = "add to queue") {
                    playerConnection?.addToQueue(listOf(mediaItem))
                    state.dismiss()
                },
                MetroContextMenuItem(label = "download") {
                    startSongDownload(context, target.id, target.title)
                    state.dismiss()
                },
            ),
            onDismissRequest = { state.dismiss() },
        )
    }
}
