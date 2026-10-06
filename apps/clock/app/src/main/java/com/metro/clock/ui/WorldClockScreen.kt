package com.metro.clock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.clock.R
import com.metro.system.MetroTileTemporalRender
import com.metro.system.MetroWorldClockCity
import com.metro.ui.MetroColors
import com.metro.ui.MetroDimens
import com.metro.ui.MetroEmptyState
import com.metro.ui.MetroListItem
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroClickable
import com.metro.ui.metroNavBarPadding
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
    val use24 = rememberSystem24Hour()
    val tick = rememberMinuteTick()
    val deviceZone = ZoneId.systemDefault()
    val deviceDate = Instant.ofEpochMilli(tick).atZone(deviceZone).toLocalDate()
    val formatter = remember(use24) {
        DateTimeFormatter.ofPattern(if (use24) "HH:mm" else "h:mm a", Locale.getDefault())
    }

    var menuCity by remember { mutableStateOf<MetroWorldClockCity?>(null) }

    if (state.worldCities.isEmpty()) {
        Column(modifier = modifier.fillMaxSize()) {
            MetroEmptyState(message = stringResource(R.string.world_clock_empty))
        }
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(state.worldCities, key = { it.id }) { city ->
                val zone = remember(city.id) {
                    runCatching { ZoneId.of(city.zoneId) }.getOrDefault(deviceZone)
                }
                val zoned = Instant.ofEpochMilli(tick).atZone(zone)
                val time = zoned.format(formatter)
                val offset = MetroTileTemporalRender.dayOffsetLabel(zoned.toLocalDate(), deviceDate)
                MetroListItem(
                    title = city.name,
                    subtitle = city.region,
                    trailing = {
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
                    },
                    onLongClick = { menuCity = city },
                    onClick = { },
                )
            }
        }

        CityContextMenu(
            city = menuCity,
            onDismiss = { menuCity = null },
            onPin = {
                menuCity?.let { ClockPin.pinWorld(context, it.id) }
                menuCity = null
            },
            onMoveUp = {
                menuCity?.let { state.moveCity(it.id, -1) }
                menuCity = null
            },
            onMoveDown = {
                menuCity?.let { state.moveCity(it.id, 1) }
                menuCity = null
            },
            onRemove = {
                menuCity?.let { state.removeCity(it.id) }
                menuCity = null
            },
        )
    }
}

/** WP-style tap-and-hold city actions. */
@Composable
private fun CityContextMenu(
    city: MetroWorldClockCity?,
    onDismiss: () -> Unit,
    onPin: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    if (city == null) return
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
                .padding(vertical = 8.dp),
        ) {
            MenuRow(stringResource(R.string.pin_to_start), onPin)
            MenuRow(stringResource(R.string.city_move_up), onMoveUp)
            MenuRow(stringResource(R.string.city_move_down), onMoveDown)
            MenuRow(stringResource(R.string.world_clock_remove), onRemove)
        }
    }
}

@Composable
private fun MenuRow(label: String, onClick: () -> Unit) {
    MetroText(
        text = label,
        style = MetroTextStyle.ListItemTitle,
        color = MetroTheme.colors.primaryText,
        modifier = Modifier
            .fillMaxWidth()
            .metroClickable(onClick = onClick)
            .padding(start = MetroDimens.ScreenHorizontalMargin)
            .padding(vertical = 14.dp),
    )
}
