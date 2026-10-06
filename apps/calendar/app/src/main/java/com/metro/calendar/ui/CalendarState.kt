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
import com.metro.calendar.data.CalendarDraft
import com.metro.calendar.data.CalendarEvent
import com.metro.calendar.data.CalendarLogic
import com.metro.calendar.data.CalendarView
import com.metro.calendar.data.CalendarWeather
import com.metro.calendar.data.CalendarWeatherReader
import com.metro.calendar.data.CalendarWeek
import com.metro.calendar.data.CalendarRepository
import com.metro.calendar.data.CalendarWriteRepository
import com.metro.calendar.data.DayBucket
import com.metro.calendar.data.HourSlot
import com.metro.calendar.data.MiniMonth
import com.metro.calendar.data.MonthGridCell
import com.metro.calendar.data.WritableCalendar
import com.metro.calendar.data.subscription.CalendarSubscription
import com.metro.calendar.data.subscription.SubscriptionUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/** In-app page stack (rendered by [com.metro.ui.MetroSubpageHost]). */
sealed interface CalendarRoute {
    data object Root : CalendarRoute
    data object Calendars : CalendarRoute
    data object AddSubscription : CalendarRoute
    data object EventDetail : CalendarRoute
    data object EventEdit : CalendarRoute
    data class SubscriptionDetail(val id: String) : CalendarRoute
}

class CalendarState(context: Context) {
    private val repository = CalendarRepository(context)
    private val writeRepository = CalendarWriteRepository(context)
    internal val appContext = context.applicationContext
    private val zoneId: ZoneId = ZoneId.systemDefault()
    private val locale: Locale = Locale.getDefault()
    private val use24Hour: Boolean = android.text.format.DateFormat.is24HourFormat(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    var generation by mutableIntStateOf(0)
        private set

    private fun notifyChanged() {
        generation++
    }

    var hasCalendarPermission: Boolean by mutableStateOf(false)
        private set

    var usingDemoData: Boolean by mutableStateOf(false)
        private set

    /** User chose to proceed without granting the device calendar permission. */
    var skippedPermissions: Boolean by mutableStateOf(false)
        private set

    /** User explicitly asked for demo data (as opposed to simply having no source yet). */
    private var demoExplicit: Boolean = false

    /** True when the latest load failed (distinct from no permission/source). */
    var loadFailed: Boolean by mutableStateOf(false)
        private set

    /** False until the first [refreshPermission] completes — avoids flashing the permission gate. */
    var permissionsChecked: Boolean by mutableStateOf(false)
        private set

    // ---- Subscriptions -----------------------------------------------------

    var subscriptions: List<CalendarSubscription> by mutableStateOf(emptyList())
        private set

    var syncing: Boolean by mutableStateOf(false)
        private set

    val hasSubscriptionSource: Boolean get() = subscriptions.any { it.enabled }

    /** True when at least one event source (device provider or ICS subscription) is available. */
    val hasAnySource: Boolean get() = hasCalendarPermission || hasSubscriptionSource

    val needsPermissionGate: Boolean
        get() = !hasCalendarPermission && subscriptions.isEmpty() && !skippedPermissions

    // ---- Navigation --------------------------------------------------------

    var route: CalendarRoute by mutableStateOf(CalendarRoute.Root)
        private set

    fun openCalendars() {
        route = CalendarRoute.Calendars
        loadWritableCalendars()
        notifyChanged()
    }

    fun loadWritableCalendars() {
        writableCalendars = writeRepository.writableCalendars()
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
        when (route) {
            is CalendarRoute.SubscriptionDetail -> route = CalendarRoute.Calendars
            CalendarRoute.AddSubscription -> route = CalendarRoute.Calendars
            CalendarRoute.EventDetail -> {
                eventDetail = null
                route = CalendarRoute.Root
            }
            CalendarRoute.EventEdit -> {
                route = if (eventDetail != null) CalendarRoute.EventDetail else CalendarRoute.Root
            }
            CalendarRoute.Calendars -> route = CalendarRoute.Root
            CalendarRoute.Root -> route = CalendarRoute.Root
        }
        notifyChanged()
    }

    var selectedEpochDay: Long by mutableStateOf(CalendarLogic.todayEpochDay(zoneId))
        private set

    /** Active top-level view (day / week / month / year / agenda). */
    var view: CalendarView by mutableStateOf(CalendarView.Agenda)
        private set

    /** Bumped by the Today action so the agenda list scrolls back to the selected day. */
    var agendaScrollRequestId: Int = 0
        private set

    /** Non-null while the event detail subpage is open. */
    var eventDetail: CalendarEvent? by mutableStateOf(null)
        private set

    var events: List<CalendarEvent> by mutableStateOf(emptyList())
        private set

    /** Current conditions from the Weather app's tile (best-effort), for the day/week headers. */
    var weather: CalendarWeather? by mutableStateOf(null)
        private set

    fun loadWeather() {
        scope.launch {
            val loaded = withContext(Dispatchers.IO) { CalendarWeatherReader.read(appContext) }
            weather = loaded
        }
    }

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

    fun dayAllDayEvents(epochDay: Long): List<CalendarEvent> =
        CalendarLogic.allDayEventsForDay(events, epochDay, zoneId)

    fun dayHourSlots(epochDay: Long): List<HourSlot> =
        CalendarLogic.buildHourSlots(
            events = events,
            epochDay = epochDay,
            zoneId = zoneId,
            use24Hour = use24Hour,
        )

    fun week(epochDay: Long): CalendarWeek =
        CalendarLogic.buildWeek(
            anchorEpochDay = epochDay,
            events = events,
            zoneId = zoneId,
            locale = locale,
        )

    fun monthGrid(epochDay: Long): List<MonthGridCell> {
        val date = LocalDate.ofEpochDay(epochDay)
        return CalendarLogic.buildMonthGrid(
            year = date.year,
            month = date.monthValue,
            events = events,
            selectedEpochDay = selectedEpochDay,
            zoneId = zoneId,
            firstDayOfWeek = CalendarLogic.firstDayOfWeek(locale),
        )
    }

    fun yearMonths(epochDay: Long): List<MiniMonth> {
        val year = LocalDate.ofEpochDay(epochDay).year
        return CalendarLogic.buildYear(
            year = year,
            events = events,
            zoneId = zoneId,
            locale = locale,
            firstDayOfWeek = CalendarLogic.firstDayOfWeek(locale),
        )
    }

    /** Mini-month for the week view's bottom-right tile (month containing [epochDay]). */
    fun weekMiniMonth(epochDay: Long): MiniMonth {
        val date = LocalDate.ofEpochDay(epochDay)
        return CalendarLogic.buildMiniMonth(
            year = date.year,
            monthValue = date.monthValue,
            events = events,
            zoneId = zoneId,
            locale = locale,
            firstDayOfWeek = CalendarLogic.firstDayOfWeek(locale),
        )
    }

    fun dayDateOverline(epochDay: Long): String =
        CalendarLogic.dateHeaderLabel(epochDay, zoneId, locale)

    /** Short weekday headers starting at the region's first day of week. */
    val monthWeekdayLabels: List<String>
        get() {
            val first = CalendarLogic.firstDayOfWeek(locale)
            return (0..6).map { offset ->
                first.plus(offset.toLong())
                    .getDisplayName(java.time.format.TextStyle.SHORT, locale)
            }
        }

    fun refreshPermission(context: Context) {
        hasCalendarPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR,
        ) == PackageManager.PERMISSION_GRANTED
        subscriptions = repository.subscriptions()
        permissionsChecked = true
        loadWeather()
        loadWritableCalendars()
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

    fun selectView(value: CalendarView) {
        if (view == value) return
        view = value
        notifyChanged()
    }

    /** Shared selected date: month-grid / week-tile drill-down moves it and opens the day. */
    fun selectDay(epochDay: Long) {
        selectedEpochDay = epochDay
        view = CalendarView.Day
        ensureRangeLoaded(selectedEpochDay)
        notifyChanged()
    }

    /**
     * Moves the selected date as the pivot pager settles on a new unit, without changing the
     * active view (the pager owns navigation within a view).
     */
    fun selectDateFromPager(epochDay: Long) {
        if (selectedEpochDay == epochDay) return
        selectedEpochDay = epochDay
        ensureRangeLoaded(epochDay)
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
        if (view == CalendarView.Agenda) {
            agendaScrollRequestId++
        }
        notifyChanged()
    }

    fun openEventDetail(event: CalendarEvent) {
        eventDetail = event
        route = CalendarRoute.EventDetail
        notifyChanged()
    }

    // ---- Write / edit -------------------------------------------------------

    var writableCalendars: List<WritableCalendar> by mutableStateOf(emptyList())
        private set

    /** Non-null while the appointment editor is open. */
    var eventDraft: CalendarDraft? by mutableStateOf(null)
        private set

    fun openNewEvent() {
        writableCalendars = writeRepository.writableCalendars()
        if (writableCalendars.isEmpty()) {
            ensureWritableCalendar()
        }
        val start = defaultStartMillis(selectedEpochDay)
        eventDraft = CalendarDraft(
            calendarId = writableCalendars.firstOrNull()?.id ?: -1L,
            startMillis = start,
            endMillis = start + 3_600_000L,
        )
        route = CalendarRoute.EventEdit
        notifyChanged()
    }

    fun openEditEvent() {
        val event = eventDetail ?: return
        if (event.readOnly) return
        writableCalendars = writeRepository.writableCalendars()
        eventDraft = CalendarDraft(
            id = event.id,
            calendarId = writeRepository.calendarIdForEvent(event.id)
                ?: writableCalendars.firstOrNull()?.id
                ?: -1L,
            title = event.title,
            location = event.location.orEmpty(),
            allDay = event.allDay,
            startMillis = event.startMillis,
            endMillis = event.endMillis,
            readOnly = false,
        )
        route = CalendarRoute.EventEdit
        notifyChanged()
    }

    fun updateDraft(transform: (CalendarDraft) -> CalendarDraft) {
        val current = eventDraft ?: return
        eventDraft = transform(current)
        notifyChanged()
    }

    fun saveDraft() {
        val draft = eventDraft ?: return
        if (draft.readOnly) return
        val ok = if (draft.id == null) {
            writeRepository.createEvent(draft) != null
        } else {
            writeRepository.updateEvent(draft)
        }
        eventDraft = null
        eventDetail = null
        route = CalendarRoute.Root
        reloadEvents()
        showStub(
            appContext.getString(
                if (ok) com.metro.calendar.R.string.event_saved else com.metro.calendar.R.string.event_save_failed,
            ),
        )
    }

    fun deleteEvent(eventId: Long) {
        val ok = writeRepository.deleteEvent(eventId)
        eventDraft = null
        eventDetail = null
        route = CalendarRoute.Root
        reloadEvents()
        showStub(
            appContext.getString(
                if (ok) com.metro.calendar.R.string.event_deleted else com.metro.calendar.R.string.event_delete_failed,
            ),
        )
    }

    private fun ensureWritableCalendar() {
        writeRepository.ensureWritableCalendar()
        writableCalendars = writeRepository.writableCalendars()
    }

    fun setCalendarVisible(calendarId: Long, visible: Boolean) {
        writeRepository.setCalendarVisible(calendarId, visible)
        writableCalendars = writeRepository.writableCalendars()
        reloadEvents()
    }

    fun setCalendarColor(calendarId: Long, colorHex: String) {
        writeRepository.setCalendarColor(calendarId, colorHex)
        writableCalendars = writeRepository.writableCalendars()
        reloadEvents()
    }

    private fun defaultStartMillis(epochDay: Long): Long {
        val date = LocalDate.ofEpochDay(epochDay)
        val today = CalendarLogic.todayEpochDay(zoneId)
        val hour = if (epochDay == today) {
            java.time.LocalTime.now(zoneId).hour + 1
        } else {
            9
        }
        return date.atTime(hour.coerceIn(0, 23), 0).atZone(zoneId).toInstant().toEpochMilli()
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
