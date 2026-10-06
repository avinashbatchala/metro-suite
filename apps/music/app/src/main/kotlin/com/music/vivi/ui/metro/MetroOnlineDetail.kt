/**
 * Music (MetroSuite port of the Vivi Music engine)
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.metro

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarMenuItem
import com.metro.ui.MetroAppBarTextButton
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroDimens
import com.metro.ui.MetroEmptyState
import com.metro.ui.MetroLoadingDots
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.music.innertube.YouTube
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import com.music.innertube.models.YTItem
import com.music.innertube.pages.AlbumPage
import com.music.innertube.pages.ArtistPage
import com.music.innertube.pages.PlaylistPage
import com.music.vivi.db.MusicDatabase
import com.music.vivi.db.entities.ArtistEntity
import com.music.vivi.db.entities.PlaylistEntity
import com.music.vivi.db.entities.PlaylistSongMap
import com.music.vivi.extensions.toMediaItem
import com.music.vivi.models.toMediaMetadata
import com.music.vivi.playback.PlayerConnection
import com.music.vivi.playback.queues.ListQueue
import com.music.vivi.playback.queues.YouTubeAlbumRadio
import com.music.vivi.playback.queues.YouTubePlaylistQueue
import com.music.vivi.playback.queues.YouTubeQueue
import java.time.LocalDateTime

// ---------------------------------------------------------------------------------------------
// Album
// ---------------------------------------------------------------------------------------------

@Composable
internal fun OnlineAlbumDetailScreen(
    playerConnection: PlayerConnection?,
    database: MusicDatabase,
    browseId: String,
    onOpenArtist: (String) -> Unit,
) {
    val context = LocalContext.current
    var page by remember { mutableStateOf<AlbumPage?>(null) }
    var loading by remember { mutableStateOf(true) }
    val albumRow by remember(browseId) { database.album(browseId) }.collectAsState(initial = null)
    val menu = remember { OnlineMenuState() }
    val addTo = rememberAddToState()

    LaunchedEffect(browseId) {
        loading = true
        YouTube.album(browseId).onSuccess { result ->
            page = result
            database.query { insert(result) }
        }
        loading = false
    }

    OnlineMenuHost(state = menu, playerConnection = playerConnection) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MetroTheme.colors.background),
        ) {
            val current = page
            if (current == null) {
                if (loading) {
                    LoadingBox()
                } else {
                    MetroEmptyState("Album unavailable.")
                }
                return@Column
            }

            val liked = albumRow?.album?.bookmarkedAt != null
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    OnlineHeader(
                        model = current.album.thumbnail,
                        title = current.album.title,
                        subtitle = listOfNotNull(
                            current.album.artists?.joinToString { it.name }?.takeIf { it.isNotBlank() },
                            current.album.year?.toString(),
                        ).joinToString(" · ").ifBlank { null },
                    )
                    OnlineSongsList(
                        songs = current.songs,
                        queueTitle = current.album.title,
                        playerConnection = playerConnection,
                        menu = menu,
                    )
                    Spacer(modifier = Modifier.height(72.dp))
                }

                MusicDetailAppBar(
                    playerConnection = playerConnection,
                    contextType = DetailPlayContext.Album,
                    contextId = current.album.browseId,
                    songIds = current.songs.map { it.id },
                    downloadedIds = emptySet(),
                    onPlay = { playerConnection?.playQueue(YouTubeAlbumRadio(current.album.playlistId)) },
                    onDownloadAll = {
                        current.songs.forEach { song -> startSongDownload(context, song.id, song.title) }
                    },
                    onRemoveDownloadAll = {
                        current.songs.forEach { song -> removeSongDownload(context, song.id) }
                    },
                    onCancelDownloads = {
                        current.songs.forEach { song -> removeSongDownload(context, song.id) }
                    },
                    menuItems = listOf(
                        MetroAppBarMenuItem("add to collection") {
                            database.query {
                                insert(current)
                                current.songs.forEach { song ->
                                    insert(song.toMediaMetadata())
                                    inLibrary(song.id, LocalDateTime.now())
                                }
                                albumRow?.album?.let { update(it.copy(inLibrary = LocalDateTime.now())) }
                            }
                        },
                        MetroAppBarMenuItem("add to playlist") {
                            addTo.open(
                                AddToTarget(
                                    title = current.album.title,
                                    songIds = current.songs.map { it.id },
                                    persist = { db ->
                                        db.query { current.songs.forEach { insert(it.toMediaMetadata()) } }
                                    },
                                ),
                            )
                        },
                        MetroAppBarMenuItem(if (liked) "unlike" else "like") {
                            database.query { albumRow?.album?.let { update(it.toggleLike()) } }
                        },
                        MetroAppBarMenuItem("view artist") {
                            current.album.artists?.firstOrNull()?.id?.let(onOpenArtist)
                        },
                        MetroAppBarMenuItem("share") { context.share(current.album.shareLink) },
                    ),
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
        AddToHost(state = addTo, database = database)
    }
}

// ---------------------------------------------------------------------------------------------
// Playlist
// ---------------------------------------------------------------------------------------------

@Composable
internal fun OnlinePlaylistDetailScreen(
    playerConnection: PlayerConnection?,
    database: MusicDatabase,
    playlistId: String,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
) {
    val context = LocalContext.current
    var page by remember { mutableStateOf<PlaylistPage?>(null) }
    var loading by remember { mutableStateOf(true) }
    val dbPlaylist by remember(playlistId) { database.playlistByBrowseId(playlistId) }
        .collectAsState(initial = null)
    val menu = remember { OnlineMenuState() }

    LaunchedEffect(playlistId) {
        loading = true
        YouTube.playlist(playlistId).onSuccess { page = it }
        loading = false
    }

    OnlineMenuHost(state = menu, playerConnection = playerConnection) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MetroTheme.colors.background),
        ) {
            val current = page
            if (current == null) {
                if (loading) LoadingBox() else MetroEmptyState("Playlist unavailable.")
                return@Column
            }

            val liked = dbPlaylist?.playlist?.bookmarkedAt != null
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    OnlineHeader(
                        model = current.playlist.thumbnail,
                        title = current.playlist.title,
                        subtitle = listOfNotNull(
                            current.playlist.author?.name?.takeIf { it.isNotBlank() },
                            current.playlist.songCountText,
                        ).joinToString(" · ").ifBlank { null },
                    )
                    OnlineSongsList(
                        songs = current.songs,
                        queueTitle = current.playlist.title,
                        playerConnection = playerConnection,
                        menu = menu,
                    )
                    Spacer(modifier = Modifier.height(72.dp))
                }

                MusicDetailAppBar(
                    playerConnection = playerConnection,
                    contextType = DetailPlayContext.Playlist,
                    contextId = null,
                    songIds = current.songs.map { it.id },
                    downloadedIds = emptySet(),
                    onPlay = {
                        playerConnection?.playQueue(
                            YouTubePlaylistQueue(
                                current.playlist.id,
                                current.playlist.title,
                                initialSongs = current.songs,
                            ),
                        )
                    },
                    onDownloadAll = {
                        current.songs.forEach { song -> startSongDownload(context, song.id, song.title) }
                    },
                    onRemoveDownloadAll = {
                        current.songs.forEach { song -> removeSongDownload(context, song.id) }
                    },
                    onCancelDownloads = {
                        current.songs.forEach { song -> removeSongDownload(context, song.id) }
                    },
                    menuItems = listOf(
                        MetroAppBarMenuItem("add to collection") {
                            addOnlinePlaylistToLibrary(database, current, markLiked = false)
                        },
                        MetroAppBarMenuItem(if (liked) "unlike" else "like") {
                            addOnlinePlaylistToLibrary(database, current, markLiked = true)
                        },
                        MetroAppBarMenuItem("start radio") {
                            current.playlist.radioEndpoint?.let { endpoint ->
                                playerConnection?.playQueue(YouTubeQueue(endpoint))
                            }
                        },
                        MetroAppBarMenuItem("share") { context.share(current.playlist.shareLink) },
                    ),
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

private fun addOnlinePlaylistToLibrary(database: MusicDatabase, page: PlaylistPage, markLiked: Boolean) {
    val entity = PlaylistEntity(
        id = "LP_" + page.playlist.id,
        name = page.playlist.title,
        browseId = page.playlist.id,
        thumbnailUrl = page.playlist.thumbnail,
        isEditable = page.playlist.isEditable,
        remoteSongCount = page.playlist.songCountText
            ?.let { Regex("""\d+""").find(it)?.value?.toIntOrNull() },
        playEndpointParams = page.playlist.playEndpoint?.params,
        shuffleEndpointParams = page.playlist.shuffleEndpoint?.params,
        radioEndpointParams = page.playlist.radioEndpoint?.params,
    )
    database.query {
        insert(entity)
        page.songs.forEachIndexed { index, song ->
            insert(song.toMediaMetadata())
            insert(
                PlaylistSongMap(
                    playlistId = entity.id,
                    songId = song.id,
                    position = index,
                    setVideoId = song.setVideoId,
                ),
            )
        }
        if (markLiked) update(entity.toggleLike())
    }
}

// ---------------------------------------------------------------------------------------------
// Artist
// ---------------------------------------------------------------------------------------------

@Composable
internal fun OnlineArtistDetailScreen(
    playerConnection: PlayerConnection?,
    database: MusicDatabase,
    artistId: String,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
) {
    val context = LocalContext.current
    var page by remember { mutableStateOf<ArtistPage?>(null) }
    var loading by remember { mutableStateOf(true) }
    val artistRow by remember(artistId) { database.artist(artistId) }.collectAsState(initial = null)
    val menu = remember { OnlineMenuState() }

    LaunchedEffect(artistId) {
        loading = true
        YouTube.artist(artistId).onSuccess { result ->
            page = result
            database.query {
                insert(
                    ArtistEntity(
                        id = result.artist.id,
                        name = result.artist.title,
                        thumbnailUrl = result.artist.thumbnail,
                        channelId = result.artist.channelId,
                    ),
                )
            }
        }
        loading = false
    }

    OnlineMenuHost(state = menu, playerConnection = playerConnection) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MetroTheme.colors.background),
        ) {
            val current = page
            if (current == null) {
                if (loading) LoadingBox() else MetroEmptyState("Artist unavailable.")
                return@Column
            }

            val subscribed = artistRow?.artist?.bookmarkedAt != null
            val topSongs = current.sections
                .firstOrNull { section -> section.items.any { it is SongItem } }
                ?.items?.filterIsInstance<SongItem>().orEmpty()

            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item(key = "header") {
                        OnlineHeader(
                            model = current.artist.thumbnail,
                            title = current.artist.title,
                            subtitle = current.subscriberCountText
                                ?: current.monthlyListenerCount,
                        )
                    }
                    current.sections.forEach { section ->
                        if (section.items.isNotEmpty()) {
                            item(key = "h:${section.title}") {
                                MetroText(
                                    text = section.title,
                                    style = MetroTextStyle.SectionHeader,
                                    color = MetroTheme.colors.accent,
                                    modifier = Modifier.padding(
                                        start = MetroDimens.ScreenHorizontalMargin,
                                        top = 12.dp,
                                        bottom = 4.dp,
                                    ),
                                )
                            }
                            items(section.items, key = { "${section.title}/${it.id}" }) { item ->
                                OnlineSectionRow(
                                    item = item,
                                    sectionSongs = section.items.filterIsInstance<SongItem>(),
                                    playerConnection = playerConnection,
                                    menu = menu,
                                    onOpenAlbum = onOpenAlbum,
                                    onOpenArtist = onOpenArtist,
                                    onOpenPlaylist = onOpenPlaylist,
                                )
                            }
                        }
                    }
                    item(key = "bottom") { Spacer(modifier = Modifier.height(72.dp)) }
                }

                MusicDetailAppBar(
                    playerConnection = playerConnection,
                    contextType = DetailPlayContext.Artist,
                    contextId = current.artist.id,
                    songIds = topSongs.map { it.id },
                    downloadedIds = emptySet(),
                    onPlay = {
                        current.artist.playEndpoint?.let { playerConnection?.playQueue(YouTubeQueue(it)) }
                    },
                    onDownloadAll = {
                        topSongs.forEach { song -> startSongDownload(context, song.id, song.title) }
                    },
                    onRemoveDownloadAll = {
                        topSongs.forEach { song -> removeSongDownload(context, song.id) }
                    },
                    onCancelDownloads = {
                        topSongs.forEach { song -> removeSongDownload(context, song.id) }
                    },
                    menuItems = listOf(
                        MetroAppBarMenuItem("add to collection") {
                            database.query {
                                insert(
                                    ArtistEntity(
                                        id = current.artist.id,
                                        name = current.artist.title,
                                        thumbnailUrl = current.artist.thumbnail,
                                        channelId = current.artist.channelId,
                                    ),
                                )
                            }
                        },
                        MetroAppBarMenuItem(if (subscribed) "unsubscribe" else "subscribe") {
                            database.query {
                                val existing = artistRow?.artist
                                if (existing != null) {
                                    update(existing.toggleLike())
                                } else {
                                    insert(
                                        ArtistEntity(
                                            id = current.artist.id,
                                            name = current.artist.title,
                                            thumbnailUrl = current.artist.thumbnail,
                                            channelId = current.artist.channelId,
                                        ).toggleLike(),
                                    )
                                }
                            }
                        },
                        MetroAppBarMenuItem("start radio") {
                            current.artist.radioEndpoint?.let { playerConnection?.playQueue(YouTubeQueue(it)) }
                        },
                    ),
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Shared helpers
// ---------------------------------------------------------------------------------------------

@Composable
private fun LoadingBox() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        MetroLoadingDots()
    }
}

@Composable
private fun OnlineHeader(model: Any?, title: String, subtitle: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MetroDimens.ScreenHorizontalMargin, vertical = 8.dp),
    ) {
        AlbumArtThumbnail(model = model, contentDescription = null, size = 168.dp)
        Spacer(modifier = Modifier.height(12.dp))
        MetroText(text = title, style = MetroTextStyle.HubTitle)
        if (!subtitle.isNullOrBlank()) {
            MetroText(
                text = subtitle,
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.secondaryText,
            )
        }
    }
}

@Composable
private fun OnlineSongsList(
    songs: List<SongItem>,
    queueTitle: String,
    playerConnection: PlayerConnection?,
    menu: OnlineMenuState,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(songs, key = { it.id }) { song ->
            OnlineSongRow(
                song = song,
                playerConnection = playerConnection,
                menu = menu,
                onPlay = {
                    playerConnection?.playQueue(
                        ListQueue(
                            title = queueTitle,
                            items = songs.map { it.toMediaItem() },
                            startIndex = songs.indexOf(song).coerceAtLeast(0),
                        ),
                    )
                },
            )
        }
    }
}

@Composable
private fun OnlineSongRow(
    song: SongItem,
    playerConnection: PlayerConnection?,
    menu: OnlineMenuState,
    onPlay: () -> Unit,
) {
    var bounds by remember { mutableStateOf(Rect.Zero) }
    MusicListRow(
        title = song.title,
        subtitle = song.artists.joinToString { it.name }.ifBlank { null },
        leading = { AlbumArtThumbnail(model = song.thumbnail, contentDescription = null) },
        onClick = onPlay,
        onLongClick = { menu.open(song, bounds) },
        modifier = Modifier.onGloballyPositioned { bounds = it.boundsInWindow() },
    )
}

@Composable
private fun OnlineSectionRow(
    item: YTItem,
    sectionSongs: List<SongItem>,
    playerConnection: PlayerConnection?,
    menu: OnlineMenuState,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
) {
    when (item) {
        is SongItem -> OnlineSongRow(
            song = item,
            playerConnection = playerConnection,
            menu = menu,
            onPlay = {
                playerConnection?.playQueue(
                    ListQueue(
                        title = "artist",
                        items = sectionSongs.map { it.toMediaItem() },
                        startIndex = sectionSongs.indexOf(item).coerceAtLeast(0),
                    ),
                )
            },
        )

        is AlbumItem -> MusicListRow(
            title = item.title,
            subtitle = item.artists?.joinToString { it.name }?.ifBlank { null },
            leading = { AlbumArtThumbnail(model = item.thumbnail, contentDescription = null) },
            onClick = { onOpenAlbum(item.browseId) },
        )

        is PlaylistItem -> MusicListRow(
            title = item.title,
            subtitle = item.songCountText,
            leading = { AlbumArtThumbnail(model = item.thumbnail, contentDescription = null) },
            onClick = { onOpenPlaylist(item.id) },
        )

        is ArtistItem -> MusicListRow(
            title = item.title,
            subtitle = item.subtext,
            leading = { AlbumArtThumbnail(model = item.thumbnail, contentDescription = null) },
            onClick = { onOpenArtist(item.id) },
        )
    }
}

private fun android.content.Context.share(link: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, link)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    startActivity(Intent.createChooser(intent, null))
}
