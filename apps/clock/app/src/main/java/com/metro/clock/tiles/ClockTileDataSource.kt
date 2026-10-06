package com.metro.clock.tiles

import android.content.Context
import android.os.SystemClock
import com.metro.clock.R
import com.metro.clock.alarms.AlarmFormat
import com.metro.clock.alarms.AlarmSchedule
import com.metro.clock.data.AlarmEntity
import com.metro.clock.data.ClockDatabase
import com.metro.clock.data.StopwatchEntity
import com.metro.clock.data.TimerEntity
import com.metro.clock.timers.TimerState
import com.metro.system.MetroPreferences
import com.metro.system.MetroTileContract
import com.metro.system.MetroTileData
import com.metro.system.MetroTilePeek
import com.metro.system.MetroTileTemporalState
import com.metro.system.MetroTileWidgetFace
import com.metro.system.MetroTileWidgetFaceKind
import com.metro.system.MetroTemporalKind
import com.metro.system.MetroWorldClockCatalog
import java.time.ZoneId
import java.util.Locale

/**
 * Builds Live Tile payloads. Timers/stopwatches/world clocks export **structured temporal state**
 * so the launcher ticks locally — the app never broadcasts a tile update per second.
 */
class ClockTileDataSource(context: Context) {
    private val appContext = context.applicationContext
    private val dao = ClockDatabase.get(appContext).dao()

    suspend fun buildTileData(tileId: String): MetroTileData? {
        val accent = MetroPreferences(appContext).accentColorHex
        val use24 = is24Hour()
        return when {
            tileId == ClockTileActions.PRIMARY ||
                tileId == MetroTileContract.DEFAULT_TILE_ID -> buildPrimary(accent, use24)
            tileId.startsWith("timer:") -> buildTimer(tileId.substringAfter(':').toLongOrNull(), accent)
            tileId.startsWith("stopwatch:") -> buildStopwatch(tileId.substringAfter(':').toLongOrNull(), accent)
            tileId.startsWith("world:") -> buildWorld(tileId.substringAfter(':'), accent)
            tileId.startsWith("alarm:") -> buildAlarm(tileId.substringAfter(':').toLongOrNull(), accent, use24)
            else -> null
        }
    }

    private suspend fun buildPrimary(accent: String, use24: Boolean): MetroTileData {
        val now = System.currentTimeMillis()
        val nowElapsed = SystemClock.elapsedRealtime()
        val alarms = dao.alarms()
        val timers = dao.timers()
        val stopwatches = dao.stopwatches()
        val nextAlarm = AlarmSchedule.nextAlarm(alarms, now, ZoneId.systemDefault())

        val ordered = ClockPeekLogic.order(timers, stopwatches, nextAlarm, nowElapsed)
        val peeks = ArrayList<MetroTilePeek>()
        // Deterministic cap: 6 visible faces TOTAL. On overflow, show 5 real faces + "+N more".
        val hasOverflow = ordered.size > ClockPeekLogic.MAX_PEEKS
        val realFaces = if (hasOverflow) ClockPeekLogic.MAX_PEEKS - 1 else ordered.size
        ordered.take(realFaces).forEach { peeks.add(it.toPeek(use24)) }
        if (hasOverflow) {
            val overflow = ordered.size - realFaces
            peeks.add(
                MetroTilePeek(
                    title = appContext.getString(R.string.tile_more, overflow),
                    packageName = appContext.packageName,
                ),
            )
        }
        if (peeks.isEmpty()) {
            peeks.add(
                MetroTilePeek(
                    title = appContext.getString(R.string.tile_no_alarm),
                    packageName = appContext.packageName,
                ),
            )
        }

        val finishedCount = timers.count { TimerState.fromName(it.state) == TimerState.FINISHED }
        return MetroTileData(
            title = appContext.getString(R.string.app_name),
            backgroundColorHex = accent,
            counter = finishedCount.takeIf { it > 0 },
            widgetFace = MetroTileWidgetFace(kind = MetroTileWidgetFaceKind.PEEK_CYCLE),
            peeks = peeks,
        )
    }

    private suspend fun buildTimer(id: Long?, accent: String): MetroTileData? {
        val timer = id?.let { dao.timer(it) } ?: return null
        return MetroTileData(
            title = timer.label,
            backgroundColorHex = accent,
            widgetFace = MetroTileWidgetFace(kind = MetroTileWidgetFaceKind.PEEK_CYCLE),
            peeks = listOf(timer.toPeek()),
        )
    }

    private suspend fun buildStopwatch(id: Long?, accent: String): MetroTileData? {
        val stopwatch = id?.let { dao.stopwatch(it) } ?: return null
        return MetroTileData(
            title = stopwatch.name,
            backgroundColorHex = accent,
            widgetFace = MetroTileWidgetFace(kind = MetroTileWidgetFaceKind.PEEK_CYCLE),
            peeks = listOf(stopwatch.toPeek()),
        )
    }

    private fun buildWorld(cityId: String, accent: String): MetroTileData? {
        val city = MetroWorldClockCatalog.byId(cityId) ?: return null
        return MetroTileData(
            title = city.name,
            backgroundColorHex = accent,
            widgetFace = MetroTileWidgetFace(kind = MetroTileWidgetFaceKind.PEEK_CYCLE),
            peeks = listOf(
                MetroTilePeek(
                    title = city.name,
                    packageName = appContext.packageName,
                    temporal = MetroTileTemporalState(
                        kind = MetroTemporalKind.WORLD_CLOCK,
                        itemId = city.id,
                        label = city.name,
                        zoneId = city.zoneId,
                    ),
                ),
            ),
        )
    }

    private suspend fun buildAlarm(id: Long?, accent: String, use24: Boolean): MetroTileData? {
        val alarm = id?.let { dao.alarm(it) } ?: return null
        val repeat = AlarmSchedule.repeatSummary(alarm, Locale.getDefault())
        return MetroTileData(
            title = AlarmFormat.time(alarm, use24),
            backgroundColorHex = accent,
            backFaceTitle = listOf(alarm.label.takeIf { it.isNotBlank() }, repeat)
                .filterNotNull()
                .joinToString(" · "),
            deepLinkUri = ClockTileActions.deepLinkFor(ClockTileActions.alarmTile(alarm.id)),
        )
    }

    private fun ClockPeekLogic.Source.toPeek(use24: Boolean): MetroTilePeek = when (this) {
        is ClockPeekLogic.FinishedTimer -> timer.toPeek(forcedFinished = true)
        is ClockPeekLogic.RunningTimer -> timer.toPeek()
        is ClockPeekLogic.RunningStopwatch -> stopwatch.toPeek()
        is ClockPeekLogic.NextAlarm -> alarm.toPeek(triggerMillis, use24)
    }

    private fun TimerEntity.toPeek(forcedFinished: Boolean = false): MetroTilePeek {
        val state = if (forcedFinished) TimerState.FINISHED else TimerState.fromName(this.state)
        return MetroTilePeek(
            title = label,
            temporal = MetroTileTemporalState(
                kind = MetroTemporalKind.TIMER,
                itemId = id.toString(),
                label = label,
                running = state == TimerState.RUNNING,
                finished = state == TimerState.FINISHED,
                targetElapsedRealtimeMillis = if (state == TimerState.RUNNING) targetElapsedRealtime else null,
                targetEpochMillis = if (state == TimerState.RUNNING) targetEpochMillis else null,
                pausedRemainingMillis = when (state) {
                    TimerState.RUNNING -> null
                    TimerState.FINISHED -> 0L
                    else -> remainingWhenPaused
                },
            ),
            packageName = appContext.packageName,
        )
    }

    private fun StopwatchEntity.toPeek(): MetroTilePeek = MetroTilePeek(
        title = name,
        temporal = MetroTileTemporalState(
            kind = MetroTemporalKind.STOPWATCH,
            itemId = id.toString(),
            label = name,
            running = running,
            accumulatedElapsedMillis = accumulatedElapsedMillis,
            runningSinceElapsedRealtimeMillis = if (running) runningSinceElapsedRealtime else null,
            runningSinceEpochMillis = if (running) runningSinceEpochMillis else null,
        ),
        packageName = appContext.packageName,
    )

    private fun AlarmEntity.toPeek(triggerMillis: Long, use24: Boolean): MetroTilePeek = MetroTilePeek(
        title = label.takeIf { it.isNotBlank() } ?: appContext.getString(R.string.alarm),
        subtitle = AlarmFormat.time(this, use24),
        body = AlarmSchedule.repeatSummary(this, Locale.getDefault()),
        temporal = MetroTileTemporalState(
            kind = MetroTemporalKind.ALARM,
            itemId = id.toString(),
            label = label,
            targetEpochMillis = triggerMillis,
        ),
        packageName = appContext.packageName,
    )

    private fun is24Hour(): Boolean =
        android.text.format.DateFormat.is24HourFormat(appContext)
}
