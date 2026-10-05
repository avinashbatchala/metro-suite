package com.pranshulgg.weather_master_app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.metro.system.MetroPreferences
import com.metro.ui.MetroActivities
import com.metro.ui.MetroAppPivotShell
import com.metro.ui.MetroSplash
import com.metro.ui.MetroSystemTheme
import com.pranshulgg.weather_master_app.core.prefs.AppPrefs.initPrefs
import com.pranshulgg.weather_master_app.data.provider.devicelocation.GetDeviceLocation
import com.pranshulgg.weather_master_app.data.store.InitializationStore
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var initializationStore: InitializationStore

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = MetroSplash.install(this)
        MetroActivities.applyLaunchTransition(this)

        val startTime = System.currentTimeMillis()
        splashScreen.setKeepOnScreenCondition {
            val initialized = initializationStore.data.value.isInitialized
            val timedOut = System.currentTimeMillis() - startTime > 5000
            !initialized && !timedOut
        }

        initPrefs(this)
        super.onCreate(savedInstanceState)

        val isDark = MetroPreferences(this).isDark

        enableEdgeToEdge(
            navigationBarStyle = if (isDark) {
                SystemBarStyle.dark(Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
            }
        )

        setContent {
            MetroSystemTheme {
                MetroAppPivotShell(
                    onExit = { MetroActivities.finishWithExitTransition(this) }
                ) {
                    WeatherMasterApp()
                }
            }
        }
    }

    val locationHelper = GetDeviceLocation()
    override fun onPause() {
        super.onPause()
        locationHelper.stopUpdates()
    }
}
