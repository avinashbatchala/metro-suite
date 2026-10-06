package com.metro.settings.data.sounds

import android.content.Context
import com.metro.system.MetroSoundCategory
import com.metro.system.MetroSoundRole
import java.util.Locale

/**
 * Reads and caches the bundled pack manifest from `assets/metro_sounds/manifest.json`.
 * The registry is Settings-local; other apps only ever see [com.metro.system.MetroSoundContract].
 */
class SettingsSoundRegistry(private val context: Context) {

    @Volatile
    private var cached: ParsedSoundPack? = null

    fun load(): ParsedSoundPack {
        cached?.let { return it }
        val json = context.assets.open(MANIFEST_ASSET).use { it.readBytes().decodeToString() }
        val parsed = MetroSoundManifest.parse(json)
        cached = parsed
        return parsed
    }

    fun assetsFor(category: MetroSoundCategory): List<MetroSoundAsset> =
        load().assets.filter { it.category == category }

    fun assetById(id: String): MetroSoundAsset? =
        load().assets.firstOrNull { it.id == id }

    fun roleAsset(role: MetroSoundRole): MetroSoundAsset? =
        load().assets.firstOrNull { it.role == role }

    fun assetTitle(id: String?): String? =
        id?.let { assetById(it)?.title }

    /** Full asset path under the APK assets (`metro_sounds/<path>`). */
    fun fullPath(asset: MetroSoundAsset): String = "$ASSET_DIR/${asset.assetPath}"

    fun assetExists(asset: MetroSoundAsset): Boolean =
        runCatching { context.assets.open(fullPath(asset)).close(); true }.getOrDefault(false)

    fun missingAssets(): List<MetroSoundAsset> = load().assets.filterNot(::assetExists)

    fun assetsForRole(role: MetroSoundRole): List<MetroSoundAsset> {
        val category = categoryForRole(role)
        return load().assets.filter { it.category == category }
    }

    companion object {
        const val ASSET_DIR = "metro_sounds"
        const val MANIFEST_ASSET = "$ASSET_DIR/manifest.json"

        fun categoryForRole(role: MetroSoundRole): MetroSoundCategory = when (role) {
            MetroSoundRole.PHONE_RINGTONE -> MetroSoundCategory.RINGTONE
            MetroSoundRole.ALARM, MetroSoundRole.TIMER -> MetroSoundCategory.ALARM
            else -> MetroSoundCategory.NOTIFICATION
        }

        /** MediaStore destination directory (relative path) for a category. */
        fun relativePathFor(category: MetroSoundCategory): String = when (category) {
            MetroSoundCategory.RINGTONE -> "Ringtones/Metro/"
            MetroSoundCategory.NOTIFICATION -> "Notifications/Metro/"
            MetroSoundCategory.ALARM -> "Alarms/Metro/"
            MetroSoundCategory.UI -> "Notifications/Metro/"
        }

        fun displayNameFor(asset: MetroSoundAsset): String =
            "${asset.title.replace('/', '_').replace('%', '_')}.ogg".ifBlank { "${asset.id}.ogg" }

        fun mimeFor(asset: MetroSoundAsset): String =
            if (asset.assetPath.lowercase(Locale.US).endsWith(".ogg")) "audio/ogg" else "audio/mpeg"
    }
}
