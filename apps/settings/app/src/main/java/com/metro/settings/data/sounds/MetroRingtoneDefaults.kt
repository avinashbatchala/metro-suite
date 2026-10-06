package com.metro.settings.data.sounds

import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import com.metro.system.MetroSoundRole

/**
 * Thin wrapper over Android's ringtone defaults. Setting a system default requires the
 * **Modify System Settings** capability (`Settings.System.canWrite`), which Settings requests only
 * when the user chooses an action that needs it.
 */
class MetroRingtoneDefaults(private val context: Context) {
    private val appContext = context.applicationContext

    fun canWrite(): Boolean = Settings.System.canWrite(appContext)

    fun currentDefault(type: Int): Uri? =
        RingtoneManager.getActualDefaultRingtoneUri(appContext, type)

    /** Sets the system default for [type]; returns false when the capability is missing. */
    fun setDefault(type: Int, uri: Uri?): Boolean {
        if (!canWrite()) return false
        return runCatching {
            RingtoneManager.setActualDefaultRingtoneUri(appContext, type, uri)
            true
        }.getOrDefault(false)
    }

    fun writeSettingsIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${appContext.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    companion object {
        /** System ringtone type for a role, or null for semantic-only roles. */
        fun systemTypeFor(role: MetroSoundRole): Int? = when (role) {
            MetroSoundRole.PHONE_RINGTONE -> RingtoneManager.TYPE_RINGTONE
            MetroSoundRole.SYSTEM_NOTIFICATION -> RingtoneManager.TYPE_NOTIFICATION
            MetroSoundRole.ALARM -> RingtoneManager.TYPE_ALARM
            else -> null
        }

        fun typeFromKey(key: String): Int? = when (key) {
            "ringtone" -> RingtoneManager.TYPE_RINGTONE
            "notification" -> RingtoneManager.TYPE_NOTIFICATION
            "alarm" -> RingtoneManager.TYPE_ALARM
            else -> null
        }
    }
}
