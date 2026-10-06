package com.metro.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.calendar.R
import com.metro.calendar.data.CalendarDraft
import com.metro.calendar.data.CalendarLogic
import com.metro.calendar.data.EventAvailability
import com.metro.calendar.data.RecurrenceRule
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroCheckBox
import com.metro.ui.MetroCircleIconButton
import com.metro.ui.MetroColors
import com.metro.ui.MetroDatePicker
import com.metro.ui.MetroDimens
import com.metro.ui.MetroListPicker
import com.metro.ui.MetroListPickerOption
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroTextBox
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.MetroTimePicker
import com.metro.ui.metroClickable
import com.metro.ui.metroNavBarPadding
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

private enum class EditorPicker { None, StartDate, StartTime, EndDate, EndTime }

/**
 * WP8.1 appointment editor. The **basic** screen is fast (subject/location/when/all-day) with a
 * `more details` link; the **advanced** screen adds calendar/reminder/repeat/status/notes. Date and
 * time use picker surfaces, not permanently-expanded wheels.
 */
@Composable
fun EventEditScreen(
    state: CalendarState,
    advanced: Boolean,
    onBack: () -> Unit,
    onMoreDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val draft = state.eventDraft
    if (draft == null) {
        androidx.compose.runtime.LaunchedEffect(Unit) { onBack() }
        return
    }

    val zone = remember { ZoneId.systemDefault() }
    val use24Hour = state.use24Hour()
    var picker by remember { mutableStateOf(EditorPicker.None) }
    val isEditing = draft.id != null

    val startDateTime = remember(draft.startMillis) { draft.startMillis.toLocalDateTime(zone) }
    val endDateTime = remember(draft.endMillis) { draft.endMillis.toLocalDateTime(zone) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background)
            .statusBarsPadding()
            .metroNavBarPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = MetroDimens.ScreenHorizontalMargin - 8.dp,
                        end = MetroDimens.ScreenHorizontalMargin,
                        top = 8.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MetroCircleIconButton(
                    type = MetroSystemIconType.Back,
                    onClick = onBack,
                    contentDescription = stringResource(R.string.cancel),
                )
            }

            MetroText(
                text = stringResource(
                    when {
                        advanced -> R.string.more_details
                        isEditing -> R.string.edit_appointment
                        else -> R.string.new_appointment
                    },
                ),
                style = MetroTextStyle.AppTitle,
                color = MetroTheme.colors.primaryText,
                modifier = Modifier.padding(horizontal = MetroDimens.ScreenHorizontalMargin),
            )

            val fieldModifier = Modifier.padding(
                start = MetroDimens.ScreenHorizontalMargin,
                end = MetroDimens.ScreenHorizontalMargin,
                top = 14.dp,
            )

            if (!advanced) {
                FieldLabel(stringResource(R.string.event_subject_label))
                MetroTextBox(
                    value = draft.title,
                    onValueChange = { v -> state.updateDraft { it.copy(title = v) } },
                    placeholder = stringResource(R.string.event_subject_label),
                    modifier = fieldModifier.fillMaxWidth(),
                )

                FieldLabel(stringResource(R.string.event_location_field))
                MetroTextBox(
                    value = draft.location,
                    onValueChange = { v -> state.updateDraft { it.copy(location = v) } },
                    placeholder = stringResource(R.string.event_location_field),
                    modifier = fieldModifier.fillMaxWidth(),
                )

                Row(modifier = fieldModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    MetroCheckBox(
                        checked = draft.allDay,
                        onCheckedChange = { c -> state.updateDraft { it.copy(allDay = c) } },
                    )
                    Spacer(Modifier.width(12.dp))
                    MetroText(
                        text = stringResource(R.string.all_day),
                        style = MetroTextStyle.ListItemTitle,
                        color = MetroTheme.colors.primaryText,
                    )
                }

                FieldLabel(stringResource(R.string.event_starts_label))
                DateTimeRow(
                    dateText = CalendarLogic.dateHeaderLabel(startDateTime.toLocalDate().toEpochDay(), zone),
                    timeText = if (draft.allDay) null else formatTime(startDateTime, use24Hour),
                    onDateClick = { picker = EditorPicker.StartDate },
                    onTimeClick = if (draft.allDay) null else { { picker = EditorPicker.StartTime } },
                    modifier = fieldModifier,
                )

                if (!draft.allDay) {
                    FieldLabel(stringResource(R.string.event_ends_label))
                    DateTimeRow(
                        dateText = CalendarLogic.dateHeaderLabel(endDateTime.toLocalDate().toEpochDay(), zone),
                        timeText = formatTime(endDateTime, use24Hour),
                        onDateClick = { picker = EditorPicker.EndDate },
                        onTimeClick = { picker = EditorPicker.EndTime },
                        modifier = fieldModifier,
                    )
                }

                MetroText(
                    text = stringResource(R.string.more_details),
                    style = MetroTextStyle.ListItemTitle,
                    color = MetroTheme.colors.accent,
                    modifier = fieldModifier
                        .fillMaxWidth()
                        .metroClickable(onClick = onMoreDetails)
                        .padding(vertical = 14.dp),
                )
            } else {
                if (state.writableCalendars.isNotEmpty()) {
                    MetroListPicker(
                        selected = draft.calendarId,
                        options = state.writableCalendars.map {
                            MetroListPickerOption(it.id, it.displayName)
                        },
                        onSelectedChange = { id -> state.updateDraft { it.copy(calendarId = id) } },
                        label = stringResource(R.string.event_calendar_label),
                        modifier = fieldModifier.fillMaxWidth(),
                    )
                }
                MetroListPicker(
                    selected = draft.reminderMinutes,
                    options = reminderOptions(),
                    onSelectedChange = { m -> state.updateDraft { it.copy(reminderMinutes = m) } },
                    label = stringResource(R.string.event_reminder_label),
                    modifier = fieldModifier.fillMaxWidth(),
                )
                MetroListPicker(
                    selected = draft.recurrence,
                    options = recurrenceOptions(),
                    onSelectedChange = { r -> state.updateDraft { it.copy(recurrence = r) } },
                    label = stringResource(R.string.event_repeat_label),
                    modifier = fieldModifier.fillMaxWidth(),
                )
                MetroListPicker(
                    selected = draft.availability,
                    options = availabilityOptions(),
                    onSelectedChange = { a -> state.updateDraft { it.copy(availability = a) } },
                    label = stringResource(R.string.event_status_label),
                    modifier = fieldModifier.fillMaxWidth(),
                )
                FieldLabel(stringResource(R.string.event_notes_label))
                MetroTextBox(
                    value = draft.notes,
                    onValueChange = { v -> state.updateDraft { it.copy(notes = v) } },
                    placeholder = stringResource(R.string.event_notes_label),
                    singleLine = false,
                    modifier = fieldModifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(120.dp))
        }

        MetroAppBar(
            icons = listOf(
                MetroAppBarIcon(
                    type = MetroSystemIconType.Check,
                    label = stringResource(R.string.save),
                    onClick = state::saveDraft,
                ),
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        // Picker surface.
        if (picker != EditorPicker.None) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .metroClickable { picker = EditorPicker.None },
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(MetroColors.DarkSecondarySurface)
                        .metroNavBarPadding()
                        .padding(vertical = 12.dp),
                ) {
                    when (picker) {
                        EditorPicker.StartDate -> MetroDatePicker(
                            year = startDateTime.year,
                            month = startDateTime.monthValue,
                            dayOfMonth = startDateTime.dayOfMonth,
                            onYear = { state.updateDraft { d -> d.withStart(d.startDateTime(zone).withYear(it), zone) } },
                            onMonth = { state.updateDraft { d -> d.withStart(d.startDateTime(zone).withMonth(it), zone) } },
                            onDay = { state.updateDraft { d -> d.withStart(d.startDateTime(zone).withDayOfMonth(it), zone) } },
                            modifier = Modifier.padding(horizontal = MetroDimens.ScreenHorizontalMargin),
                        )
                        EditorPicker.StartTime -> MetroTimePicker(
                            hour = startDateTime.hour,
                            minute = startDateTime.minute,
                            use24Hour = use24Hour,
                            onHour = { h -> state.updateDraft { d -> d.withStart(d.startDateTime(zone).withHour(h), zone) } },
                            onMinute = { m -> state.updateDraft { d -> d.withStart(d.startDateTime(zone).withMinute(m), zone) } },
                            modifier = Modifier.padding(horizontal = MetroDimens.ScreenHorizontalMargin),
                        )
                        EditorPicker.EndDate -> MetroDatePicker(
                            year = endDateTime.year,
                            month = endDateTime.monthValue,
                            dayOfMonth = endDateTime.dayOfMonth,
                            onYear = { state.updateDraft { d -> d.withEnd(d.endDateTime(zone).withYear(it), zone) } },
                            onMonth = { state.updateDraft { d -> d.withEnd(d.endDateTime(zone).withMonth(it), zone) } },
                            onDay = { state.updateDraft { d -> d.withEnd(d.endDateTime(zone).withDayOfMonth(it), zone) } },
                            modifier = Modifier.padding(horizontal = MetroDimens.ScreenHorizontalMargin),
                        )
                        EditorPicker.EndTime -> MetroTimePicker(
                            hour = endDateTime.hour,
                            minute = endDateTime.minute,
                            use24Hour = use24Hour,
                            onHour = { h -> state.updateDraft { d -> d.withEnd(d.endDateTime(zone).withHour(h), zone) } },
                            onMinute = { m -> state.updateDraft { d -> d.withEnd(d.endDateTime(zone).withMinute(m), zone) } },
                            modifier = Modifier.padding(horizontal = MetroDimens.ScreenHorizontalMargin),
                        )
                        EditorPicker.None -> Unit
                    }
                    MetroText(
                        text = stringResource(R.string.done),
                        style = MetroTextStyle.ListItemTitle,
                        color = MetroTheme.colors.accent,
                        modifier = Modifier
                            .align(Alignment.End)
                            .metroClickable { picker = EditorPicker.None }
                            .padding(horizontal = MetroDimens.ScreenHorizontalMargin, vertical = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    MetroText(
        text = text,
        style = MetroTextStyle.SectionHeader,
        color = MetroTheme.colors.secondaryText,
        modifier = Modifier.padding(
            start = MetroDimens.ScreenHorizontalMargin,
            end = MetroDimens.ScreenHorizontalMargin,
            top = 14.dp,
        ),
    )
}

@Composable
private fun DateTimeRow(
    dateText: String,
    timeText: String?,
    onDateClick: () -> Unit,
    onTimeClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        MetroText(
            text = dateText,
            style = MetroTextStyle.ListItemTitle,
            color = MetroTheme.colors.primaryText,
            maxLines = 1,
            modifier = Modifier
                .weight(1f)
                .metroClickable(onClick = onDateClick)
                .padding(vertical = 12.dp),
        )
        if (timeText != null && onTimeClick != null) {
            MetroText(
                text = timeText,
                style = MetroTextStyle.ListItemTitle,
                color = MetroTheme.colors.accent,
                maxLines = 1,
                modifier = Modifier
                    .metroClickable(onClick = onTimeClick)
                    .padding(vertical = 12.dp, horizontal = 8.dp),
            )
        }
    }
}

private fun formatTime(dateTime: LocalDateTime, use24Hour: Boolean): String {
    val pattern = if (use24Hour) "HH:mm" else "h:mm a"
    return dateTime.format(java.time.format.DateTimeFormatter.ofPattern(pattern))
}

@Composable
private fun reminderOptions(): List<MetroListPickerOption<Int?>> = listOf(
    MetroListPickerOption(null, stringResource(R.string.reminder_none)),
    MetroListPickerOption(0, stringResource(R.string.reminder_at_time)),
    MetroListPickerOption(5, stringResource(R.string.reminder_5)),
    MetroListPickerOption(10, stringResource(R.string.reminder_10)),
    MetroListPickerOption(30, stringResource(R.string.reminder_30)),
    MetroListPickerOption(60, stringResource(R.string.reminder_60)),
    MetroListPickerOption(1440, stringResource(R.string.reminder_1440)),
)

@Composable
private fun recurrenceOptions(): List<MetroListPickerOption<RecurrenceRule>> = listOf(
    MetroListPickerOption(RecurrenceRule.None, stringResource(R.string.repeat_never)),
    MetroListPickerOption(RecurrenceRule.Daily, stringResource(R.string.repeat_daily)),
    MetroListPickerOption(RecurrenceRule.Weekly, stringResource(R.string.repeat_weekly)),
    MetroListPickerOption(RecurrenceRule.Monthly, stringResource(R.string.repeat_monthly)),
    MetroListPickerOption(RecurrenceRule.Yearly, stringResource(R.string.repeat_yearly)),
)

@Composable
private fun availabilityOptions(): List<MetroListPickerOption<EventAvailability>> =
    EventAvailability.entries.map { MetroListPickerOption(it, it.title) }

private fun Long.toLocalDateTime(zone: ZoneId): LocalDateTime =
    Instant.ofEpochMilli(this).atZone(zone).toLocalDateTime()

private fun CalendarDraft.startDateTime(zone: ZoneId): LocalDateTime = startMillis.toLocalDateTime(zone)
private fun CalendarDraft.endDateTime(zone: ZoneId): LocalDateTime = endMillis.toLocalDateTime(zone)

private fun CalendarDraft.withStart(dateTime: LocalDateTime, zone: ZoneId): CalendarDraft {
    val newStart = dateTime.atZone(zone).toInstant().toEpochMilli()
    val duration = (endMillis - startMillis).coerceAtLeast(0)
    return copy(startMillis = newStart, endMillis = newStart + duration)
}

private fun CalendarDraft.withEnd(dateTime: LocalDateTime, zone: ZoneId): CalendarDraft {
    val newEnd = dateTime.atZone(zone).toInstant().toEpochMilli()
    return copy(endMillis = if (newEnd < startMillis) startMillis + 60_000L else newEnd)
}
