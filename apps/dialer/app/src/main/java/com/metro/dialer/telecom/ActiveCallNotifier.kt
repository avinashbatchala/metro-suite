package com.metro.dialer.telecom

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.metro.dialer.InCallActivity
import com.metro.dialer.R
import com.metro.system.MetroPreferences

/**
 * Ongoing active-call notification shown while the in-call UI is backgrounded.
 *
 * Uses the platform chronometer (stable connected timestamp) rather than re-posting the
 * notification every second. The Metro notifications overlay reads this notification to present
 * the green return-to-call strip.
 */
object ActiveCallNotifier {
    const val NOTIFICATION_ID = 0xC411
    const val NOTIFICATION_TAG = "active_call"
    private const val CHANNEL_ID = "metro_active_call"

    fun start(context: Context, call: MetroTelecomCall) {
        ensureChannel(context.applicationContext)
        post(context.applicationContext, call)
    }

    fun update(context: Context, call: MetroTelecomCall) = start(context, call)

    fun stop(context: Context) {
        NotificationManagerCompat.from(context.applicationContext)
            .cancel(NOTIFICATION_TAG, NOTIFICATION_ID)
    }

    fun postFromSession(context: Context, session: MetroCallSessionState) {
        val call = session.primaryCall ?: return
        start(context, call)
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun post(context: Context, call: MetroTelecomCall) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, InCallActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val endIntent = PendingIntent.getBroadcast(
            context,
            1,
            Intent(context, EndCallActionReceiver::class.java).setAction(EndCallActionReceiver.ACTION_END),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val connected = call.state.isConnected
        val statusText = when (call.state) {
            MetroCallState.ACTIVE, MetroCallState.HOLDING -> {
                if (call.state == MetroCallState.HOLDING) {
                    context.getString(R.string.on_hold)
                } else {
                    context.getString(R.string.active_call)
                }
            }
            MetroCallState.DIALING, MetroCallState.CONNECTING ->
                context.getString(R.string.dialling)
            MetroCallState.RINGING -> context.getString(R.string.calling)
            else -> context.getString(R.string.active_call)
        }

        val accent = runCatching {
            MetroPreferences(context).accentColor.toArgb()
        }.getOrDefault(0xFF1BA1E2.toInt())

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_phone)
            .setContentTitle(call.primaryLabel)
            .setContentText(statusText)
            .setSubText(context.getString(R.string.active_call_notification_label))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setColor(accent)
            .setContentIntent(contentIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                R.drawable.ic_notification_phone,
                context.getString(R.string.end_call),
                endIntent,
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val connectTime = call.connectTimeMillis
        if (connected && connectTime != null) {
            builder
                .setUsesChronometer(true)
                .setWhen(connectTime)
                .setShowWhen(true)
                .setChronometerCountDown(false)
        }

        runCatching {
            manager.notify(NOTIFICATION_TAG, NOTIFICATION_ID, builder.build())
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.deleteNotificationChannel("metro_active_call_quiet")
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.active_call_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.active_call_channel_description)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(null, null)
            },
        )
    }
}
