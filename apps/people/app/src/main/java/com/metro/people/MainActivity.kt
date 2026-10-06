package com.metro.people

import android.Manifest
import android.content.Intent
import android.net.Uri
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.metro.people.tiles.PeopleTileRefresh
import com.metro.people.ui.PeopleShell
import com.metro.people.ui.PeopleState
import com.metro.people.ui.PermissionScreen
import com.metro.ui.MetroActivities
import com.metro.ui.MetroSplash
import com.metro.ui.MetroAppPivotShell
import com.metro.ui.MetroLoadingScreen
import com.metro.ui.MetroSystemTheme

class MainActivity : ComponentActivity() {
    private val requestContacts = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionResult?.invoke(granted)
    }

    private val requestWriteContacts = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        writePermissionResult?.invoke(granted)
    }

    private val pickVcf = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri ?: return@registerForActivityResult
        runCatching {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        peopleState?.onVcfPicked(uri) ?: run { pendingVcfUri = uri }
    }

    private var permissionResult: ((Boolean) -> Unit)? = null
    private var writePermissionResult: ((Boolean) -> Unit)? = null
    private var peopleState: PeopleState? = null
    private var onDeepLink: ((Intent) -> Unit)? = null
    private var pendingVcfUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        MetroSplash.install(this)
        super.onCreate(savedInstanceState)
        MetroActivities.applyLaunchTransition(this)
        enableEdgeToEdge()
        pendingVcfUri = extractVCardUri(intent)
        setContent {
            val context = LocalContext.current
            val state = remember { PeopleState(context).also { peopleState = it } }
            var permissionTick by remember { mutableStateOf(0) }
            var deepLinkTick by remember { mutableStateOf(0) }
            var latestDeepLink by remember { mutableStateOf(intent) }
            val generation = state.generation
            @Suppress("UNUSED_VARIABLE")
            val observePeopleState = generation

            DisposableEffect(Unit) {
                state.requestWriteContactsPermission = {
                    writePermissionResult = { granted -> state.onWritePermissionResult(granted) }
                    requestWriteContacts.launch(Manifest.permission.WRITE_CONTACTS)
                }
                onDeepLink = { deepIntent ->
                    latestDeepLink = deepIntent
                    deepLinkTick++
                }
                onDispose {
                    state.requestWriteContactsPermission = null
                    onDeepLink = null
                }
            }

            DisposableEffect(this@MainActivity) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        permissionTick++
                    }
                }
                lifecycle.addObserver(observer)
                onDispose { lifecycle.removeObserver(observer) }
            }

            DisposableEffect(permissionTick) {
                state.refreshPermission(context)
                if (state.hasContactsPermission) {
                    state.reloadContacts()
                }
                PeopleTileRefresh.request(context)
                onDispose { }
            }

            LaunchedEffect(deepLinkTick, latestDeepLink, state.hasContactsPermission) {
                if (state.hasContactsPermission) {
                    state.openDeepLink(latestDeepLink?.data)
                }
            }

            LaunchedEffect(Unit) {
                pendingVcfUri?.let { uri ->
                    pendingVcfUri = null
                    state.onVcfPicked(uri)
                }
            }

            MetroSystemTheme {
                MetroAppPivotShell(
                    modifier = Modifier.fillMaxSize(),
                    onExit = { MetroActivities.finishWithExitTransition(this@MainActivity) },
                ) {
                    when {
                        !state.permissionsChecked -> {
                            MetroLoadingScreen(modifier = Modifier.fillMaxSize())
                        }
                        state.hasContactsPermission -> {
                            PeopleShell(
                                state = state,
                                onImportContacts = { pickVcf.launch(VCARD_PICK_MIME_TYPES) },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        else -> {
                            PermissionScreen(
                                onRequestPermission = {
                                    permissionResult = { granted ->
                                        state.onPermissionResult(granted)
                                    }
                                    requestContacts.launch(Manifest.permission.READ_CONTACTS)
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
        val vcfUri = extractVCardUri(intent)
        if (vcfUri != null) {
            peopleState?.onVcfPicked(vcfUri) ?: run { pendingVcfUri = vcfUri }
            return
        }
        onDeepLink?.invoke(intent) ?: peopleState?.openDeepLink(intent.data)
    }

    /** Returns a content/file URI when the intent is an inbound vCard/vcf view request. */
    private fun extractVCardUri(intent: Intent?): Uri? {
        if (intent?.action != Intent.ACTION_VIEW) return null
        val data = intent.data ?: return null
        val mime = intent.type?.lowercase()
        val looksLikeVCard = mime in VCARD_MIME_TYPES ||
            data.lastPathSegment?.endsWith(".vcf", ignoreCase = true) == true
        return if (looksLikeVCard) data else null
    }

    private companion object {
        /**
         * Includes a wildcard entry so files whose MIME type is misreported (octet-stream /
         * text/plain) can still be chosen; the parser validates the actual content.
         */
        val VCARD_PICK_MIME_TYPES = arrayOf(
            "text/vcard",
            "text/x-vcard",
            "text/directory",
            "text/plain",
            "application/octet-stream",
            "*/*",
        )

        val VCARD_MIME_TYPES = setOf(
            "text/vcard",
            "text/x-vcard",
            "text/directory",
            "application/vcard",
        )
    }
}
