package com.metro.clock.ui

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.clock.R
import com.metro.ui.MetroEmptyState
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroClickable
import com.metro.system.MetroTileTemporalRender
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun WorldClockScreen(
    state: ClockState,
    modifier: Modifier = Modifier,
) {
    @Suppress("UNUSED_VARIABLE")
    val generation = state.generation
    val context = LocalContext.current
    val use24 = remember { DateFormat.is24HourFormat(context) }
    val tick = rememberWallClockTick(1000)
    val deviceZone = ZoneId.systemDefault()
    val deviceDate = Instant.ofEpochMilli(tick).atZone(deviceZone).toLocalDate()
    val formatter = if (use24) {
        DateTimeFormatter.ofPattern("HH:mm", Locale.US)
    } else {
        DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    }

    if (state.worldCities.isEmpty()) {
        Column(modifier = modifier.fillMaxSize()) {
            MetroEmptyState(message = stringResource(R.string.world_clock_empty))
        }
        return
    }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(state.worldCities, key = { it.id }) { city ->
            val zone = remember(city.id) {
                runCatching { ZoneId.of(city.zoneId) }.getOrDefault(deviceZone)
            }
            val zoned = Instant.ofEpochMilli(tick).atZone(zone)
            val time = zoned.format(formatter).uppercase(Locale.US)
            val offset = MetroTileTemporalRender.dayOffsetLabel(zoned.toLocalDate(), deviceDate)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .metroClickable { ClockPin.pinWorld(context, city.id) }
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    MetroText(text = city.name, style = MetroTextStyle.ListItemTitle)
                    MetroText(
                        text = city.region,
                        style = MetroTextStyle.ListItemSubtitle,
                        color = MetroTheme.colors.secondaryText,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    MetroText(text = time, style = MetroTextStyle.HubTitle)
                    if (offset != null) {
                        MetroText(
                            text = offset,
                            style = MetroTextStyle.ListItemSubtitle,
                            color = MetroTheme.colors.secondaryText,
                        )
                    }
                }
                MetroText(
                    text = stringResource(R.string.world_clock_remove),
                    style = MetroTextStyle.Body,
                    color = MetroTheme.colors.accent,
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .metroClickable { state.removeCity(city.id) },
                )
            }
        }
    }
}
