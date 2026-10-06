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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.metro.ui.MetroAppBarDefaults
import com.pranshulgg.weather_master_app.core.model.domain.location.Location
import com.pranshulgg.weather_master_app.core.prefs.AppPrefsState
import com.pranshulgg.weather_master_app.data.store.WeatherStoreState
import com.pranshulgg.weather_master_app.data.store.WeatherUnitsStoreState
import com.pranshulgg.weather_master_app.feature.locations.ui.PlacesPivotContent
import com.pranshulgg.weather_master_app.feature.main.ui.WeatherPalette
import com.pranshulgg.weather_master_app.feature.main.ui.layouts.PhoneLayout

@Composable
fun MainScreenScaffold(
    weatherStore: WeatherStoreState,
    onEditLocation: () -> Unit,
    onLocationSelect: (Location) -> Unit,
    context: Context,
    onWeatherSourceInfoClick: () -> Unit,
    prefs: AppPrefsState,
    units: WeatherUnitsStoreState,
    onOpenSearch: () -> Unit,
) {
    val weather = remember(weatherStore.weather) { weatherStore.weather }
    val alerts = remember(weatherStore.alerts) { weatherStore.alerts }

    val units = units.units

    Box(modifier = Modifier.fillMaxSize().background(WeatherPalette.BingBlue)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
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
                            alerts = alerts,
                            prefs = prefs,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        // No active weather yet (first run / no saved places): surface the
                        // Places pivot so a location can be added.
                        PlacesPivotContent(
                            onLocationSelect = onLocationSelect,
                            onAddPlace = onOpenSearch,
                            onEdit = onEditLocation,
                            onPin = {},
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
