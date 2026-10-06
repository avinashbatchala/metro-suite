package com.metro.clock.timers

import android.content.Context
import android.os.SystemClock
import com.metro.clock.data.ClockDatabase
import com.metro.clock.data.TimerEntity
import com.metro.clock.tiles.ClockTileRefresh

/**
 * Owns timer state transitions and schedules a completion alarm per running timer. Persists state
 * transitions and timestamps only — never ticking values.
 */
class TimerRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dao = ClockDatabase.get(appContext).dao()
    private val scheduler = TimerScheduler(appContext)

    suspend fun timers(): List<TimerEntity> = dao.timers()

    suspend fun timer(id: Long): TimerEntity? = dao.timer(id)

    suspend fun create(label: String, durationMillis: Long): Long {
        val order = (dao.timers().maxOfOrNull { it.createdOrder } ?: 0L) + 1L
        val id = dao.insertTimer(
            TimerEntity(
                label = label.ifBlank { "timer" },
                durationMillis = durationMillis.coerceAtLeast(0L),
                state = TimerState.READY.name,
                remainingWhenPaused = durationMillis.coerceAtLeast(0L),
                targetElapsedRealtime = 0L,
                targetEpochMillis = 0L,
                createdOrder = order,
            ),
        )
        ClockTileRefresh.request(appContext, tileIdFor(id))
        ClockTileRefresh.request(appContext)
        return id
    }

    suspend fun start(id: Long) {
        val timer = dao.timer(id) ?: return
        val remaining = when (TimerState.fromName(timer.state)) {
            TimerState.RUNNING -> return
            TimerState.READY, TimerState.FINISHED -> timer.durationMillis
            TimerState.PAUSED -> timer.remainingWhenPaused
        }
        scheduleRunning(timer, remaining)
    }

    suspend fun pause(id: Long) {
        val timer = dao.timer(id) ?: return
        if (TimerState.fromName(timer.state) != TimerState.RUNNING) return
        val remaining = TimerLogic.remainingMillis(timer, SystemClock.elapsedRealtime())
        dao.updateTimer(
            timer.copy(
                state = TimerState.PAUSED.name,
                remainingWhenPaused = remaining,
            ),
        )
        scheduler.cancel(id)
        refresh(id)
    }

    suspend fun resume(id: Long) {
        val timer = dao.timer(id) ?: return
        if (TimerState.fromName(timer.state) != TimerState.PAUSED) return
        scheduleRunning(timer, timer.remainingWhenPaused)
    }

    suspend fun reset(id: Long) {
        val timer = dao.timer(id) ?: return
        scheduler.cancel(id)
        dao.updateTimer(
            timer.copy(
                state = TimerState.READY.name,
                remainingWhenPaused = timer.durationMillis,
                targetElapsedRealtime = 0L,
                targetEpochMillis = 0L,
            ),
        )
        refresh(id)
    }

    suspend fun delete(id: Long) {
        scheduler.cancel(id)
        TimerNotifications.cancel(appContext, id)
        dao.deleteTimer(id)
        ClockTileRefresh.request(appContext, tileIdFor(id))
        ClockTileRefresh.request(appContext)
    }

    /** Boot / process-death recovery: recompute remaining from the wall-clock target. */
    suspend fun restore(
        nowElapsedRealtime: Long = SystemClock.elapsedRealtime(),
        nowEpochMillis: Long = System.currentTimeMillis(),
    ) {
        dao.timers().forEach { timer ->
            when (TimerState.fromName(timer.state)) {
                TimerState.RUNNING -> {
                    val remaining = (timer.targetEpochMillis - nowEpochMillis).coerceAtLeast(0L)
                    if (timer.targetEpochMillis <= 0L) {
                        // Unknown wall target — fall back to recomputing from duration.
                        scheduleRunning(timer, timer.durationMillis)
                    } else if (remaining <= 0L) {
                        dao.updateTimer(timer.copy(state = TimerState.FINISHED.name, remainingWhenPaused = 0L))
                        TimerNotifications.notifyFinished(appContext, timer)
                    } else {
                        scheduleRunning(timer, remaining, nowElapsedRealtime, nowEpochMillis)
                    }
                }
                else -> scheduler.cancel(timer.id)
            }
        }
        ClockTileRefresh.request(appContext)
    }

    private suspend fun scheduleRunning(
        timer: TimerEntity,
        remaining: Long,
        nowElapsedRealtime: Long = SystemClock.elapsedRealtime(),
        nowEpochMillis: Long = System.currentTimeMillis(),
    ) {
        val updated = timer.copy(
            state = TimerState.RUNNING.name,
            remainingWhenPaused = remaining,
            targetElapsedRealtime = nowElapsedRealtime + remaining,
            targetEpochMillis = nowEpochMillis + remaining,
        )
        dao.updateTimer(updated)
        scheduler.schedule(updated)
        TimerNotifications.cancel(appContext, timer.id)
        refresh(timer.id)
    }

    private fun refresh(id: Long) {
        ClockTileRefresh.request(appContext, tileIdFor(id))
        ClockTileRefresh.request(appContext)
    }

    companion object {
        fun tileIdFor(id: Long): String = "timer:$id"
    }
}
