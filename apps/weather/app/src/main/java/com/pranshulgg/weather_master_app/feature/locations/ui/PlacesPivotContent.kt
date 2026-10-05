package com.pranshulgg.weather_master_app.feature.locations.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroDimens
import com.metro.ui.MetroListItem
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.pranshulgg.weather_master_app.R
import com.pranshulgg.weather_master_app.core.model.domain.location.Location
import com.pranshulgg.weather_master_app.core.model.weather.WeatherCondition
import com.pranshulgg.weather_master_app.core.model.weather.toIcon
import com.pranshulgg.weather_master_app.core.ui.components.SettingsTileIcon
import com.pranshulgg.weather_master_app.core.ui.snackbar.SnackbarManager
import com.pranshulgg.weather_master_app.core.utils.formatters.getCurrentTimeFor
import com.pranshulgg.weather_master_app.core.utils.formatters.getLastUpdatedTimeString
import com.pranshulgg.weather_master_app.data.provider.devicelocation.rememberBackgroundLocationPermissionLauncher
import com.pranshulgg.weather_master_app.data.provider.devicelocation.rememberLocationPermissionLauncher
import com.pranshulgg.weather_master_app.feature.locations.LocationsScreenViewModel
import com.pranshulgg.weather_master_app.feature.shared.components.LocationItem
import com.pranshulgg.weather_master_app.feature.shared.ui.SharedDialogs

/**
 * Windows 10 "places" pivot: the full location manager (no drawer). A single
 * LazyColumn lists the current-location action, all saved locations (tap to switch,
 * long-press for default/edit/delete) and the "pin to start" action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacesPivotContent(
    onLocationSelect: (Location) -> Unit,
    onAddPlace: () -> Unit,
    onEdit: () -> Unit,
    onPin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: LocationsScreenViewModel = hiltViewModel()
    val locationStore = viewModel.location.collectAsState().value
    val context = LocalContext.current

    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Expanded, SheetValue.Hidden)
    )
    val uiState = viewModel.uiState

    val weatherForTotalLocations by viewModel.weatherForTotalLocations.collectAsStateWithLifecycle(
        initialValue = emptyList()
    )
    val alertsForTotalLocations by viewModel.alertsForTotalLocations.collectAsStateWithLifecycle(
        initialValue = emptyList()
    )

    val weatherMap = weatherForTotalLocations.associateBy { it.location.id }
    val alertMap = alertsForTotalLocations.groupBy { it?.locationId }

    var backgroundLocationPermissionInfoDialogOpen by remember { mutableStateOf(false) }
    var locationPermissionInfoDialogOpen by remember { mutableStateOf(false) }

    val requestLocation = rememberLocationPermissionLauncher(
        onForegroundGranted = { viewModel.saveDeviceLocation() },
        onDenied = { SnackbarManager.show(R.string.location_permission_required) }
    )
    val requestBackgroundLocation = rememberBackgroundLocationPermissionLauncher(
        onGranted = { viewModel.saveDeviceLocation() },
        onContinueWithoutBackground = { viewModel.saveDeviceLocation() },
        onDenied = {
            SnackbarManager.show(R.string.location_permission_required)
            backgroundLocationPermissionInfoDialogOpen = true
        }
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        item {
            MetroListItem(
                title = "add a place",
                leading = { SettingsTileIcon(R.drawable.search_24px) },
                onClick = onAddPlace
            )
        }

        val showDeviceLocationCard = locationStore.locations.none { it.isDeviceLocation }
        if (showDeviceLocationCard) {
            item {
                UseDeviceLocationRow(
                    onClick = {
                        if (!uiState.value.isDeviceLocationLoading) {
                            locationPermissionInfoDialogOpen = true
                        }
                    },
                    isLoading = uiState.value.isDeviceLocationLoading
                )
            }
        }

        item {
            MetroText(
                text = "saved places",
                style = MetroTextStyle.SectionHeader,
                color = MetroTheme.colors.accent,
                modifier = Modifier.padding(
                    start = MetroDimens.ScreenHorizontalMargin,
                    top = 8.dp,
                    bottom = 4.dp,
                )
            )
        }

        itemsIndexed(locationStore.locations, key = { _, item -> item.id }) { _, location ->
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
                isSelected = location.id == locationStore.activeLocation?.id,
                onLongClick = { viewModel.showBottomSheet(location) },
                isDefault = location.isDefault,
                isDeviceLocation = location.isDeviceLocation,
                isAlertAvailable = alert.isNotEmpty()
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            MetroBorderButton(
                text = "pin to start",
                onClick = onPin,
                modifier = Modifier.padding(start = MetroDimens.ScreenHorizontalMargin)
            )
            MetroText(
                text = "Pins the weather tile to the MetroSuite Start screen.",
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.padding(
                    start = MetroDimens.ScreenHorizontalMargin,
                    top = 8.dp,
                    bottom = 24.dp,
                )
            )
        }
    }

    LocationScreenConfirmationDialog(viewModel)
    LocationScreenSheet(viewModel, sheetState, onEdit = onEdit)

    SharedDialogs.DeviceBackgroundLocationPermissionInfoDialog(
        show = backgroundLocationPermissionInfoDialogOpen,
        onConfirm = {
            backgroundLocationPermissionInfoDialogOpen = false
            requestBackgroundLocation()
        },
        onDismiss = { backgroundLocationPermissionInfoDialogOpen = false }
    )
    SharedDialogs.DeviceLocationPermissionInfoDialog(
        show = locationPermissionInfoDialogOpen,
        onConfirm = { requestLocation() },
        onDismiss = { locationPermissionInfoDialogOpen = false }
    )
}
