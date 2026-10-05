package com.pranshulgg.weather_master_app.feature.intro

import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroDimens
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.pranshulgg.weather_master_app.R
import com.pranshulgg.weather_master_app.core.model.domain.location.Address
import com.pranshulgg.weather_master_app.core.model.sources.Source
import com.pranshulgg.weather_master_app.core.model.domain.location.Location
import com.pranshulgg.weather_master_app.core.ui.components.Gap
import com.pranshulgg.weather_master_app.core.ui.components.Symbol
import com.pranshulgg.weather_master_app.core.ui.components.WeatherIconBox
import com.pranshulgg.weather_master_app.core.ui.navigation.NavRoutes
import com.pranshulgg.weather_master_app.core.ui.snackbar.SnackbarManager
import com.pranshulgg.weather_master_app.core.utils.ids.UuidGenerator
import com.pranshulgg.weather_master_app.data.provider.devicelocation.DeviceLocation
import com.pranshulgg.weather_master_app.data.provider.devicelocation.GetDeviceLocation
import com.pranshulgg.weather_master_app.data.provider.devicelocation.getCountryCode
import com.pranshulgg.weather_master_app.data.provider.devicelocation.rememberBackgroundLocationPermissionLauncher
import com.pranshulgg.weather_master_app.data.provider.devicelocation.rememberLocationPermissionLauncher
import com.pranshulgg.weather_master_app.feature.shared.ui.SharedDialogs
import java.time.ZoneId
import java.util.TimeZone

@Composable
fun IntroScreen(navController: NavController) {

    val context = LocalContext.current
    val viewModel: IntroScreenViewModel = hiltViewModel()
    var isLoading by remember { mutableStateOf(false) }
    var backgroundLocationPermissionInfoDialogOpen by remember { mutableStateOf(false) }
    var locationPermissionInfoDialogOpen by remember { mutableStateOf(false) }
    val isImportingBackup = viewModel.isImportingBackup

    val importBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importBackup(it) }
    }

    val continueWithLocation = {
        isLoading = true
        GetDeviceLocation().getDeviceLocation(
            context,
            onTimeout = {
                SnackbarManager.show(R.string.current_location_not_found)
                isLoading = false
            }) { location ->
            if (location.latitude == null || location.longitude == null) {
                SnackbarManager.show(R.string.current_location_not_found)
                isLoading = false
                return@getDeviceLocation
            }

            viewModel.saveDeviceLocation(location)
        }
    }

    val requestLocation = rememberLocationPermissionLauncher(
        onForegroundGranted = {
            continueWithLocation()
//            backgroundLocationPermissionInfoDialogOpen = true
        },
        onDenied = {
            SnackbarManager.show(R.string.location_permission_required)
            isLoading = false
        }
    )

    val requestBackgroundLocation = rememberBackgroundLocationPermissionLauncher(
        onGranted = {
            continueWithLocation()
        },
        onContinueWithoutBackground = {
            continueWithLocation()
        },
        onDenied = {
            SnackbarManager.show(R.string.location_permission_required)
            backgroundLocationPermissionInfoDialogOpen = true
            isLoading = false
        }
    )

    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background)
    ) {

        Box(
            Modifier.fillMaxSize()
        ) {

            Column(
                Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = MetroDimens.ScreenHorizontalMargin),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon()
                Gap(24.dp)
                MetroText(
                    text = "Let’s Get Your Forecast",
                    style = MetroTextStyle.SectionHeader,
                    textAlign = TextAlign.Center
                )
                Gap(8.dp)
                MetroText(
                    text = "Allow location access to see your local forecast",
                    style = MetroTextStyle.Body,
                    color = MetroTheme.colors.secondaryText,
                    textAlign = TextAlign.Center
                )
                Gap(28.dp)
                MetroBorderButton(
                    text = if (isLoading) "Loading..." else "Enable Location",
                    onClick = { locationPermissionInfoDialogOpen = true },
                    enabled = !isLoading && !isImportingBackup
                )
                Gap(12.dp)
                MetroBorderButton(
                    text = "Search for a City",
                    onClick = { navController.navigate(NavRoutes.SEARCH) },
                    enabled = !isLoading && !isImportingBackup
                )
                Gap(4.dp)
                MetroBorderButton(
                    text = if (isImportingBackup) "Importing..." else "Import a backup",
                    onClick = { importBackupLauncher.launch(arrayOf("application/json")) },
                    enabled = !isLoading && !isImportingBackup
                )
            }

            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .rotate(60f)
                    .alpha(0.2f)
            ) {
                WeatherIconBox(R.drawable.weather_very_hot, size = 150.dp)
            }

            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(y = 100.dp)
                    .rotate(60f)
                    .alpha(0.1f)
            ) {
                WeatherIconBox(R.drawable.weather_clear_night, size = 150.dp)
            }

            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .offset(y = 500.dp)
                    .rotate(60f)
                    .alpha(0.1f)
            ) {
                WeatherIconBox(R.drawable.weather_clear_day, size = 150.dp)
            }

            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .rotate(60f)
                    .alpha(0.1f)
            ) {
                WeatherIconBox(R.drawable.weather_very_cold, size = 150.dp)
            }
        }
    }

    SharedDialogs.DeviceBackgroundLocationPermissionInfoDialog(
        show = backgroundLocationPermissionInfoDialogOpen,
        onConfirm = {
            backgroundLocationPermissionInfoDialogOpen = false
            requestBackgroundLocation()
        },
        onDismiss = { backgroundLocationPermissionInfoDialogOpen = false }
    )

    SharedDialogs.DeviceLocationPermissionInfoDialog(
        show = locationPermissionInfoDialogOpen,
        onConfirm = {
            requestLocation()
        },
        onDismiss = { locationPermissionInfoDialogOpen = false }
    )
}

@Composable
private fun Icon() {
    Box(
        modifier = Modifier
            .size(100.dp)
            .background(MetroTheme.colors.accent),
        contentAlignment = Alignment.Center
    ) {
        Symbol(
            R.drawable.location_on_24px,
            size = 56.dp,
            color = MetroTheme.colors.background
        )
    }
}

fun DeviceLocation.toDomain(context: Context): Location {


    val formattedLatitude = kotlin.math.round(latitude!! * 100000) / 100000
    val formattedLongitude = kotlin.math.round(longitude!! * 100000) / 100000



    return Location(
        id = UuidGenerator.generateId(),
        name = "$formattedLatitude, $formattedLongitude",
        latitude = formattedLatitude,
        longitude = formattedLongitude,
        country = "",
        timezone = ZoneId.systemDefault().id,
        countryCode = "",
        state = "",
        source = Source.OPEN_METEO,
        isFavorite = false,
        isPinned = false,
        isDefault = false, // Repository can handle it
        isDeviceLocation = true
    )

}