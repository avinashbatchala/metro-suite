package com.metro.clock.ui

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.clock.R
import com.metro.clock.alarms.AlarmFormat
import com.metro.clock.alarms.AlarmSchedule
import com.metro.ui.MetroEmptyState
import com.metro.ui.MetroListItem
import com.metro.ui.MetroToggleSwitch
import java.util.Locale

@Composable
fun AlarmsScreen(
    state: ClockState,
    modifier: Modifier = Modifier,
) {
    @Suppress("UNUSED_VARIABLE")
    val generation = state.generation
    val context = LocalContext.current
    val use24 = remember { DateFormat.is24HourFormat(context) }

    if (state.alarms.isEmpty()) {
        Column(modifier = modifier.fillMaxSize()) {
            MetroEmptyState(message = stringResource(R.string.alarms_empty))
        }
        return
    }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(state.alarms, key = { it.id }) { alarm ->
            val repeat = AlarmSchedule.repeatSummary(alarm, Locale.getDefault())
            val subtitle = listOf(alarm.label.takeIf { it.isNotBlank() }, repeat)
                .filterNotNull()
                .joinToString(" · ")
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
