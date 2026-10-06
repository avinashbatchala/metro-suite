package com.metro.system

import android.content.ContentResolver
import android.net.Uri

/**
 * Read-only suite contract for resolving semantic Metro sounds. Hosted by `com.metro.settings`
 * (authority [AUTHORITY]). Consumer apps call [resolve]/[resolveUri] and fall back safely to null
 * when the pack is not installed or the provider is unavailable.
 *
 * Settings persists the semantic selection (sound id) separately from the MediaStore URI, so a URI
 * can be repaired/reinstalled without losing the user's choice.
 */
object MetroSoundContract {
    const val AUTHORITY = "com.metro.settings.sounds"
    const val HOST_PACKAGE = "com.metro.settings"

    const val PATH_ROLE = "role"
    const val PATH_PACK = "pack"

    object Columns {
        const val ROLE = "role"
        const val SOUND_ID = "sound_id"
        const val TITLE = "title"
        const val CATEGORY = "category"
        const val URI = "uri"
        const val PACK_VERSION = "pack_version"
        const val INSTALLED = "installed"
    }

    fun roleUri(role: MetroSoundRole): Uri =
        Uri.parse("content://$AUTHORITY/$PATH_ROLE/${role.name}")

    fun packUri(): Uri = Uri.parse("content://$AUTHORITY/$PATH_PACK")

    /** Resolves the selected sound for [role], or null when unavailable/uninstalled. */
    fun resolve(resolver: ContentResolver, role: MetroSoundRole): MetroSoundDescriptor? {
        return try {
            resolver.query(roleUri(role), null, null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return null
                val id = cursor.stringOrNull(Columns.SOUND_ID) ?: return null
                val title = cursor.stringOrNull(Columns.TITLE) ?: id
                val category = MetroSoundCategory.fromKey(cursor.stringOrNull(Columns.CATEGORY))
                    ?: MetroSoundCategory.NOTIFICATION
                MetroSoundDescriptor(id = id, title = title, role = role, category = category)
            }
        } catch (_: Exception) {
            null
        }
    }

    /** The installed MediaStore URI for [role], or null when not installed/resolvable. */
    fun resolveUri(resolver: ContentResolver, role: MetroSoundRole): Uri? {
        return try {
            resolver.query(roleUri(role), null, null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return null
                cursor.stringOrNull(Columns.URI)?.takeIf { it.isNotBlank() }?.let(Uri::parse)
            }
        } catch (_: Exception) {
            null
        }
    }

    /** Installed pack version from Settings, or null when the pack has not been installed. */
    fun installedPackVersion(resolver: ContentResolver): Int? {
        return try {
            resolver.query(packUri(), null, null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return null
                if (cursor.intOrNull(Columns.INSTALLED) != 1) return null
                cursor.intOrNull(Columns.PACK_VERSION)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun android.database.Cursor.stringOrNull(column: String): String? {
        val index = getColumnIndex(column)
        if (index < 0 || isNull(index)) return null
        return getString(index)
    }

    private fun android.database.Cursor.intOrNull(column: String): Int? {
        val index = getColumnIndex(column)
        if (index < 0 || isNull(index)) return null
        return getInt(index)
    }
}
