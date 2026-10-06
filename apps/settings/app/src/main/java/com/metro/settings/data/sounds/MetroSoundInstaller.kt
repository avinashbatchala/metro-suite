package com.metro.settings.data.sounds

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.metro.system.MetroSoundCategory
import java.io.File

/**
 * Installs the bundled OGG pack into Android MediaStore (`Ringtones/Metro/`, `Notifications/Metro/`,
 * `Alarms/Metro/`) using scoped APIs — no broad storage permission.
 *
 * Installation is idempotent: a stored URI is reused when still valid, otherwise an existing row is
 * matched by `DISPLAY_NAME` (+ `RELATIVE_PATH`), otherwise a new row is inserted. A failure on one
 * asset never corrupts the others.
 */
class MetroSoundInstaller(
    context: Context,
    private val registry: SettingsSoundRegistry,
    private val store: MetroSoundStore,
) {
    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver

    data class Result(
        val installed: Int,
        val reused: Int,
        val failed: Int,
        val packVersion: Int,
        val warnings: List<String>,
    ) {
        val anyInstalled: Boolean get() = installed + reused > 0
    }

    fun isInstalled(): Boolean = store.installedPackVersion() != null

    /** Installs/repairs every non-UI pack asset and records the pack version. */
    fun install(): Result {
        val pack = registry.load()
        val warnings = pack.warnings.toMutableList()
        registry.missingAssets().forEach { warnings += "missing bundled asset: ${it.assetPath}" }

        var installed = 0
        var reused = 0
        var failed = 0

        pack.assets
            .filter { it.category != MetroSoundCategory.UI }
            .forEach { asset ->
                if (!registry.assetExists(asset)) {
                    failed++
                    return@forEach
                }
                val existing = resolveExistingUri(asset)
                if (existing != null) {
                    store.setInstalledUri(asset.id, existing.toString())
                    reused++
                    return@forEach
                }
                val uri = insertAsset(asset)
                if (uri != null) {
                    store.setInstalledUri(asset.id, uri.toString())
                    installed++
                } else {
                    failed++
                    warnings += "failed to install ${asset.assetPath}"
                }
            }

        if (failed == 0 || installed + reused > 0) {
            store.setInstalledPackVersion(pack.pack.version)
        }
        return Result(installed, reused, failed, pack.pack.version, warnings)
    }

    /** A still-valid stored URI for [asset], repairing the store entry, or null. */
    fun existingUri(asset: MetroSoundAsset): Uri? = resolveExistingUri(asset)

    private fun resolveExistingUri(asset: MetroSoundAsset): Uri? {
        store.installedUri(asset.id)?.let { stored ->
            val uri = runCatching { Uri.parse(stored) }.getOrNull()
            if (uri != null && rowExists(uri)) return uri
            store.removeInstalledUri(asset.id)
        }

        val displayName = SettingsSoundRegistry.displayNameFor(asset)
        val relativePath = SettingsSoundRegistry.relativePathFor(asset.category)
        val columns = arrayOf(MediaStore.Audio.Media._ID)
        val selection: String
        val args: Array<String>
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            selection = "${MediaStore.Audio.Media.DISPLAY_NAME}=? AND ${MediaStore.Audio.Media.RELATIVE_PATH}=?"
            args = arrayOf(displayName, relativePath)
        } else {
            selection = "${MediaStore.Audio.Media.DISPLAY_NAME}=?"
            args = arrayOf(displayName)
        }
        return runCatching {
            resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, columns, selection, args, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        cursor.getLong(0),
                    )
                } else {
                    null
                }
            }
        }.getOrNull()
    }

    private fun rowExists(uri: Uri): Boolean = runCatching {
        resolver.query(uri, arrayOf(MediaStore.Audio.Media._ID), null, null, null)
            ?.use { it.count > 0 }
            ?: false
    }.getOrDefault(false)

    private fun insertAsset(asset: MetroSoundAsset): Uri? {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, SettingsSoundRegistry.displayNameFor(asset))
            put(MediaStore.Audio.Media.MIME_TYPE, SettingsSoundRegistry.mimeFor(asset))
            put(MediaStore.Audio.Media.TITLE, asset.title)
            put(MediaStore.Audio.Media.IS_MUSIC, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Audio.Media.RELATIVE_PATH, SettingsSoundRegistry.relativePathFor(asset.category))
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            } else {
                // Legacy path — requires WRITE_EXTERNAL_STORAGE, which we deliberately do not
                // request; the insert fails gracefully and is reported as a failure.
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RINGTONES),
                    "Metro",
                )
                put(MediaStore.Audio.Media.DATA, File(dir, SettingsSoundRegistry.displayNameFor(asset)).absolutePath)
            }
        }
        val uri = runCatching { resolver.insert(collection, values) }.getOrNull() ?: return null
        return try {
            resolver.openOutputStream(uri)?.use { output ->
                appContext.assets.open(registry.fullPath(asset)).use { input -> input.copyTo(output) }
            } ?: run {
                resolver.delete(uri, null, null)
                return null
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                resolver.update(
                    uri,
                    ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) },
                    null,
                    null,
                )
            }
            uri
        } catch (_: Exception) {
            runCatching { resolver.delete(uri, null, null) }
            null
        }
    }
}
