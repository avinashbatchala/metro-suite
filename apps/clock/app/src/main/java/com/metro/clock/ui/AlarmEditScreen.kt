package com.metro.clock.ui

import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.DisposableEffect
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
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroCheckBox
import com.metro.ui.MetroColors
import com.metro.ui.MetroDimens
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroTextBox
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroClickable
import com.metro.ui.metroNavBarPadding
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

private enum class AlarmPicker { None, Time, Repeat, Sound, Snooze }

private val SnoozeOptions = listOf(5, 10, 15, 20, 30)

private data class SoundOption(val title: String, val uri: String?)

/**
 * Authentic WP8.1 alarm editor: a field form (`Time`, `Repeats`, `Sound`, `Name`, `Snooze time`)
 * with the ApplicationBar for save/delete — not a permanently-visible time wheel.
 */
@Composable
fun AlarmEditScreen(
    state: ClockState,
    alarmId: Long?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val use24 = rememberSystem24Hour()
    val existing = remember(alarmId, state.generation) { state.alarm(alarmId) }

    var hour by remember { mutableIntStateOf(existing?.hour ?: 7) }
    var minute by remember { mutableIntStateOf(existing?.minute ?: 0) }
    var label by remember { mutableStateOf(existing?.label.orEmpty()) }
    var mask by remember { mutableIntStateOf(existing?.repeatMask ?: 0) }
    var snooze by remember { mutableIntStateOf(existing?.snoozeMinutes ?: 10) }
    var soundUri by remember { mutableStateOf(existing?.soundUri) }
    var picker by remember { mutableStateOf(AlarmPicker.None) }

    val soundOptions = remember { buildSoundOptions(context) }
    val soundTitle = soundOptions.firstOrNull { it.uri == soundUri }?.title
        ?: stringResource(R.string.alarm_sound_default)

    var previewRingtone by remember { mutableStateOf<Ringtone?>(null) }
    fun stopPreview() {
        runCatching { previewRingtone?.stop() }
        previewRingtone = null
    }
    fun preview(uriString: String?) {
        stopPreview()
        val uri = uriString?.let(Uri::parse) ?: return
        previewRingtone = runCatching { RingtoneManager.getRingtone(context, uri)?.apply { play() } }.getOrNull()
    }
    DisposableEffect(Unit) { onDispose { stopPreview() } }
    DisposableEffect(picker) { onDispose { stopPreview() } }

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
            MetroText(
                text = stringResource(R.string.pivot_alarms).uppercase(),
                style = MetroTextStyle.AppTitle,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.padding(
                    start = MetroDimens.ScreenHorizontalMargin,
                    top = 8.dp,
                ),
            )
            MetroText(
                text = stringResource(
                    if (existing == null) R.string.alarm_title_new else R.string.alarm_title_edit,
                ),
                style = MetroTextStyle.PageTitle,
                modifier = Modifier.padding(
                    start = MetroDimens.ScreenHorizontalMargin,
                    bottom = 8.dp,
                ),
            )

            FieldRow(
                label = stringResource(R.string.alarm_time),
                value = com.metro.clock.alarms.AlarmFormat.time(hour, minute, use24),
                onClick = { picker = AlarmPicker.Time },
            )
            FieldRow(
                label = stringResource(R.string.alarm_repeats),
                value = alarmRepeatSummary(mask),
                onClick = { picker = AlarmPicker.Repeat },
            )
            FieldRow(
                label = stringResource(R.string.alarm_sound),
                value = soundTitle,
                onClick = { picker = AlarmPicker.Sound },
            )

            FieldLabel(stringResource(R.string.alarm_name))
            MetroTextBox(
                value = label,
                onValueChange = { label = it },
                placeholder = stringResource(R.string.alarm_label_placeholder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MetroDimens.ScreenHorizontalMargin, vertical = 4.dp),
            )

            FieldRow(
                label = stringResource(R.string.alarm_snooze_time),
                value = stringResource(R.string.alarm_snooze_minutes, snooze),
                onClick = { picker = AlarmPicker.Snooze },
            )

            Spacer(modifier = Modifier.height(120.dp))
        }

        MetroAppBar(
            icons = buildList {
                add(
                    MetroAppBarIcon(
                        type = MetroSystemIconType.Check,
                        label = stringResource(R.string.alarm_save),
                        onClick = {
                            state.saveAlarm(
                                id = alarmId,
                                hour = hour,
                                minute = minute,
                                label = label,
                                repeatMask = mask,
                                vibrate = existing?.vibrate ?: true,
                                snoozeMinutes = snooze,
                                soundUri = soundUri,
                            )
                            onBack()
                        },
                    ),
                )
                if (existing != null) {
                    add(
                        MetroAppBarIcon(
                            type = MetroSystemIconType.Delete,
                            label = stringResource(R.string.alarm_delete),
                            onClick = {
                                state.deleteAlarm(existing.id)
                                onBack()
                            },
                        ),
                    )
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        PickerOverlay(picker = picker, onDismiss = { picker = AlarmPicker.None }) {
            when (picker) {
                AlarmPicker.Time -> MetroTimePicker(
                    hour = hour,
                    minute = minute,
                    use24Hour = use24,
                    onHour = { hour = it },
                    onMinute = { minute = it },
                    modifier = Modifier.padding(horizontal = MetroDimens.ScreenHorizontalMargin),
                )
                AlarmPicker.Repeat -> RepeatPicker(mask = mask, onMaskChange = { mask = it })
                AlarmPicker.Sound -> SoundPicker(
                    options = soundOptions,
                    selectedUri = soundUri,
                    onSelect = { option ->
                        soundUri = option.uri
                        preview(option.uri)
                    },
                )
                AlarmPicker.Snooze -> SnoozePicker(
                    selected = snooze,
                    onSelect = { snooze = it },
                )
                AlarmPicker.None -> Unit
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
            top = 16.dp,
        ),
    )
}

@Composable
private fun FieldRow(label: String, value: String, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        FieldLabel(label)
        MetroText(
            text = value,
            style = MetroTextStyle.ListItemTitle,
            color = MetroTheme.colors.primaryText,
            modifier = Modifier
                .fillMaxWidth()
                .metroClickable(onClick = onClick)
                .padding(
                    start = MetroDimens.ScreenHorizontalMargin,
                    end = MetroDimens.ScreenHorizontalMargin,
                    top = 2.dp,
                    bottom = 6.dp,
                ),
        )
    }
}

/** Flat Metro overlay hosting a picker with a `done` affordance. */
@Composable
private fun PickerOverlay(
    picker: AlarmPicker,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    if (picker == AlarmPicker.None) return
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .metroClickable(onClick = onDismiss),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MetroColors.DarkSecondarySurface)
                .metroNavBarPadding()
                .padding(vertical = 12.dp),
        ) {
            content()
            MetroText(
                text = stringResource(R.string.done),
                style = MetroTextStyle.ListItemTitle,
                color = MetroTheme.colors.accent,
                modifier = Modifier
                    .align(Alignment.End)
                    .metroClickable(onClick = onDismiss)
                    .padding(horizontal = MetroDimens.ScreenHorizontalMargin, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun RepeatPicker(mask: Int, onMaskChange: (Int) -> Unit) {
    val all = AlarmSchedule.WEEKDAYS or AlarmSchedule.WEEKENDS
    val options = listOf(
        stringResource(R.string.alarm_repeat_only_once) to 0,
        stringResource(R.string.alarm_repeat_daily) to all,
        stringResource(R.string.alarm_repeat_weekdays) to AlarmSchedule.WEEKDAYS,
        stringResource(R.string.alarm_repeat_weekends) to AlarmSchedule.WEEKENDS,
    )
    options.forEach { (label, value) ->
        OverlayOption(label = label, selected = mask == value) { onMaskChange(value) }
    }
    // Custom days (locale-aware full names; bit0 = Monday … bit6 = Sunday).
    MetroText(
        text = stringResource(R.string.alarm_repeat_custom),
        style = MetroTextStyle.SectionHeader,
        color = MetroTheme.colors.secondaryText,
        modifier = Modifier.padding(
            start = MetroDimens.ScreenHorizontalMargin,
            top = 12.dp,
            bottom = 4.dp,
        ),
    )
    Column(modifier = Modifier.padding(horizontal = MetroDimens.ScreenHorizontalMargin)) {
        DayOfWeek.values().forEach { day ->
            val bit = 1 shl (day.value - 1)
            val checked = mask and bit != 0
            Row(verticalAlignment = Alignment.CenterVertically) {
                MetroCheckBox(
                    checked = checked,
                    onCheckedChange = { c -> onMaskChange(if (c) mask or bit else mask and bit.inv()) },
                )
                MetroText(
                    text = day.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                    style = MetroTextStyle.ListItemTitle,
                    color = MetroTheme.colors.primaryText,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun SoundPicker(
    options: List<SoundOption>,
    selectedUri: String?,
    onSelect: (SoundOption) -> Unit,
) {
    options.forEach { option ->
        OverlayOption(label = option.title, selected = option.uri == selectedUri) { onSelect(option) }
    }
}

@Composable
private fun SnoozePicker(selected: Int, onSelect: (Int) -> Unit) {
    SnoozeOptions.forEach { minutes ->
        OverlayOption(
            label = stringResource(R.string.alarm_snooze_minutes, minutes),
            selected = selected == minutes,
        ) { onSelect(minutes) }
    }
}

@Composable
private fun OverlayOption(label: String, selected: Boolean, onClick: () -> Unit) {
    MetroText(
        text = label,
        style = MetroTextStyle.ListItemTitle,
        color = if (selected) MetroTheme.colors.accent else MetroTheme.colors.primaryText,
        modifier = Modifier
            .fillMaxWidth()
            .metroClickable(onClick = onClick)
            .padding(start = MetroDimens.ScreenHorizontalMargin)
            .padding(vertical = 12.dp),
    )
}

private fun buildSoundOptions(context: android.content.Context): List<SoundOption> {
    val options = mutableListOf(SoundOption("default", null))
    runCatching {
        val manager = RingtoneManager(context).apply { setType(RingtoneManager.TYPE_ALARM) }
        val count = manager.cursor?.count ?: 0
        for (position in 0 until count) {
            val uri = manager.getRingtoneUri(position)?.toString() ?: continue
            val title = runCatching { manager.getRingtone(position)?.getTitle(context) }.getOrNull()
            options += SoundOption(title ?: "alarm", uri)
        }
    }
    return options.distinctBy { it.uri }
}
