package com.metro.clock.alarms

import android.content.Context
import com.metro.clock.data.AlarmEntity
import com.metro.clock.data.ClockDatabase
import com.metro.clock.tiles.ClockTileRefresh
import java.time.ZoneId

class AlarmRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dao = ClockDatabase.get(appContext).dao()
    private val scheduler = AlarmScheduler(appContext)

    suspend fun alarms(): List<AlarmEntity> = dao.alarms()

    suspend fun alarm(id: Long): AlarmEntity? = dao.alarm(id)

    suspend fun create(hour: Int, minute: Int): Long =
        insert(AlarmEntity(hour = hour, minute = minute))

    suspend fun insert(alarm: AlarmEntity): Long {
        val id = dao.insertAlarm(alarm)
        dao.alarm(id)?.let { scheduler.scheduleNext(it) }
        ClockTileRefresh.request(appContext)
        return id
    }

    suspend fun save(alarm: AlarmEntity) {
        dao.updateAlarm(alarm)
        if (alarm.enabled) scheduler.scheduleNext(alarm) else scheduler.cancel(alarm.id)
        ClockTileRefresh.request(appContext)
    }

    suspend fun setEnabled(id: Long, enabled: Boolean) {
        val alarm = dao.alarm(id) ?: return
        val updated = alarm.copy(enabled = enabled)
        dao.updateAlarm(updated)
        if (enabled) scheduler.scheduleNext(updated) else scheduler.cancel(id)
        ClockTileRefresh.request(appContext)
    }

    suspend fun delete(id: Long) {
        scheduler.cancel(id)
        dao.deleteAlarm(id)
        ClockTileRefresh.request(appContext)
    }

    /** Called when the user dismisses a ringing alarm. */
    suspend fun dismissAlarm(id: Long) {
        val alarm = dao.alarm(id)
        if (alarm != null && alarm.repeatMask == 0) {
            dao.updateAlarm(alarm.copy(enabled = false))
        }
        scheduler.cancel(id)
        ClockTileRefresh.request(appContext)
    }

    /** Re-arm a ringing alarm after its snooze interval. */
    suspend fun snoozeAlarm(id: Long) {
        val alarm = dao.alarm(id) ?: return
        scheduler.snooze(id, alarm.snoozeMinutes)
    }

    /** Recompute every enabled alarm's next occurrence (boot / time change / package update). */
    suspend fun restoreAll(zone: ZoneId = ZoneId.systemDefault(), now: Long = System.currentTimeMillis()) {
        dao.alarms().forEach { alarm ->
            if (alarm.enabled) scheduler.scheduleNext(alarm, zone, now) else scheduler.cancel(alarm.id)
        }
    }
}
