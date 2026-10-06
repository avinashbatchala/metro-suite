package com.metro.clock.ui

import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.metro.clock.R
import com.metro.clock.alarms.AlarmRepository
import com.metro.clock.data.AlarmEntity
import com.metro.clock.data.StopwatchEntity
import com.metro.clock.data.StopwatchLapEntity
import com.metro.clock.data.TimerEntity
import com.metro.clock.stopwatch.StopwatchRepository
import com.metro.clock.timers.TimerRepository
import com.metro.clock.timers.TimerState
import com.metro.clock.worldclock.WorldClockRepository
import com.metro.system.MetroWorldClockCatalog
import com.metro.system.MetroWorldClockCity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

enum class ClockPivot(@StringRes val titleRes: Int) {
    Alarms(R.string.pivot_alarms),
    World(R.string.pivot_world_clock),
    Timer(R.string.pivot_timer),
    Stopwatch(R.string.pivot_stopwatch),
}

sealed interface ClockRoute {
    data object Root : ClockRoute
    data class AlarmEdit(val alarmId: Long?) : ClockRoute
    data object CityPicker : ClockRoute
    data object TimerEdit : ClockRoute
    data class TimerDetail(val timerId: Long) : ClockRoute
    data class StopwatchDetail(val stopwatchId: Long) : ClockRoute
}

/** Central Clock state: repositories, pivot/route navigation and deep-link routing. */
class ClockState(context: Context) {
    private val appContext = context.applicationContext
    private val alarmRepo = AlarmRepository(appContext)
    private val timerRepo = TimerRepository(appContext)
    private val stopwatchRepo = StopwatchRepository(appContext)
    private val worldRepo = WorldClockRepository(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    var generation by mutableIntStateOf(0)
        private set

    var pivot: ClockPivot = ClockPivot.Alarms
        private set

    var route: ClockRoute = ClockRoute.Root
        private set

    var alarms: List<AlarmEntity> = emptyList()
        private set
    var timers: List<TimerEntity> = emptyList()
        private set
    var stopwatches: List<StopwatchEntity> = emptyList()
        private set
    var worldCities: List<MetroWorldClockCity> = emptyList()
        private set

    /** Set by the Activity so the app can request POST_NOTIFICATIONS when needed. */
    var requestNotificationPermission: (() -> Unit)? = null

    private fun notifyChanged() {
        generation++
    }

    private fun ensureNotifications() {
        requestNotificationPermission?.invoke()
    }

    fun initialize() {
        scope.launch {
            worldRepo.seedDefaultsIfEmpty()
            refresh()
        }
    }

    fun refresh() {
        scope.launch {
            alarms = alarmRepo.alarms()
            timers = timerRepo.timers()
            stopwatches = stopwatchRepo.stopwatches()
            worldCities = worldRepo.cities()
            notifyChanged()
        }
    }

    // ---- Navigation --------------------------------------------------------

    fun selectPivot(value: ClockPivot) {
        if (pivot == value) return
        pivot = value
        notifyChanged()
    }

    fun openAlarmEdit(alarmId: Long?) {
        route = ClockRoute.AlarmEdit(alarmId)
        notifyChanged()
    }

    fun openCityPicker() {
        route = ClockRoute.CityPicker
        notifyChanged()
    }

    fun openTimerEdit() {
        route = ClockRoute.TimerEdit
        notifyChanged()
    }

    fun openTimerDetail(timerId: Long) {
        route = ClockRoute.TimerDetail(timerId)
        notifyChanged()
    }

    fun openStopwatchDetail(stopwatchId: Long) {
        route = ClockRoute.StopwatchDetail(stopwatchId)
        notifyChanged()
    }

    fun closeRoute() {
        route = ClockRoute.Root
        notifyChanged()
    }

    fun handleDeepLink(uri: Uri?) {
        if (uri == null) return
        val segments = uri.pathSegments
        when (segments.firstOrNull()) {
            "alarms" -> pivot = ClockPivot.Alarms
            "alarm" -> {
                pivot = ClockPivot.Alarms
                segments.getOrNull(1)?.toLongOrNull()?.let { route = ClockRoute.AlarmEdit(it) }
            }
            "timers" -> pivot = ClockPivot.Timer
            "timer" -> {
                pivot = ClockPivot.Timer
                segments.getOrNull(1)?.toLongOrNull()?.let { route = ClockRoute.TimerDetail(it) }
            }
            "stopwatch" -> {
                pivot = ClockPivot.Stopwatch
                segments.getOrNull(1)?.toLongOrNull()?.let { route = ClockRoute.StopwatchDetail(it) }
            }
            "world" -> {
                pivot = ClockPivot.World
            }
        }
        notifyChanged()
    }

    // ---- Alarms ------------------------------------------------------------

    fun alarm(id: Long?): AlarmEntity? = id?.let { id2 -> alarms.firstOrNull { it.id == id2 } }

    fun saveAlarm(
        id: Long?,
        hour: Int,
        minute: Int,
        label: String,
        repeatMask: Int,
        vibrate: Boolean,
        snoozeMinutes: Int,
        soundUri: String?,
    ) {
        ensureNotifications()
        scope.launch {
            val existing = id?.let { alarmRepo.alarm(it) }
            val alarm = (existing ?: AlarmEntity(hour = hour, minute = minute)).copy(
                hour = hour,
                minute = minute,
                label = label.trim(),
                repeatMask = repeatMask,
                vibrate = vibrate,
                snoozeMinutes = snoozeMinutes,
                soundUri = soundUri,
                enabled = true,
            )
            if (existing == null) alarmRepo.insert(alarm) else alarmRepo.save(alarm)
            refresh()
        }
    }

    fun setAlarmEnabled(id: Long, enabled: Boolean) {
        scope.launch {
            alarmRepo.setEnabled(id, enabled)
            refresh()
        }
    }

    fun deleteAlarm(id: Long) {
        scope.launch {
            alarmRepo.delete(id)
            route = ClockRoute.Root
            refresh()
        }
    }

    // ---- World clock -------------------------------------------------------

    fun addCity(cityId: String) {
        scope.launch {
            worldRepo.add(cityId)
            refresh()
        }
    }

    fun removeCity(cityId: String) {
        scope.launch {
            worldRepo.remove(cityId)
            refresh()
        }
    }

    fun isCitySelected(cityId: String): Boolean = worldCities.any { it.id == cityId }

    // ---- Timers ------------------------------------------------------------

    fun timer(id: Long): TimerEntity? = timers.firstOrNull { it.id == id }

    fun createTimer(label: String, durationMillis: Long) {
        ensureNotifications()
        scope.launch {
            val id = timerRepo.create(label, durationMillis)
            timerRepo.start(id)
            route = ClockRoute.TimerDetail(id)
            refresh()
        }
    }

    fun startTimer(id: Long) = scope.launch { timerRepo.start(id); refresh() }

    fun pauseTimer(id: Long) = scope.launch { timerRepo.pause(id); refresh() }

    fun resumeTimer(id: Long) = scope.launch { timerRepo.resume(id); refresh() }

    fun resetTimer(id: Long) = scope.launch { timerRepo.reset(id); refresh() }

    fun deleteTimer(id: Long) {
        scope.launch {
            timerRepo.delete(id)
            route = ClockRoute.Root
            refresh()
        }
    }

    // ---- Stopwatch ---------------------------------------------------------

    fun stopwatch(id: Long): StopwatchEntity? = stopwatches.firstOrNull { it.id == id }

    fun createStopwatch(name: String) {
        ensureNotifications()
        scope.launch {
            val id = stopwatchRepo.create(name)
            stopwatchRepo.start(id)
            route = ClockRoute.StopwatchDetail(id)
            refresh()
        }
    }

    fun startStopwatch(id: Long) = scope.launch { stopwatchRepo.start(id); refresh() }

    fun pauseStopwatch(id: Long) = scope.launch { stopwatchRepo.pause(id); refresh() }

    fun lapStopwatch(id: Long) = scope.launch { stopwatchRepo.lap(id); refresh() }

    fun resetStopwatch(id: Long) = scope.launch { stopwatchRepo.reset(id); refresh() }

    fun renameStopwatch(id: Long, name: String) = scope.launch { stopwatchRepo.rename(id, name); refresh() }

    fun deleteStopwatch(id: Long) {
        scope.launch {
            stopwatchRepo.delete(id)
            route = ClockRoute.Root
            refresh()
        }
    }

    fun laps(stopwatchId: Long, onLoaded: (List<StopwatchLapEntity>) -> Unit) {
        scope.launch { onLoaded(stopwatchRepo.laps(stopwatchId)) }
    }

    fun nextAlarm(): Pair<AlarmEntity, Long>? {
        val now = System.currentTimeMillis()
        return alarms.asSequence()
            .filter { it.enabled }
            .mapNotNull { alarm ->
                com.metro.clock.alarms.AlarmSchedule
                    .nextTrigger(alarm, now, java.time.ZoneId.systemDefault())
                    ?.let { alarm to it }
            }
            .minByOrNull { it.second }
    }

    fun cities() = MetroWorldClockCatalog.cities

    fun searchCities(query: String) = MetroWorldClockCatalog.search(query)
}
