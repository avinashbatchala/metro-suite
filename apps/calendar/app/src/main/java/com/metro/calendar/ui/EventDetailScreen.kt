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
import com.metro.calendar.data.CalendarAttendee
import com.metro.calendar.data.CalendarEvent
import com.metro.calendar.data.CalendarLogic
import com.metro.calendar.data.CalendarSourceType
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroCircleIconButton
import com.metro.ui.MetroDimens
import com.metro.ui.MetroMessageDialog
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

/** WP8.1 event detail: title, when/where, calendar, attendees, status, notes. */
@Composable
fun EventDetailScreen(
    event: CalendarEvent,
    attendees: List<CalendarAttendee>,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    val startDay = CalendarLogic.epochDayFromMillis(event.startMillis)
    val accent = eventAccent(event)

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MetroTheme.colors.background)
                .statusBarsPadding()
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
                    contentDescription = stringResource(R.string.app_name),
                )
            }

            MetroText(
                text = event.title,
                style = MetroTextStyle.HubTitle,
                color = accent,
                modifier = Modifier.padding(
                    start = MetroDimens.ScreenHorizontalMargin,
                    end = MetroDimens.ScreenHorizontalMargin,
                    top = 4.dp,
                ),
            )

            DetailField(
                label = stringResource(R.string.event_when_label),
                value = CalendarLogic.dateHeaderLabel(startDay) + "\n" + CalendarLogic.tileTimeRange(event),
            )
            event.location?.takeIf { it.isNotBlank() }?.let {
                DetailField(label = stringResource(R.string.event_location_label), value = it)
            }
            event.calendarName?.takeIf { it.isNotBlank() }?.let {
                DetailField(label = stringResource(R.string.event_calendar_label), value = it)
            }
            if (event.availability != com.metro.calendar.data.EventAvailability.Busy) {
                DetailField(label = stringResource(R.string.event_status_label), value = event.availability.title)
            }
            if (event.recurring) {
                DetailField(label = stringResource(R.string.event_repeat_label), value = stringResource(R.string.repeats))
            }

            if (attendees.isNotEmpty()) {
                DetailField(
                    label = stringResource(R.string.event_attendees_label),
                    value = stringResource(R.string.people_count, attendees.size),
                )
                attendees.forEach { attendee ->
                    val label = attendee.name ?: attendee.email ?: return@forEach
                    MetroText(
                        text = label,
                        style = MetroTextStyle.ListItemTitle,
                        color = MetroTheme.colors.primaryText,
                        modifier = Modifier.padding(
                            start = MetroDimens.ScreenHorizontalMargin,
                            end = MetroDimens.ScreenHorizontalMargin,
                            bottom = 2.dp,
                        ),
                    )
                }
            }

            event.description?.takeIf { it.isNotBlank() }?.let {
                DetailField(label = stringResource(R.string.event_notes_label), value = it)
            }

            if (event.sourceType == CalendarSourceType.SUBSCRIPTION) {
                Spacer(modifier = Modifier.height(16.dp))
                MetroText(
                    text = stringResource(R.string.subscription_read_only),
                    style = MetroTextStyle.Body,
                    color = MetroTheme.colors.secondaryText,
                    modifier = Modifier.padding(horizontal = MetroDimens.ScreenHorizontalMargin),
                )
            }

            Spacer(modifier = Modifier.height(96.dp))
        }

        if (event.canEdit) {
            MetroAppBar(
                icons = listOf(
                    MetroAppBarIcon(
                        type = MetroSystemIconType.Edit,
                        label = stringResource(R.string.edit_event),
                        onClick = onEdit,
                    ),
                    MetroAppBarIcon(
                        type = MetroSystemIconType.Delete,
                        label = stringResource(R.string.delete_event),
                        onClick = { confirmDelete = true },
                    ),
                ),
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        if (confirmDelete) {
            MetroMessageDialog(
                title = stringResource(R.string.delete_event_title),
                body = stringResource(R.string.delete_event_body),
                confirmLabel = stringResource(R.string.delete_event),
                dismissLabel = stringResource(R.string.cancel),
                onConfirm = {
                    confirmDelete = false
                    onDelete()
                },
                onDismiss = { confirmDelete = false },
                onDismissRequest = { confirmDelete = false },
            )
        }
    }
}

@Composable
private fun DetailField(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = MetroDimens.ScreenHorizontalMargin,
                end = MetroDimens.ScreenHorizontalMargin,
                top = 14.dp,
            ),
    ) {
        MetroText(
            text = label,
            style = MetroTextStyle.SectionHeader,
            color = MetroTheme.colors.secondaryText,
            modifier = Modifier.padding(bottom = 2.dp),
        )
        MetroText(
            text = value,
            style = MetroTextStyle.ListItemTitle,
            color = MetroTheme.colors.primaryText,
        )
    }
}
