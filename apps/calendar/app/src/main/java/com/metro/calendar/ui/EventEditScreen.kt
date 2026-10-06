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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.calendar.R
import com.metro.calendar.data.CalendarDraft
import com.metro.calendar.data.CalendarLogic
import com.metro.calendar.data.RecurrenceRule
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroCheckBox
import com.metro.ui.MetroCircleIconButton
import com.metro.ui.MetroDatePicker
import com.metro.ui.MetroDimens
import com.metro.ui.MetroListPicker
import com.metro.ui.MetroListPickerOption
import com.metro.ui.MetroMessageDialog
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

private enum class EditorField { None, StartDate, StartTime, EndDate, EndTime }

@Composable
fun EventEditScreen(
    state: CalendarState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val draft = state.eventDraft
    if (draft == null) {
        androidx.compose.runtime.LaunchedEffect(Unit) { onBack() }
        return
    }

    val zone = remember { ZoneId.systemDefault() }
    val use24Hour = remember { android.text.format.DateFormat.is24HourFormat(state.appContext) }
    var expandedField by remember { mutableStateOf(EditorField.None) }
    var confirmDelete by remember { mutableStateOf(false) }

    val startDateTime = remember(draft.startMillis) { draft.startMillis.toLocalDateTime(zone) }
    val endDateTime = remember(draft.endMillis) { draft.endMillis.toLocalDateTime(zone) }
    val isEditing = draft.id != null

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
                    if (isEditing) R.string.edit_appointment else R.string.new_appointment,
                ),
                style = MetroTextStyle.AppTitle,
                color = MetroTheme.colors.primaryText,
                modifier = Modifier.padding(horizontal = MetroDimens.ScreenHorizontalMargin),
            )

            val fieldModifier = Modifier.padding(
                start = MetroDimens.ScreenHorizontalMargin,
                end = MetroDimens.ScreenHorizontalMargin,
                top = 16.dp,
            )

            FieldLabel(stringResource(R.string.event_subject_label))
            MetroTextBox(
                value = draft.title,
                onValueChange = { value -> state.updateDraft { it.copy(title = value) } },
                placeholder = stringResource(R.string.event_subject_label),
                modifier = fieldModifier.fillMaxWidth(),
            )

            FieldLabel(stringResource(R.string.event_location_field))
            MetroTextBox(
                value = draft.location,
                onValueChange = { value -> state.updateDraft { it.copy(location = value) } },
                placeholder = stringResource(R.string.event_location_field),
                modifier = fieldModifier.fillMaxWidth(),
            )

            Row(
                modifier = fieldModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MetroCheckBox(
                    checked = draft.allDay,
                    onCheckedChange = { checked ->
                        state.updateDraft { current -> current.copy(allDay = checked) }
                    },
                )
                Spacer(modifier = Modifier.width(12.dp))
                MetroText(
                    text = stringResource(R.string.all_day),
                    style = MetroTextStyle.ListItemTitle,
                    color = MetroTheme.colors.primaryText,
                )
            }

            // Start
            FieldLabel(stringResource(R.string.event_starts_label))
            DateTimeRow(
                dateText = CalendarLogic.dateHeaderLabel(startDateTime.toLocalDate().toEpochDay(), zone),
                timeText = if (draft.allDay) null else formatTime(startDateTime, use24Hour),
                onDateClick = {
                    expandedField = if (expandedField == EditorField.StartDate) EditorField.None else EditorField.StartDate
                },
                onTimeClick = if (draft.allDay) null else {
                    {
                        expandedField = if (expandedField == EditorField.StartTime) EditorField.None else EditorField.StartTime
                    }
                },
                modifier = fieldModifier,
            )
            if (expandedField == EditorField.StartDate) {
                MetroDatePicker(
                    year = startDateTime.year,
                    month = startDateTime.monthValue,
                    dayOfMonth = startDateTime.dayOfMonth,
                    onYear = { state.updateDraft { d -> d.withStart(d.startDateTime(zone).withYear(it), zone) } },
                    onMonth = { state.updateDraft { d -> d.withStart(d.startDateTime(zone).withMonth(it), zone) } },
                    onDay = { state.updateDraft { d -> d.withStart(d.startDateTime(zone).withDayOfMonth(it), zone) } },
                    modifier = fieldModifier,
                )
            }
            if (expandedField == EditorField.StartTime) {
                MetroTimePicker(
                    hour = startDateTime.hour,
                    minute = startDateTime.minute,
                    use24Hour = use24Hour,
                    onHour = { hour -> state.updateDraft { d -> d.withStart(d.startDateTime(zone).withHour(hour), zone) } },
                    onMinute = { minute -> state.updateDraft { d -> d.withStart(d.startDateTime(zone).withMinute(minute), zone) } },
                    modifier = fieldModifier,
                )
            }

            // End
            if (!draft.allDay) {
                FieldLabel(stringResource(R.string.event_ends_label))
                DateTimeRow(
                    dateText = CalendarLogic.dateHeaderLabel(endDateTime.toLocalDate().toEpochDay(), zone),
                    timeText = formatTime(endDateTime, use24Hour),
                    onDateClick = {
                        expandedField = if (expandedField == EditorField.EndDate) EditorField.None else EditorField.EndDate
                    },
                    onTimeClick = {
                        expandedField = if (expandedField == EditorField.EndTime) EditorField.None else EditorField.EndTime
                    },
                    modifier = fieldModifier,
                )
                if (expandedField == EditorField.EndDate) {
                    MetroDatePicker(
                        year = endDateTime.year,
                        month = endDateTime.monthValue,
                        dayOfMonth = endDateTime.dayOfMonth,
                        onYear = { state.updateDraft { d -> d.withEnd(d.endDateTime(zone).withYear(it), zone) } },
                        onMonth = { state.updateDraft { d -> d.withEnd(d.endDateTime(zone).withMonth(it), zone) } },
                        onDay = { state.updateDraft { d -> d.withEnd(d.endDateTime(zone).withDayOfMonth(it), zone) } },
                        modifier = fieldModifier,
                    )
                }
                if (expandedField == EditorField.EndTime) {
                    MetroTimePicker(
                        hour = endDateTime.hour,
                        minute = endDateTime.minute,
                        use24Hour = use24Hour,
                        onHour = { hour -> state.updateDraft { d -> d.withEnd(d.endDateTime(zone).withHour(hour), zone) } },
                        onMinute = { minute -> state.updateDraft { d -> d.withEnd(d.endDateTime(zone).withMinute(minute), zone) } },
                        modifier = fieldModifier,
                    )
                }
            }

            // Calendar
            if (state.writableCalendars.isNotEmpty()) {
                val options = state.writableCalendars.map {
                    MetroListPickerOption(it.id, it.displayName)
                }
                MetroListPicker(
                    selected = draft.calendarId,
                    options = options,
                    onSelectedChange = { id -> state.updateDraft { it.copy(calendarId = id) } },
                    label = stringResource(R.string.event_calendar_label),
                    modifier = fieldModifier.fillMaxWidth(),
                )
            }

            // Reminder
            val reminders = reminderOptions()
            MetroListPicker(
                selected = draft.reminderMinutes,
                options = reminders,
                onSelectedChange = { minutes -> state.updateDraft { it.copy(reminderMinutes = minutes) } },
                label = stringResource(R.string.event_reminder_label),
                modifier = fieldModifier.fillMaxWidth(),
            )

            // Recurrence
            MetroListPicker(
                selected = draft.recurrence,
                options = recurrenceOptions(),
                onSelectedChange = { value -> state.updateDraft { it.copy(recurrence = value) } },
                label = stringResource(R.string.event_repeat_label),
                modifier = fieldModifier.fillMaxWidth(),
            )

            FieldLabel(stringResource(R.string.event_notes_label))
            MetroTextBox(
                value = draft.notes,
                onValueChange = { value -> state.updateDraft { it.copy(notes = value) } },
                placeholder = stringResource(R.string.event_notes_label),
                singleLine = false,
                modifier = fieldModifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(120.dp))
        }

        MetroAppBar(
            icons = buildList {
                add(
                    MetroAppBarIcon(
                        type = MetroSystemIconType.Check,
                        label = stringResource(R.string.save),
                        onClick = state::saveDraft,
                    ),
                )
                if (isEditing) {
                    add(
                        MetroAppBarIcon(
                            type = MetroSystemIconType.Delete,
                            label = stringResource(R.string.delete_event),
                            onClick = { confirmDelete = true },
                        ),
                    )
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        if (confirmDelete) {
            MetroMessageDialog(
                title = stringResource(R.string.delete_event_title),
                body = stringResource(R.string.delete_event_body),
                confirmLabel = stringResource(R.string.delete_event),
                dismissLabel = stringResource(R.string.cancel),
                onConfirm = {
                    confirmDelete = false
                    draft.id?.let(state::deleteEvent)
                },
                onDismiss = { confirmDelete = false },
                onDismissRequest = { confirmDelete = false },
            )
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
            top = 16.dp,
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
