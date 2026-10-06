package com.metro.training.notify

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.metro.training.MainActivity
import com.metro.training.R

/**
 * Ongoing workout notification. Shows a live chronometer (counts up during the workout, or down
 * during a rest) without any per-second tick from the app.
 */
object WorkoutNotifier {
    const val NOTIFICATION_ID = 0x7A17
    const val NOTIFICATION_TAG = "training_workout"
    private const val CHANNEL_ID = "metro_training_workout"

    fun showWorkout(context: Context, routineName: String, startedAt: Long) {
        post(context, routineName, title = routineName, whenMillis = startedAt, countDown = false, text = "workout in progress")
    }

    fun showRest(context: Context, routineName: String, restDeadline: Long) {
        post(context, routineName, title = routineName, whenMillis = restDeadline, countDown = true, text = "rest")
    }

    fun stop(context: Context) {
        NotificationManagerCompat.from(context.applicationContext).cancel(NOTIFICATION_TAG, NOTIFICATION_ID)
    }

    private fun post(
        context: Context,
        routineName: String,
        title: String,
        whenMillis: Long,
        countDown: Boolean,
        text: String,
    ) {
        val appContext = context.applicationContext
        ensureChannel(appContext)
        val manager = NotificationManagerCompat.from(appContext)
        if (!manager.areNotificationsEnabled()) return

        val open = PendingIntent.getActivity(
            appContext,
            0,
            Intent(appContext, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_training)
            .setContentTitle(title.ifBlank { "Workout" })
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(true)
            .setWhen(whenMillis)
            .setUsesChronometer(true)
            .setChronometerCountDown(countDown)
            .setContentIntent(open)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        runCatching { manager.notify(NOTIFICATION_TAG, NOTIFICATION_ID, notification) }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.workout_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.workout_channel_description)
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            },
        )
    }
}
