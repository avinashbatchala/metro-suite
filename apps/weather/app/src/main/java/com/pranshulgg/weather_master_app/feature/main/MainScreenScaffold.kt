package com.pranshulgg.weather_master_app.feature.main

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroSystemIconType
import com.pranshulgg.weather_master_app.core.model.domain.location.Location
import com.pranshulgg.weather_master_app.core.prefs.AppPrefsState
import com.pranshulgg.weather_master_app.core.ui.navigation.NavRoutes
import com.pranshulgg.weather_master_app.data.store.WeatherStoreState
import com.pranshulgg.weather_master_app.data.store.WeatherUnitsStoreState
import com.pranshulgg.weather_master_app.feature.locations.ui.PlacesPivotContent
import com.pranshulgg.weather_master_app.feature.main.ui.layouts.PhoneLayout

@Composable
fun MainScreenScaffold(
    navController: NavController,
    weatherStore: WeatherStoreState,
    onRefresh: () -> Unit,
    onEditLocation: () -> Unit,
    onLocationSelect: (Location) -> Unit,
    context: Context,
    onWeatherSourceInfoClick: () -> Unit,
    prefs: AppPrefsState,
    units: WeatherUnitsStoreState,
) {
    val weather = remember(weatherStore.weather) { weatherStore.weather }
    val alerts = remember(weatherStore.alerts) { weatherStore.alerts }

    val units = units.units

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = MetroAppBarDefaults.BarHeight)
        ) {
            AnimatedContent(
                modifier = Modifier.fillMaxSize(),
                targetState = weather,
                contentKey = { it?.location?.id },
                transitionSpec = { fadeIn() togetherWith fadeOut() }
            ) { weather ->
                Column(modifier = Modifier.fillMaxSize()) {
                    if (weather != null) {
                        PhoneLayout(
                            weather = weather,
                            units = units,
                            context = context,
                            navController = navController,
                            alerts = alerts,
                            prefs = prefs,
                            onLocationSelect = onLocationSelect,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        // No active weather yet (first run / no saved places): surface the
                        // Places pivot so a location can be added.
                        PlacesPivotContent(
                            onLocationSelect = onLocationSelect,
                            onAddPlace = { navController.navigate(NavRoutes.SEARCH) },
                            onEdit = onEditLocation,
                            onPin = {},
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        MetroAppBar(
            icons = listOf(
                MetroAppBarIcon(
                    type = MetroSystemIconType.Search,
                    label = "search",
                    contentDescription = "Search locations",
                    onClick = { navController.navigate(NavRoutes.SEARCH) },
                ),
                MetroAppBarIcon(
                    type = MetroSystemIconType.Settings,
                    label = "settings",
                    contentDescription = "Settings",
                    onClick = { navController.navigate(NavRoutes.SETTINGS) },
                ),
                MetroAppBarIcon(
                    type = MetroSystemIconType.Refresh,
                    label = "refresh",
                    contentDescription = "Refresh weather",
                    onClick = onRefresh,
                ),
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
