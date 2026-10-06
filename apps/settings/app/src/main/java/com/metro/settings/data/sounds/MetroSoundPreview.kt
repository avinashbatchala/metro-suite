package com.metro.settings.data.sounds

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import com.metro.system.MetroNotificationChannels
import com.metro.system.MetroSoundRole

/**
 * Single-instance sound preview for Settings. Uses [Ringtone] (no background service) and stops any
 * previous preview when a new one starts or when the page is left.
 */
object MetroSoundPreview {
    private var current: Ringtone? = null
    private var currentUri: Uri? = null

    fun play(context: Context, uri: Uri, role: MetroSoundRole) {
        stop()
        val ringtone = runCatching { RingtoneManager.getRingtone(context, uri) }.getOrNull() ?: return
        runCatching {
            ringtone.audioAttributes = MetroNotificationChannels.audioAttributesFor(role)
            ringtone.play()
        }.onSuccess {
            current = ringtone
            currentUri = uri
        }
    }

    fun stop() {
        runCatching { current?.stop() }
        current = null
        currentUri = null
    }

    fun isPlaying(): Boolean = runCatching { current?.isPlaying == true }.getOrDefault(false)

    fun playingUri(): Uri? = currentUri
}
