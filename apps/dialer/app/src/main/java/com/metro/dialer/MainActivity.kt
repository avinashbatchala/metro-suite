package com.metro.dialer

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.metro.dialer.telecom.MetroTelecomBridge
import com.metro.dialer.telecom.MetroTelecomSetup
import com.metro.dialer.ui.DialerRoute
import com.metro.dialer.ui.DialerShell
import com.metro.dialer.ui.DialerViewModel
import com.metro.dialer.ui.PermissionScreen
import com.metro.ui.MetroActivities
import com.metro.ui.MetroAppPivotShell
import com.metro.ui.MetroLoadingScreen
import com.metro.ui.MetroSplash
import com.metro.ui.MetroSystemTheme

class MainActivity : ComponentActivity() {
    private val viewModel: DialerViewModel by viewModels()
    private val dialNavigationSignal = mutableStateOf(0)

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        viewModel.onPermissionResult(
            callLogGranted = results[Manifest.permission.READ_CALL_LOG] == true,
            contactsGranted = results[Manifest.permission.READ_CONTACTS] == true,
            callPhoneGranted = results[Manifest.permission.CALL_PHONE] == true,
        )
    }

    private val requestFeaturePermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        viewModel.refreshAfterPermissions()
    }

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* best effort */ }

    private val requestDefaultDialer = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { /* role result handled on next resume */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        MetroSplash.install(this)
        super.onCreate(savedInstanceState)
        MetroActivities.applyLaunchTransition(this)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            var permissionTick by remember { mutableStateOf(0) }
            var skippedDefaultDialer by remember { mutableStateOf(false) }
            val pendingDialNavigation by dialNavigationSignal

            DisposableEffect(this@MainActivity) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) permissionTick++
                }
                lifecycle.addObserver(observer)
                onDispose { lifecycle.removeObserver(observer) }
            }

            LaunchedEffect(permissionTick) {
                viewModel.refreshPermissions()
                viewModel.reloadSpeedDial()
                viewModel.reloadContacts()
                viewModel.reloadHistory()
            }

            val isDefaultDialer = MetroTelecomBridge.isDefaultDialer(context)
            val needsCallPhone = !viewModel.hasCallPhonePermission
            val needsSetup = needsCallPhone || (!isDefaultDialer && !skippedDefaultDialer)
            var notificationsRequested by remember { mutableStateOf(false) }

            // Notifications are requested only once the core phone role is in place — not lumped
            // into the call-permission request.
            LaunchedEffect(needsSetup, permissionTick) {
                if (needsSetup || notificationsRequested) return@LaunchedEffect
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@LaunchedEffect
                val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                if (!granted) {
                    notificationsRequested = true
                    requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            LaunchedEffect(pendingDialNavigation) {
                if (pendingDialNavigation == 0) return@LaunchedEffect
                val dialIntent = intent ?: return@LaunchedEffect
                when (dialIntent.action) {
                    ACTION_ADD_CALL -> {
                        viewModel.openDialPad()
                        setIntent(Intent(Intent.ACTION_MAIN))
                    }
                    Intent.ACTION_CALL -> {
                        val uri = dialIntent.data ?: return@LaunchedEffect
                        MetroTelecomBridge.handleCallIntent(context, uri)
                        setIntent(Intent(Intent.ACTION_MAIN))
                    }
                    else -> {
                        val uri = dialIntent.data?.takeIf { it.scheme == "tel" }
                        if (uri != null) {
                            viewModel.handleDialIntent(uri)
                        } else if (dialIntent.action == Intent.ACTION_DIAL) {
                            viewModel.openDialPad()
                        }
                    }
                }
            }

            MetroSystemTheme {
                MetroAppPivotShell(
                    modifier = Modifier.fillMaxSize(),
                    onExit = { MetroActivities.finishWithExitTransition(this@MainActivity) },
                ) {
                    when {
                        !viewModel.permissionsChecked -> {
                            MetroLoadingScreen(modifier = Modifier.fillMaxSize())
                        }
                        needsSetup -> {
                            PermissionScreen(
                                hasCallPhonePermission = viewModel.hasCallPhonePermission,
                                isDefaultDialer = isDefaultDialer,
                                onRequestPermissions = {
                                    requestPermissions.launch(
                                        arrayOf(Manifest.permission.CALL_PHONE),
                                    )
                                },
                                onRequestDefaultDialer = {
                                    MetroTelecomSetup.createDefaultDialerRequestIntent(context)
                                        ?.let { requestDefaultDialer.launch(it) }
                                },
                                onContinue = { skippedDefaultDialer = true },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        else -> {
                            DialerShell(
                                state = viewModel,
                                onRequestPermissions = { permissions ->
                                    requestFeaturePermissions.launch(permissions)
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        dialNavigationSignal.value++
    }

    companion object {
        const val ACTION_ADD_CALL = "com.metro.dialer.action.ADD_CALL"
    }
}
