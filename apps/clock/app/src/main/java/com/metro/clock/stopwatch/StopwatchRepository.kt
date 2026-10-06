package com.metro.clock.stopwatch

import android.content.Context
import android.os.SystemClock
import com.metro.clock.data.ClockDatabase
import com.metro.clock.data.StopwatchEntity
import com.metro.clock.data.StopwatchLapEntity
import com.metro.clock.tiles.ClockTileRefresh

class StopwatchRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dao = ClockDatabase.get(appContext).dao()

    suspend fun stopwatches(): List<StopwatchEntity> = dao.stopwatches()

    suspend fun stopwatch(id: Long): StopwatchEntity? = dao.stopwatch(id)

    suspend fun laps(id: Long): List<StopwatchLapEntity> = dao.laps(id)

    suspend fun create(name: String): Long {
        val order = (dao.stopwatches().maxOfOrNull { it.createdOrder } ?: 0L) + 1L
        val id = dao.insertStopwatch(
            StopwatchEntity(
                name = name.ifBlank { "stopwatch" },
                running = false,
                accumulatedElapsedMillis = 0L,
                runningSinceElapsedRealtime = 0L,
                runningSinceEpochMillis = 0L,
                createdOrder = order,
            ),
        )
        refresh(id)
        return id
    }

    suspend fun start(id: Long) {
        val stopwatch = dao.stopwatch(id) ?: return
        if (stopwatch.running) return
        dao.updateStopwatch(
            stopwatch.copy(
                running = true,
                runningSinceElapsedRealtime = SystemClock.elapsedRealtime(),
                runningSinceEpochMillis = System.currentTimeMillis(),
            ),
        )
        refresh(id)
    }

    suspend fun pause(id: Long) {
        val stopwatch = dao.stopwatch(id) ?: return
        if (!stopwatch.running) return
        val elapsed = StopwatchLogic.elapsedMillis(stopwatch, SystemClock.elapsedRealtime())
        dao.updateStopwatch(
            stopwatch.copy(
                running = false,
                accumulatedElapsedMillis = elapsed,
                runningSinceElapsedRealtime = 0L,
                runningSinceEpochMillis = 0L,
            ),
        )
        refresh(id)
    }

    suspend fun lap(id: Long) {
        val stopwatch = dao.stopwatch(id) ?: return
        val elapsed = StopwatchLogic.elapsedMillis(stopwatch, SystemClock.elapsedRealtime())
        val existing = dao.laps(id)
        val lastTotal = existing.firstOrNull()?.totalDurationMillis ?: 0L
        val lapNumber = (existing.maxOfOrNull { it.lapNumber } ?: 0) + 1
        if (elapsed <= lastTotal) return
        dao.insertLap(
            StopwatchLapEntity(
                stopwatchId = id,
                lapNumber = lapNumber,
                lapDurationMillis = elapsed - lastTotal,
                totalDurationMillis = elapsed,
            ),
        )
        refresh(id)
    }

    suspend fun reset(id: Long) {
        val stopwatch = dao.stopwatch(id) ?: return
        dao.deleteLaps(id)
        dao.updateStopwatch(
            stopwatch.copy(
                running = false,
                accumulatedElapsedMillis = 0L,
                runningSinceElapsedRealtime = 0L,
                runningSinceEpochMillis = 0L,
            ),
        )
        refresh(id)
    }

    suspend fun rename(id: Long, name: String) {
        val stopwatch = dao.stopwatch(id) ?: return
        dao.updateStopwatch(stopwatch.copy(name = name.ifBlank { stopwatch.name }))
        refresh(id)
    }

    suspend fun delete(id: Long) {
        dao.deleteLaps(id)
        dao.deleteStopwatch(id)
        ClockTileRefresh.request(appContext, tileIdFor(id))
        ClockTileRefresh.request(appContext)
    }

    /**
     * Reboot recovery: a stopwatch that was running keeps running, with the wall-clock gap added to
     * its accumulated time (documented semantic).
     */
    suspend fun restore(
        nowElapsedRealtime: Long = SystemClock.elapsedRealtime(),
        nowEpochMillis: Long = System.currentTimeMillis(),
    ) {
        dao.stopwatches().filter { it.running }.forEach { stopwatch ->
            val wallGap = if (stopwatch.runningSinceEpochMillis > 0L) {
                (nowEpochMillis - stopwatch.runningSinceEpochMillis).coerceAtLeast(0L)
            } else {
                0L
            }
            dao.updateStopwatch(
                stopwatch.copy(
                    accumulatedElapsedMillis = stopwatch.accumulatedElapsedMillis + wallGap,
                    runningSinceElapsedRealtime = nowElapsedRealtime,
                    runningSinceEpochMillis = nowEpochMillis,
                ),
            )
        }
        ClockTileRefresh.request(appContext)
    }

    private fun refresh(id: Long) {
        ClockTileRefresh.request(appContext, tileIdFor(id))
        ClockTileRefresh.request(appContext)
    }

    companion object {
        fun tileIdFor(id: Long): String = "stopwatch:$id"
    }
}
