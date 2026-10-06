package com.metro.clock.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.metro.clock.MainActivity
import com.metro.clock.data.AlarmEntity
import java.time.ZoneId

/**
 * Schedules exact alarms via `AlarmManager.setAlarmClock` (the correct API for a genuine alarm
 * clock: exempt from Doze, shown in the system status bar). Every alarm has a stable PendingIntent
 * identity keyed by its row id.
 */
class AlarmScheduler(private val context: Context) {

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExactAlarms(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

    /** Schedules [alarm] at [triggerAtMillis], or cancels it when null. */
    fun schedule(alarm: AlarmEntity, triggerAtMillis: Long?) {
        if (triggerAtMillis == null || !alarm.enabled) {
            cancel(alarm.id)
            return
        }
        val operation = alarmPendingIntent(alarm.id)
        val show = showPendingIntent(alarm.id)
        val info = AlarmManager.AlarmClockInfo(triggerAtMillis, show)
        runCatching { alarmManager.setAlarmClock(info, operation) }
    }

    fun cancel(alarmId: Long) {
        runCatching { alarmManager.cancel(alarmPendingIntent(alarmId)) }
    }

    /** Recomputes the next occurrence for [alarm] and schedules it. */
    fun scheduleNext(alarm: AlarmEntity, zone: ZoneId = ZoneId.systemDefault(), now: Long = System.currentTimeMillis()) {
        schedule(alarm, AlarmSchedule.nextTrigger(alarm, now, zone))
    }

    /** One-off snooze: re-ring [alarmId] after [minutes], leaving the alarm's own schedule intact. */
    fun snooze(alarmId: Long, minutes: Int, now: Long = System.currentTimeMillis()) {
        val trigger = now + minutes.coerceAtLeast(1) * 60_000L
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_SNOOZED, true)
            data = Uri.parse("metroclock://alarm/snooze/$alarmId")
        }
        val pi = PendingIntent.getBroadcast(
            context,
            snoozeRequestCode(alarmId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        runCatching {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
        }
    }

    private fun alarmPendingIntent(alarmId: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            data = Uri.parse("metroclock://alarm/$alarmId")
        }
        return PendingIntent.getBroadcast(
            context,
            alarmRequestCode(alarmId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun showPendingIntent(alarmId: Long): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("metro://clock/alarm/$alarmId")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return PendingIntent.getActivity(
            context,
            showRequestCode(alarmId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun alarmRequestCode(id: Long): Int = (id and 0x0FFF_FFFFL).toInt()
    private fun snoozeRequestCode(id: Long): Int = ((id and 0x0FFF_FFFFL).toInt()) xor 0x4000_0000
    private fun showRequestCode(id: Long): Int = ((id and 0x0FFF_FFFFL).toInt()) xor 0x2000_0000
}
