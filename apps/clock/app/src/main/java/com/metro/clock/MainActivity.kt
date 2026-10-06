package com.metro.clock

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.metro.clock.ui.ClockShell
import com.metro.clock.ui.ClockState
import com.metro.ui.MetroActivities
import com.metro.ui.MetroAppPivotShell
import com.metro.ui.MetroSplash
import com.metro.ui.MetroSystemTheme

class MainActivity : ComponentActivity() {

    private val requestNotifications = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    private var clockState: ClockState? = null
    private var onDeepLink: ((Intent) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        MetroSplash.install(this)
        super.onCreate(savedInstanceState)
        MetroActivities.applyLaunchTransition(this)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val state = remember { ClockState(context).also { clockState = it } }
            var deepLinkTick by remember { mutableStateOf(0) }
            var latestIntent by remember { mutableStateOf(intent) }
            @Suppress("UNUSED_VARIABLE")
            val generation = state.generation

            DisposableEffect(Unit) {
                onDeepLink = { deepIntent ->
                    latestIntent = deepIntent
                    deepLinkTick++
                }
                onDispose { onDeepLink = null }
            }

            DisposableEffect(state) {
                state.requestNotificationPermission = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                onDispose { state.requestNotificationPermission = null }
            }

            LaunchedEffect(Unit) { state.initialize() }
            LaunchedEffect(deepLinkTick, latestIntent) {
                state.handleDeepLink(latestIntent?.data)
            }

            MetroSystemTheme {
                MetroAppPivotShell(
                    modifier = Modifier.fillMaxSize(),
                    onExit = { MetroActivities.finishWithExitTransition(this@MainActivity) },
                ) {
                    ClockShell(state = state, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        onDeepLink?.invoke(intent) ?: clockState?.handleDeepLink(intent.data)
    }
}
