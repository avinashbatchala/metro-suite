package com.metro.clock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.metro.clock.alarms.AlarmNotifications
import com.metro.clock.alarms.AlarmRepository
import com.metro.clock.stopwatch.StopwatchRepository
import com.metro.clock.tiles.ClockTileRefresh
import com.metro.clock.timers.TimerNotifications
import com.metro.clock.timers.TimerRepository
import com.metro.clock.worldclock.WorldClockRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Restores Clock state after boot / time change / package update:
 * alarms are rescheduled to their next occurrence, running timers are resolved against the
 * wall-clock target, running stopwatches absorb the wall-clock gap, and tiles refresh.
 */
class ClockRestoreReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AlarmNotifications.ensureChannels(appContext)
                TimerNotifications.ensureChannels(appContext)
                AlarmRepository(appContext).restoreAll()
                TimerRepository(appContext).restore()
                StopwatchRepository(appContext).restore()
                WorldClockRepository(appContext).seedDefaultsIfEmpty()
                ClockTileRefresh.request(appContext)
            } finally {
                pending.finish()
            }
        }
    }
}
