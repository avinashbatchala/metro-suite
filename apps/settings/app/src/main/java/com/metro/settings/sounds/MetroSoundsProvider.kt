package com.metro.settings.sounds

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import com.metro.settings.data.sounds.MetroSoundsController
import com.metro.system.MetroSoundContract
import com.metro.system.MetroSoundRole

/**
 * Read-only suite provider exposing the selected Metro sound for a semantic role. Consumer apps read
 * via [MetroSoundContract]; they never see Settings storage, asset paths, or preferences.
 *
 * Authority: `com.metro.settings.sounds`
 * Paths: `/role/<ROLE>` (resolved sound) and `/pack` (installation status/version).
 */
class MetroSoundsProvider : ContentProvider() {

    private val matcher = UriMatcher(UriMatcher.NO_MATCH).apply {
        addURI(MetroSoundContract.AUTHORITY, "${MetroSoundContract.PATH_ROLE}/*", CODE_ROLE)
        addURI(MetroSoundContract.AUTHORITY, MetroSoundContract.PATH_PACK, CODE_PACK)
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? {
        val context = context ?: return null
        return when (matcher.match(uri)) {
            CODE_ROLE -> {
                val role = MetroSoundRole.fromKey(uri.lastPathSegment) ?: return null
                val info = MetroSoundsController(context).roleInfo(role)
                MatrixCursor(ROLE_COLUMNS).apply {
                    addRow(
                        arrayOf(
                            role.name,
                            info.soundId,
                            info.title,
                            info.category.name,
                            info.uri?.toString(),
                        ),
                    )
                }
            }
            CODE_PACK -> {
                val store = MetroSoundsController(context).store
                MatrixCursor(PACK_COLUMNS).apply {
                    addRow(arrayOf<Any?>(store.installedPackVersion(), if (store.installedPackVersion() != null) 1 else 0))
                }
            }
            else -> null
        }
    }

    override fun getType(uri: Uri): String? = when (matcher.match(uri)) {
        CODE_ROLE -> "vnd.android.cursor.item/vnd.metro.sound"
        CODE_PACK -> "vnd.android.cursor.item/vnd.metro.soundpack"
        else -> null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    private companion object {
        const val CODE_ROLE = 1
        const val CODE_PACK = 2

        val ROLE_COLUMNS = arrayOf(
            MetroSoundContract.Columns.ROLE,
            MetroSoundContract.Columns.SOUND_ID,
            MetroSoundContract.Columns.TITLE,
            MetroSoundContract.Columns.CATEGORY,
            MetroSoundContract.Columns.URI,
        )

        val PACK_COLUMNS = arrayOf(
            MetroSoundContract.Columns.PACK_VERSION,
            MetroSoundContract.Columns.INSTALLED,
        )
    }
}
