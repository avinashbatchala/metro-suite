package com.metro.calendar.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.metro.calendar.data.CalendarAttendee
import com.metro.calendar.data.CalendarDraft
import com.metro.calendar.data.CalendarEvent
import com.metro.calendar.data.CalendarInfo
import com.metro.calendar.data.CalendarLogic
import com.metro.calendar.data.CalendarPreferences
import com.metro.calendar.data.CalendarPresentation
import com.metro.calendar.data.CalendarRepository
import com.metro.calendar.data.CalendarView
import com.metro.calendar.data.CalendarWeather
import com.metro.calendar.data.CalendarWeatherReader
import com.metro.calendar.data.CalendarWeek
import com.metro.calendar.data.CalendarWriteRepository
import com.metro.calendar.data.DayBucket
import com.metro.calendar.data.MiniMonth
import com.metro.calendar.data.MonthGridCell
import com.metro.calendar.data.TimelineEvent
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
    data object Settings : CalendarRoute
    data object AddSubscription : CalendarRoute
    data object EventDetail : CalendarRoute
    data object EventEdit : CalendarRoute
    data object EventEditDetails : CalendarRoute
    data class SubscriptionDetail(val id: String) : CalendarRoute
}

class CalendarState(context: Context) {
    private val repository = CalendarRepository(context)
    private val writeRepository = CalendarWriteRepository(context)
    private val calendarPrefs = CalendarPreferences(context)
    internal val appContext = context.applicationContext

    private var zoneId: ZoneId = ZoneId.systemDefault()
    private var locale: Locale = Locale.getDefault()
    private var use24Hour: Boolean = android.text.format.DateFormat.is24HourFormat(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    var generation by mutableIntStateOf(0)
        private set

    private fun notifyChanged() {
        generation++
    }

    // ---- Permissions -------------------------------------------------------

    var hasReadPermission: Boolean by mutableStateOf(false)
        private set
    var hasWritePermission: Boolean by mutableStateOf(false)
        private set

    /** False until the first [refreshSettings] completes. */
    var permissionsChecked: Boolean by mutableStateOf(false)
        private set

    // ---- Subscriptions -----------------------------------------------------

    var subscriptions: List<CalendarSubscription> by mutableStateOf(emptyList())
        private set

    var syncing: Boolean by mutableStateOf(false)
        private set

    val hasSubscriptionSource: Boolean get() = subscriptions.any { it.enabled }

    /** True when the user has neither device access nor any subscription. */
    val hasAnySource: Boolean get() = hasReadPermission || subscriptions.isNotEmpty()

    // ---- Calendars (settings) ----------------------------------------------

    var readableCalendars: List<CalendarInfo> by mutableStateOf(emptyList())
        private set
    var writableCalendars: List<CalendarInfo> by mutableStateOf(emptyList())
        private set

    // ---- Navigation --------------------------------------------------------

    var route: CalendarRoute by mutableStateOf(CalendarRoute.Root)
        private set

    fun openSettings() {
        route = CalendarRoute.Settings
        loadCalendars()
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

    /** Route-aware Back for subpages. */
    fun routeBack() {
        when (route) {
            is CalendarRoute.SubscriptionDetail -> route = CalendarRoute.Settings
            CalendarRoute.AddSubscription -> route = CalendarRoute.Settings
            CalendarRoute.Settings -> route = CalendarRoute.Root
            CalendarRoute.EventEditDetails -> route = CalendarRoute.EventEdit
            CalendarRoute.EventDetail -> {
                eventDetail = null
                route = CalendarRoute.Root
            }
            CalendarRoute.EventEdit -> {
                eventDraft = null
                route = if (eventDetail != null) CalendarRoute.EventDetail else CalendarRoute.Root
            }
            CalendarRoute.Root -> route = CalendarRoute.Root
        }
        notifyChanged()
    }

    // ---- View / presentation ----------------------------------------------

    var view: CalendarView by mutableStateOf(CalendarView.Week)
        private set

    var presentation: CalendarPresentation by mutableStateOf(CalendarPresentation.Calendar)
        private set

    /** True while the WP8.1 View selector popup is open. */
    var viewMenuOpen: Boolean by mutableStateOf(false)

    fun selectView(value: CalendarView) {
        viewMenuOpen = false
        if (view == value) return
        view = value
        detailPaneEpochDay = null
        if (presentation == CalendarPresentation.Agenda && value >= CalendarView.Month) {
            presentation = CalendarPresentation.Calendar
        }
        reloadEvents()
        notifyChanged()
    }

    /** Ellipsis: show agenda (Day/Week) or show calendar. */
    fun togglePresentation() {
        presentation = presentation.toggled()
        notifyChanged()
    }

    // ---- Selected period ---------------------------------------------------

    var selectedEpochDay: Long by mutableStateOf(CalendarLogic.todayEpochDay(zoneId))
        private set

    /** The day whose appointments are expanded in the Week/Month panes (null = none). */
    var detailPaneEpochDay: Long? by mutableStateOf(null)
        private set

    /** Bumped by the Today action so the agenda/day list scrolls to now. */
    var scrollToNowRequestId: Int = 0
        private set

    fun goToToday() {
        selectedEpochDay = CalendarLogic.todayEpochDay(zoneId)
        detailPaneEpochDay = null
        ensureRangeLoaded()
        scrollToNowRequestId++
        notifyChanged()
    }

    /** Expand a day's appointments within the current Week/Month view (does not leave the view). */
    fun selectDay(epochDay: Long) {
        detailPaneEpochDay = epochDay
        notifyChanged()
    }

    fun clearDetailPane() {
        if (detailPaneEpochDay != null) {
            detailPaneEpochDay = null
            notifyChanged()
        }
    }

    /** Deliberate drill-down into the full Day view. */
    fun openDay(epochDay: Long) {
        selectedEpochDay = epochDay
        detailPaneEpochDay = null
        selectView(CalendarView.Day)
    }

    /** Year → Month drill-down (must not jump to Day). */
    fun openMonth(year: Int, monthValue: Int) {
        selectedEpochDay = CalendarLogic.monthAnchorEpochDay(year, monthValue)
        detailPaneEpochDay = null
        view = CalendarView.Month
        ensureRangeLoaded()
        notifyChanged()
    }

    /** Year → Month for the current day's month. */
    fun openMonthOf(epochDay: Long) {
        val date = LocalDate.ofEpochDay(epochDay)
        openMonth(date.year, date.monthValue)
    }

    /** Pager settle: move the selected day without changing the view. */
    fun selectDateFromPager(epochDay: Long) {
        if (selectedEpochDay == epochDay) return
        selectedEpochDay = epochDay
        detailPaneEpochDay = null
        ensureRangeLoaded()
        notifyChanged()
    }

    // ---- Events ------------------------------------------------------------

    var events: List<CalendarEvent> by mutableStateOf(emptyList())
        private set

    var loading: Boolean by mutableStateOf(false)
        private set

    var loadFailed: Boolean by mutableStateOf(false)
        private set

    private var loadedStartDay: Long = Long.MIN_VALUE
    private var loadedEndDay: Long = Long.MIN_VALUE
    private var loadToken: Int = 0

    fun reloadEvents() {
        loadedStartDay = Long.MIN_VALUE
        loadedEndDay = Long.MIN_VALUE
        ensureRangeLoaded()
    }

    private fun ensureRangeLoaded() {
        val (start, end) = CalendarLogic.visibleRangeDays(view, selectedEpochDay)
        if (loadedStartDay != Long.MIN_VALUE && start >= loadedStartDay && end <= loadedEndDay) return
        val token = ++loadToken
        loading = true
        loadFailed = false
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { repository.loadEventsForDays(start, end) }
            }
            if (token != loadToken) return@launch
            loading = false
            result.onSuccess {
                events = it
                loadedStartDay = start
                loadedEndDay = end
            }.onFailure {
                events = emptyList()
                loadFailed = true
            }
            notifyChanged()
        }
    }

    // ---- Derived view models ----------------------------------------------

    val week: CalendarWeek
        get() = CalendarLogic.buildWeek(
            anchorEpochDay = selectedEpochDay,
            events = events,
            zoneId = zoneId,
            locale = locale,
            selectedEpochDay = selectedEpochDay,
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

    fun timeline(epochDay: Long): List<TimelineEvent> =
        CalendarLogic.buildTimeline(events, epochDay, zoneId)

    fun allDayForDay(epochDay: Long): List<CalendarEvent> =
        CalendarLogic.timelineAllDayEvents(events, epochDay, zoneId)

    fun eventsForDay(epochDay: Long): List<CalendarEvent> =
        CalendarLogic.eventsForDay(events, epochDay, zoneId)

    val monthWeekdayLabels: List<String>
        get() {
            val first = CalendarLogic.firstDayOfWeek(locale)
            return (0..6).map { offset ->
                first.plus(offset.toLong())
                    .getDisplayName(java.time.format.TextStyle.SHORT, locale)
            }
        }

    fun dateOverline(epochDay: Long): String =
        CalendarLogic.dateHeaderLabel(epochDay, zoneId, locale)

    fun monthName(epochDay: Long): String =
        CalendarLogic.monthNameLower(epochDay, zoneId, locale)

    fun yearLabel(epochDay: Long): String = CalendarLogic.yearLabel(epochDay, zoneId)

    fun dayLabel(epochDay: Long, style: java.time.format.TextStyle = java.time.format.TextStyle.FULL): String =
        LocalDate.ofEpochDay(epochDay).dayOfWeek.getDisplayName(style, locale).lowercase(locale)

    fun use24Hour(): Boolean = use24Hour

    fun timeLabel(millis: Long): String =
        CalendarLogic.formatEventTime(
            CalendarEvent(0, "", millis, millis, false, "#FFFFFF", null, null),
            zoneId,
            locale,
            use24Hour,
        )

    fun eventTimeLabel(event: CalendarEvent): String =
        CalendarLogic.formatEventTime(event, zoneId, locale, use24Hour)

    fun eventDurationLabel(event: CalendarEvent): String =
        CalendarLogic.formatEventDuration(event, zoneId)

    /** Agenda list for the selected Day or Week period only (later WP8.1 update behavior). */
    val agendaBuckets: List<DayBucket>
        get() {
            val (start, end) = when (view) {
                CalendarView.Week -> {
                    val date = LocalDate.ofEpochDay(selectedEpochDay)
                    val offset = (date.dayOfWeek.value - CalendarLogic.firstDayOfWeek(locale).value + 7) % 7
                    date.minusDays(offset.toLong()).toEpochDay() to 7
                }
                else -> selectedEpochDay to 1
            }
            return CalendarLogic.groupIntoAgendaBuckets(events, start, end, zoneId)
                .map { it.copy(headerLabel = CalendarLogic.dateHeaderLabel(it.epochDay, zoneId, locale)) }
        }

    // ---- Weather (date-correct only) --------------------------------------

    var weather: CalendarWeather? by mutableStateOf(null)
        private set

    private fun loadWeather() {
        val today = CalendarLogic.todayEpochDay(zoneId)
        if (selectedEpochDay != today) {
            weather = null
            return
        }
        scope.launch {
            val loaded = withContext(Dispatchers.IO) { CalendarWeatherReader.read(appContext) }
            weather = loaded
        }
    }

    // ---- Settings / permissions -------------------------------------------

    fun refreshSettings(context: Context) {
        zoneId = ZoneId.systemDefault()
        locale = Locale.getDefault()
        use24Hour = android.text.format.DateFormat.is24HourFormat(context)
        hasReadPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR,
        ) == PackageManager.PERMISSION_GRANTED
        hasWritePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CALENDAR,
        ) == PackageManager.PERMISSION_GRANTED
        subscriptions = repository.subscriptions()
        permissionsChecked = true
        loadCalendars()
        loadWeather()
        reloadEvents()
        notifyChanged()
    }

    fun onReadPermissionResult(granted: Boolean) {
        hasReadPermission = granted
        reloadEvents()
    }

    fun onWritePermissionResult(granted: Boolean) {
        hasWritePermission = granted
        notifyChanged()
    }

    fun loadCalendars() {
        if (!hasReadPermission) {
            readableCalendars = emptyList()
            writableCalendars = emptyList()
            return
        }
        readableCalendars = writeRepository.readableCalendars()
        writableCalendars = readableCalendars.filter { it.canWrite }
    }

    // ---- Calendar settings (local overrides) ------------------------------

    fun isCalendarVisible(calendar: CalendarInfo): Boolean =
        calendarPrefs.isVisible(CalendarPreferences.deviceKey(calendar.id)) && calendar.isVisible

    fun calendarColorOverride(calendar: CalendarInfo): String? =
        calendarPrefs.colorOverride(CalendarPreferences.deviceKey(calendar.id))

    fun setCalendarVisible(calendar: CalendarInfo, visible: Boolean) {
        calendarPrefs.setVisible(CalendarPreferences.deviceKey(calendar.id), visible)
        reloadEvents()
        notifyChanged()
    }

    fun setCalendarColor(calendar: CalendarInfo, colorHex: String) {
        calendarPrefs.setColor(CalendarPreferences.deviceKey(calendar.id), colorHex)
        reloadEvents()
        notifyChanged()
    }

    fun isSubscriptionVisible(subscription: CalendarSubscription): Boolean =
        calendarPrefs.isVisible(CalendarPreferences.subscriptionKey(subscription.id)) && subscription.enabled

    fun setSubscriptionVisible(subscription: CalendarSubscription, visible: Boolean) {
        calendarPrefs.setVisible(CalendarPreferences.subscriptionKey(subscription.id), visible)
        setSubscriptionEnabled(subscription.id, visible)
    }

    fun setSubscriptionEnabled(id: String, enabled: Boolean) {
        repository.setSubscriptionEnabled(id, enabled)
        subscriptions = repository.subscriptions()
        reloadEvents()
        notifyChanged()
    }

    // ---- Event detail ------------------------------------------------------

    var eventDetail: CalendarEvent? by mutableStateOf(null)
        private set

    var eventAttendees: List<CalendarAttendee> by mutableStateOf(emptyList())
        private set

    fun openEventDetail(event: CalendarEvent) {
        eventDetail = event
        eventAttendees = emptyList()
        route = CalendarRoute.EventDetail
        if (event.sourceType == com.metro.calendar.data.CalendarSourceType.DEVICE) {
            scope.launch {
                val list = withContext(Dispatchers.IO) { repository.attendees(event.id) }
                if (route == CalendarRoute.EventDetail && eventDetail?.id == event.id) {
                    eventAttendees = list
                }
            }
        }
        notifyChanged()
    }

    // ---- Long-press event actions -----------------------------------------

    /** Non-null while a long-press context menu is open on an editable event. */
    var eventMenuTarget: CalendarEvent? by mutableStateOf(null)
        private set

    fun openEventMenu(event: CalendarEvent) {
        if (!event.canEdit) return
        eventMenuTarget = event
        notifyChanged()
    }

    fun closeEventMenu() {
        eventMenuTarget = null
        notifyChanged()
    }

    // ---- Editor ------------------------------------------------------------

    var eventDraft: CalendarDraft? by mutableStateOf(null)
        private set

    fun openNewEvent(epochDay: Long = selectedEpochDay) {
        if (!hasWritePermission) {
            requestWrite = true
            notifyChanged()
            return
        }
        writableCalendars = writeRepository.writableCalendars()
        if (writableCalendars.isEmpty()) {
            writeRepository.ensureWritableCalendar()
            writableCalendars = writeRepository.writableCalendars()
        }
        val start = defaultStartMillis(epochDay)
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
        if (!event.canEdit) return
        if (!hasWritePermission) {
            requestWrite = true
            notifyChanged()
            return
        }
        writableCalendars = writeRepository.writableCalendars()
        eventDraft = CalendarDraft(
            id = event.id,
            calendarId = event.calendarId,
            title = event.title,
            location = event.location.orEmpty(),
            notes = event.description.orEmpty(),
            allDay = event.allDay,
            startMillis = event.startMillis,
            endMillis = event.endMillis,
            availability = event.availability,
            recurrence = com.metro.calendar.data.RecurrenceRule.None,
        )
        route = CalendarRoute.EventEdit
        notifyChanged()
    }

    /** Flag consumed by MainActivity to launch the WRITE_CALENDAR request. */
    var requestWrite: Boolean by mutableStateOf(false)
        private set

    fun consumeWriteRequest() {
        requestWrite = false
    }

    fun updateDraft(transform: (CalendarDraft) -> CalendarDraft) {
        val current = eventDraft ?: return
        eventDraft = transform(current)
        notifyChanged()
    }

    fun openEditorDetails() {
        route = CalendarRoute.EventEditDetails
        notifyChanged()
    }

    fun closeEditorDetails() {
        route = CalendarRoute.EventEdit
        notifyChanged()
    }

    fun saveDraft() {
        val draft = eventDraft ?: return
        if (draft.readOnly || !hasWritePermission) return
        val ok = if (draft.id == null) {
            writeRepository.createEvent(draft) != null
        } else {
            writeRepository.updateEvent(draft)
        }
        eventDraft = null
        eventDetail = null
        // Return to the origin context (same view/period), not an arbitrary view.
        route = CalendarRoute.Root
        reloadEvents()
        if (!ok) {
            statusMessage = appContext.getString(com.metro.calendar.R.string.event_save_failed)
        }
        notifyChanged()
    }

    fun deleteEvent(eventId: Long) {
        val ok = writeRepository.deleteEvent(eventId)
        eventDraft = null
        eventDetail = null
        route = CalendarRoute.Root
        reloadEvents()
        if (!ok) {
            statusMessage = appContext.getString(com.metro.calendar.R.string.event_delete_failed)
        }
        notifyChanged()
    }

    /** Quick Event: create a default-duration appointment from an empty Day slot. */
    fun quickCreateEvent(epochDay: Long, startMinute: Int, title: String): Boolean {
        if (!hasWritePermission) {
            requestWrite = true
            notifyChanged()
            return false
        }
        val date = LocalDate.ofEpochDay(epochDay)
        val start = date.atStartOfDay(zoneId).plusMinutes(startMinute.toLong()).toInstant().toEpochMilli()
        writableCalendars = writeRepository.writableCalendars()
        if (writableCalendars.isEmpty()) {
            writeRepository.ensureWritableCalendar()
            writableCalendars = writeRepository.writableCalendars()
        }
        val draft = CalendarDraft(
            calendarId = writableCalendars.firstOrNull()?.id ?: -1L,
            title = title,
            startMillis = start,
            endMillis = start + 30 * 60_000L,
        )
        val created = writeRepository.createEvent(draft) != null
        if (created) reloadEvents()
        return created
    }

    // ---- Status ------------------------------------------------------------

    /** Inline status text (replaces Android Toasts). Auto-cleared by the UI. */
    var statusMessage: String? by mutableStateOf(null)

    fun clearStatus() {
        statusMessage = null
    }

    // ---- Subscription actions ---------------------------------------------

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

    fun syncSubscription(id: String) {
        if (syncing) return
        syncing = true
        notifyChanged()
        scope.launch {
            withContext(Dispatchers.IO) { repository.syncSubscription(id) }
            syncing = false
            subscriptions = repository.subscriptions()
            reloadEvents()
            notifyChanged()
        }
    }

    /** Manual refresh of every enabled subscription (moved out of the root menu). */
    fun syncAllSubscriptions() {
        if (syncing || !hasSubscriptionSource) return
        syncing = true
        notifyChanged()
        scope.launch {
            val failures = withContext(Dispatchers.IO) { repository.syncAllSubscriptions() }
            syncing = false
            subscriptions = repository.subscriptions()
            reloadEvents()
            if (failures > 0) {
                statusMessage = appContext.getString(com.metro.calendar.R.string.sync_failed_count, failures)
            }
            notifyChanged()
        }
    }

    private fun defaultStartMillis(epochDay: Long): Long {
        val date = LocalDate.ofEpochDay(epochDay)
        val today = CalendarLogic.todayEpochDay(zoneId)
        val hour = if (epochDay == today) java.time.LocalTime.now(zoneId).hour + 1 else 9
        return date.atTime(hour.coerceIn(0, 23), 0).atZone(zoneId).toInstant().toEpochMilli()
    }
}
