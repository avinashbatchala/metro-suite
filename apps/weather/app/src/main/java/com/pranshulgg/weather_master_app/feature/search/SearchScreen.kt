package com.pranshulgg.weather_master_app.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.pranshulgg.weather_master_app.R
import com.pranshulgg.weather_master_app.core.model.domain.location.Location
import com.pranshulgg.weather_master_app.core.model.domain.weather.ApiKey
import com.pranshulgg.weather_master_app.core.model.sources.SearchSource
import com.pranshulgg.weather_master_app.core.model.sources.Source
import com.pranshulgg.weather_master_app.core.prefs.LocalAppPrefs
import com.pranshulgg.weather_master_app.core.ui.components.LargeTopBarScaffold
import com.pranshulgg.weather_master_app.core.ui.components.NavigateUpBtn
import com.pranshulgg.weather_master_app.core.ui.components.Symbol
import com.metro.ui.MetroDimens
import com.metro.ui.MetroEmptyState
import com.metro.ui.MetroListItem
import com.metro.ui.MetroTextBox
import com.metro.ui.MetroTheme
import com.pranshulgg.weather_master_app.core.ui.navigation.NavRoutes
import com.pranshulgg.weather_master_app.core.utils.formatters.toTitleCase
import com.pranshulgg.weather_master_app.feature.search.ui.SearchScreenBottomSheets
import com.pranshulgg.weather_master_app.feature.shared.ui.SharedBottomSheet
import kotlinx.coroutines.delay

data class SearchUiState(
    val query: String = "",
    val source: SearchSource = SearchSource.OPEN_METEO,
    val isSearchSourcePickerSheetOpen: Boolean = false,
    val isWeatherSourcesForLocationSheetOpen: Boolean = false,
    val apiKeys: List<ApiKey> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavController) {

    val viewModel: SearchScreenViewModel = hiltViewModel()
    val results = viewModel.results
    val loading = viewModel.loading
    val uiState by viewModel.uiState
    val prefs = LocalAppPrefs.current
    var selectedLocation by remember { mutableStateOf<Location?>(null) }
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Expanded, SheetValue.Hidden)
    )

    LaunchedEffect(Unit) {
        viewModel.updateSource(prefs.searchSource, prefs)
    }

    LaunchedEffect(uiState.query) {
        if (uiState.query.isNotBlank()) {
            delay(300)
            viewModel.search(uiState.query)
        }
    }

    LargeTopBarScaffold(
        title = stringResource(R.string.search),
        navigationIcon = { NavigateUpBtn(navController) },
        actions = {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clickable { viewModel.showSearchSourcePickerSheet() },
                contentAlignment = Alignment.Center
            ) {
                Symbol(
                    R.drawable.settings_24px,
                    color = MetroTheme.colors.primaryText,
                    size = 22.dp
                )
            }
        }
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MetroTheme.colors.background)
                .padding(horizontal = MetroDimens.ScreenHorizontalMargin)
                .verticalScroll(rememberScrollState())
        ) {
            MetroTextBox(
                value = uiState.query,
                onValueChange = viewModel::updateQuery,
                placeholder = "city name"
            )
            Spacer(modifier = Modifier.height(8.dp))

            when {
                loading -> MetroEmptyState(message = "searching…")
                uiState.query.isBlank() -> MetroEmptyState(message = "Type a city name to add a place.")
                results.isEmpty() -> MetroEmptyState(message = "No places found.")
                else -> {
                    results.forEach { loc ->
                        val subtitle = buildString {
                            if (loc.state.isNotBlank()) append("${loc.state.toTitleCase()}, ")
                            append(loc.country.toTitleCase())
                        }
                        MetroListItem(
                            title = loc.name,
                            subtitle = subtitle,
                            leading = {
                                Symbol(
                                    R.drawable.location_on_24px,
                                    color = MetroTheme.colors.secondaryText,
                                )
                            },
                            onClick = {
                                selectedLocation = loc
                                viewModel.showWeatherSourcesForLocationSheet()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    SearchScreenBottomSheets.SearchSourcePickerSheet(prefs, viewModel, uiState, sheetState)

    SharedBottomSheet.WeatherSourcesForLocationSheet(
        countryCode = selectedLocation?.countryCode,
        show = uiState.isWeatherSourcesForLocationSheetOpen,
        selectedSource = selectedLocation?.source ?: Source.OPEN_METEO,
        onSave = {
            viewModel.hideWeatherSourcesForLocationSheet()
            viewModel.saveLocation(
                selectedLocation,
                onBack = { navController.popBackStack() },
                onReset = { },
                it
            )
        },
        onDismiss = viewModel::hideWeatherSourcesForLocationSheet,
        sheetState = sheetState,
        onClickApiConfig = {
            navController.navigate(NavRoutes.API_KEYS_CONFIG)
            viewModel.hideWeatherSourcesForLocationSheet()
        },
        apiKeys = uiState.apiKeys
    )
}
