package com.pranshulgg.weather_master_app.core.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroAppBarMenuItem
import com.metro.ui.MetroColors
import com.metro.ui.MetroDimens
import com.metro.ui.MetroSubpageHost
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroNavBarPadding
import com.pranshulgg.weather_master_app.core.ui.snackbar.SnackbarManager
import com.pranshulgg.weather_master_app.feature.editlocation.EditLocationScreen
import com.pranshulgg.weather_master_app.feature.locations.LocationsScreen
import com.pranshulgg.weather_master_app.feature.locations.LocationsScreenViewModel
import com.pranshulgg.weather_master_app.feature.main.MainScreen
import com.pranshulgg.weather_master_app.feature.main.MainScreenViewModel
import com.pranshulgg.weather_master_app.feature.main.ui.WeatherPalette
import com.pranshulgg.weather_master_app.feature.main.ui.layouts.DailyDetailScreen
import com.pranshulgg.weather_master_app.feature.search.SearchScreen
import com.pranshulgg.weather_master_app.feature.settings.SettingsScreen
import com.pranshulgg.weather_master_app.feature.settings.about.AboutScreen
import com.pranshulgg.weather_master_app.feature.settings.about.license.LicenseScreen
import com.pranshulgg.weather_master_app.feature.settings.about.privacy.PrivacyPolicyScreen
import com.pranshulgg.weather_master_app.feature.settings.about.terms.TermsConditionsScreen
import com.pranshulgg.weather_master_app.feature.settings.background.BackgroundUpdatesScreen
import com.pranshulgg.weather_master_app.feature.settings.units.UnitsScreen
import com.pranshulgg.weather_master_app.feature.shared.WeatherViewModel
import com.pranshulgg.weather_master_app.synergy.WeatherTilePin
import kotlinx.coroutines.delay

/**
 * In-app Metro page stack for the Weather client. The Bing Weather home panorama plus
 * search / locations / daily-detail / settings drill-ins, hosted by [MetroSubpageHost] under a
 * persistent bottom [MetroAppBar]. Material snackbars are replaced by a flat Metro status line.
 */
@Composable
fun WeatherShell(modifier: Modifier = Modifier) {
    val weatherViewModel: WeatherViewModel = hiltViewModel()
    val mainViewModel: MainScreenViewModel = hiltViewModel()
    val locationsViewModel: LocationsScreenViewModel = hiltViewModel()
    val locationStore by mainViewModel.location.collectAsState()
    val weatherStore by mainViewModel.weather.collectAsState()
    val unitsStore by mainViewModel.units.collectAsState()
    val context = LocalContext.current

    var route: WeatherRoute by remember { mutableStateOf<WeatherRoute>(WeatherRoute.Hub) }

    val onRefresh: () -> Unit = {
        weatherViewModel.setActiveLoading()
        weatherViewModel.refreshWeather(locationStore.activeLocation)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WeatherPalette.BingBlue)
            .statusBarsPadding()
            .metroNavBarPadding(),
    ) {
        MetroSubpageHost(
            route = route,
            isRoot = { it is WeatherRoute.Hub },
            parentOf = { it.parentRoute() },
            loadKeyOf = { it.loadKey() },
            onGoBack = { route = route.parentRoute() },
            modifier = Modifier.fillMaxSize(),
            rootContent = {
                MainScreen(
                    weatherViewModel = weatherViewModel,
                    onOpenSearch = { route = WeatherRoute.Search },
                    onEditLocation = { route = WeatherRoute.EditLocation },
                    onRemoveLocation = { locationsViewModel.deleteLocation(it.id) },
                    onSetHome = { locationsViewModel.updateDefaultLocation(it.id) },
                    onPinLocation = { WeatherTilePin.pin(context, it, size = "4x2") },
                    onOpenDailyDetail = { index -> route = WeatherRoute.DailyDetail(index) },
                )
            },
            subpageContent = { current ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = MetroAppBarDefaults.BarHeight),
                ) {
                    when (current) {
                        WeatherRoute.Hub -> Unit
                        WeatherRoute.Search -> SearchScreen(
                            onLocationSaved = { route = WeatherRoute.Hub },
                        )
                        WeatherRoute.Locations -> LocationsScreen(
                            onBack = { route = WeatherRoute.Hub },
                            onAddPlace = { route = WeatherRoute.Search },
                            onEditLocation = { route = WeatherRoute.EditLocation },
                            onLocationSelect = { location ->
                                if (locationStore.activeLocation?.id != location.id) {
                                    weatherViewModel.setActiveLoading()
                                    weatherViewModel.setActiveLocation(location, skipLoading = true)
                                }
                                route = WeatherRoute.Hub
                            },
                            onPin = {
                                locationStore.activeLocation?.let { location ->
                                    WeatherTilePin.pin(context = context, location = location, size = "4x2")
                                }
                            },
                        )
                        is WeatherRoute.DailyDetail -> {
                            val weather = weatherStore.weather
                            if (weather != null) {
                                DailyDetailScreen(
                                    weather = weather,
                                    units = unitsStore.units,
                                    is24 = android.text.format.DateFormat.is24HourFormat(context),
                                    initialIndex = current.index,
                                )
                            }
                        }
                        WeatherRoute.Settings -> SettingsScreen(
                            onOpenUnits = { route = WeatherRoute.Units },
                            onOpenBackgroundUpdates = { route = WeatherRoute.BackgroundUpdates },
                            onOpenAbout = { route = WeatherRoute.About },
                        )
                        WeatherRoute.Units -> UnitsScreen()
                        WeatherRoute.BackgroundUpdates -> BackgroundUpdatesScreen()
                        WeatherRoute.About -> AboutScreen(
                            onOpenTerms = { route = WeatherRoute.Terms },
                            onOpenPrivacy = { route = WeatherRoute.Privacy },
                            onOpenLicense = { route = WeatherRoute.License },
                        )
                        WeatherRoute.Terms -> TermsConditionsScreen()
                        WeatherRoute.Privacy -> PrivacyPolicyScreen()
                        WeatherRoute.License -> LicenseScreen()
                        WeatherRoute.EditLocation -> EditLocationScreen(
                            onBack = { route = WeatherRoute.Hub },
                        )
                    }
                }
            },
        )

        val isHub = route is WeatherRoute.Hub
        MetroAppBar(
            icons = if (isHub) {
                listOf(
                    MetroAppBarIcon(
                        type = MetroSystemIconType.Pin,
                        label = "pin to start",
                        contentDescription = "Pin to start",
                        onClick = {
                            locationStore.activeLocation?.let { location ->
                                WeatherTilePin.pin(context, location, size = "4x2")
                            }
                        },
                    ),
                    MetroAppBarIcon(
                        type = MetroSystemIconType.Search,
                        label = "search",
                        contentDescription = "Search locations",
                        onClick = { route = WeatherRoute.Search },
                    ),
                    MetroAppBarIcon(
                        type = MetroSystemIconType.Target,
                        label = "current location",
                        contentDescription = "Current location",
                        onClick = { weatherViewModel.saveDeviceLocation() },
                    ),
                )
            } else {
                emptyList()
            },
            menuItems = if (isHub) {
                listOf(
                    MetroAppBarMenuItem(text = "refresh", onClick = onRefresh),
                    MetroAppBarMenuItem(text = "settings", onClick = { route = WeatherRoute.Settings }),
                )
            } else {
                emptyList()
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        MetroStatusBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = MetroAppBarDefaults.BarHeight),
        )
    }
}

/** Flat Metro status line replacing Material snackbars. */
@Composable
private fun MetroStatusBar(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var status by remember { mutableStateOf<Pair<Int, String>?>(null) }
    LaunchedEffect(Unit) {
        var counter = 0
        SnackbarManager.events.collect { event ->
            status = (counter++) to context.getString(event.messageResource)
        }
    }
    status?.let { (id, text) ->
        LaunchedEffect(id) {
            delay(4000)
            status = null
        }
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(MetroColors.DarkSecondarySurface)
                .padding(horizontal = MetroDimens.ScreenHorizontalMargin, vertical = 10.dp),
        ) {
            MetroText(
                text = text,
                style = MetroTextStyle.Body,
                color = MetroTheme.colors.primaryText,
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Routes
// ---------------------------------------------------------------------------------------------

sealed interface WeatherRoute {
    data object Hub : WeatherRoute
    data object Search : WeatherRoute
    data object Locations : WeatherRoute
    data object Settings : WeatherRoute
    data object Units : WeatherRoute
    data object BackgroundUpdates : WeatherRoute
    data object About : WeatherRoute
    data object Terms : WeatherRoute
    data object Privacy : WeatherRoute
    data object License : WeatherRoute
    data object EditLocation : WeatherRoute
    data class DailyDetail(val index: Int) : WeatherRoute
}

private fun WeatherRoute.parentRoute(): WeatherRoute = when (this) {
    WeatherRoute.Units,
    WeatherRoute.BackgroundUpdates,
    WeatherRoute.About,
    -> WeatherRoute.Settings

    WeatherRoute.Terms,
    WeatherRoute.Privacy,
    WeatherRoute.License,
    -> WeatherRoute.About

    WeatherRoute.Search,
    WeatherRoute.Locations,
    WeatherRoute.Settings,
    WeatherRoute.EditLocation,
    is WeatherRoute.DailyDetail,
    -> WeatherRoute.Hub

    WeatherRoute.Hub -> WeatherRoute.Hub
}

private fun WeatherRoute.loadKey(): Any = when (this) {
    WeatherRoute.Hub -> "Hub"
    WeatherRoute.Search -> "Search"
    WeatherRoute.Locations -> "Locations"
    WeatherRoute.Settings -> "Settings"
    WeatherRoute.Units -> "Units"
    WeatherRoute.BackgroundUpdates -> "BackgroundUpdates"
    WeatherRoute.About -> "About"
    WeatherRoute.Terms -> "Terms"
    WeatherRoute.Privacy -> "Privacy"
    WeatherRoute.License -> "License"
    WeatherRoute.EditLocation -> "EditLocation"
    is WeatherRoute.DailyDetail -> "DailyDetail:${index}"
}
