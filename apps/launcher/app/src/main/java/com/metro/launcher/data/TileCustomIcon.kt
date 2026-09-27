package com.metro.launcher.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream

/**
 * Per-pin custom tile icon JPEG — chosen via the Start-background-style photo picker + crop.
 * Crop aspect matches the pin's tile footprint (1:1 or 2:1).
 */
object TileCustomIcon {
    private const val DIR_NAME = "tile_icons"
    private const val DRAFT_DIR_NAME = "tile_icons_draft"
    const val MAX_EDGE_PX = 1024

    fun file(context: Context, packageName: String, tileId: String): File =
        File(dir(context), fileName(packageName, tileId))

    fun draftFile(context: Context, packageName: String, tileId: String): File =
        File(draftDir(context), fileName(packageName, tileId))

    fun isSet(context: Context, packageName: String, tileId: String): Boolean =
        file(context, packageName, tileId).exists()

    fun hasDraft(context: Context, packageName: String, tileId: String): Boolean =
        draftFile(context, packageName, tileId).exists()

    fun saveDraft(context: Context, packageName: String, tileId: String, bitmap: Bitmap): Boolean {
        val scaled = scaleDownIfNeeded(bitmap)
        val outFile = draftFile(context, packageName, tileId)
        val ok = writeJpeg(outFile, scaled)
        if (scaled !== bitmap) scaled.recycle()
        return ok
    }

    /** Promotes a draft crop to the permanent pin file (or clears both when [enabled] is false). */
    fun commit(
        context: Context,
        packageName: String,
        tileId: String,
        enabled: Boolean,
    ) {
        val appContext = context.applicationContext
        val permanent = file(appContext, packageName, tileId)
        val draft = draftFile(appContext, packageName, tileId)
        if (!enabled) {
            runCatching { permanent.delete() }
            runCatching { draft.delete() }
            return
        }
        when {
            draft.exists() -> {
                runCatching { permanent.delete() }
                if (!draft.renameTo(permanent)) {
                    draft.copyTo(permanent, overwrite = true)
                    draft.delete()
                }
            }
            !permanent.exists() -> Unit
        }
    }

    fun clearDraft(context: Context, packageName: String, tileId: String) {
        runCatching { draftFile(context, packageName, tileId).delete() }
    }

    fun clear(context: Context, packageName: String, tileId: String) {
        val appContext = context.applicationContext
        runCatching { file(appContext, packageName, tileId).delete() }
        runCatching { draftFile(appContext, packageName, tileId).delete() }
    }

    /**
     * Prefers an unsaved draft crop, then the committed pin icon.
     */
    fun decodeForPreview(
        context: Context,
        packageName: String,
        tileId: String,
        opts: BitmapFactory.Options? = null,
    ): Bitmap? {
        val draft = draftFile(context, packageName, tileId)
        if (draft.exists()) {
            return runCatching {
                BitmapFactory.decodeFile(draft.absolutePath, opts)
            }.getOrNull()
        }
        return decode(context, packageName, tileId, opts)
    }

    fun decode(
        context: Context,
        packageName: String,
        tileId: String,
        opts: BitmapFactory.Options? = null,
    ): Bitmap? {
        val path = file(context, packageName, tileId)
        if (!path.exists()) return null
        return runCatching {
            BitmapFactory.decodeFile(path.absolutePath, opts)
        }.getOrNull()
    }

    private fun dir(context: Context): File =
        File(context.applicationContext.filesDir, DIR_NAME).also { it.mkdirs() }

    private fun draftDir(context: Context): File =
        File(context.applicationContext.filesDir, DRAFT_DIR_NAME).also { it.mkdirs() }

    private fun fileName(packageName: String, tileId: String): String {
        val safePkg = packageName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val safeId = tileId.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        return "${safePkg}__$safeId.jpg"
    }

    private fun writeJpeg(outFile: File, bitmap: Bitmap): Boolean =
        runCatching {
            outFile.parentFile?.mkdirs()
            FileOutputStream(outFile).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            }
            true
        }.getOrDefault(false)

    private fun scaleDownIfNeeded(bitmap: Bitmap): Bitmap {
        val maxEdge = maxOf(bitmap.width, bitmap.height)
        if (maxEdge <= MAX_EDGE_PX) return bitmap
        val scale = MAX_EDGE_PX.toFloat() / maxEdge.toFloat()
        val w = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val h = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, w, h, true)
    }
}
