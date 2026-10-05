package com.pranshulgg.weather_master_app.feature.locations.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pranshulgg.weather_master_app.R
import com.pranshulgg.weather_master_app.core.model.domain.alerts.Alert
import com.pranshulgg.weather_master_app.core.model.domain.location.Location
import com.pranshulgg.weather_master_app.core.model.domain.weather.Weather
import com.pranshulgg.weather_master_app.core.model.weather.WeatherCondition
import com.pranshulgg.weather_master_app.core.model.weather.toIcon
import com.pranshulgg.weather_master_app.core.ui.components.SettingsTileIcon
import com.metro.ui.MetroDimens
import com.metro.ui.MetroListItem
import com.metro.ui.MetroLoadingDots
import com.pranshulgg.weather_master_app.core.utils.formatters.getCurrentTimeFor
import com.pranshulgg.weather_master_app.core.utils.formatters.getLastUpdatedTimeString
import com.pranshulgg.weather_master_app.feature.shared.components.LocationItem

@Composable
fun LocationsScreenContent(
    locations: List<Location>,
    onLongClick: (Location) -> Unit,
    onLocationSelect: (Location) -> Unit,
    activeLocation: Location? = null,
    weatherForTotalLocations: List<Weather> = emptyList(),
    onAddCurrentLocation: () -> Unit,
    isDeviceLocationLoading: Boolean,
    alertsForTotalLocations: List<Alert?>
) {
    val weatherMap = weatherForTotalLocations.associateBy { it.location.id }
    val alertMap = alertsForTotalLocations.groupBy { it?.locationId }
    val context = LocalContext.current

    AnimatedContent(
        targetState = locations,
        transitionSpec = { fadeIn() togetherWith fadeOut() }
    ) { locations ->
        LazyColumn(
            modifier = Modifier.padding(horizontal = MetroDimens.ScreenHorizontalMargin),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            val showDeviceLocationCard = locations.none { it.isDeviceLocation }

            if (showDeviceLocationCard) {
                item {
                    UseDeviceLocationRow(
                        onClick = { if (!isDeviceLocationLoading) onAddCurrentLocation() },
                        isLoading = isDeviceLocationLoading
                    )
                    Spacer(modifier = Modifier.padding(top = 8.dp))
                }
            }
            itemsIndexed(locations, key = { _, item -> item.id }) { index, location ->
                val weather = weatherMap[location.id]
                val alert = alertMap[location.id] ?: emptyList()
                val icon = weather?.current?.weatherCondition ?: WeatherCondition.NO_CONDITION_FOUND
                val description = if (weather != null && weather.current.lastUpdatedInMilli != -1L) {
                    stringResource(
                        R.string.time_last_updated,
                        getLastUpdatedTimeString(context, weather.current.lastUpdatedInMilli)
                    )
                } else {
                    stringResource(R.string.weather_no_data)
                }

                LocationItem(
                    title = location.name,
                    description = description,
                    onClick = { onLocationSelect(location) },
                    icon = icon.toIcon(
                        targetTimeMilli = if (weather != null) {
                            getCurrentTimeFor(weather.location.timezone)
                        } else {
                            System.currentTimeMillis()
                        },
                        daily = weather?.daily?.firstOrNull()
                    ),
                    isSelected = location.id == activeLocation?.id,
                    onLongClick = { onLongClick(location) },
                    isDefault = location.isDefault,
                    isDeviceLocation = location.isDeviceLocation,
                    isAlertAvailable = alert.isNotEmpty()
                )

                if (index == locations.size - 1) {
                    Spacer(modifier = Modifier.padding(bottom = 120.dp))
                }
            }
        }
    }
}

@Composable
internal fun UseDeviceLocationRow(onClick: () -> Unit, isLoading: Boolean = false) {
    MetroListItem(
        title = stringResource(R.string.location_use_current),
        subtitle = stringResource(R.string.location_use_current_secondary),
        enabled = !isLoading,
        leading = {
            Box(
                modifier = Modifier.size(44.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    MetroLoadingDots()
                } else {
                    SettingsTileIcon(R.drawable.location_searching_24px)
                }
            }
        },
        onClick = onClick
    )
}
