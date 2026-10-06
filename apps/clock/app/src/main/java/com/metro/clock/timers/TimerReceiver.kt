package com.metro.clock.timers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.metro.clock.data.ClockDatabase
import com.metro.clock.tiles.ClockTileRefresh
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Fires when an individual timer reaches zero: marks it FINISHED and notifies. */
class TimerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TIMER) return
        val timerId = intent.getLongExtra(EXTRA_TIMER_ID, -1L)
        if (timerId < 0L) return
        val appContext = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = ClockDatabase.get(appContext).dao()
                val timer = dao.timer(timerId) ?: return@launch
                if (TimerState.fromName(timer.state) != TimerState.RUNNING) return@launch
                dao.updateTimer(
                    timer.copy(
                        state = TimerState.FINISHED.name,
                        remainingWhenPaused = 0L,
                    ),
                )
                TimerNotifications.notifyFinished(appContext, timer)
                ClockTileRefresh.request(appContext)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_TIMER = "com.metro.clock.action.TIMER"
        const val EXTRA_TIMER_ID = "timer_id"
    }
}
