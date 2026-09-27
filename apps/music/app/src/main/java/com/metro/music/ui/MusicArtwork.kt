package com.metro.music.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.metro.music.ytmusic.ArtistAboutClient
import com.metro.ui.MetroAppGlyphs
import com.metro.ui.MetroTheme

/**
 * Square album cover with a secondary-surface placeholder + suite music glyph while loading
 * or when art is missing (blueprint Page 3 — albums rows).
 */
@Composable
fun AlbumArtThumbnail(
    model: Any?,
    modifier: Modifier = Modifier,
    size: Dp = AlbumArtThumbnailSize,
    contentDescription: String? = null,
) {
    SubcomposeAsyncImage(
        model = model?.let { artRequest(it) },
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        contentScale = ContentScale.Crop,
    ) {
        when (painter.state) {
            is AsyncImagePainter.State.Success ->
                SubcomposeAsyncImageContent(modifier = Modifier.fillMaxSize())
            else ->
                MusicArtPlaceholder(iconSize = size * 0.45f)
        }
    }
}

/**
 * Full-width artist about hero — placeholder until Wikipedia/Wikimedia art resolves.
 */
@Composable
fun AboutHeroImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    placeholderHeight: Dp = AboutHeroPlaceholderHeight,
) {
    val context = LocalContext.current
    val request = url?.let {
        ImageRequest.Builder(context)
            .data(it)
            .addHeader("User-Agent", ArtistAboutClient.USER_AGENT)
            .crossfade(true)
            .build()
    }
    SubcomposeAsyncImage(
        model = request,
        contentDescription = contentDescription,
        modifier = modifier.fillMaxWidth(),
        contentScale = ContentScale.FillWidth,
    ) {
        when (painter.state) {
            is AsyncImagePainter.State.Success ->
                SubcomposeAsyncImageContent(
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth,
                )
            else ->
                MusicArtPlaceholder(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(placeholderHeight),
                    iconSize = 56.dp,
                )
        }
    }
}

@Composable
fun MusicArtPlaceholder(
    modifier: Modifier = Modifier.fillMaxSize(),
    iconSize: Dp = 24.dp,
) {
    Box(
        modifier = modifier.background(MetroTheme.colors.secondarySurface),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = MetroAppGlyphs.Music),
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            // Glyph paths are white; tint to secondary text so light theme stays readable.
            colorFilter = ColorFilter.tint(MetroTheme.colors.secondaryText),
        )
    }
}

@Composable
private fun artRequest(model: Any): ImageRequest {
    val context = LocalContext.current
    return ImageRequest.Builder(context)
        .data(model)
        .crossfade(true)
        .build()
}

val AlbumArtThumbnailSize = 56.dp
private val AboutHeroPlaceholderHeight = 200.dp
