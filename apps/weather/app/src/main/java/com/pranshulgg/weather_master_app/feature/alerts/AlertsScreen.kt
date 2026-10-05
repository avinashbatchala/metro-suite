package com.pranshulgg.weather_master_app.feature.alerts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.metro.ui.MetroDimens
import com.metro.ui.MetroEmptyState
import com.metro.ui.MetroTheme
import com.pranshulgg.weather_master_app.core.model.domain.alerts.Alert
import com.pranshulgg.weather_master_app.core.model.domain.location.Location
import com.pranshulgg.weather_master_app.core.prefs.LocalAppPrefs
import com.pranshulgg.weather_master_app.core.ui.components.Gap
import com.pranshulgg.weather_master_app.core.ui.components.LargeTopBarScaffold
import com.pranshulgg.weather_master_app.core.ui.components.NavigateUpBtn
import com.pranshulgg.weather_master_app.feature.alerts.components.AlertCard

data class AlertsScreenUiState(
    val alerts: List<Alert?> = emptyList(),
    val location: Location? = null
)

@Composable
fun AlertsScreen(navController: NavController, locationId: String) {

    val viewModel: AlertsScreenViewModel = hiltViewModel()
    val uiState = viewModel.uiState.value
    val prefs = LocalAppPrefs.current

    LaunchedEffect(Unit) {
        viewModel.getAlertsForLocation(locationId)
        viewModel.getLocation(locationId)
    }


    if (!uiState.alerts.isNotEmpty()) {
        MetroEmptyState(message = "No alerts")
        return
    }

    LargeTopBarScaffold(
        title = "Alerts",
        navigationIcon = { NavigateUpBtn(navController) },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MetroTheme.colors.background)
                    .verticalScroll(rememberScrollState())
                    .padding(
                        top = paddingValues.calculateTopPadding(),
                        start = MetroDimens.ScreenHorizontalMargin,
                        end = MetroDimens.ScreenHorizontalMargin,
                    ),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            uiState.alerts.filterNotNull().forEachIndexed { index, it ->
                AlertCard(it, prefs, uiState.location!!.timezone, RectangleShape)
            }

            Gap(WindowInsets.systemBars.asPaddingValues().calculateBottomPadding() + 30.dp)

        }

    }
}
