package com.metro.calendar.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.metro.calendar.data.CalendarEvent
import com.metro.calendar.data.CalendarLogic
import com.metro.calendar.data.CalendarPivot
import com.metro.calendar.data.CalendarRepository
import com.metro.calendar.data.DayBucket
import com.metro.calendar.data.HourSlot
import com.metro.calendar.data.MonthGridCell
import com.metro.calendar.data.subscription.CalendarSubscription
import com.metro.calendar.data.subscription.SubscriptionUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

/** In-app page stack (rendered by [com.metro.ui.MetroSubpageHost]). */
sealed interface CalendarRoute {
    data object Root : CalendarRoute
    data object Calendars : CalendarRoute
    data object AddSubscription : CalendarRoute
    data class SubscriptionDetail(val id: String) : CalendarRoute
}

class CalendarState(context: Context) {
    private val repository = CalendarRepository(context)
    internal val appContext = context.applicationContext
    private val zoneId: ZoneId = ZoneId.systemDefault()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    var generation by mutableIntStateOf(0)
        private set

    private fun notifyChanged() {
        generation++
    }

    var hasCalendarPermission: Boolean = false
        private set

    var usingDemoData: Boolean = false
        private set

    /** User chose to proceed without granting the device calendar permission. */
    var skippedPermissions: Boolean = false
        private set

    /** User explicitly asked for demo data (as opposed to simply having no source yet). */
    private var demoExplicit: Boolean = false

    /** True when the latest load failed (distinct from no permission/source). */
    var loadFailed: Boolean = false
        private set

    /** False until the first [refreshPermission] completes — avoids flashing the permission gate. */
    var permissionsChecked: Boolean = false
        private set

    // ---- Subscriptions -----------------------------------------------------

    var subscriptions: List<CalendarSubscription> = emptyList()
        private set

    var syncing: Boolean = false
        private set

    val hasSubscriptionSource: Boolean get() = subscriptions.any { it.enabled }

    /** True when at least one event source (device provider or ICS subscription) is available. */
    val hasAnySource: Boolean get() = hasCalendarPermission || hasSubscriptionSource

    val needsPermissionGate: Boolean
        get() = !hasCalendarPermission && subscriptions.isEmpty() && !skippedPermissions

    // ---- Navigation --------------------------------------------------------

    var route: CalendarRoute = CalendarRoute.Root
        private set

    fun openCalendars() {
        route = CalendarRoute.Calendars
        notifyChanged()
    }

    fun openAddSubscription() {
        route = CalendarRoute.AddSubscription
        notifyChanged()
    }

    fun openSubscriptionDetail(id: String) {
        route = CalendarRoute.SubscriptionDetail(id)
        notifyChanged()
    }

    /** Route-aware Back for [CalendarRoute]s outside the subpage host's own Back handling. */
    fun routeBack() {
        route = when (val current = route) {
            is CalendarRoute.SubscriptionDetail -> CalendarRoute.Calendars
            CalendarRoute.AddSubscription -> CalendarRoute.Calendars
            CalendarRoute.Calendars -> CalendarRoute.Root
            CalendarRoute.Root -> CalendarRoute.Root
        }
        notifyChanged()
    }

    var selectedEpochDay: Long = CalendarLogic.todayEpochDay(zoneId)
        private set

    /** Top-level pivot (blueprint): agenda → day → month. */
    var pivot: CalendarPivot = CalendarPivot.Agenda
        private set

    /** Bumped by the Today action so the agenda list scrolls back to the selected day. */
    var agendaScrollRequestId: Int = 0
        private set

    /** Non-null while the read-only event detail overlay is open. */
    var eventDetail: CalendarEvent? = null
        private set
    var eventDetailExiting: Boolean = false
        private set
    var eventDetailEpoch: Int = 0
        private set

    var events: List<CalendarEvent> = emptyList()
        private set

    private var loadedStartEpochDay: Long = 0L
    private var loadedEndEpochDay: Long = 0L
    private var rangeLoaded: Boolean = false

    val agendaBuckets: List<DayBucket>
        get() = CalendarLogic.groupIntoAgendaBuckets(
            events = events,
            startEpochDay = selectedEpochDay,
            dayCount = AGENDA_DAY_COUNT,
            zoneId = zoneId,
        )

    val dayAllDayEvents: List<CalendarEvent>
        get() = CalendarLogic.allDayEventsForDay(events, selectedEpochDay, zoneId)

    val dayHourSlots: List<HourSlot>
        get() = CalendarLogic.buildHourSlots(events, selectedEpochDay, zoneId = zoneId)

    val monthGrid: List<MonthGridCell>
        get() {
            val date = LocalDate.ofEpochDay(selectedEpochDay)
            return CalendarLogic.buildMonthGrid(
                year = date.year,
                month = date.monthValue,
                events = events,
                selectedEpochDay = selectedEpochDay,
                zoneId = zoneId,
            )
        }

    fun refreshPermission(context: Context) {
        hasCalendarPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR,
        ) == PackageManager.PERMISSION_GRANTED
        subscriptions = repository.subscriptions()
        permissionsChecked = true
        notifyChanged()
    }

    fun onPermissionResult(granted: Boolean) {
        hasCalendarPermission = granted
        skippedPermissions = false
        demoExplicit = false
        reloadEvents()
    }

    fun continueWithDemo() {
        skippedPermissions = true
        demoExplicit = true
        reloadEvents()
    }

    /** Proceed without the device permission and jump straight to the add-a-calendar form. */
    fun skipToAddSubscription() {
        skippedPermissions = true
        demoExplicit = false
        route = CalendarRoute.AddSubscription
        reloadEvents()
    }

    /**
     * Loads the merged event stream (device provider + ICS subscriptions). Demo data is only used
     * when the user skipped the permission gate and has no subscriptions.
     */
    fun reloadEvents() {
        subscriptions = repository.subscriptions()
        if (hasAnySource) {
            usingDemoData = false
            val loaded = runCatching {
                repository.loadEventsAround(selectedEpochDay, LOAD_RADIUS_DAYS)
            }
            if (loaded.isSuccess) {
                events = loaded.getOrDefault(emptyList())
                loadFailed = false
                loadedStartEpochDay = selectedEpochDay - LOAD_RADIUS_DAYS
                loadedEndEpochDay = selectedEpochDay + LOAD_RADIUS_DAYS
                rangeLoaded = true
            } else {
                events = emptyList()
                loadFailed = true
                rangeLoaded = false
            }
        } else if (demoExplicit) {
            usingDemoData = true
            loadFailed = false
            events = repository.loadDemoEvents()
            rangeLoaded = false
        } else {
            events = emptyList()
            loadFailed = false
            rangeLoaded = false
        }
        notifyChanged()
    }

    /**
     * Pulls fresh events whenever the selected date drifts near the edge of the loaded window, so
     * paging days/months always reflects the real calendar.
     */
    private fun ensureRangeLoaded(epochDay: Long) {
        if (!hasAnySource || usingDemoData) return
        if (rangeLoaded &&
            epochDay in (loadedStartEpochDay + RELOAD_MARGIN_DAYS)..(loadedEndEpochDay - RELOAD_MARGIN_DAYS)
        ) {
            return
        }
        val loaded = runCatching { repository.loadEventsAround(epochDay, LOAD_RADIUS_DAYS) }
        if (loaded.isFailure) {
            loadFailed = true
            notifyChanged()
            return
        }
        events = loaded.getOrDefault(emptyList())
        loadFailed = false
        loadedStartEpochDay = epochDay - LOAD_RADIUS_DAYS
        loadedEndEpochDay = epochDay + LOAD_RADIUS_DAYS
        rangeLoaded = true
    }

    fun selectPivot(value: CalendarPivot) {
        if (pivot == value) return
        pivot = value
        notifyChanged()
    }

    /** Shared selected date: month-grid drill-down and day navigation both move it. */
    fun selectDay(epochDay: Long) {
        selectedEpochDay = epochDay
        pivot = CalendarPivot.Day
        ensureRangeLoaded(selectedEpochDay)
        notifyChanged()
    }

    fun shiftDay(deltaDays: Long) {
        selectedEpochDay = LocalDate.ofEpochDay(selectedEpochDay)
            .plusDays(deltaDays)
            .toEpochDay()
        ensureRangeLoaded(selectedEpochDay)
        notifyChanged()
    }

    fun shiftMonth(deltaMonths: Long) {
        selectedEpochDay = LocalDate.ofEpochDay(selectedEpochDay)
            .plusMonths(deltaMonths)
            .toEpochDay()
        ensureRangeLoaded(selectedEpochDay)
        notifyChanged()
    }

    fun goToToday() {
        selectedEpochDay = CalendarLogic.todayEpochDay(zoneId)
        ensureRangeLoaded(selectedEpochDay)
        if (pivot == CalendarPivot.Agenda) {
            agendaScrollRequestId++
        }
        notifyChanged()
    }

    fun openEventDetail(event: CalendarEvent) {
        eventDetail = event
        eventDetailExiting = false
        eventDetailEpoch++
        notifyChanged()
    }

    fun beginCloseEventDetail() {
        if (eventDetail == null || eventDetailExiting) return
        eventDetailExiting = true
        notifyChanged()
    }

    fun finishCloseEventDetail() {
        eventDetail = null
        eventDetailExiting = false
        notifyChanged()
    }

    // ---- Subscription actions ---------------------------------------------

    /** Validates and stores a subscription, then fetches it once. Returns false on invalid URL. */
    fun addSubscription(name: String, url: String, colorHex: String): Boolean {
        if (!SubscriptionUrl.isValid(url)) return false
        val subscription = repository.addSubscription(name, url, colorHex) ?: return false
        subscriptions = repository.subscriptions()
        notifyChanged()
        syncSubscription(subscription.id)
        return true
    }

    fun removeSubscription(id: String) {
        repository.removeSubscription(id)
        subscriptions = repository.subscriptions()
        reloadEvents()
    }

    fun setSubscriptionEnabled(id: String, enabled: Boolean) {
        repository.setSubscriptionEnabled(id, enabled)
        subscriptions = repository.subscriptions()
        reloadEvents()
    }

    /** Fetches a single subscription, reloads the stream, and surfaces the outcome. */
    fun syncSubscription(id: String) {
        if (syncing) return
        syncing = true
        notifyChanged()
        scope.launch {
            val outcome = withContext(Dispatchers.IO) { repository.syncSubscription(id) }
            syncing = false
            subscriptions = repository.subscriptions()
            reloadEvents()
            showStub(syncMessage(outcome))
        }
    }

    /** Fetches every enabled subscription (manual "sync calendars"). */
    fun syncNow() {
        refreshPermission(appContext)
        if (!hasSubscriptionSource) {
            reloadEvents()
            showStub(appContext.getString(com.metro.calendar.R.string.sync_done))
            return
        }
        if (syncing) return
        syncing = true
        notifyChanged()
        scope.launch {
            val failures = withContext(Dispatchers.IO) { repository.syncAllSubscriptions() }
            syncing = false
            subscriptions = repository.subscriptions()
            reloadEvents()
            showStub(
                if (failures == 0) {
                    appContext.getString(com.metro.calendar.R.string.sync_done)
                } else {
                    appContext.getString(com.metro.calendar.R.string.sync_failed_count, failures)
                },
            )
        }
    }

    private fun syncMessage(
        outcome: com.metro.calendar.data.subscription.IcsCalendarSource.SyncOutcome,
    ): String = when (outcome) {
        is com.metro.calendar.data.subscription.IcsCalendarSource.SyncOutcome.Failure -> outcome.message
        else -> appContext.getString(com.metro.calendar.R.string.sync_done)
    }

    fun showStub(message: String) {
        Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
    }

    private companion object {
        const val LOAD_RADIUS_DAYS = 120
        const val RELOAD_MARGIN_DAYS = 21
        const val AGENDA_DAY_COUNT = 90
    }
}
