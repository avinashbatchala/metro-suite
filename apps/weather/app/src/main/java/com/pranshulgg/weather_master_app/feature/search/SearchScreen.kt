package com.pranshulgg.weather_master_app.feature.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.pranshulgg.weather_master_app.R
import com.pranshulgg.weather_master_app.core.model.domain.location.Location
import com.pranshulgg.weather_master_app.core.model.domain.weather.ApiKey
import com.pranshulgg.weather_master_app.core.model.sources.SearchSource
import com.pranshulgg.weather_master_app.core.model.sources.Source
import com.pranshulgg.weather_master_app.core.prefs.LocalAppPrefs
import com.pranshulgg.weather_master_app.core.ui.components.Symbol
import com.pranshulgg.weather_master_app.core.ui.components.WeatherPageHeader
import com.metro.ui.MetroDimens
import com.metro.ui.MetroEmptyState
import com.metro.ui.MetroListItem
import com.metro.ui.MetroTextBox
import com.metro.ui.MetroTheme
import com.metro.ui.metroClickable
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
fun SearchScreen(onLocationSaved: () -> Unit) {

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

    Column(modifier = Modifier.fillMaxSize()) {
        WeatherPageHeader(title = stringResource(R.string.search))

        Row(verticalAlignment = Alignment.CenterVertically) {
            MetroTextBox(
                value = uiState.query,
                onValueChange = viewModel::updateQuery,
                placeholder = "city name",
                modifier = Modifier
                    .weight(1f)
                    .padding(start = MetroDimens.ScreenHorizontalMargin)
            )
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .padding(end = MetroDimens.ScreenHorizontalMargin)
                    .metroClickable { viewModel.showSearchSourcePickerSheet() },
                contentAlignment = Alignment.Center
            ) {
                Symbol(
                    R.drawable.settings_24px,
                    color = MetroTheme.colors.primaryText,
                    size = 22.dp
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = MetroDimens.ScreenHorizontalMargin)
                .verticalScroll(rememberScrollState())
        ) {
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
                onBack = onLocationSaved,
                onReset = { },
                it
            )
        },
        onDismiss = viewModel::hideWeatherSourcesForLocationSheet,
        sheetState = sheetState,
        onClickApiConfig = {
            viewModel.hideWeatherSourcesForLocationSheet()
        },
        apiKeys = uiState.apiKeys
    )
}
