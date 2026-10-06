package com.metro.clock.timers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.metro.clock.data.TimerEntity

/**
 * Schedules a per-timer completion alarm on the monotonic clock (`ELAPSED_REALTIME_WAKEUP`), so
 * multiple concurrent timers never overwrite each other (each has a stable PendingIntent identity).
 */
class TimerScheduler(private val context: Context) {

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExactAlarms(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) alarmManager.canScheduleExactAlarms() else true

    fun schedule(timer: TimerEntity) {
        if (TimerState.fromName(timer.state) != TimerState.RUNNING) {
            cancel(timer.id)
            return
        }
        runCatching {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                timer.targetElapsedRealtime,
                pendingIntent(timer.id),
            )
        }
    }

    fun cancel(timerId: Long) {
        runCatching { alarmManager.cancel(pendingIntent(timerId)) }
    }

    private fun pendingIntent(timerId: Long): PendingIntent {
        val intent = Intent(context, TimerReceiver::class.java).apply {
            action = TimerReceiver.ACTION_TIMER
            putExtra(TimerReceiver.EXTRA_TIMER_ID, timerId)
            data = Uri.parse("metroclock://timer/$timerId")
        }
        return PendingIntent.getBroadcast(
            context,
            (timerId and 0x0FFF_FFFFL).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
