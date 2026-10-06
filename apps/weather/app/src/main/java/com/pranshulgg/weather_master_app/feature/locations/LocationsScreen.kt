package com.pranshulgg.weather_master_app.feature.locations

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pranshulgg.weather_master_app.R
import com.pranshulgg.weather_master_app.core.model.domain.location.Location
import com.pranshulgg.weather_master_app.core.ui.components.Symbol
import com.pranshulgg.weather_master_app.core.ui.components.WeatherPageHeader
import com.pranshulgg.weather_master_app.core.ui.snackbar.SnackbarManager
import com.pranshulgg.weather_master_app.data.provider.devicelocation.rememberBackgroundLocationPermissionLauncher
import com.pranshulgg.weather_master_app.data.provider.devicelocation.rememberLocationPermissionLauncher
import com.pranshulgg.weather_master_app.feature.locations.ui.LocationScreenConfirmationDialog
import com.pranshulgg.weather_master_app.feature.locations.ui.LocationScreenSheet
import com.pranshulgg.weather_master_app.feature.locations.ui.LocationsScreenContent
import com.pranshulgg.weather_master_app.feature.shared.ui.SharedDialogs
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroDimens
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroClickable

data class LocationsScreenUiState(
    val isConfirmationDialogOpen: Boolean = false,
    val longClickedLocation: Location? = null,
    val isBottomSheetOpen: Boolean = false,
    val isDeviceLocationLoading: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LocationsScreen(
    onBack: () -> Unit,
    onAddPlace: () -> Unit,
    onEditLocation: () -> Unit,
    onLocationSelect: (Location) -> Unit,
    onPin: () -> Unit = {}
) {
    val viewModel: LocationsScreenViewModel = hiltViewModel()
    val locationStore = viewModel.location.collectAsState().value

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

    Column(modifier = Modifier.fillMaxSize()) {
        WeatherPageHeader(title = stringResource(R.string.locations))

        Box(modifier = Modifier.weight(1f)) {
            LocationsScreenContent(
                locationStore.locations,
                onLongClick = { viewModel.showBottomSheet(it) },
                onLocationSelect = { onLocationSelect(it) },
                activeLocation = locationStore.activeLocation,
                weatherForTotalLocations,
                onAddCurrentLocation = { locationPermissionInfoDialogOpen = true },
                uiState.value.isDeviceLocationLoading,
                alertsForTotalLocations
            )
        }

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
                bottom = 16.dp,
            )
        )
    }

    LocationScreenConfirmationDialog(viewModel)
    LocationScreenSheet(viewModel, sheetState, onEdit = onEditLocation)

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

@Composable
private fun LocAction(icon: Int, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .metroClickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Symbol(icon, desc = description, color = MetroTheme.colors.primaryText, size = 22.dp)
    }
}
