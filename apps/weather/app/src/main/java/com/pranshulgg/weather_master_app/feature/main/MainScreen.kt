package com.pranshulgg.weather_master_app.feature.main

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.pranshulgg.weather_master_app.BuildConfig
import com.pranshulgg.weather_master_app.R
import com.pranshulgg.weather_master_app.core.model.domain.toMessageRes
import com.pranshulgg.weather_master_app.core.model.domain.weather.WeatherBlock
import com.pranshulgg.weather_master_app.core.model.domain.weather.WeatherUnits
import com.pranshulgg.weather_master_app.core.prefs.LocalAppPrefs
import com.pranshulgg.weather_master_app.core.ui.navigation.NavRoutes
import com.pranshulgg.weather_master_app.core.ui.snackbar.SnackbarManager
import com.pranshulgg.weather_master_app.data.provider.devicelocation.rememberLocationPermissionLauncher
import com.pranshulgg.weather_master_app.feature.main.ui.MainScreenBottomSheets
import com.pranshulgg.weather_master_app.feature.main.ui.MainScreenDialogs
import com.pranshulgg.weather_master_app.feature.shared.WeatherViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class MainScreenWeatherUiState(
    val weatherUnits: WeatherUnits = WeatherUnits.getDefault(),
    val blocks: List<WeatherBlock> = WeatherBlock.getDefault(),
)

data class MainScreenUiState(
    val isWeatherSourcesForLocationSheetOpen: Boolean = false,
    val isWeatherSourcesInfoForLocationSheetOpen: Boolean = false,
    val isNewVersionAvailable: Boolean = false,
    val lastestVersionUrl: String = "https://github.com/PranshulGG/WeatherMaster/releases/latest",
    val isUnsupportedSourceDialogOpen: Boolean = false,
    val isGooglePlayStoreRelease: Boolean = BuildConfig.IS_PLAYSTORE_BUILD
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController, weatherViewModel: WeatherViewModel) {
    val viewModel: MainScreenViewModel = hiltViewModel()
    val uiState = viewModel.uiState.value
    val uriHandler = LocalUriHandler.current
    val prefs = LocalAppPrefs.current

    val locationStore = viewModel.location.collectAsState().value
    val weatherStore = viewModel.weather.collectAsState().value
    val unitsStore = viewModel.units.collectAsState().value

    val context = LocalContext.current
    val activeLocation = locationStore.activeLocation

    val scope = rememberCoroutineScope()

    // First run: no saved places yet, so ask for location and add the device location.
    val requestLocation = rememberLocationPermissionLauncher(
        onForegroundGranted = { weatherViewModel.saveDeviceLocation() },
        onDenied = { SnackbarManager.show(R.string.location_permission_required) }
    )

    var autoLocationHandled by remember { mutableStateOf(false) }
    LaunchedEffect(locationStore.locations) {
        // Wait a beat for the store to load before deciding there are truly no places.
        if (!autoLocationHandled && locationStore.locations.isEmpty()) {
            delay(1200)
            if (!autoLocationHandled && locationStore.locations.isEmpty()) {
                autoLocationHandled = true
                requestLocation()
            }
        }
    }

    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Expanded, SheetValue.Hidden)
    )

    LaunchedEffect(weatherViewModel.isUnSupportedSource) {
        if (weatherViewModel.isUnSupportedSource) {
            viewModel.showUnsupportedSelectedSourceDialog()
        }
    }

    LaunchedEffect(Unit) {
        weatherViewModel.errors.collect { exp ->
            SnackbarManager.show(messageResource = exp.toMessageRes())
        }
    }

    LaunchedEffect(uiState.isNewVersionAvailable) {
        if (uiState.isNewVersionAvailable) {
            SnackbarManager.show(
                R.string.message_new_version_available,
                actionLabel = R.string.action_view,
                onAction = {
                    uriHandler.openUri(uiState.lastestVersionUrl)
                },
                duration = SnackbarDuration.Indefinite
            )

            viewModel.dismissNewVersionSnackbar()
        }
    }

    MainScreenScaffold(
        navController = navController,
        weatherStore = weatherStore,
        onRefresh = {
            weatherViewModel.setActiveLoading()
            weatherViewModel.refreshWeather(activeLocation)
        },
        onEditLocation = {
            navController.navigate(NavRoutes.EDIT_LOCATION)
        },
        onLocationSelect = { location ->
            if (activeLocation?.id != location.id) {
                weatherViewModel.setActiveLoading()
                scope.launch {
                    weatherViewModel.setActiveLocation(location, skipLoading = true)
                }
            }
        },
        context = context,
        onWeatherSourceInfoClick = viewModel::showWeatherSourcesInfoForLocationSheet,
        prefs = prefs,
        units = unitsStore
    )

    // WEATHER SOURCES INFO DIALOG
    MainScreenBottomSheets.WeatherSourcesInfoForLocationSheet(viewModel, activeLocation, sheetState)

    // SOURCE NOT AVAILABLE
    MainScreenDialogs.UnsupportedSelectedSourceDialog(
        show = uiState.isUnsupportedSourceDialogOpen,
        onDismiss = viewModel::hideUnsupportedSelectedSourceDialog,
        onConfirm = {
            locationStore.activeLocation?.let {
                navController.navigate(NavRoutes.EDIT_LOCATION)
            }
        }
    )
}
