package com.pranshulgg.weather_master_app.feature.editlocation

import android.annotation.SuppressLint
import com.metro.ui.metroClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FabPosition
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.metro.ui.MetroDimens
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.pranshulgg.weather_master_app.R
import com.pranshulgg.weather_master_app.core.model.domain.location.Location
import com.pranshulgg.weather_master_app.core.model.domain.weather.ApiKey
import com.pranshulgg.weather_master_app.core.model.sources.Source
import com.pranshulgg.weather_master_app.core.model.weather.openmeteo.OpenMeteoModel
import com.pranshulgg.weather_master_app.core.ui.components.Gap
import com.pranshulgg.weather_master_app.core.ui.components.SettingSection
import com.pranshulgg.weather_master_app.core.ui.components.SettingTile
import com.pranshulgg.weather_master_app.core.ui.components.Symbol
import com.pranshulgg.weather_master_app.core.ui.components.WeatherPageHeader
import com.pranshulgg.weather_master_app.core.ui.snackbar.SnackbarManager
import com.pranshulgg.weather_master_app.feature.editlocation.ui.EditLocationBottomSheet
import com.pranshulgg.weather_master_app.feature.editlocation.ui.EditLocationScreenDialogs
import com.pranshulgg.weather_master_app.feature.shared.WeatherViewModel
import com.pranshulgg.weather_master_app.feature.shared.ui.SharedBottomSheet


data class EditLocationScreenUiState(
    val isWeatherSourcesForLocationSheetOpen: Boolean = false,
    val selectedWeatherSource: Source? = null,
    val selectedAlertSource: Source? = null,
    val selectedAirQualitySource: Source? = null,
    val isAlertSourcesSheetOpen: Boolean = false,
    val isAirQualitySourcesSheetOpen: Boolean = false,
    val isEditLocationNameSheetOpen: Boolean = false,
    val isConfirmationDialogOpen: Boolean = false,
    val isOpenMeteoModelsSheetOpen: Boolean = false,
    val selectedOpenMeteoModel: OpenMeteoModel? = null,
    val apiKeys: List<ApiKey> = emptyList()
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EditLocationScreen(
    onBack: () -> Unit
) {

    val viewModel: EditLocationViewModel = hiltViewModel()
    val uiState = viewModel.uiState.value
    val locations = viewModel.locations.collectAsState().value

    if (locations.activeLocation == null) return

    var isSaveExecuted by remember { mutableStateOf(false) }


    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Expanded, SheetValue.Hidden)
    )

    val colorDesc = MetroTheme.colors.accent


    val locationText = buildString {
        append(locations.activeLocation.name)
        if (locations.activeLocation.country.isNotBlank()) {
            append(", ")
        }
        if (locations.activeLocation.state.isNotBlank()) {
            append(locations.activeLocation.state)
            append(", ")
        }
        append(locations.activeLocation.country)
    }

    val locationName =
        if (!locations.activeLocation.customName.isNullOrBlank()) locations.activeLocation.customName
        else locationText

    var currentLocationName by remember { mutableStateOf(locationName) }

    val effectiveWeatherSource =
        uiState.selectedWeatherSource ?: locations.activeLocation.source

    val effectiveOpenMeteoModel =
        uiState.selectedOpenMeteoModel
            ?: locations.activeLocation.openMeteoModel

    val selectedWeatherSourceString = buildString {
        append(effectiveWeatherSource.displayName)

        effectiveWeatherSource.countryNameRes?.let {
            append(" (${stringResource(it)})")
        }

        if (effectiveWeatherSource == Source.OPEN_METEO) {
            append(" (${effectiveOpenMeteoModel.displayName})")
        }
    }

    val effectiveAlertSource = uiState.selectedAlertSource ?: locations.activeLocation.alertSource
    val selectedAlertSourceString = buildString {
        append(effectiveAlertSource.displayName)
        effectiveAlertSource.countryNameRes?.let {
            append(" (${stringResource(it)})")
        }
    }

    val effectiveAirQualitySource =
        uiState.selectedAirQualitySource ?: locations.activeLocation.airQualitySource
    val selectedAirQualitySourceString = buildString {
        append(effectiveAirQualitySource.displayName)
        effectiveAirQualitySource.countryNameRes?.let {
            append(" (${stringResource(it)})")
        }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        WeatherPageHeader(title = stringResource(R.string.location_edit))

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
        ) {
            SettingSection(
                tiles = listOf(
                    SettingTile.ActionTile(
                        title = stringResource(R.string.location_name),
                        description = currentLocationName,
                        onClick = {
                            viewModel.showEditLocationNameSheet()
                        },
                        trailing = {
                            Box(
                                modifier = Modifier.metroClickable { currentLocationName = locationText },
                                contentAlignment = Alignment.Center
                            ) {
                                Symbol(R.drawable.refresh_24px)
                            }
                        },
                        colorDesc = colorDesc,
                        overline = {
                            MetroText(
                                text = stringResource(R.string.action_requires_restart),
                                style = MetroTextStyle.ListItemSubtitle,
                                color = MetroTheme.colors.secondaryText,
                            )
                        }
                    )
                )
            )


            Gap(10.dp)
            SettingSection(
                tiles = listOf(
                    SettingTile.ActionTile(
                        title = stringResource(R.string.weather_source),
                        description = selectedWeatherSourceString,
                        colorDesc = colorDesc,
                        onClick = {
                            viewModel.showWeatherSourcesForLocationSheet()
                        },
                        trailing = {
                            val showButton = if (uiState.selectedWeatherSource != null)
                                uiState.selectedWeatherSource == Source.OPEN_METEO else
                                locations.activeLocation.source == Source.OPEN_METEO

                            if (showButton) {
                                Box(
                                    modifier = Modifier.metroClickable { viewModel.showOpenMeteoModelsSheet() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Symbol(R.drawable.settings_24px)
                                }
                            }

                        }
                    ),
                    SettingTile.ActionTile(
                        title = stringResource(R.string.weather_alert_source),
                        description = selectedAlertSourceString,
                        colorDesc = colorDesc,
                        onClick = {
                            viewModel.showAlertSourcesSheet()
                        }
                    ),
                    SettingTile.ActionTile(
                        title = stringResource(R.string.weather_airquality_source),
                        description = selectedAirQualitySourceString,
                        colorDesc = colorDesc,
                        onClick = {
                            viewModel.showAirQualitySourcesSheet()
                        }
                    )
                )
            )
            MetroText(
                text = "${stringResource(R.string.location_latitude)}: ${locations.activeLocation.latitude}, ${
                    stringResource(
                        R.string.location_longitude
                    )
                }: ${locations.activeLocation.longitude}",
                style = MetroTextStyle.Body,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.padding(
                    top = 10.dp,
                    start = MetroDimens.ScreenHorizontalMargin,
                    end = MetroDimens.ScreenHorizontalMargin,
                )
            )
            Gap(26.dp)
            ButtonWithIcon(
                onClick = {
                    if (!isSaveExecuted) {
                        viewModel.saveLocationName(
                            if (currentLocationName.trim() == locationText.trim()) null else currentLocationName.trim(),
                            locations.activeLocation.id
                        )
                        viewModel.updateSources(
                            locations.activeLocation,
                            uiState.selectedWeatherSource ?: locations.activeLocation.source,
                            uiState.selectedAirQualitySource
                                ?: locations.activeLocation.airQualitySource,
                            uiState.selectedAlertSource ?: locations.activeLocation.alertSource,
                            uiState.selectedOpenMeteoModel
                                ?: locations.activeLocation.openMeteoModel,
                            onBack = {
                                onBack()
                            }
                        )
                    }
                    isSaveExecuted = true
                },
                text = stringResource(R.string.action_save_changes),
                icon = R.drawable.check_24px
            )
            Gap(8.dp)
            ButtonWithIcon(
                onClick = {
                    viewModel.updateDefaultLocation(locations.activeLocation.id)
                    onBack()
                },
                text = stringResource(R.string.action_set_default),
                icon = R.drawable.home_pin_24px,
            )
            Gap(8.dp)
            ButtonWithIcon(
                onClick = {
                    if (locations.activeLocation.isDefault) {
                        SnackbarManager.show(R.string.error_delete_default_location)
                        return@ButtonWithIcon
                    }
                    viewModel.showConfirmationDialog()
                },
                text = stringResource(R.string.action_delete),
                icon = R.drawable.delete_24px,
            )

            // WEATHER SOURCES SHEET
            SharedBottomSheet.WeatherSourcesForLocationSheet(
                countryCode = locations.activeLocation.countryCode,
                show = uiState.isWeatherSourcesForLocationSheetOpen,
                isEditing = true,
                selectedSource = uiState.selectedWeatherSource ?: locations.activeLocation.source,
                onSave = {
                    viewModel.updateSelectedWeatherSource(it)
                    if (it == Source.OPEN_METEO) {
                        viewModel.updateSelectedOpenMeteoModel(
                            uiState.selectedOpenMeteoModel
                                ?: locations.activeLocation.openMeteoModel
                        )
                    }
                },
                onDismiss = viewModel::hideWeatherSourcesForLocationSheet,
                sheetState = sheetState,
                onClickApiConfig = {},
                showActions = false,
                apiKeys = uiState.apiKeys
            )


            // ALERT SOURCES SHEET
            EditLocationBottomSheet.AlertSourcesSheet(
                show = uiState.isAlertSourcesSheetOpen,
                sheetState = sheetState,
                selectedSource = uiState.selectedAlertSource
                    ?: locations.activeLocation.alertSource,
                onSave = {
                    viewModel.updateSelectedAlertSource(it)
                },
                onDismiss = viewModel::hideAlertSourcesSheet,
                onClickApiConfig = {},
                apiKeys = uiState.apiKeys,
                countryCode = locations.activeLocation.countryCode
            )

            // AIR QUALITY SOURCES SHEET
            EditLocationBottomSheet.AirQualitySourcesSheet(
                show = uiState.isAirQualitySourcesSheetOpen,
                sheetState = sheetState,
                selectedSource = uiState.selectedAirQualitySource
                    ?: locations.activeLocation.airQualitySource,
                onSave = {
                    viewModel.updateSelectedAirQualitySource(it)
                },
                onDismiss = viewModel::hideAirQualitySourcesSheet,
                onClickApiConfig = {},
                apiKeys = uiState.apiKeys,
                countryCode = locations.activeLocation.countryCode
            )

            // EDIT LOCATION NAME SHEET
            EditLocationBottomSheet.EditLocationNameSheet(
                show = uiState.isEditLocationNameSheetOpen,
                sheetState = sheetState,
                onDismiss = {
                    viewModel.hideEditLocationNameSheet()
                },
                onSave = {
                    if (it.isNotBlank()) {
                        currentLocationName = it
                    }
                },
                value = currentLocationName,
            )

            // CONFIRMATION DIALOG
            EditLocationScreenDialogs.EditLocationScreenConfirmationDialog(
                viewModel,
                onConfirm = {
                    viewModel.deleteLocation(locations.activeLocation.id)
                    viewModel.hideConfirmationDialog()
                    onBack()
                }
            )

            // OPEN METEO MODELS SHEET
            EditLocationBottomSheet.OpenMeteoModelsSheet(
                show = uiState.isOpenMeteoModelsSheetOpen,
                selectedModel = uiState.selectedOpenMeteoModel
                    ?: locations.activeLocation.openMeteoModel,
                sheetState = sheetState,
                onDismiss = {
                    viewModel.hideOpenMeteoModelsSheet()
                },
                onSave = {
                    viewModel.updateSelectedOpenMeteoModel(it)
                },
            )

        }

        Gap(WindowInsets.systemBars.asPaddingValues().calculateBottomPadding())
    }
}


@Composable
private fun ButtonWithIcon(
    onClick: () -> Unit,
    text: String,
    icon: Int,
    enabled: Boolean = true
) {

    com.metro.ui.MetroBorderButton(
        text = text,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = MetroDimens.ScreenHorizontalMargin),
        enabled = enabled
    )
}