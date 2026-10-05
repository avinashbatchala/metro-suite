package com.pranshulgg.weather_master_app.core.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.pranshulgg.weather_master_app.feature.editlocation.EditLocationScreen
import com.pranshulgg.weather_master_app.feature.main.MainScreen
import com.pranshulgg.weather_master_app.feature.search.SearchScreen
import com.pranshulgg.weather_master_app.feature.settings.SettingsScreen
import com.pranshulgg.weather_master_app.feature.settings.about.AboutScreen
import com.pranshulgg.weather_master_app.feature.settings.about.license.LicenseScreen
import com.pranshulgg.weather_master_app.feature.settings.about.privacy.PrivacyPolicyScreen
import com.pranshulgg.weather_master_app.feature.settings.about.terms.TermsConditionsScreen
import com.pranshulgg.weather_master_app.feature.settings.background.BackgroundUpdatesScreen
import com.pranshulgg.weather_master_app.feature.settings.units.UnitsScreen
import com.pranshulgg.weather_master_app.feature.shared.WeatherViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppNavHost(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
) {

    Box(
        Modifier.fillMaxSize()
    ) {
        NavHost(
            navController = navController,
            startDestination = "root",
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer),
            enterTransition = { NavTransitions.enter() },
            exitTransition = { NavTransitions.exit() },
            popEnterTransition = { NavTransitions.popEnter() },
            popExitTransition = { NavTransitions.popExit() }
        ) {
            navigation(
                route = "root",
                startDestination = NavRoutes.MAIN
            ) {
                composable(
                    NavRoutes.MAIN
                ) { backStackEntry ->

                    val rootEntry = androidx.compose.runtime.remember(backStackEntry) {
                        navController.getBackStackEntry("root")
                    }
                    val weatherViewModel: WeatherViewModel = hiltViewModel(rootEntry)
                    MainScreen(navController, weatherViewModel)
                }
                composable(
                    NavRoutes.SEARCH
                ) {
                    SearchScreen(navController)
                }
                composable(
                    NavRoutes.SETTINGS
                ) {
                    SettingsScreen(navController)
                }
                composable(
                    NavRoutes.UNITS
                ) {
                    UnitsScreen(navController)
                }
                composable(
                    NavRoutes.BACKGROUND_UPDATES
                ) {
                    BackgroundUpdatesScreen(navController)
                }
                composable(
                    NavRoutes.ABOUT
                ) {
                    AboutScreen(navController)
                }
                composable(
                    NavRoutes.TERMS_CONDITIONS
                ) {
                    TermsConditionsScreen(navController)
                }
                composable(
                    NavRoutes.PRIVACY_POLICY
                ) {
                    PrivacyPolicyScreen(navController)
                }
                composable(
                    NavRoutes.LICENSE
                ) {
                    LicenseScreen(navController)
                }
                composable(
                    route = NavRoutes.EDIT_LOCATION,
                ) {
                    EditLocationScreen(navController)
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(
                    bottom = WindowInsets.navigationBars.asPaddingValues()
                        .calculateBottomPadding()
                )
        )

    }

}
