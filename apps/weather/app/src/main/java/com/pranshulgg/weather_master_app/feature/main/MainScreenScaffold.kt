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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroSystemIconType
import com.pranshulgg.weather_master_app.core.model.domain.location.Location
import com.pranshulgg.weather_master_app.core.model.domain.weather.WeatherBlock
import com.pranshulgg.weather_master_app.core.prefs.AppPrefsState
import com.pranshulgg.weather_master_app.core.ui.navigation.NavRoutes
import com.pranshulgg.weather_master_app.data.store.WeatherBlocksStoreState
import com.pranshulgg.weather_master_app.data.store.WeatherStoreState
import com.pranshulgg.weather_master_app.data.store.WeatherUnitsStoreState
import com.pranshulgg.weather_master_app.feature.main.ui.layouts.PhoneLayout
import com.pranshulgg.weather_master_app.feature.main.ui.layouts.TabletLayout

@Composable
fun MainScreenScaffold(
    navController: NavController,
    weatherStore: WeatherStoreState,
    onRefresh: () -> Unit,
    onEditLocation: () -> Unit,
    onLocationSelect: (Location) -> Unit,
    context: Context,
    onWeatherSourceInfoClick: () -> Unit,
    isTabletLike: Boolean = false,
    prefs: AppPrefsState,
    units: WeatherUnitsStoreState,
    isLoading: Boolean,
    activeLocation: Location?,
    weatherBlocks: WeatherBlocksStoreState,
    onUpdateBlocks: (List<WeatherBlock>) -> Unit,
) {
    val weather = remember(weatherStore.weather) { weatherStore.weather }
    val airQuality = remember(weatherStore.airQuality) { weatherStore.airQuality }
    val alerts = remember(weatherStore.alerts) { weatherStore.alerts }

    val layoutDirection = LocalLayoutDirection.current
    val units = units.units
    val isFroggyLayout = false
    val isShowSummary = prefs.isShowSummary

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
                        if (!isTabletLike) {
                            PhoneLayout(
                                weather,
                                units,
                                context,
                                isFroggyLayout,
                                navController,
                                alerts,
                                prefs,
                                onWeatherSourceInfoClick,
                                isShowSummary,
                                airQuality,
                                weatherBlocks,
                                onUpdateBlocks,
                                onLocationSelect,
                                onScroll = {},
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Box(modifier = Modifier.weight(1f)) {
                                TabletLayout(
                                    weather,
                                    units,
                                    context,
                                    isFroggyLayout,
                                    navController,
                                    alerts,
                                    prefs,
                                    onWeatherSourceInfoClick,
                                    isShowSummary,
                                    airQuality,
                                    androidx.compose.foundation.layout.PaddingValues(0.dp),
                                    layoutDirection,
                                    weatherBlocks,
                                    onUpdateBlocks
                                )
                            }
                        }
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
