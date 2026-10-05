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
import java.time.LocalDate
import java.time.ZoneId

class CalendarState(context: Context) {
    private val repository = CalendarRepository(context)
    internal val appContext = context.applicationContext
    private val zoneId: ZoneId = ZoneId.systemDefault()

    var generation by mutableIntStateOf(0)
        private set

    private fun notifyChanged() {
        generation++
    }

    var hasCalendarPermission: Boolean = false
        private set

    var usingDemoData: Boolean = false
        private set

    var skippedPermissions: Boolean = false
        private set

    /** True when the calendar provider query itself failed (distinct from no permission). */
    var loadFailed: Boolean = false
        private set

    /** False until the first [refreshPermission] completes — avoids flashing the permission gate. */
    var permissionsChecked: Boolean = false
        private set

    val needsPermissionGate: Boolean
        get() = !hasCalendarPermission && !skippedPermissions

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
        permissionsChecked = true
        notifyChanged()
    }

    fun onPermissionResult(granted: Boolean) {
        hasCalendarPermission = granted
        skippedPermissions = false
        if (granted) {
            reloadEvents()
        } else {
            usingDemoData = true
            loadFailed = false
            events = repository.loadDemoEvents()
        }
        notifyChanged()
    }

    fun continueWithDemo() {
        skippedPermissions = true
        usingDemoData = true
        loadFailed = false
        events = repository.loadDemoEvents()
        notifyChanged()
    }

    /**
     * Loads real provider events. Demo data is only used when the user skipped the permission
     * gate — a provider query failure shows the empty/error state instead of fake events.
     */
    fun reloadEvents() {
        if (hasCalendarPermission) {
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
        } else if (skippedPermissions) {
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
     * Pulls fresh provider events whenever the selected date drifts near the edge of the loaded
     * window, so paging days/months always reflects the real calendar.
     */
    private fun ensureRangeLoaded(epochDay: Long) {
        if (!hasCalendarPermission || usingDemoData) return
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

    /** Force a fresh pull from the device calendar provider (Google/Exchange/local accounts). */
    fun syncNow() {
        if (hasCalendarPermission) {
            reloadEvents()
        } else {
            refreshPermission(appContext)
            if (hasCalendarPermission) reloadEvents() else notifyChanged()
        }
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
