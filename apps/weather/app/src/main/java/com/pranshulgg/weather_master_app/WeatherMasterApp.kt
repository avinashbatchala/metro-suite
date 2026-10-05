package com.pranshulgg.weather_master_app

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pranshulgg.weather_master_app.core.prefs.AppPrefs
import com.pranshulgg.weather_master_app.core.prefs.LocalAppPrefs
import com.pranshulgg.weather_master_app.core.ui.navigation.AppNavHost
import com.pranshulgg.weather_master_app.core.ui.navigation.NavRoutes
import com.pranshulgg.weather_master_app.core.ui.snackbar.LocalSnackbarHostState
import com.pranshulgg.weather_master_app.core.ui.snackbar.SnackbarManager
import kotlinx.coroutines.flow.collectLatest

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WeatherMasterApp() {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        SnackbarManager.events.collectLatest { event ->
            snackbarHostState.currentSnackbarData?.dismiss()

            val result = snackbarHostState.showSnackbar(
                message = context.getString(
                    event.messageResource,
                    event.messageArgs
                ),
                actionLabel = if (event.actionLabel != null) context.getString(event.actionLabel) else null,
                withDismissAction = event.actionLabel == null,
                duration = event.duration
            )

            if (result == SnackbarResult.ActionPerformed) {
                event.onAction?.invoke()
            }
        }
    }

    // Back pops the in-app navigation stack first; on Home it defers to the app-open
    // pivot shell, which plays the flip-out and exits to Start.
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    BackHandler(enabled = currentRoute != null && currentRoute != NavRoutes.MAIN) {
        navController.popBackStack()
    }

    CompositionLocalProvider(
        LocalSnackbarHostState provides snackbarHostState,
        LocalAppPrefs provides AppPrefs.state()
    ) {
        AppNavHost(
            navController,
            snackbarHostState
        )
    }
}