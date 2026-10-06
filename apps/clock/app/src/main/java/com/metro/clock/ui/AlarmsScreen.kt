package com.metro.clock.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.clock.R
import com.metro.clock.alarms.AlarmFormat
import com.metro.ui.MetroEmptyState
import com.metro.ui.MetroListItem
import com.metro.ui.MetroToggleSwitch

@Composable
fun AlarmsScreen(
    state: ClockState,
    modifier: Modifier = Modifier,
) {
    @Suppress("UNUSED_VARIABLE")
    val generation = state.generation
    val use24 = rememberSystem24Hour()

    if (state.alarms.isEmpty()) {
        Column(modifier = modifier.fillMaxSize()) {
            MetroEmptyState(message = stringResource(R.string.alarms_empty))
        }
        return
    }

    // WP8.1 orders alarms by time of day; repeating and one-time alarms sit together.
    val ordered = state.alarms.sortedWith(compareBy({ it.hour }, { it.minute }, { it.id }))

    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(ordered, key = { it.id }) { alarm ->
            val repeat = alarmRepeatSummary(alarm.repeatMask)
            val name = alarm.label.ifBlank { stringResource(R.string.alarm) }
            val subtitle = listOf(name, repeat).joinToString(" · ")
            MetroListItem(
                title = AlarmFormat.time(alarm, use24),
                subtitle = subtitle,
                trailing = {
                    MetroToggleSwitch(
                        checked = alarm.enabled,
                        onCheckedChange = { state.setAlarmEnabled(alarm.id, it) },
                        showStatus = false,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                },
                onClick = { state.openAlarmEdit(alarm.id) },
            )
        }
    }
}
