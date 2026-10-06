/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.metro

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.metro.ui.MetroAppBarTextButton
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroColors
import com.metro.ui.MetroDimens
import com.metro.ui.MetroEmptyState
import com.metro.ui.MetroLetterTile
import com.metro.ui.MetroListItem
import com.metro.ui.MetroLoadingScreen
import com.metro.ui.MetroMediaGlyph
import com.metro.ui.MetroMediaGlyphIcon
import com.metro.ui.MetroPanorama
import com.metro.ui.metroNavBarPadding
import com.metro.ui.MetroPivot
import com.metro.ui.MetroSubpageHost
import com.metro.ui.MetroSystemIcon
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroClickable
import com.metro.ui.metroStickyLetterHeader
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalDownloadUtil
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.db.entities.Album
import com.music.vivi.db.entities.Playlist
import com.music.vivi.db.entities.Song
import com.music.vivi.extensions.toMediaItem
import com.music.vivi.playback.PlayerConnection
import com.music.vivi.playback.queues.ListQueue
import com.music.vivi.playback.queues.LocalAlbumRadio
import com.music.vivi.viewmodels.LibraryAlbumsViewModel
import com.music.vivi.viewmodels.LibraryArtistsViewModel
import com.music.vivi.viewmodels.LibraryPlaylistsViewModel
import com.music.vivi.viewmodels.LibrarySongsViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Dense Xbox Music list rows. */
private val RowVerticalPadding = 4.dp
private val RowOneLineMinHeight = 48.dp
private val RowTwoLineMinHeight = 60.dp

internal val AlbumArtThumbnailSize = 56.dp

/**
 * In-app Metro page stack for the music client. Mirrors the suite music app shell:
 * panorama hub -> collection / search / queue / downloads subpages.
 */
@Composable
fun MetroMusicApp(onExit: () -> Unit = {}) {
    val playerConnection = LocalPlayerConnection.current
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current

    var route: MetroRoute by remember { mutableStateOf<MetroRoute>(MetroRoute.Hub) }
    var collectionPivot by rememberSaveable { mutableIntStateOf(0) }

    val openCollection: (Int) -> Unit = {
        collectionPivot = it
        route = MetroRoute.Collection
    }

    // Own the Back gesture: pop sub-pages back to the Hub; on the Hub, exit the app
    // (this handler is composed last, so it takes precedence over the pivot shell's).
    androidx.activity.compose.BackHandler(enabled = true) {
        if (route is MetroRoute.Hub) onExit() else route = route.parentRoute()
    }

    Box(modifier = Modifier.fillMaxSize().statusBarsPadding().metroNavBarPadding()) {
        MetroSubpageHost(
            route = route,
            isRoot = { it is MetroRoute.Hub },
            parentOf = { it.parentRoute() },
            loadKeyOf = { it.loadKey() },
            onGoBack = { route = route.parentRoute() },
            modifier = Modifier.fillMaxSize(),
            rootContent = {
                MetroHub(
                    playerConnection = playerConnection,
                    onOpenCollection = openCollection,
                    onOpenSearch = { route = MetroRoute.Search },
                    onOpenQueue = { route = MetroRoute.Queue },
                    onOpenDownloads = { route = MetroRoute.Downloads },
                    onOpenSettings = { route = MetroRoute.Settings },
                )
            },
            subpageContent = { current ->
                val requestExit = com.metro.ui.LocalMetroSubpageExit.current
                val onBack = { requestExit?.invoke() }
                when (current) {
                    MetroRoute.Collection -> CollectionScreen(
                        playerConnection = playerConnection,
                        database = database,
                        initialPivot = collectionPivot,
                        onPivotChange = { collectionPivot = it },
                        onOpenAlbum = { route = MetroRoute.AlbumDetail(it) },
                        onOpenArtist = { route = MetroRoute.ArtistDetail(it) },
                        onOpenPlaylist = { route = MetroRoute.PlaylistDetail(it) },
                    )
                    MetroRoute.Search -> SearchScreen(
                        playerConnection = playerConnection,
                        onOpenAlbum = { route = MetroRoute.OnlineAlbumDetail(it) },
                        onOpenArtist = { route = MetroRoute.OnlineArtistDetail(it) },
                        onOpenPlaylist = { route = MetroRoute.OnlinePlaylistDetail(it) },
                    )
                    MetroRoute.Queue -> QueueScreen(playerConnection = playerConnection)
                    MetroRoute.Downloads -> DownloadsScreen(database = database, downloaded = downloadUtil)
                    MetroRoute.Settings -> SettingsScreen(
                        onOpenDownloads = { route = MetroRoute.Downloads },
                        onOpenRecognition = { route = MetroRoute.Recognition },
                        onOpenEq = { route = MetroRoute.Eq },
                    )
                    MetroRoute.Recognition -> RecognitionScreen()
                    MetroRoute.Eq -> EqScreen()
                    is MetroRoute.AlbumDetail -> AlbumDetailScreen(
                        database = database,
                        playerConnection = playerConnection,
                        albumId = current.id,
                    )
                    is MetroRoute.ArtistDetail -> ArtistDetailScreen(
                        database = database,
                        playerConnection = playerConnection,
                        artistId = current.id,
                    )
                    is MetroRoute.PlaylistDetail -> PlaylistDetailScreen(
                        database = database,
                        playerConnection = playerConnection,
                        playlistId = current.id,
                    )
                    is MetroRoute.OnlineAlbumDetail -> OnlineAlbumDetailScreen(
                        playerConnection = playerConnection,
                        database = database,
                        browseId = current.id,
                        onOpenArtist = { route = MetroRoute.OnlineArtistDetail(it) },
                    )
                    is MetroRoute.OnlineArtistDetail -> OnlineArtistDetailScreen(
                        playerConnection = playerConnection,
                        database = database,
                        artistId = current.id,
                        onOpenAlbum = { route = MetroRoute.OnlineAlbumDetail(it) },
                        onOpenArtist = { route = MetroRoute.OnlineArtistDetail(it) },
                        onOpenPlaylist = { route = MetroRoute.OnlinePlaylistDetail(it) },
                    )
                    is MetroRoute.OnlinePlaylistDetail -> OnlinePlaylistDetailScreen(
                        playerConnection = playerConnection,
                        database = database,
                        playlistId = current.id,
                        onOpenAlbum = { route = MetroRoute.OnlineAlbumDetail(it) },
                        onOpenArtist = { route = MetroRoute.OnlineArtistDetail(it) },
                    )
                    MetroRoute.Hub -> Unit
                }
            },
        )

        val appBarVisible = route is MetroRoute.Collection || route is MetroRoute.Hub
        MetroAppBar(
            visible = appBarVisible,
            icons = listOf(
                MetroAppBarIcon(
                    type = MetroSystemIconType.Search,
                    label = "search",
                    onClick = { route = MetroRoute.Search },
                ),
                MetroAppBarIcon(
                    label = "queue",
                    onClick = { route = MetroRoute.Queue },
                    icon = { color ->
                        MetroMediaGlyphIcon(
                            glyph = MetroMediaGlyph.Queue,
                            color = color,
                            glyphSize = 40.dp,
                        )
                    },
                ),
                MetroAppBarIcon(
                    type = MetroSystemIconType.Settings,
                    label = "settings",
                    onClick = { route = MetroRoute.Settings },
                ),
            ),
            menuItems = listOf(
                MetroAppBarMenuItem("collection") { route = MetroRoute.Collection },
                MetroAppBarMenuItem("get music") { route = MetroRoute.Hub },
                MetroAppBarMenuItem("downloads") { route = MetroRoute.Downloads },
                MetroAppBarMenuItem("recognition") { route = MetroRoute.Recognition },
                MetroAppBarMenuItem("equalizer") { route = MetroRoute.Eq },
                MetroAppBarMenuItem("settings") { route = MetroRoute.Settings },
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Routes
// ---------------------------------------------------------------------------------------------

sealed interface MetroRoute {
    data object Hub : MetroRoute
    data object Collection : MetroRoute
    data object Search : MetroRoute
    data object Queue : MetroRoute
    data object Downloads : MetroRoute
    data object Settings : MetroRoute
    data object Recognition : MetroRoute
    data object Eq : MetroRoute
    data class AlbumDetail(val id: String) : MetroRoute
    data class ArtistDetail(val id: String) : MetroRoute
    data class PlaylistDetail(val id: String) : MetroRoute
    data class OnlineAlbumDetail(val id: String) : MetroRoute
    data class OnlineArtistDetail(val id: String) : MetroRoute
    data class OnlinePlaylistDetail(val id: String) : MetroRoute
}

private fun MetroRoute.parentRoute(): MetroRoute = when (this) {
    is MetroRoute.AlbumDetail,
    is MetroRoute.ArtistDetail,
    is MetroRoute.PlaylistDetail,
    -> MetroRoute.Collection

    is MetroRoute.OnlineAlbumDetail,
    is MetroRoute.OnlineArtistDetail,
    is MetroRoute.OnlinePlaylistDetail,
    -> MetroRoute.Search

    else -> MetroRoute.Hub
}

private fun MetroRoute.loadKey(): Any = when (this) {
    MetroRoute.Hub -> "Hub"
    MetroRoute.Collection -> "Collection"
    MetroRoute.Search -> "Search"
    MetroRoute.Queue -> "Queue"
    MetroRoute.Downloads -> "Downloads"
    MetroRoute.Settings -> "Settings"
    MetroRoute.Recognition -> "Recognition"
    MetroRoute.Eq -> "Eq"
    is MetroRoute.AlbumDetail -> "Album:$id"
    is MetroRoute.ArtistDetail -> "Artist:$id"
    is MetroRoute.PlaylistDetail -> "Playlist:$id"
    is MetroRoute.OnlineAlbumDetail -> "OnlineAlbum:$id"
    is MetroRoute.OnlineArtistDetail -> "OnlineArtist:$id"
    is MetroRoute.OnlinePlaylistDetail -> "OnlinePlaylist:$id"
}

// ---------------------------------------------------------------------------------------------
// Hub
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MetroHub(
    playerConnection: PlayerConnection?,
    onOpenCollection: (Int) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    Column(modifier = Modifier.fillMaxSize()) {
        MetroAppTitle("MUSIC")
        MetroText(
            text = "metro music",
            style = MetroTextStyle.PageTitle,
            modifier = Modifier.padding(start = MetroDimens.ScreenHorizontalMargin, bottom = 4.dp),
        )
        MetroPanorama(
            titles = listOf("collection", "get music", "now playing", "downloads"),
            pagerState = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 72.dp),
            pageContent = { page ->
                when (page) {
                    0 -> CollectionHubPane(onOpenPivot = onOpenCollection)
                    1 -> GetMusicPane(
                        onOpenSearch = onOpenSearch,
                        onOpenSettings = onOpenSettings,
                    )
                    2 -> NowPlayingPane(
                        playerConnection = playerConnection,
                        onOpenQueue = onOpenQueue,
                    )
                    else -> DownloadsHubPane(onOpenDownloads = onOpenDownloads)
                }
            },
        )
    }
}

@Composable
private fun CollectionHubPane(onOpenPivot: (Int) -> Unit) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        val links = listOf(
            "songs" to 0,
            "albums" to 1,
            "artists" to 2,
            "playlists" to 3,
        )
        links.forEach { (label, pivot) ->
            MetroListItem(
                title = label,
                titleStyle = MetroTextStyle.HubLink,
                verticalPadding = 8.dp,
                oneLineMinHeight = 56.dp,
                onClick = { onOpenPivot(pivot) },
            )
        }
    }
}

@Composable
private fun GetMusicPane(
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = MetroDimens.ScreenHorizontalMargin)
            .padding(top = 24.dp),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val tileSize = (maxWidth - 8.dp) / 2
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HubAccentTile(
                        title = "search",
                        type = MetroSystemIconType.Search,
                        onClick = onOpenSearch,
                        modifier = Modifier.size(tileSize),
                    )
                    HubAccentTile(
                        title = "settings",
                        type = MetroSystemIconType.Settings,
                        onClick = onOpenSettings,
                        modifier = Modifier.size(tileSize),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        MetroText(
            text = "Search and stream from YouTube Music.",
            style = MetroTextStyle.Body,
            color = MetroTheme.colors.secondaryText,
        )
    }
}

@Composable
private fun DownloadsHubPane(onOpenDownloads: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = MetroDimens.ScreenHorizontalMargin)
            .padding(top = 24.dp),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val tileSize = (maxWidth - 8.dp) / 2
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HubAccentTile(
                        title = "downloads",
                        type = MetroSystemIconType.Save,
                        onClick = onOpenDownloads,
                        modifier = Modifier.size(tileSize),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        MetroText(
            text = "Songs saved on this device.",
            style = MetroTextStyle.Body,
            color = MetroTheme.colors.secondaryText,
        )
    }
}

@Composable
private fun HubAccentTile(
    title: String,
    type: MetroSystemIconType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = MetroTheme.colors.accent
    val content = MetroColors.tileContentColor(background)
    BoxWithConstraints(
        modifier = modifier
            .background(background)
            .metroClickable(onClick = onClick)
            .padding(8.dp),
    ) {
        val iconSize = minOf(maxWidth, maxHeight) * 0.54f
        MetroSystemIcon(
            type = type,
            iconSize = iconSize,
            color = content,
            showCircle = false,
            modifier = Modifier.align(Alignment.Center),
        )
        MetroText(
            text = title,
            style = MetroTextStyle.ListItemTitle,
            color = content,
            maxLines = 1,
            modifier = Modifier.align(Alignment.BottomStart),
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Collection
// ---------------------------------------------------------------------------------------------

@Composable
private fun CollectionScreen(
    playerConnection: PlayerConnection?,
    database: com.music.vivi.db.MusicDatabase,
    initialPivot: Int,
    onPivotChange: (Int) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
) {
    val pagerState = rememberPagerState(
        initialPage = initialPivot,
        pageCount = { 4 },
    )
    androidx.compose.runtime.LaunchedEffect(pagerState.currentPage) {
        onPivotChange(pagerState.currentPage)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background)
            .padding(bottom = 72.dp),
    ) {
        MetroAppTitle("MUSIC")
        MetroPivot(
            titles = listOf("songs", "albums", "artists", "playlists"),
            pagerState = pagerState,
        ) { page ->
            when (page) {
                0 -> SongsPane(playerConnection)
                1 -> AlbumsPane(playerConnection, database, onOpenAlbum)
                2 -> ArtistsPane(onOpenArtist)
                else -> PlaylistsPane(onOpenPlaylist)
            }
        }
    }
}

@Composable
private fun SongsPane(playerConnection: PlayerConnection?) {
    val viewModel: LibrarySongsViewModel = hiltViewModel()
    val songs by viewModel.allSongs.collectAsState()
    if (songs.isEmpty()) {
        MetroEmptyState("No songs.")
        return
    }
    val songMenu = rememberSongContextMenuState()
    SongContextMenuHost(state = songMenu, playerConnection = playerConnection) {
        MetroLetterList(
            items = songs,
            labelOf = { it.title },
            keyOf = { it.id },
        ) { song ->
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

@Composable
private fun AlbumsPane(
    playerConnection: PlayerConnection?,
    database: com.music.vivi.db.MusicDatabase,
    onOpenAlbum: (String) -> Unit,
) {
    val viewModel: LibraryAlbumsViewModel = hiltViewModel()
    val albums by viewModel.allAlbums.collectAsState()
    if (albums.isEmpty()) {
        MetroEmptyState("No albums.")
        return
    }
    val scope = rememberCoroutineScope()
    MetroLetterList(
        items = albums,
        labelOf = { it.title },
        keyOf = { it.id },
    ) { album ->
        MusicListRow(
            title = album.title,
            subtitle = album.artists.joinToString { it.name }.ifBlank { null },
            onClick = {
                if (playerConnection != null) {
                    scope.launch {
                        val withSongs = database.albumWithSongs(album.id).first()
                        if (withSongs != null) {
                            playerConnection.playQueue(LocalAlbumRadio(withSongs))
                        } else {
                            val albumSongs = database.albumSongs(album.id).first()
                            if (albumSongs.isNotEmpty()) {
                                playerConnection.playQueue(
                                    ListQueue(
                                        title = album.title,
                                        items = albumSongs.map { it.toMediaItem() },
                                    ),
                                )
                            }
                        }
                    }
                }
            },
            leading = {
                AlbumArtThumbnail(
                    model = album.thumbnailUrl,
                    contentDescription = album.title,
                )
            },
        )
    }
}

@Composable
private fun ArtistsPane(onOpenArtist: (String) -> Unit) {
    val viewModel: LibraryArtistsViewModel = hiltViewModel()
    val artists by viewModel.allArtists.collectAsState()
    if (artists.isEmpty()) {
        MetroEmptyState("No artists.")
        return
    }
    MetroLetterList(
        items = artists,
        labelOf = { it.title },
        keyOf = { it.id },
    ) { artist ->
        MusicListRow(
            title = artist.title,
            subtitle = if (artist.songCount > 0) "${artist.songCount} songs" else null,
            onClick = { onOpenArtist(artist.id) },
        )
    }
}

@Composable
private fun PlaylistsPane(onOpenPlaylist: (String) -> Unit) {
    val viewModel: LibraryPlaylistsViewModel = hiltViewModel()
    val playlists by viewModel.allPlaylists.collectAsState()
    if (playlists.isEmpty()) {
        MetroEmptyState("No playlists.")
        return
    }
    MetroLetterList(
        items = playlists,
        labelOf = { it.title },
        keyOf = { it.id },
    ) { playlist ->
        MusicListRow(
            title = playlist.title,
            subtitle = if (playlist.songCount > 0) "${playlist.songCount} songs" else null,
            onClick = { onOpenPlaylist(playlist.id) },
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Detail screens
// ---------------------------------------------------------------------------------------------

@Composable
private fun AlbumDetailScreen(
    database: com.music.vivi.db.MusicDatabase,
    playerConnection: PlayerConnection?,
    albumId: String,
) {
    val songs by remember(albumId) { database.albumSongs(albumId) }
        .collectAsState(initial = emptyList())
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            MetroAppTitle("MUSIC")
            DetailSongsList(
                songs = songs,
                playerConnection = playerConnection,
                onPlay = { index ->
                    playerConnection?.playQueue(
                        ListQueue(
                            title = "album",
                            items = songs.map { it.toMediaItem() },
                            startIndex = index,
                        ),
                    )
                },
                modifier = Modifier.padding(bottom = 64.dp),
            )
        }
        MetroAppBar(
            textButtons = listOf(
                MetroAppBarTextButton("download all") {
                    songs.forEach { song ->
                        if (!song.song.isDownloaded) startSongDownload(context, song)
                    }
                },
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun ArtistDetailScreen(
    database: com.music.vivi.db.MusicDatabase,
    playerConnection: PlayerConnection?,
    artistId: String,
) {
    val songs by remember(artistId) { database.artistSongsByCreateDateAsc(artistId) }
        .collectAsState(initial = emptyList())
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background),
    ) {
        MetroAppTitle("MUSIC")
        DetailSongsList(
            songs = songs,
            playerConnection = playerConnection,
            onPlay = { index ->
                playerConnection?.playQueue(
                    ListQueue(
                        title = "artist",
                        items = songs.map { it.toMediaItem() },
                        startIndex = index,
                    ),
                )
            },
        )
    }
}

@Composable
private fun PlaylistDetailScreen(
    database: com.music.vivi.db.MusicDatabase,
    playerConnection: PlayerConnection?,
    playlistId: String,
) {
    val playlistSongs by remember(playlistId) { database.playlistSongs(playlistId) }
        .collectAsState(initial = emptyList())
    val songs = playlistSongs.map { it.song }
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            MetroAppTitle("MUSIC")
            DetailSongsList(
                songs = songs,
                playerConnection = playerConnection,
                onPlay = { index ->
                    playerConnection?.playQueue(
                        ListQueue(
                            title = "playlist",
                            items = songs.map { it.toMediaItem() },
                            startIndex = index,
                        ),
                    )
                },
                modifier = Modifier.padding(bottom = 64.dp),
            )
        }
        MetroAppBar(
            textButtons = listOf(
                MetroAppBarTextButton("download all") {
                    songs.forEach { song ->
                        if (!song.song.isDownloaded) startSongDownload(context, song)
                    }
                },
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun DetailSongsList(
    songs: List<Song>,
    playerConnection: PlayerConnection?,
    onPlay: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (songs.isEmpty()) {
        MetroEmptyState("No songs.")
        return
    }
    val songMenu = rememberSongContextMenuState()
    SongContextMenuHost(
        state = songMenu,
        playerConnection = playerConnection,
        modifier = modifier,
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(songs, key = { index, song -> "${song.id}#$index" }) { index, song ->
                SongListRow(
                    song = song,
                    onClick = { onPlay(index) },
                    onLongClick = { rect -> songMenu.open(song, rect) },
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Shared list primitives
// ---------------------------------------------------------------------------------------------

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

/** App-title overline plus the large page title, with normal vertical padding. */
@Composable
internal fun MetroPageTitle(title: String) {
    MetroAppTitle("MUSIC")
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
