package com.metro.clock.alarms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.metro.clock.R
import com.metro.clock.data.AlarmEntity

/** Alarm notification channel + the high-priority (full-screen) ringing notification. */
object AlarmNotifications {

    const val CHANNEL_ALARMS = "alarms"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ALARMS) == null) {
            val channel = NotificationChannel(
                CHANNEL_ALARMS,
                context.getString(R.string.alarm_notification_channel),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.alarm_notification_channel_description)
                setSound(null, null)
                enableVibration(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun notifyRinging(context: Context, alarm: AlarmEntity, triggerMillis: Long) {
        ensureChannels(context)
        val fullScreen = PendingIntent.getActivity(
            context,
            alarm.id.toInt(),
            AlarmRingingActivity.intent(context, alarm.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val dismiss = alarmAction(context, alarm.id, AlarmRingingActivity.ACTION_DISMISS)
        val snooze = alarmAction(context, alarm.id, AlarmRingingActivity.ACTION_SNOOZE)

        val notification = NotificationCompat.Builder(context, CHANNEL_ALARMS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(alarm.label.ifBlank { context.getString(R.string.alarm_ringing) })
            .setContentText(AlarmFormat.time(alarm))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreen, true)
            .setContentIntent(fullScreen)
            .addAction(0, context.getString(R.string.alarm_snooze_action), snooze)
            .addAction(0, context.getString(R.string.alarm_dismiss), dismiss)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(alarmNotificationId(alarm.id), notification)
        }
    }

    fun cancelRinging(context: Context, alarmId: Long) {
        runCatching {
            NotificationManagerCompat.from(context).cancel(alarmNotificationId(alarmId))
        }
    }

    fun alarmNotificationId(alarmId: Long): Int = (0x1000_0000 + (alarmId and 0x0FFF_FFFF)).toInt()

    private fun alarmAction(context: Context, alarmId: Long, action: String): PendingIntent {
        val intent = AlarmRingingActivity.intent(context, alarmId).apply { this.action = action }
        return PendingIntent.getActivity(
            context,
            (alarmId.toInt() xor action.hashCode()),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
