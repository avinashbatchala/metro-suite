/**
 * Music (MetroSuite port of the Vivi Music engine)
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.metro

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroAppBarMenuItem
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroColors
import com.metro.ui.MetroDimens
import com.metro.ui.MetroEmptyState
import com.metro.ui.MetroLetterTile
import com.metro.ui.MetroListItem
import com.metro.ui.MetroMediaGlyph
import com.metro.ui.MetroMediaGlyphIcon
import com.metro.ui.MetroPanorama
import com.metro.ui.MetroSubpageHost
import com.metro.ui.MetroSystemIcon
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.MetroToggleSwitch
import com.metro.ui.metroClickable
import com.metro.ui.metroNavBarPadding
import com.metro.ui.metroStickyLetterHeader
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalDownloadUtil
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.db.entities.Album
import com.music.vivi.db.entities.Artist
import com.music.vivi.db.entities.EventWithSong
import com.music.vivi.db.entities.Playlist
import com.music.vivi.db.entities.PlaylistEntity
import com.music.vivi.db.entities.Song
import com.music.vivi.extensions.toMediaItem
import com.music.vivi.playback.PlayerConnection
import com.music.vivi.playback.queues.ListQueue
import com.music.vivi.playback.queues.LocalAlbumRadio
import com.music.vivi.viewmodels.LibraryAlbumsViewModel
import com.music.vivi.viewmodels.LibraryArtistsViewModel
import com.music.vivi.viewmodels.LibraryPlaylistsViewModel
import com.music.vivi.viewmodels.LibrarySongsViewModel
import com.music.vivi.viewmodels.MoodAndGenresViewModel
import com.music.vivi.viewmodels.NewReleaseViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Dense Xbox Music list rows. */
private val RowVerticalPadding = 4.dp
private val RowOneLineMinHeight = 48.dp
private val RowTwoLineMinHeight = 60.dp

internal val AlbumArtThumbnailSize = 56.dp

/**
 * Mature WP8.1 Music shell: a real in-app navigation stack over a four-pane root panorama
 * (`recent plays · collection · get music · now playing`). One root brand: `music`.
 */
@Composable
fun MetroMusicApp(onExit: () -> Unit = {}) {
    val playerConnection = LocalPlayerConnection.current
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current

    val stack = remember { mutableStateListOf<MetroRoute>(MetroRoute.Hub) }
    val current = stack.last()
    val push: (MetroRoute) -> Unit = { route -> stack.add(route) }
    val pop: () -> Unit = { if (stack.size > 1) stack.removeAt(stack.lastIndex) }

    // Persist the root panorama pane so returning from a subpage lands back on it (e.g. Now Playing).
    var hubPage by rememberSaveable { mutableIntStateOf(0) }

    BackHandler(enabled = true) {
        if (stack.size > 1) pop() else onExit()
    }

    Box(modifier = Modifier.fillMaxSize().statusBarsPadding().metroNavBarPadding()) {
        MetroSubpageHost(
            route = current,
            isRoot = { it is MetroRoute.Hub },
            parentOf = { stack.getOrNull(stack.lastIndex - 1) ?: MetroRoute.Hub },
            loadKeyOf = { it.loadKey() },
            onGoBack = pop,
            modifier = Modifier.fillMaxSize(),
            rootContent = {
                HubPanorama(
                    playerConnection = playerConnection,
                    database = database,
                    initialPage = hubPage,
                    onPageChange = { hubPage = it },
                    onOpen = push,
                )
            },
            subpageContent = { route ->
                val requestExit = com.metro.ui.LocalMetroSubpageExit.current
                val onBack = { requestExit?.invoke() ?: pop() }
                when (route) {
                    MetroRoute.Hub -> Unit

                    // Collection categories
                    MetroRoute.CollectionArtists -> CollectionArtistsScreen(
                        playerConnection = playerConnection,
                        onBack = onBack,
                        onOpenArtist = { push(MetroRoute.ArtistDetail(it)) },
                        onSearch = { push(MetroRoute.Search) },
                    )
                    MetroRoute.CollectionAlbums -> CollectionAlbumsScreen(
                        playerConnection = playerConnection,
                        onBack = onBack,
                        onOpenAlbum = { push(MetroRoute.AlbumDetail(it)) },
                        onSearch = { push(MetroRoute.Search) },
                    )
                    MetroRoute.CollectionSongs -> CollectionSongsScreen(
                        playerConnection = playerConnection,
                        onBack = onBack,
                        onSearch = { push(MetroRoute.Search) },
                    )
                    MetroRoute.CollectionGenres -> GenresScreen(
                        onBack = onBack,
                        onOpenStation = { id, title -> push(MetroRoute.GenreStation(id, title)) },
                        onSearch = { push(MetroRoute.Search) },
                    )
                    MetroRoute.CollectionPlaylists -> CollectionPlaylistsScreen(
                        playerConnection = playerConnection,
                        onBack = onBack,
                        onOpenPlaylist = { push(MetroRoute.PlaylistDetail(it)) },
                        onNewPlaylist = { push(MetroRoute.NewPlaylist) },
                        onSearch = { push(MetroRoute.Search) },
                    )
                    MetroRoute.CollectionRadio -> RadioScreen(
                        database = database,
                        playerConnection = playerConnection,
                        onBack = onBack,
                        onOpenAlbum = { push(MetroRoute.AlbumDetail(it)) },
                    )

                    // Discovery
                    MetroRoute.GetMusicNewReleases -> NewReleasesScreen(
                        onBack = onBack,
                        onOpenAlbum = { push(MetroRoute.OnlineAlbumDetail(it)) },
                    )

                    MetroRoute.Search -> SearchScreen(
                        database = database,
                        playerConnection = playerConnection,
                        onBack = onBack,
                        onOpenAlbum = { push(MetroRoute.OnlineAlbumDetail(it)) },
                        onOpenArtist = { push(MetroRoute.OnlineArtistDetail(it)) },
                        onOpenPlaylist = { push(MetroRoute.OnlinePlaylistDetail(it)) },
                    )
                    MetroRoute.Queue -> QueueScreen(playerConnection = playerConnection)
                    MetroRoute.Downloads -> DownloadsScreen(database = database, downloaded = downloadUtil)
                    MetroRoute.Settings -> SettingsScreen(
                        onOpenDownloads = { push(MetroRoute.Downloads) },
                        onOpenRecognition = { push(MetroRoute.Recognition) },
                        onOpenEq = { push(MetroRoute.Eq) },
                    )
                    MetroRoute.Recognition -> RecognitionScreen()
                    MetroRoute.Eq -> EqScreen()
                    MetroRoute.NewPlaylist -> NewPlaylistScreen(
                        database = database,
                        onBack = onBack,
                    )

                    // Details
                    is MetroRoute.AlbumDetail -> AlbumDetailScreen(
                        database = database,
                        playerConnection = playerConnection,
                        albumId = route.id,
                        onBack = onBack,
                        onOpenArtist = { push(MetroRoute.ArtistDetail(it)) },
                    )
                    is MetroRoute.ArtistDetail -> ArtistDetailScreen(
                        database = database,
                        playerConnection = playerConnection,
                        artistId = route.id,
                        onBack = onBack,
                        onOpenAlbum = { push(MetroRoute.AlbumDetail(it)) },
                        onOpenOnlineArtist = { push(MetroRoute.OnlineArtistDetail(it)) },
                    )
                    is MetroRoute.PlaylistDetail -> PlaylistDetailScreen(
                        database = database,
                        playerConnection = playerConnection,
                        playlistId = route.id,
                        onBack = onBack,
                        onOpenAlbum = { push(MetroRoute.AlbumDetail(it)) },
                        onOpenArtist = { push(MetroRoute.ArtistDetail(it)) },
                    )
                    is MetroRoute.GenreStation -> OnlinePlaylistDetailScreen(
                        playerConnection = playerConnection,
                        database = database,
                        playlistId = route.browseId,
                        onOpenAlbum = { push(MetroRoute.OnlineAlbumDetail(it)) },
                        onOpenArtist = { push(MetroRoute.OnlineArtistDetail(it)) },
                    )
                    is MetroRoute.OnlineAlbumDetail -> OnlineAlbumDetailScreen(
                        playerConnection = playerConnection,
                        database = database,
                        browseId = route.id,
                        onOpenArtist = { push(MetroRoute.OnlineArtistDetail(it)) },
                    )
                    is MetroRoute.OnlineArtistDetail -> OnlineArtistDetailScreen(
                        playerConnection = playerConnection,
                        database = database,
                        artistId = route.id,
                        onOpenAlbum = { push(MetroRoute.OnlineAlbumDetail(it)) },
                        onOpenArtist = { push(MetroRoute.OnlineArtistDetail(it)) },
                        onOpenPlaylist = { push(MetroRoute.OnlinePlaylistDetail(it)) },
                    )
                    is MetroRoute.OnlinePlaylistDetail -> OnlinePlaylistDetailScreen(
                        playerConnection = playerConnection,
                        database = database,
                        playlistId = route.id,
                        onOpenAlbum = { push(MetroRoute.OnlineAlbumDetail(it)) },
                        onOpenArtist = { push(MetroRoute.OnlineArtistDetail(it)) },
                    )
                }
            },
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Routes + stack keys
// ---------------------------------------------------------------------------------------------

sealed interface MetroRoute {
    data object Hub : MetroRoute
    data object CollectionArtists : MetroRoute
    data object CollectionAlbums : MetroRoute
    data object CollectionSongs : MetroRoute
    data object CollectionGenres : MetroRoute
    data object CollectionPlaylists : MetroRoute
    data object CollectionRadio : MetroRoute
    data object GetMusicNewReleases : MetroRoute
    data object Search : MetroRoute
    data object Queue : MetroRoute
    data object Downloads : MetroRoute
    data object Settings : MetroRoute
    data object Recognition : MetroRoute
    data object Eq : MetroRoute
    data object NewPlaylist : MetroRoute
    data class AlbumDetail(val id: String) : MetroRoute
    data class ArtistDetail(val id: String) : MetroRoute
    data class PlaylistDetail(val id: String) : MetroRoute
    data class GenreStation(val browseId: String, val title: String) : MetroRoute
    data class OnlineAlbumDetail(val id: String) : MetroRoute
    data class OnlineArtistDetail(val id: String) : MetroRoute
    data class OnlinePlaylistDetail(val id: String) : MetroRoute
}

private fun MetroRoute.loadKey(): Any = when (this) {
    MetroRoute.Hub -> "Hub"
    MetroRoute.CollectionArtists -> "CollectionArtists"
    MetroRoute.CollectionAlbums -> "CollectionAlbums"
    MetroRoute.CollectionSongs -> "CollectionSongs"
    MetroRoute.CollectionGenres -> "CollectionGenres"
    MetroRoute.CollectionPlaylists -> "CollectionPlaylists"
    MetroRoute.CollectionRadio -> "CollectionRadio"
    MetroRoute.GetMusicNewReleases -> "GetMusicNewReleases"
    MetroRoute.Search -> "Search"
    MetroRoute.Queue -> "Queue"
    MetroRoute.Downloads -> "Downloads"
    MetroRoute.Settings -> "Settings"
    MetroRoute.Recognition -> "Recognition"
    MetroRoute.Eq -> "Eq"
    MetroRoute.NewPlaylist -> "NewPlaylist"
    is MetroRoute.AlbumDetail -> "Album:$id"
    is MetroRoute.ArtistDetail -> "Artist:$id"
    is MetroRoute.PlaylistDetail -> "Playlist:$id"
    is MetroRoute.GenreStation -> "Genre:$browseId"
    is MetroRoute.OnlineAlbumDetail -> "OnlineAlbum:$id"
    is MetroRoute.OnlineArtistDetail -> "OnlineArtist:$id"
    is MetroRoute.OnlinePlaylistDetail -> "OnlinePlaylist:$id"
}

// ---------------------------------------------------------------------------------------------
// Root panorama
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HubPanorama(
    playerConnection: PlayerConnection?,
    database: com.music.vivi.db.MusicDatabase,
    initialPage: Int,
    onPageChange: (Int) -> Unit,
    onOpen: (MetroRoute) -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 4 })
    val page = pagerState.currentPage
    val scope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) { onPageChange(pagerState.currentPage) }

    Column(modifier = Modifier.fillMaxSize()) {
        MetroText(
            text = "music",
            style = MetroTextStyle.PageTitle,
            modifier = Modifier.padding(
                start = MetroDimens.ScreenHorizontalMargin,
                top = 4.dp,
            ),
        )
        MetroPanorama(
            titles = listOf("recent plays", "collection", "get music", "now playing"),
            pagerState = pagerState,
            onTitleClick = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 72.dp),
            pageContent = { index ->
                when (index) {
                    0 -> RecentPlaysPane(
                        database = database,
                        playerConnection = playerConnection,
                        onOpenAlbum = { onOpen(MetroRoute.AlbumDetail(it)) },
                        onOpenArtist = { onOpen(MetroRoute.ArtistDetail(it)) },
                    )
                    1 -> CollectionPane(onOpen = onOpen)
                    2 -> GetMusicPane(onOpen = onOpen)
                    else -> NowPlayingPane(
                        playerConnection = playerConnection,
                        onOpenQueue = { onOpen(MetroRoute.Queue) },
                    )
                }
            },
        )
    }

    HubAppBar(page = page, playerConnection = playerConnection, onOpen = onOpen)
}

@Composable
private fun HubAppBar(
    page: Int,
    playerConnection: PlayerConnection?,
    onOpen: (MetroRoute) -> Unit,
) {
    val context = LocalContext.current
    val media = playerConnection?.mediaMetadata?.collectAsState()?.value
    val downloadUtil = LocalDownloadUtil.current
    val download by remember(media?.id) { downloadUtil.getDownload(media?.id.orEmpty()) }
        .collectAsState(initial = null)
    val downloaded = download?.state == androidx.media3.exoplayer.offline.Download.STATE_COMPLETED

    val icons = if (page == 3) {
        // Queue lives in the Now Playing content; the app bar keeps only secondary commands.
        emptyList()
    } else {
        listOf(
            MetroAppBarIcon(
                type = MetroSystemIconType.Search,
                label = "search",
                onClick = { onOpen(MetroRoute.Search) },
            ),
        )
    }

    val menu = if (page == 3) {
        buildList {
            if (media != null) {
                add(MetroAppBarMenuItem(if (downloaded) "remove download" else "download") {
                    if (downloaded) removeSongDownload(context, media.id)
                    else startSongDownload(context, media.id, media.title.toString())
                })
            }
            add(MetroAppBarMenuItem("identify song") { onOpen(MetroRoute.Recognition) })
            add(MetroAppBarMenuItem("equalizer") { onOpen(MetroRoute.Eq) })
            add(MetroAppBarMenuItem("settings") { onOpen(MetroRoute.Settings) })
        }
    } else {
        listOf(
            MetroAppBarMenuItem("downloads") { onOpen(MetroRoute.Downloads) },
            MetroAppBarMenuItem("identify song") { onOpen(MetroRoute.Recognition) },
            MetroAppBarMenuItem("equalizer") { onOpen(MetroRoute.Eq) },
            MetroAppBarMenuItem("settings") { onOpen(MetroRoute.Settings) },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MetroAppBar(
            icons = icons,
            menuItems = menu,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Recent plays
// ---------------------------------------------------------------------------------------------

@Composable
private fun RecentPlaysPane(
    database: com.music.vivi.db.MusicDatabase,
    playerConnection: PlayerConnection?,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
) {
    val events by remember { database.events() }.collectAsState(initial = emptyList())
    val currentSong = playerConnection?.currentSong?.collectAsState(initial = null)?.value
    val albums = remember(events) { aggregateRecentAlbums(events) }
    val artists = remember(events) { aggregateRecentArtists(events) }
    val songs = remember(events, currentSong) { recentSongs(events, currentSong) }

    if (songs.isEmpty() && albums.isEmpty() && artists.isEmpty()) {
        Column(modifier = Modifier.padding(top = 12.dp)) {
            MetroText(
                text = "Nothing played yet.",
                style = MetroTextStyle.Body,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.padding(horizontal = MetroDimens.ScreenHorizontalMargin),
            )
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
        if (songs.isNotEmpty()) {
            item(key = "recent-songs-header") { SectionHeader("recently played") }
            itemsIndexed(songs, key = { _, song -> "rs-${song.id}" }) { index, song ->
                SongListRow(
                    song = song,
                    onClick = {
                        playerConnection?.playQueue(
                            ListQueue(
                                title = "recent plays",
                                items = songs.map { it.toMediaItem() },
                                startIndex = index,
                            ),
                        )
                    },
                )
            }
        }
        if (albums.isNotEmpty()) {
            item(key = "recent-albums-header") { SectionHeader("albums") }
            items(albums, key = { "ra-${it.id}" }) { album ->
                MusicListRow(
                    title = album.title,
                    subtitle = null,
                    leading = { AlbumArtThumbnail(model = album.thumbnailUrl, contentDescription = album.title) },
                    onClick = { onOpenAlbum(album.id) },
                )
            }
        }
        if (artists.isNotEmpty()) {
            item(key = "recent-artists-header") { SectionHeader("artists") }
            items(artists, key = { "rt-${it.id}" }) { artist ->
                MusicListRow(
                    title = artist.title,
                    subtitle = null,
                    leading = { AlbumArtThumbnail(model = artist.thumbnailUrl, contentDescription = artist.title) },
                    onClick = { onOpenArtist(artist.id) },
                )
            }
        }
    }
}

private data class RecentAlbum(val id: String, val title: String, val thumbnailUrl: String?)
private data class RecentArtist(val id: String, val title: String, val thumbnailUrl: String?)

/** Recently played songs, newest first; the currently-playing track is shown immediately. */
private fun recentSongs(events: List<EventWithSong>, currentSong: Song?): List<Song> {
    val seen = LinkedHashMap<String, Song>()
    currentSong?.let { seen[it.id] = it }
    events.forEach { event -> seen.putIfAbsent(event.song.id, event.song) }
    return seen.values.take(8).toList()
}

private fun aggregateRecentAlbums(events: List<EventWithSong>): List<RecentAlbum> {
    val seen = LinkedHashMap<String, RecentAlbum>()
    events.forEach { event ->
        val album = event.song.album ?: return@forEach
        if (album.id.isBlank()) return@forEach
        seen.putIfAbsent(album.id, RecentAlbum(album.id, album.title, album.thumbnailUrl))
    }
    return seen.values.take(12).toList()
}

private fun aggregateRecentArtists(events: List<EventWithSong>): List<RecentArtist> {
    val seen = LinkedHashMap<String, RecentArtist>()
    events.forEach { event ->
        event.song.artists.forEach { artistRow ->
            if (artistRow.id.isNotBlank()) {
                seen.putIfAbsent(artistRow.id, RecentArtist(artistRow.id, artistRow.name, artistRow.thumbnailUrl))
            }
        }
    }
    return seen.values.take(8).toList()
}

// ---------------------------------------------------------------------------------------------
// Collection + Get Music panes
// ---------------------------------------------------------------------------------------------

@Composable
private fun CollectionPane(onOpen: (MetroRoute) -> Unit) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        val links = listOf(
            "artists" to MetroRoute.CollectionArtists,
            "albums" to MetroRoute.CollectionAlbums,
            "songs" to MetroRoute.CollectionSongs,
            "genres" to MetroRoute.CollectionGenres,
            "playlists" to MetroRoute.CollectionPlaylists,
            "radio" to MetroRoute.CollectionRadio,
        )
        links.forEach { (label, route) ->
            HubLinkRow(label = label, onClick = { onOpen(route) })
        }
    }
}

@Composable
private fun GetMusicPane(onOpen: (MetroRoute) -> Unit) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        HubLinkRow(label = "search", onClick = { onOpen(MetroRoute.Search) })
        HubLinkRow(label = "browse by genre", onClick = { onOpen(MetroRoute.CollectionGenres) })
        HubLinkRow(label = "new releases", onClick = { onOpen(MetroRoute.GetMusicNewReleases) })
    }
}

/** WP8.1 panorama hub link: one line, overflowing the screen edge rather than wrapping/clipping. */
@Composable
private fun HubLinkRow(label: String, onClick: () -> Unit) {
    MetroText(
        text = label,
        style = MetroTextStyle.HubLink,
        maxLines = 1,
        softWrap = false,
        overflow = androidx.compose.ui.text.style.TextOverflow.Clip,
        modifier = Modifier
            .padding(
                start = MetroDimens.ScreenHorizontalMargin,
                top = 6.dp,
                bottom = 6.dp,
            )
            .wrapContentWidth(align = Alignment.Start, unbounded = true)
            .metroClickable(onClick = onClick),
    )
}

// ---------------------------------------------------------------------------------------------
// Collection category screens
// ---------------------------------------------------------------------------------------------

@Composable
private fun CollectionArtistsScreen(
    playerConnection: PlayerConnection?,
    onBack: () -> Unit,
    onOpenArtist: (String) -> Unit,
    onSearch: () -> Unit,
) {
    val viewModel: LibraryArtistsViewModel = hiltViewModel()
    val artists by viewModel.allArtists.collectAsState()
    CategoryScreenScaffold(title = "artists", onBack = onBack, onSearch = onSearch) {
        if (artists.isEmpty()) {
            MetroEmptyState("No artists in your collection.")
        } else {
            MetroLetterList(items = artists, labelOf = { it.title }, keyOf = { it.id }) { artist ->
                MusicListRow(
                    title = artist.title,
                    subtitle = if (artist.songCount > 0) "${artist.songCount} songs" else null,
                    onClick = { onOpenArtist(artist.id) },
                )
            }
        }
    }
}

@Composable
private fun CollectionAlbumsScreen(
    playerConnection: PlayerConnection?,
    onBack: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onSearch: () -> Unit,
) {
    val viewModel: LibraryAlbumsViewModel = hiltViewModel()
    val albums by viewModel.allAlbums.collectAsState()
    CategoryScreenScaffold(title = "albums", onBack = onBack, onSearch = onSearch) {
        if (albums.isEmpty()) {
            MetroEmptyState("No albums in your collection.")
        } else {
            MetroLetterList(items = albums, labelOf = { it.title }, keyOf = { it.id }) { album ->
                MusicListRow(
                    title = album.title,
                    subtitle = album.artists.joinToString { it.name }.ifBlank { null },
                    leading = { AlbumArtThumbnail(model = album.thumbnailUrl, contentDescription = album.title) },
                    onClick = { onOpenAlbum(album.id) },
                )
            }
        }
    }
}

@Composable
private fun CollectionSongsScreen(
    playerConnection: PlayerConnection?,
    onBack: () -> Unit,
    onSearch: () -> Unit,
) {
    val viewModel: LibrarySongsViewModel = hiltViewModel()
    val songs by viewModel.allSongs.collectAsState()
    val songMenu = rememberSongContextMenuState()
    SongContextMenuHost(state = songMenu, playerConnection = playerConnection) {
        CategoryScreenScaffold(title = "songs", onBack = onBack, onSearch = onSearch) {
            if (songs.isEmpty()) {
                MetroEmptyState("No songs in your collection.")
            } else {
                MetroLetterList(items = songs, labelOf = { it.title }, keyOf = { it.id }) { song ->
                    SongListRow(
                        song = song,
                        onClick = {
                            playerConnection?.playQueue(
                                ListQueue(
                                    title = "songs",
                                    items = songs.map { it.toMediaItem() },
                                    startIndex = songs.indexOf(song).coerceAtLeast(0),
                                ),
                            )
                        },
                        onLongClick = { rect -> songMenu.open(song, rect) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CollectionPlaylistsScreen(
    playerConnection: PlayerConnection?,
    onBack: () -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onNewPlaylist: () -> Unit,
    onSearch: () -> Unit,
) {
    val viewModel: LibraryPlaylistsViewModel = hiltViewModel()
    val playlists by viewModel.allPlaylists.collectAsState()
    Box(modifier = Modifier.fillMaxSize()) {
        CategoryScreenScaffold(title = "playlists", onBack = onBack, onSearch = onSearch) {
            if (playlists.isEmpty()) {
                MetroEmptyState("No playlists yet.")
            } else {
                MetroLetterList(items = playlists, labelOf = { it.title }, keyOf = { it.id }) { playlist ->
                    MusicListRow(
                        title = playlist.title,
                        subtitle = if (playlist.songCount > 0) "${playlist.songCount} songs" else null,
                        onClick = { onOpenPlaylist(playlist.id) },
                    )
                }
            }
        }
        MetroAppBar(
            icons = listOf(
                MetroAppBarIcon(type = MetroSystemIconType.Add, label = "new playlist", onClick = onNewPlaylist),
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun RadioScreen(
    database: com.music.vivi.db.MusicDatabase,
    playerConnection: PlayerConnection?,
    onBack: () -> Unit,
    onOpenAlbum: (String) -> Unit,
) {
    val events by remember { database.events() }.collectAsState(initial = emptyList())
    val albums = remember(events) { aggregateRecentAlbums(events) }
    val scope = rememberCoroutineScope()
    CategoryScreenScaffold(title = "radio", onBack = onBack, onSearch = null) {
        if (albums.isEmpty()) {
            MetroEmptyState("Play something to start radio.")
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item(key = "radio-hint") {
                    MetroText(
                        text = "Stations built from what you play.",
                        style = MetroTextStyle.Body,
                        color = MetroTheme.colors.secondaryText,
                        modifier = Modifier.padding(
                            start = MetroDimens.ScreenHorizontalMargin,
                            top = 4.dp,
                            bottom = 8.dp,
                        ),
                    )
                }
                items(albums, key = { "radio-${it.id}" }) { album ->
                    MusicListRow(
                        title = album.title,
                        subtitle = null,
                        leading = { AlbumArtThumbnail(model = album.thumbnailUrl, contentDescription = album.title) },
                        onClick = {
                            if (playerConnection != null) {
                                scope.launch {
                                    val withSongs = database.albumWithSongs(album.id).first()
                                    if (withSongs != null) playerConnection.playQueue(LocalAlbumRadio(withSongs))
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun NewReleasesScreen(
    onBack: () -> Unit,
    onOpenAlbum: (String) -> Unit,
) {
    val viewModel: NewReleaseViewModel = hiltViewModel()
    val albums by viewModel.newReleaseAlbums.collectAsState()
    CategoryScreenScaffold(title = "new releases", onBack = onBack, onSearch = null) {
        if (albums.isEmpty()) {
            MetroEmptyState("No new releases.")
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(albums, key = { it.browseId }) { album ->
                    MusicListRow(
                        title = album.title,
                        subtitle = album.artists?.joinToString { it.name }?.ifBlank { null },
                        leading = { AlbumArtThumbnail(model = album.thumbnail, contentDescription = album.title) },
                        onClick = { onOpenAlbum(album.browseId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NewPlaylistScreen(
    database: com.music.vivi.db.MusicDatabase,
    onBack: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var keepOffline by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            MetroText(
                text = "NEW PLAYLIST",
                style = MetroTextStyle.SectionHeader,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.padding(
                    start = MetroDimens.ScreenHorizontalMargin,
                    top = 12.dp,
                ),
            )
            MetroText(
                text = "playlist settings",
                style = MetroTextStyle.PageTitle,
                modifier = Modifier.padding(
                    start = MetroDimens.ScreenHorizontalMargin,
                    bottom = 8.dp,
                ),
            )
            com.metro.ui.MetroTextBox(
                value = name,
                onValueChange = { name = it },
                placeholder = "playlist name",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MetroDimens.ScreenHorizontalMargin, vertical = 8.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MetroDimens.ScreenHorizontalMargin, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MetroToggleSwitch(
                    checked = keepOffline,
                    onCheckedChange = { keepOffline = it },
                    label = "keep playlist offline",
                )
            }
        }

        MetroAppBar(
            icons = listOf(
                MetroAppBarIcon(
                    type = MetroSystemIconType.Check,
                    label = "save",
                    onClick = {
                        val entity = PlaylistEntity(name = name.trim().ifBlank { "new playlist" }, isLocal = true)
                        scope.launch {
                            database.query { insert(entity) }
                            if (keepOffline) PlaylistOffline.set(context, entity.id, true)
                            onBack()
                        }
                    },
                ),
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Detail screens (local collection)
// ---------------------------------------------------------------------------------------------

@Composable
private fun AlbumDetailScreen(
    database: com.music.vivi.db.MusicDatabase,
    playerConnection: PlayerConnection?,
    albumId: String,
    onBack: () -> Unit,
    onOpenArtist: (String) -> Unit,
) {
    val album by remember(albumId) { database.album(albumId) }.collectAsState(initial = null)
    val songs by remember(albumId) { database.albumSongs(albumId) }.collectAsState(initial = emptyList())
    val context = LocalContext.current
    val songMenu = rememberSongContextMenuState()
    val addTo = rememberAddToState()

    SongContextMenuHost(state = songMenu, playerConnection = playerConnection) {
        Box(modifier = Modifier.fillMaxSize().background(MetroTheme.colors.background)) {
            Column(modifier = Modifier.fillMaxSize()) {
                DetailHeader(
                    model = album?.thumbnailUrl,
                    title = album?.title ?: "album",
                    subtitle = album?.artists?.joinToString { it.name }?.ifBlank { null },
                    onBack = onBack,
                    onClickSubtitle = album?.artists?.firstOrNull()?.id?.let { id -> { onOpenArtist(id) } },
                )
                if (songs.isEmpty()) {
                    MetroEmptyState("No tracks.")
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        itemsIndexed(songs, key = { index, song -> "${song.id}#$index" }) { index, song ->
                            SongListRow(
                                song = song,
                                onClick = {
                                    playerConnection?.playQueue(
                                        ListQueue(
                                            title = album?.title ?: "album",
                                            items = songs.map { it.toMediaItem() },
                                            startIndex = index,
                                        ),
                                    )
                                },
                                onLongClick = { rect -> songMenu.open(song, rect) },
                            )
                        }
                        item { Spacer(modifier = Modifier.height(72.dp)) }
                    }
                }
            }

            MusicDetailAppBar(
                playerConnection = playerConnection,
                contextType = DetailPlayContext.Album,
                contextId = albumId,
                songIds = songs.map { it.id },
                downloadedIds = songs.filter { it.song.isDownloaded }.map { it.id }.toSet(),
                onPlay = {
                    playerConnection?.playQueue(
                        ListQueue(title = album?.title ?: "album", items = songs.map { it.toMediaItem() }),
                    )
                },
                onDownloadAll = {
                    songs.forEach { if (!it.song.isDownloaded) startSongDownload(context, it) }
                },
                onRemoveDownloadAll = { songs.forEach { removeSongDownload(context, it.id) } },
                onCancelDownloads = { songs.forEach { removeSongDownload(context, it.id) } },
                menuItems = listOf(
                    MetroAppBarMenuItem("add to playlist") {
                        addTo.open(
                            AddToTarget(title = album?.title ?: "album", songIds = songs.map { it.id }),
                        )
                    },
                    MetroAppBarMenuItem("view artist") {
                        album?.artists?.firstOrNull()?.id?.let(onOpenArtist)
                    },
                ),
                modifier = Modifier.align(Alignment.BottomCenter),
            )
            AddToHost(state = addTo, database = database)
        }
    }
}

@Composable
private fun ArtistDetailScreen(
    database: com.music.vivi.db.MusicDatabase,
    playerConnection: PlayerConnection?,
    artistId: String,
    onBack: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenOnlineArtist: (String) -> Unit,
) {
    val artist by remember(artistId) { database.artist(artistId) }.collectAsState(initial = null)
    val songs by remember(artistId) { database.artistSongsByCreateDateAsc(artistId) }
        .collectAsState(initial = emptyList())
    val songMenu = rememberSongContextMenuState()
    val context = LocalContext.current

    SongContextMenuHost(state = songMenu, playerConnection = playerConnection) {
        Box(modifier = Modifier.fillMaxSize().background(MetroTheme.colors.background)) {
            Column(modifier = Modifier.fillMaxSize()) {
                DetailHeader(
                    model = artist?.thumbnailUrl,
                    title = artist?.title ?: "artist",
                    subtitle = if (songs.isNotEmpty()) "${songs.size} songs in collection" else null,
                    onBack = onBack,
                )
                if (songs.isEmpty()) {
                    MetroEmptyState("No songs in your collection.")
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        itemsIndexed(songs, key = { index, song -> "${song.id}#$index" }) { index, song ->
                            SongListRow(
                                song = song,
                                onClick = {
                                    playerConnection?.playQueue(
                                        ListQueue(
                                            title = artist?.title ?: "artist",
                                            items = songs.map { it.toMediaItem() },
                                            startIndex = index,
                                        ),
                                    )
                                },
                                onLongClick = { rect -> songMenu.open(song, rect) },
                            )
                        }
                        item { Spacer(modifier = Modifier.height(72.dp)) }
                    }
                }
            }

            MusicDetailAppBar(
                playerConnection = playerConnection,
                contextType = DetailPlayContext.Artist,
                contextId = artistId,
                songIds = songs.map { it.id },
                downloadedIds = songs.filter { it.song.isDownloaded }.map { it.id }.toSet(),
                onPlay = {
                    playerConnection?.playQueue(
                        ListQueue(title = artist?.title ?: "artist", items = songs.map { it.toMediaItem() }),
                    )
                },
                onDownloadAll = {
                    songs.forEach { if (!it.song.isDownloaded) startSongDownload(context, it) }
                },
                onRemoveDownloadAll = { songs.forEach { removeSongDownload(context, it.id) } },
                onCancelDownloads = { songs.forEach { removeSongDownload(context, it.id) } },
                menuItems = listOf(
                    MetroAppBarMenuItem("view online") { onOpenOnlineArtist(artistId) },
                ),
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun PlaylistDetailScreen(
    database: com.music.vivi.db.MusicDatabase,
    playerConnection: PlayerConnection?,
    playlistId: String,
    onBack: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
) {
    val playlistSongs by remember(playlistId) { database.playlistSongs(playlistId) }
        .collectAsState(initial = emptyList())
    val songs = remember(playlistSongs) { playlistSongs.map { it.song } }
    val playlist by remember(playlistId) { database.playlist(playlistId) }.collectAsState(initial = null)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val songMenu = rememberSongContextMenuState()
    var keepOffline by remember(playlistId) { mutableStateOf(PlaylistOffline.get(context, playlistId)) }

    SongContextMenuHost(state = songMenu, playerConnection = playerConnection) {
        Box(modifier = Modifier.fillMaxSize().background(MetroTheme.colors.background)) {
            Column(modifier = Modifier.fillMaxSize()) {
                DetailHeader(
                    model = playlist?.playlist?.thumbnailUrl,
                    title = playlist?.title ?: "playlist",
                    subtitle = if (songs.isNotEmpty()) "${songs.size} songs" else null,
                    onBack = onBack,
                )
                if (songs.isEmpty()) {
                    MetroEmptyState("This playlist is empty.")
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        itemsIndexed(songs, key = { index, song -> "${song.id}#$index" }) { index, song ->
                            SongListRow(
                                song = song,
                                onClick = {
                                    playerConnection?.playQueue(
                                        ListQueue(
                                            title = playlist?.title ?: "playlist",
                                            items = songs.map { it.toMediaItem() },
                                            startIndex = index,
                                        ),
                                    )
                                },
                                onLongClick = { rect -> songMenu.open(song, rect) },
                            )
                        }
                        item { Spacer(modifier = Modifier.height(72.dp)) }
                    }
                }
            }

            MusicDetailAppBar(
                playerConnection = playerConnection,
                contextType = DetailPlayContext.Playlist,
                contextId = null,
                songIds = songs.map { it.id },
                downloadedIds = songs.filter { it.song.isDownloaded }.map { it.id }.toSet(),
                onPlay = {
                    playerConnection?.playQueue(
                        ListQueue(title = playlist?.title ?: "playlist", items = songs.map { it.toMediaItem() }),
                    )
                },
                onDownloadAll = {
                    songs.forEach { if (!it.song.isDownloaded) startSongDownload(context, it) }
                },
                onRemoveDownloadAll = { songs.forEach { removeSongDownload(context, it.id) } },
                onCancelDownloads = { songs.forEach { removeSongDownload(context, it.id) } },
                menuItems = listOf(
                    MetroAppBarMenuItem(if (keepOffline) "keep offline: on" else "keep offline: off") {
                        keepOffline = !keepOffline
                        PlaylistOffline.set(context, playlistId, keepOffline)
                        if (keepOffline) {
                            songs.forEach { if (!it.song.isDownloaded) startSongDownload(context, it) }
                        }
                    },
                ),
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Shared list primitives
// ---------------------------------------------------------------------------------------------

@Composable
private fun SectionHeader(text: String) {
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

/** Contextual category page: back affordance, title, optional search app bar. */
@Composable
private fun CategoryScreenScaffold(
    title: String,
    onBack: () -> Unit,
    onSearch: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            MetroText(
                text = "\u2039",
                style = MetroTextStyle.ListItemTitle,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier
                    .padding(start = MetroDimens.ScreenHorizontalMargin, top = 8.dp)
                    .metroClickable(onClick = onBack),
            )
            com.metro.ui.MetroPageHeader(title = title)
            Box(modifier = Modifier.fillMaxSize()) { content() }
        }
        if (onSearch != null) {
            MetroAppBar(
                icons = listOf(
                    MetroAppBarIcon(type = MetroSystemIconType.Search, label = "search", onClick = onSearch),
                ),
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun DetailHeader(
    model: Any?,
    title: String,
    subtitle: String?,
    onBack: () -> Unit,
    onClickSubtitle: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MetroDimens.ScreenHorizontalMargin, vertical = 8.dp),
    ) {
        MetroText(
            text = "\u2039",
            style = MetroTextStyle.ListItemTitle,
            color = MetroTheme.colors.secondaryText,
            modifier = Modifier.metroClickable(onClick = onBack),
        )
        AlbumArtThumbnail(model = model, contentDescription = null, size = 168.dp)
        Spacer(modifier = Modifier.height(12.dp))
        MetroText(text = title, style = MetroTextStyle.HubTitle)
        if (!subtitle.isNullOrBlank()) {
            MetroText(
                text = subtitle,
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.secondaryText,
                modifier = if (onClickSubtitle != null) {
                    Modifier.metroClickable(onClick = onClickSubtitle)
                } else {
                    Modifier
                },
            )
        }
    }
}

/** Large page title (no mechanical app-name overline). */
@Composable
internal fun MetroPageTitle(title: String) {
    MetroText(
        text = title,
        style = MetroTextStyle.PageTitle,
        modifier = Modifier.padding(
            start = MetroDimens.ScreenHorizontalMargin,
            top = 4.dp,
            bottom = 8.dp,
        ),
    )
}

internal enum class DetailPlayContext { Album, Artist, Playlist }

/**
 * Shared WP8.1 detail app bar: stateful Play/Pause (per context), global Shuffle toggle, and a
 * stateful Download control (outline → in-progress `%` → downloaded), plus the `…` overflow.
 */
@Composable
internal fun MusicDetailAppBar(
    playerConnection: PlayerConnection?,
    contextType: DetailPlayContext,
    contextId: String?,
    songIds: List<String>,
    downloadedIds: Set<String>,
    onPlay: () -> Unit,
    onDownloadAll: () -> Unit,
    onRemoveDownloadAll: () -> Unit,
    onCancelDownloads: () -> Unit,
    menuItems: List<MetroAppBarMenuItem>,
    modifier: Modifier = Modifier,
) {
    val currentSong = playerConnection?.currentSong?.collectAsState(initial = null)?.value
    val isPlaying = playerConnection?.isPlaying?.collectAsState(initial = false)?.value ?: false
    val shuffle = playerConnection?.shuffleModeEnabled?.collectAsState(initial = false)?.value ?: false
    val downloads by LocalDownloadUtil.current.downloads.collectAsState()

    val active = when (contextType) {
        DetailPlayContext.Album -> contextId != null && currentSong?.song?.albumId == contextId
        DetailPlayContext.Artist -> contextId != null && currentSong?.artists?.any { it.id == contextId } == true
        DetailPlayContext.Playlist -> currentSong != null && songIds.contains(currentSong.song.id)
    }
    val showPause = active && isPlaying

    val completedCount = songIds.count { id ->
        id in downloadedIds || downloads[id]?.state == androidx.media3.exoplayer.offline.Download.STATE_COMPLETED
    }
    val allDownloaded = songIds.isNotEmpty() && completedCount == songIds.size
    val inFlight = songIds.mapNotNull { downloads[it] }.filter {
        it.state == androidx.media3.exoplayer.offline.Download.STATE_DOWNLOADING ||
            it.state == androidx.media3.exoplayer.offline.Download.STATE_QUEUED
    }
    val percent = if (inFlight.isEmpty()) {
        0
    } else {
        (inFlight.sumOf { it.percentDownloaded.toDouble() } / inFlight.size).toInt().coerceIn(0, 100)
    }

    val icons = buildList {
        add(
            MetroAppBarIcon(
                type = if (showPause) MetroSystemIconType.Pause else MetroSystemIconType.Play,
                label = if (showPause) "pause" else "play",
                onClick = { if (active) playerConnection?.togglePlayPause() else onPlay() },
            ),
        )
        add(
            MetroAppBarIcon(
                label = "shuffle",
                selected = shuffle,
                onClick = { runCatching { playerConnection?.player?.shuffleModeEnabled = !shuffle } },
                icon = { color ->
                    MetroMediaGlyphIcon(glyph = MetroMediaGlyph.Shuffle, color = color, glyphSize = 40.dp)
                },
            ),
        )
        if (songIds.isNotEmpty()) {
            if (inFlight.isNotEmpty()) {
                add(
                    MetroAppBarIcon(
                        label = "downloading",
                        onClick = onCancelDownloads,
                        icon = { color ->
                            MetroText(
                                text = "$percent%",
                                style = MetroTextStyle.ListItemSubtitle,
                                color = color,
                            )
                        },
                    ),
                )
            } else {
                add(
                    MetroAppBarIcon(
                        type = MetroSystemIconType.Save,
                        label = if (allDownloaded) "remove download" else "download",
                        selected = allDownloaded,
                        onClick = { if (allDownloaded) onRemoveDownloadAll() else onDownloadAll() },
                    ),
                )
            }
        }
    }

    MetroAppBar(icons = icons, menuItems = menuItems, modifier = modifier)
}

/** Dense collection row. */
@Composable
internal fun MusicListRow(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    MetroListItem(
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        leading = leading,
        trailing = trailing,
        verticalPadding = RowVerticalPadding,
        oneLineMinHeight = if (leading != null) AlbumArtThumbnailSize + 8.dp else RowOneLineMinHeight,
        twoLineMinHeight = if (leading != null) AlbumArtThumbnailSize + 8.dp else RowTwoLineMinHeight,
        singleLine = true,
        onClick = onClick,
        onLongClick = onLongClick,
    )
}

/** Square album cover with a secondary-surface placeholder. */
@Composable
internal fun AlbumArtThumbnail(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = AlbumArtThumbnailSize,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(MetroTheme.colors.secondarySurface),
        contentAlignment = Alignment.Center,
    ) {
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

/** Collection list grouped under sticky letter markers. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun <T> MetroLetterList(
    items: List<T>,
    labelOf: (T) -> String,
    keyOf: (T) -> Any,
    modifier: Modifier = Modifier,
    row: @Composable (T) -> Unit,
) {
    val grouped = remember(items) {
        items.groupBy { item ->
            val first = labelOf(item).firstOrNull()?.uppercaseChar()
            if (first != null && first.isLetter()) first else '#'
        }.toSortedMap()
    }
    LazyColumn(modifier = modifier.fillMaxSize()) {
        grouped.forEach { (letter, sectionRows) ->
            metroStickyLetterHeader(letter) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MetroTheme.colors.background)
                        .padding(
                            horizontal = MetroDimens.ScreenHorizontalMargin,
                            vertical = 4.dp,
                        ),
                ) {
                    MetroLetterTile(letter = letter)
                }
            }
            items(sectionRows, key = { keyOf(it) }) { item -> row(item) }
        }
    }
}
