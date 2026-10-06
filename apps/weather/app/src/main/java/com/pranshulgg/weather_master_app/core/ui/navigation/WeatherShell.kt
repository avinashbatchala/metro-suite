package com.pranshulgg.weather_master_app.core.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroAppBarTextButton
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroSubpageHost
import com.metro.ui.MetroTheme
import com.metro.ui.metroNavBarPadding
import com.pranshulgg.weather_master_app.core.ui.snackbar.LocalSnackbarHostState
import com.pranshulgg.weather_master_app.feature.editlocation.EditLocationScreen
import com.pranshulgg.weather_master_app.feature.main.MainScreen
import com.pranshulgg.weather_master_app.feature.main.MainScreenViewModel
import com.pranshulgg.weather_master_app.feature.search.SearchScreen
import com.pranshulgg.weather_master_app.feature.settings.SettingsScreen
import com.pranshulgg.weather_master_app.feature.settings.about.AboutScreen
import com.pranshulgg.weather_master_app.feature.settings.about.license.LicenseScreen
import com.pranshulgg.weather_master_app.feature.settings.about.privacy.PrivacyPolicyScreen
import com.pranshulgg.weather_master_app.feature.settings.about.terms.TermsConditionsScreen
import com.pranshulgg.weather_master_app.feature.settings.background.BackgroundUpdatesScreen
import com.pranshulgg.weather_master_app.feature.settings.units.UnitsScreen
import com.pranshulgg.weather_master_app.feature.shared.WeatherViewModel

/**
 * In-app Metro page stack for the weather client. Mirrors the suite house shell: the home hub
 * plus search / settings / units / about drill-ins, all hosted by [MetroSubpageHost] under a
 * persistent bottom [MetroAppBar] anchored to [Alignment.BottomCenter].
 */
@Composable
fun WeatherShell(modifier: Modifier = Modifier) {
    val snackbarHostState: SnackbarHostState = LocalSnackbarHostState.current
    val weatherViewModel: WeatherViewModel = hiltViewModel()
    val mainViewModel: MainScreenViewModel = hiltViewModel()
    val locationStore by mainViewModel.location.collectAsState()

    var route: WeatherRoute by remember { mutableStateOf<WeatherRoute>(WeatherRoute.Hub) }

    val onRefresh: () -> Unit = {
        weatherViewModel.setActiveLoading()
        weatherViewModel.refreshWeather(locationStore.activeLocation)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background)
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
                        type = MetroSystemIconType.Search,
                        label = "search",
                        contentDescription = "Search locations",
                        onClick = { route = WeatherRoute.Search },
                    ),
                    MetroAppBarIcon(
                        type = MetroSystemIconType.Settings,
                        label = "settings",
                        contentDescription = "Settings",
                        onClick = { route = WeatherRoute.Settings },
                    ),
                    MetroAppBarIcon(
                        type = MetroSystemIconType.Refresh,
                        label = "refresh",
                        contentDescription = "Refresh weather",
                        onClick = onRefresh,
                    ),
                )
            } else {
                emptyList()
            },
            textButtons = if (isHub) {
                emptyList()
            } else {
                listOf(
                    MetroAppBarTextButton(
                        text = "back",
                        onClick = { route = route.parentRoute() },
                    ),
                )
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = MetroAppBarDefaults.BarHeight),
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Routes
// ---------------------------------------------------------------------------------------------

sealed interface WeatherRoute {
    data object Hub : WeatherRoute
    data object Search : WeatherRoute
    data object Settings : WeatherRoute
    data object Units : WeatherRoute
    data object BackgroundUpdates : WeatherRoute
    data object About : WeatherRoute
    data object Terms : WeatherRoute
    data object Privacy : WeatherRoute
    data object License : WeatherRoute
    data object EditLocation : WeatherRoute
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
    WeatherRoute.Settings,
    WeatherRoute.EditLocation,
    -> WeatherRoute.Hub

    WeatherRoute.Hub -> WeatherRoute.Hub
}

private fun WeatherRoute.loadKey(): Any = when (this) {
    WeatherRoute.Hub -> "Hub"
    WeatherRoute.Search -> "Search"
    WeatherRoute.Settings -> "Settings"
    WeatherRoute.Units -> "Units"
    WeatherRoute.BackgroundUpdates -> "BackgroundUpdates"
    WeatherRoute.About -> "About"
    WeatherRoute.Terms -> "Terms"
    WeatherRoute.Privacy -> "Privacy"
    WeatherRoute.License -> "License"
    WeatherRoute.EditLocation -> "EditLocation"
}
