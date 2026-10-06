package com.metro.clock.alarms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.metro.clock.data.ClockDatabase
import com.metro.clock.tiles.ClockTileRefresh
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fires when a scheduled alarm's time arrives. Starts the ringing experience, disables one-time
 * alarms, and re-schedules repeating alarms to their next concrete occurrence.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_ALARM) return
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        if (alarmId < 0L) return
        val snoozed = intent.getBooleanExtra(EXTRA_SNOOZED, false)
        val appContext = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = ClockDatabase.get(appContext).dao()
                val alarm = dao.alarm(alarmId) ?: return@launch
                if (!alarm.enabled && !snoozed) return@launch

                AlarmNotifications.ensureChannels(appContext)
                AlarmNotifications.notifyRinging(appContext, alarm, System.currentTimeMillis())
                runCatching {
                    appContext.startActivity(
                        AlarmRingingActivity.intent(appContext, alarm.id)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }

                val scheduler = AlarmScheduler(appContext)
                if (!snoozed) {
                    if (alarm.repeatMask == 0) {
                        // One-time alarm: disable after firing.
                        dao.updateAlarm(alarm.copy(enabled = false))
                    } else {
                        scheduler.scheduleNext(alarm)
                    }
                    ClockTileRefresh.request(appContext)
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_ALARM = "com.metro.clock.action.ALARM"
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_SNOOZED = "snoozed"
    }
}
