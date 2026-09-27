package com.metro.launcher.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.metro.launcher.R
import com.metro.launcher.data.PinnedTileSize
import com.metro.launcher.data.TileCustomIcon
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroDimens
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

/** Long edge for the stored tile icon JPEG; short edge follows the crop aspect. */
private const val CropTargetLongEdge = 512

/**
 * Photo crop for a custom tile icon — pan/pinch inside a frame matching [tileSize]'s
 * footprint (1:1 or 2:1), then check to write a draft JPEG.
 */
@Composable
fun TileIconCropScreen(
    sourceUri: Uri,
    packageName: String,
    tileId: String,
    tileSize: PinnedTileSize,
    onSaved: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var sourceBitmap by remember(sourceUri) { mutableStateOf<Bitmap?>(null) }
    var loadFailed by remember(sourceUri) { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var viewportWidthPx by remember { mutableFloatStateOf(0f) }
    var viewportHeightPx by remember { mutableFloatStateOf(0f) }
    val aspect = when (tileSize) {
        PinnedTileSize.FourByTwo -> 2f
        else -> 1f
    }

    LaunchedEffect(sourceUri) {
        val decoded = withContext(Dispatchers.IO) { decodeBitmapForCrop(context, sourceUri) }
        if (decoded == null) {
            loadFailed = true
        } else {
            sourceBitmap = decoded
            scale = 1f
            offset = Offset.Zero
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background),
    ) {
        when {
            loadFailed -> {
                MetroText(
                    text = stringResource(R.string.tile_customize_icon_load_failed),
                    style = MetroTextStyle.Body,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(MetroDimens.ScreenHorizontalMargin),
                )
            }
            sourceBitmap != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(androidx.compose.ui.graphics.Color.Black),
                    contentAlignment = Alignment.Center,
                ) {
                    TileIconCropViewport(
                        bitmap = sourceBitmap!!,
                        scale = scale,
                        offset = offset,
                        onViewportSized = { w, h ->
                            viewportWidthPx = w
                            viewportHeightPx = h
                        },
                        onTransform = { nextScale, nextOffset ->
                            scale = nextScale
                            offset = nextOffset
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MetroDimens.ScreenHorizontalMargin)
                            .aspectRatio(aspect),
                    )
                }
            }
        }

        MetroAppBar(
            icons = listOf(
                MetroAppBarIcon(
                    type = MetroSystemIconType.Check,
                    label = stringResource(R.string.tile_customize_save),
                    onClick = {
                        val bmp = sourceBitmap ?: return@MetroAppBarIcon
                        if (saving || viewportWidthPx <= 0f || viewportHeightPx <= 0f) {
                            return@MetroAppBarIcon
                        }
                        saving = true
                        val viewW = viewportWidthPx
                        val viewH = viewportHeightPx
                        val userScale = scale
                        val userOffset = offset
                        scope.launch {
                            val cropped = withContext(Dispatchers.Default) {
                                renderCroppedTileIcon(
                                    source = bmp,
                                    viewWidthPx = viewW,
                                    viewHeightPx = viewH,
                                    userScale = userScale,
                                    offset = userOffset,
                                )
                            }
                            val ok = withContext(Dispatchers.IO) {
                                TileCustomIcon.saveDraft(
                                    context = context,
                                    packageName = packageName,
                                    tileId = tileId,
                                    bitmap = cropped,
                                )
                            }
                            if (cropped !== bmp) cropped.recycle()
                            saving = false
                            if (ok) onSaved()
                        }
                    },
                    enabled = sourceBitmap != null && !saving && !loadFailed,
                ),
                MetroAppBarIcon(
                    type = MetroSystemIconType.Close,
                    label = stringResource(R.string.tile_customize_close),
                    onClick = onCancel,
                ),
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun TileIconCropViewport(
    bitmap: Bitmap,
    scale: Float,
    offset: Offset,
    onViewportSized: (Float, Float) -> Unit,
    onTransform: (Float, Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier.background(androidx.compose.ui.graphics.Color.Black),
    ) {
        val density = LocalDensity.current
        val viewW = with(density) { maxWidth.toPx() }
        val viewH = with(density) { maxHeight.toPx() }
        LaunchedEffect(viewW, viewH) {
            onViewportSized(viewW, viewH)
        }

        val cover = max(viewW / bitmap.width, viewH / bitmap.height)
        val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(bitmap, viewW, viewH, scale, offset) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val nextScale = (scale * zoom).coerceIn(1f, 4f)
                        val drawnW = bitmap.width * cover * nextScale
                        val drawnH = bitmap.height * cover * nextScale
                        val maxX = max(0f, (drawnW - viewW) / 2f)
                        val maxY = max(0f, (drawnH - viewH) / 2f)
                        onTransform(
                            nextScale,
                            Offset(
                                x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                                y = (offset.y + pan.y).coerceIn(-maxY, maxY),
                            ),
                        )
                    }
                },
        ) {
            val drawnW = bitmap.width * cover * scale
            val drawnH = bitmap.height * cover * scale
            val left = (size.width - drawnW) / 2f + offset.x
            val top = (size.height - drawnH) / 2f + offset.y
            drawImage(
                image = imageBitmap,
                dstOffset = IntOffset(left.toInt(), top.toInt()),
                dstSize = IntSize(
                    drawnW.toInt().coerceAtLeast(1),
                    drawnH.toInt().coerceAtLeast(1),
                ),
            )
        }
    }
}

/** Long edge cap when decoding the source photo for crop (avoids OOM on 12MP+ images). */
private const val CropDecodeMaxEdge = 2048

private fun decodeBitmapForCrop(context: android.content.Context, uri: Uri): Bitmap? {
    return runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }
        var sample = 1
        val w = bounds.outWidth.coerceAtLeast(1)
        val h = bounds.outHeight.coerceAtLeast(1)
        while (w / sample > CropDecodeMaxEdge || h / sample > CropDecodeMaxEdge) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, opts)
        }
    }.getOrNull()
}

/**
 * Maps the visible crop viewport onto a JPEG sized to the tile aspect.
 */
internal fun renderCroppedTileIcon(
    source: Bitmap,
    viewWidthPx: Float,
    viewHeightPx: Float,
    userScale: Float,
    offset: Offset,
): Bitmap {
    val cover = max(viewWidthPx / source.width, viewHeightPx / source.height)
    val scale = cover * userScale
    val drawnW = source.width * scale
    val drawnH = source.height * scale
    val viewLeft = (viewWidthPx - drawnW) / 2f + offset.x
    val viewTop = (viewHeightPx - drawnH) / 2f + offset.y

    val aspect = viewWidthPx / viewHeightPx
    val targetW: Int
    val targetH: Int
    if (aspect >= 1f) {
        targetW = CropTargetLongEdge
        targetH = (CropTargetLongEdge / aspect).toInt().coerceAtLeast(1)
    } else {
        targetH = CropTargetLongEdge
        targetW = (CropTargetLongEdge * aspect).toInt().coerceAtLeast(1)
    }
    val scaleToTarget = targetW / viewWidthPx
    val out = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    canvas.drawColor(android.graphics.Color.BLACK)
    val matrix = Matrix().apply {
        postScale(scale, scale)
        postTranslate(viewLeft, viewTop)
        postScale(scaleToTarget, scaleToTarget)
    }
    canvas.drawBitmap(source, matrix, null)
    return out
}
