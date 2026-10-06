package com.metro.clock.ui

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.clock.R
import com.metro.clock.alarms.AlarmSchedule
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroCheckBox
import com.metro.ui.MetroCircleIconButton
import com.metro.ui.MetroListPicker
import com.metro.ui.MetroPageHeader
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroTextBox
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroToggleSwitch
import com.metro.ui.MetroTheme
import com.metro.ui.metroClickable

private val DAY_LABELS = listOf("M", "T", "W", "T", "F", "S", "S")

@Composable
fun AlarmEditScreen(
    state: ClockState,
    alarmId: Long?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val use24 = remember { DateFormat.is24HourFormat(context) }
    val existing = remember(alarmId, state.generation) { state.alarm(alarmId) }

    var hour by remember { mutableIntStateOf(existing?.hour ?: 7) }
    var minute by remember { mutableIntStateOf(existing?.minute ?: 0) }
    var label by remember { mutableStateOf(existing?.label.orEmpty()) }
    var mask by remember { mutableIntStateOf(existing?.repeatMask ?: 0) }
    var vibrate by remember { mutableStateOf(existing?.vibrate ?: true) }
    var snooze by remember { mutableIntStateOf(existing?.snoozeMinutes ?: 5) }

    val snoozeOptions = listOf(1, 2, 5, 10, 15, 30)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier.padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MetroCircleIconButton(
                type = MetroSystemIconType.Back,
                onClick = onBack,
                contentDescription = stringResource(R.string.back),
            )
        }
        MetroPageHeader(title = stringResource(R.string.alarm))

        MetroTimePicker(
            hour = hour,
            minute = minute,
            use24Hour = use24,
            onHour = { hour = it },
            onMinute = { minute = it },
            modifier = Modifier.padding(vertical = 8.dp),
        )

        MetroTextBox(
            value = label,
            onValueChange = { label = it },
            placeholder = stringResource(R.string.alarm_label_placeholder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )

        MetroText(
            text = stringResource(R.string.alarm_repeat),
            style = MetroTextStyle.SectionHeader,
            color = MetroTheme.colors.secondaryText,
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            DAY_LABELS.forEachIndexed { index, dayLabel ->
                val bit = 1 shl index
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MetroCheckBox(
                        checked = mask and bit != 0,
                        onCheckedChange = { checked ->
                            mask = if (checked) mask or bit else mask and bit.inv()
                        },
                    )
                    MetroText(text = dayLabel, style = MetroTextStyle.ListItemSubtitle)
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PresetButton(stringResource(R.string.alarm_repeat_daily), mask == (AlarmSchedule.WEEKDAYS or AlarmSchedule.WEEKENDS)) {
                mask = AlarmSchedule.WEEKDAYS or AlarmSchedule.WEEKENDS
            }
            PresetButton(stringResource(R.string.alarm_repeat_weekdays), mask == AlarmSchedule.WEEKDAYS) {
                mask = AlarmSchedule.WEEKDAYS
            }
            PresetButton(stringResource(R.string.alarm_repeat_weekends), mask == AlarmSchedule.WEEKENDS) {
                mask = AlarmSchedule.WEEKENDS
            }
        }

        MetroToggleSwitch(
            checked = vibrate,
            onCheckedChange = { vibrate = it },
            label = stringResource(R.string.alarm_vibrate),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
        )

        MetroListPicker(
            options = snoozeOptions.map { stringResource(R.string.alarm_snooze_minutes, it) },
            selectedOptionIndex = snoozeOptions.indexOf(snooze).coerceAtLeast(0),
            onSelectOption = { snooze = snoozeOptions[it] },
            label = stringResource(R.string.alarm_snooze),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))
        MetroBorderButton(
            text = stringResource(R.string.alarm_save),
            onClick = {
                state.saveAlarm(
                    id = alarmId,
                    hour = hour,
                    minute = minute,
                    label = label,
                    repeatMask = mask,
                    vibrate = vibrate,
                    snoozeMinutes = snooze,
                    soundUri = existing?.soundUri,
                )
                onBack()
            },
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        if (existing != null) {
            Spacer(modifier = Modifier.height(12.dp))
            MetroBorderButton(
                text = stringResource(R.string.alarm_delete),
                onClick = {
                    state.deleteAlarm(existing.id)
                    onBack()
                },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
private fun PresetButton(text: String, selected: Boolean, onClick: () -> Unit) {
    MetroText(
        text = text,
        style = MetroTextStyle.Body,
        color = if (selected) MetroTheme.colors.accent else MetroTheme.colors.primaryText,
        modifier = Modifier
            .metroClickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
    )
}
