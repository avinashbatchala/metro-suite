package com.metro.calendar

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.metro.calendar.data.subscription.SubscriptionSyncWorker
import com.metro.calendar.tiles.CalendarTileRefresh
import com.metro.calendar.ui.CalendarShell
import com.metro.calendar.ui.CalendarState
import com.metro.ui.MetroActivities
import com.metro.ui.MetroAppPivotShell
import com.metro.ui.MetroSplash
import com.metro.ui.MetroSystemTheme

class MainActivity : ComponentActivity() {
    private val requestRead = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        readResult?.invoke(granted)
    }

    private val requestWrite = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        writeResult?.invoke(granted)
    }

    private var readResult: ((Boolean) -> Unit)? = null
    private var writeResult: ((Boolean) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        MetroSplash.install(this)
        super.onCreate(savedInstanceState)
        MetroActivities.applyLaunchTransition(this)
        enableEdgeToEdge()
        SubscriptionSyncWorker.schedulePeriodic(this)
        setContent {
            val context = LocalContext.current
            val state = remember { CalendarState(context) }
            var resumeTick by remember { mutableIntStateOf(0) }

            DisposableEffect(this@MainActivity) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        resumeTick++
                    }
                }
                lifecycle.addObserver(observer)
                onDispose { lifecycle.removeObserver(observer) }
            }

            // Refresh permissions, timezone, clock format and events on launch/resume.
            DisposableEffect(resumeTick) {
                readResult = { granted -> state.onReadPermissionResult(granted) }
                writeResult = { granted -> state.onWritePermissionResult(granted) }
                state.refreshSettings(context)
                CalendarTileRefresh.request(context)
                onDispose { }
            }

            MetroSystemTheme {
                MetroAppPivotShell(
                    modifier = Modifier.fillMaxSize(),
                    onExit = { MetroActivities.finishWithExitTransition(this@MainActivity) },
                ) {
                    CalendarShell(
                        state = state,
                        onRequestReadPermission = {
                            requestRead.launch(Manifest.permission.READ_CALENDAR)
                        },
                        onRequestWritePermission = {
                            requestWrite.launch(Manifest.permission.WRITE_CALENDAR)
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}
