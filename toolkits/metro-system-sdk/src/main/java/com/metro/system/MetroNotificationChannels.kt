package com.metro.system

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.provider.Settings

/**
 * Shared helpers for Metro notification-channel sound defaults and channel settings deep links.
 *
 * Android notification channels become **user-controlled after creation**; these helpers therefore
 * only apply a Metro default when the channel is first created. Existing channels are never mutated
 * or recreated.
 */
object MetroNotificationChannels {

    /** Opens Android's per-channel notification settings for [packageName]/[channelId]. */
    fun openChannelSettings(context: Context, packageName: String, channelId: String) {
        val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    /**
     * Applies the Metro sound for [role] to a **newly created** channel only. Safe no-op when the
     * channel already exists (user owns it) or when the sound is not installed.
     */
    fun applyInitialSound(
        context: Context,
        manager: NotificationManager,
        channel: NotificationChannel,
        role: MetroSoundRole,
    ) {
        if (manager.getNotificationChannel(channel.id) != null) return
        val uri = MetroSoundContract.resolveUri(context.contentResolver, role) ?: return
        channel.setSound(uri, audioAttributesFor(role))
    }

    /** Sensible audio attributes per role (never routes alarms through the music stream). */
    fun audioAttributesFor(role: MetroSoundRole): AudioAttributes = when (role) {
        MetroSoundRole.PHONE_RINGTONE -> AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        MetroSoundRole.ALARM, MetroSoundRole.TIMER -> AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        else -> AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
    }
}
