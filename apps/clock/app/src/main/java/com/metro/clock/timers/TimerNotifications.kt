package com.metro.clock.timers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.metro.clock.MainActivity
import com.metro.clock.R
import com.metro.clock.data.TimerEntity
import com.metro.system.MetroNotificationChannels
import com.metro.system.MetroSoundRole

object TimerNotifications {

    const val CHANNEL_TIMERS = "timers"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_TIMERS) == null) {
            val channel = NotificationChannel(
                CHANNEL_TIMERS,
                context.getString(R.string.timer_notification_channel),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.timer_notification_channel_description)
            }
            // First creation only: adopt the suite's Metro TIMER sound when installed.
            // Existing channels are user-owned and never mutated.
            MetroNotificationChannels.applyInitialSound(
                context = context,
                manager = manager,
                channel = channel,
                role = MetroSoundRole.TIMER,
            )
            manager.createNotificationChannel(channel)
        }
    }

    fun notifyFinished(context: Context, timer: TimerEntity) {
        ensureChannels(context)
        val deepLink = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("metro://clock/timer/${timer.id}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            timer.id.toInt(),
            deepLink,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_TIMERS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.timer_finished_title))
            .setContentText(timer.label)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(notificationId(timer.id), notification)
        }
    }

    fun cancel(context: Context, timerId: Long) {
        runCatching { NotificationManagerCompat.from(context).cancel(notificationId(timerId)) }
    }

    private fun notificationId(timerId: Long): Int =
        (0x2000_0000 + (timerId and 0x0FFF_FFFF)).toInt()
}
