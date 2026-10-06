/**
 * Music (MetroSuite port of the Vivi Music engine)
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.metro

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.metro.ui.MetroTextBox
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroClickable
import com.music.innertube.YouTube
import com.music.innertube.YouTube.SearchFilter.Companion.FILTER_ALBUM
import com.music.innertube.YouTube.SearchFilter.Companion.FILTER_ARTIST
import com.music.innertube.YouTube.SearchFilter.Companion.FILTER_COMMUNITY_PLAYLIST
import com.music.innertube.YouTube.SearchFilter.Companion.FILTER_FEATURED_PLAYLIST
import com.music.innertube.YouTube.SearchFilter.Companion.FILTER_SONG
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import com.music.innertube.models.YTItem
import com.music.vivi.db.MusicDatabase
import com.music.vivi.db.entities.Album
import com.music.vivi.db.entities.Artist
import com.music.vivi.db.entities.Playlist
import com.music.vivi.db.entities.Song
import com.music.vivi.extensions.toMediaItem
import com.music.vivi.models.toMediaMetadata
import com.music.vivi.playback.PlayerConnection
import com.music.vivi.playback.queues.ListQueue
import com.music.vivi.playback.queues.YouTubeQueue
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private enum class SearchFilter(val label: String) {
    All("all"),
    Songs("songs"),
    Albums("albums"),
    Artists("artists"),
    Playlists("playlists"),
}

private const val SearchDebounceMs = 400L

private data class LocalResults(
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
) {
    val isEmpty: Boolean get() = songs.isEmpty() && albums.isEmpty() && artists.isEmpty() && playlists.isEmpty()
}

/**
 * One coherent search over the user's Collection and the online catalogue. A single type selector
 * (`all · songs · albums · artists · playlists`); results are merged with local first. Videos are
 * intentionally not a primary Music pivot.
 */
@Composable
internal fun SearchScreen(
    database: MusicDatabase,
    playerConnection: PlayerConnection?,
    onBack: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var debounced by remember { mutableStateOf("") }
    var filter by remember { mutableIntStateOf(0) }

    var localResults by remember { mutableStateOf(LocalResults()) }
    var onlineItems by remember { mutableStateOf<List<YTItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val menu = remember { OnlineMenuState() }

    LaunchedEffect(query, filter) {
        delay(SearchDebounceMs)
        val q = query.trim()
        if (q.isEmpty()) {
            localResults = LocalResults()
            onlineItems = emptyList()
            debounced = q
            return@LaunchedEffect
        }
        debounced = q
        loading = true

        localResults = LocalResults(
            songs = database.searchSongs(q).first(),
            albums = database.searchAlbums(q).first(),
            artists = database.searchArtists(q).first(),
            playlists = database.searchPlaylists(q).first(),
        )

        val collected = mutableListOf<YTItem>()
        val filters = when (SearchFilter.entries[filter]) {
            SearchFilter.All -> listOf(
                FILTER_SONG, FILTER_ALBUM, FILTER_ARTIST,
                FILTER_FEATURED_PLAYLIST, FILTER_COMMUNITY_PLAYLIST,
            )
            SearchFilter.Songs -> listOf(FILTER_SONG)
            SearchFilter.Albums -> listOf(FILTER_ALBUM)
            SearchFilter.Artists -> listOf(FILTER_ARTIST)
            SearchFilter.Playlists -> listOf(FILTER_FEATURED_PLAYLIST, FILTER_COMMUNITY_PLAYLIST)
        }
        for (f in filters) {
            YouTube.search(q, f).onSuccess { collected += it.items }
        }
        if (q == debounced) {
            onlineItems = collected.distinctBy { it.id }
        }
        loading = false
    }

    OnlineMenuHost(state = menu, playerConnection = playerConnection) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MetroTheme.colors.background),
        ) {
            MetroText(
                text = "\u2039",
                style = MetroTextStyle.ListItemTitle,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier
                    .metroClickable(onClick = onBack)
                    .padding(start = MetroDimens.ScreenHorizontalMargin, top = 8.dp),
            )
            MetroText(
                text = "search",
                style = MetroTextStyle.PageTitle,
                modifier = Modifier.padding(
                    start = MetroDimens.ScreenHorizontalMargin,
                    bottom = 4.dp,
                ),
            )
            MetroTextBox(
                value = query,
                onValueChange = { query = it },
                placeholder = "search",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MetroDimens.ScreenHorizontalMargin, vertical = 8.dp),
            )
            SearchFilterTabs(selected = filter, onSelect = { filter = it })

            when {
                debounced.isBlank() -> MetroEmptyState("Search your collection and the online catalogue.")
                localResults.isEmpty && onlineItems.isEmpty() && loading -> LoadingRow()
                localResults.isEmpty && onlineItems.isEmpty() -> MetroEmptyState("No results.")
                else -> {
                    val selected = SearchFilter.entries[filter]
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        if (!localResults.isEmpty) {
                            item(key = "local-header") { SectionLabel("in your collection") }
                            if (selected == SearchFilter.All || selected == SearchFilter.Songs) {
                                items(localResults.songs, key = { "ls-${it.id}" }) { song ->
                                    SongListRow(
                                        song = song,
                                        onClick = {
                                            playerConnection?.playQueue(
                                                ListQueue(
                                                    title = "search",
                                                    items = localResults.songs.map { it.toMediaItem() },
                                                    startIndex = localResults.songs.indexOf(song).coerceAtLeast(0),
                                                ),
                                            )
                                        },
                                        leading = { AlbumArtThumbnail(model = song.thumbnailUrl, contentDescription = null) },
                                    )
                                }
                            }
                            if (selected == SearchFilter.All || selected == SearchFilter.Albums) {
                                items(localResults.albums, key = { "la-${it.id}" }) { album ->
                                    MusicListRow(
                                        title = album.title,
                                        subtitle = album.artists.joinToString { it.name }.ifBlank { null },
                                        leading = { AlbumArtThumbnail(model = album.thumbnailUrl, contentDescription = null) },
                                        onClick = { onOpenAlbum(album.id) },
                                    )
                                }
                            }
                            if (selected == SearchFilter.All || selected == SearchFilter.Artists) {
                                items(localResults.artists, key = { "lr-${it.id}" }) { artist ->
                                    MusicListRow(
                                        title = artist.title,
                                        onClick = { onOpenArtist(artist.id) },
                                    )
                                }
                            }
                            if (selected == SearchFilter.All || selected == SearchFilter.Playlists) {
                                items(localResults.playlists, key = { "lp-${it.id}" }) { playlist ->
                                    MusicListRow(
                                        title = playlist.title,
                                        onClick = { onOpenPlaylist(playlist.id) },
                                    )
                                }
                            }
                        }

                        if (onlineItems.isNotEmpty()) {
                            item(key = "online-header") { SectionLabel("online results") }
                            items(onlineItems, key = { "on-${it.id}" }) { item ->
                                OnlineSearchRow(
                                    item = item,
                                    playerConnection = playerConnection,
                                    menu = menu,
                                    onOpenAlbum = onOpenAlbum,
                                    onOpenArtist = onOpenArtist,
                                    onOpenPlaylist = onOpenPlaylist,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchFilterTabs(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MetroDimens.ScreenHorizontalMargin, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        SearchFilter.entries.forEachIndexed { index, entry ->
            MetroText(
                text = entry.label,
                style = MetroTextStyle.ListItemTitle,
                color = if (index == selected) MetroTheme.colors.accent else MetroTheme.colors.secondaryText,
                modifier = Modifier.metroClickable { onSelect(index) },
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    MetroText(
        text = text,
        style = MetroTextStyle.SectionHeader,
        color = MetroTheme.colors.accent,
        modifier = Modifier.padding(
            start = MetroDimens.ScreenHorizontalMargin,
            top = 12.dp,
            bottom = 4.dp,
        ),
    )
}

@Composable
private fun LoadingRow() {
    Box(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
        MetroLoadingDots()
    }
}

@Composable
private fun OnlineSearchRow(
    item: YTItem,
    playerConnection: PlayerConnection?,
    menu: OnlineMenuState,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
) {
    var bounds by remember { mutableStateOf(Rect.Zero) }
    val subtitle: String? = when (item) {
        is SongItem -> item.artists.joinToString { it.name }.ifBlank { null }
        is AlbumItem -> item.artists?.joinToString { it.name }?.ifBlank { null }
        is ArtistItem -> item.subtext?.ifBlank { null }
        is PlaylistItem -> item.songCountText
    }
    val onClick: () -> Unit = {
        when (item) {
            is SongItem -> playerConnection?.playQueue(
                YouTubeQueue(WatchEndpoint(videoId = item.id), item.toMediaMetadata()),
            )
            is AlbumItem -> onOpenAlbum(item.browseId)
            is ArtistItem -> onOpenArtist(item.id)
            is PlaylistItem -> onOpenPlaylist(item.id)
        }
    }
    MusicListRow(
        title = item.title,
        subtitle = subtitle,
        onClick = onClick,
        onLongClick = if (item is SongItem) ({ menu.open(item, bounds) }) else null,
        modifier = Modifier.onGloballyPositioned { bounds = it.boundsInWindow() },
        leading = { AlbumArtThumbnail(model = item.thumbnail, contentDescription = null) },
    )
}

// ---------------------------------------------------------------------------------------------
// Long-press menu for an online song result
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
internal fun OnlineMenuHost(
    state: OnlineMenuState,
    playerConnection: PlayerConnection?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val database = com.music.vivi.LocalDatabase.current
    val addTo = rememberAddToState()

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
                MetroContextMenuItem(label = "add to…") {
                    addTo.open(
                        AddToTarget(
                            title = target.title,
                            songIds = listOf(target.id),
                            persist = { db -> db.query { insert(target.toMediaMetadata()) } },
                        ),
                    )
                    state.dismiss()
                },
                MetroContextMenuItem(label = "add to collection") {
                    database.query { insert(target.toMediaMetadata()) { it.toggleLibrary() } }
                    state.dismiss()
                },
                MetroContextMenuItem(label = "download") {
                    startSongDownload(context, target.id, target.title)
                    state.dismiss()
                },
                MetroContextMenuItem(label = "start radio") {
                    playerConnection?.playQueue(YouTubeQueue(WatchEndpoint(videoId = target.id)))
                    state.dismiss()
                },
            ),
            onDismissRequest = { state.dismiss() },
        )
    }

    AddToHost(state = addTo, database = database)
}
