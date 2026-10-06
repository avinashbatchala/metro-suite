/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import com.metro.ui.MetroActivities
import com.metro.ui.MetroAppPivotShell
import com.metro.ui.MetroSplash
import com.metro.ui.MetroSystemTheme
import com.music.innertube.YouTube
import com.music.innertube.models.WatchEndpoint
import com.music.vivi.constants.AppLanguageKey
import com.music.vivi.constants.DisableScreenshotKey
import com.music.vivi.constants.StopMusicOnTaskClearKey
import com.music.vivi.constants.SYSTEM_DEFAULT
import com.music.vivi.db.MusicDatabase
import com.music.vivi.models.toMediaMetadata
import com.music.vivi.playback.DownloadUtil
import com.music.vivi.playback.MusicService
import com.music.vivi.playback.MusicService.MusicBinder
import com.music.vivi.playback.PlayerConnection
import com.music.vivi.playback.queues.YouTubeQueue
import com.music.vivi.ui.metro.MetroMusicApp
import com.music.vivi.utils.SyncUtils
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.get
import com.music.vivi.utils.reportException
import com.music.vivi.utils.setAppLocale
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    companion object {
        private const val ACTION_SEARCH = "com.music.vivi.action.SEARCH"
        private const val ACTION_LIBRARY = "com.music.vivi.action.LIBRARY"
    }

    @Inject
    lateinit var database: MusicDatabase

    @Inject
    lateinit var downloadUtil: DownloadUtil

    @Inject
    lateinit var syncUtils: SyncUtils

    private var pendingIntent: Intent? = null

    private var playerConnection by mutableStateOf<PlayerConnection?>(null)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is MusicBinder) {
                try {
                    playerConnection = PlayerConnection(this@MainActivity, service, database, lifecycleScope)
                    Timber.tag("MainActivity").d("PlayerConnection created successfully")
                } catch (e: Exception) {
                    Timber.tag("MainActivity").e(e, "Failed to create PlayerConnection")
                    // Retry after a delay of 500ms
                    lifecycleScope.launch {
                        delay(500)
                        try {
                            playerConnection = PlayerConnection(this@MainActivity, service, database, lifecycleScope)
                        } catch (e2: Exception) {
                            Timber.tag("MainActivity").e(e2, "Failed to create PlayerConnection on retry")
                        }
                    }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            playerConnection?.dispose()
            playerConnection = null
        }
    }

    override fun onStart() {
        super.onStart()
        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1000)
            }
        }

        bindService(
            Intent(this, MusicService::class.java),
            serviceConnection,
            BIND_AUTO_CREATE
        )
    }

    override fun onStop() {
        unbindService(serviceConnection)
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (dataStore.get(StopMusicOnTaskClearKey, false) &&
            playerConnection?.isPlaying?.value == true &&
            isFinishing
        ) {
            stopService(Intent(this, MusicService::class.java))
            unbindService(serviceConnection)
            playerConnection = null
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLinkIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        MetroSplash.install(this)
        super.onCreate(savedInstanceState)
        MetroActivities.applyLaunchTransition(this)
        enableEdgeToEdge()
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_LTR
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            val locale = dataStore[AppLanguageKey]
                ?.takeUnless { it == SYSTEM_DEFAULT }
                ?.let { Locale.forLanguageTag(it) }
                ?: Locale.getDefault()
            setAppLocale(this, locale)
        }

        lifecycleScope.launch {
            dataStore.data
                .map { it[DisableScreenshotKey] ?: false }
                .distinctUntilChanged()
                .collectLatest {
                    if (it) {
                        window.setFlags(
                            WindowManager.LayoutParams.FLAG_SECURE,
                            WindowManager.LayoutParams.FLAG_SECURE,
                        )
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
        }

        setContent {
            MetroVivimusicRoot(
                playerConnection = playerConnection,
                database = database,
                downloadUtil = downloadUtil,
                syncUtils = syncUtils,
            )
        }
    }

    @Composable
    private fun MetroVivimusicRoot(
        playerConnection: PlayerConnection?,
        database: MusicDatabase,
        downloadUtil: DownloadUtil,
        syncUtils: SyncUtils,
    ) {
        CompositionLocalProvider(
            LocalDatabase provides database,
            LocalPlayerConnection provides playerConnection,
            LocalPlayerAwareWindowInsets provides WindowInsets.systemBars,
            LocalDownloadUtil provides downloadUtil,
            LocalSyncUtils provides syncUtils,
            LocalSnackbarHostState provides Unit,
        ) {
            LaunchedEffect(Unit) {
                val intent = pendingIntent ?: this@MainActivity.intent
                if (intent != null) {
                    handleDeepLinkIntent(intent)
                    pendingIntent = null
                }
            }
            MetroSystemTheme {
                val exitApp = { MetroActivities.finishWithExitTransition(this@MainActivity) }
                MetroAppPivotShell(
                    modifier = Modifier.fillMaxSize(),
                    onExit = exitApp,
                ) {
                    MetroMusicApp(onExit = exitApp)
                }
            }
        }
    }

    private fun handleDeepLinkIntent(intent: Intent) {
        val uri = intent.data ?: intent.extras?.getString(Intent.EXTRA_TEXT)?.toUri() ?: return
        intent.data = null
        intent.removeExtra(Intent.EXTRA_TEXT)
        val coroutineScope = lifecycle.coroutineScope

        val path = uri.pathSegments.firstOrNull()
        val playlistId = uri.getQueryParameter("list")
        val videoId = when {
            path == "watch" -> uri.getQueryParameter("v")
            uri.host == "youtu.be" -> uri.pathSegments.firstOrNull()
            else -> null
        }

        if (videoId != null) {
            coroutineScope.launch(Dispatchers.IO) {
                YouTube.queue(listOf(videoId), playlistId).onSuccess { queue ->
                    withContext(Dispatchers.Main) {
                        playerConnection?.playQueue(
                            YouTubeQueue(
                                WatchEndpoint(videoId = queue.firstOrNull()?.id, playlistId = playlistId),
                                queue.firstOrNull()?.toMediaMetadata(),
                            )
                        )
                    }
                }.onFailure {
                    reportException(it)
                }
            }
        } else if (playlistId != null) {
            coroutineScope.launch(Dispatchers.IO) {
                YouTube.queue(null, playlistId).onSuccess { queue ->
                    val firstItem = queue.firstOrNull()
                    withContext(Dispatchers.Main) {
                        playerConnection?.playQueue(
                            YouTubeQueue(
                                WatchEndpoint(videoId = firstItem?.id, playlistId = playlistId),
                                firstItem?.toMediaMetadata(),
                            )
                        )
                    }
                }.onFailure {
                    reportException(it)
                }
            }
        }
    }
}

val LocalDatabase = staticCompositionLocalOf<MusicDatabase> { error("No database provided") }
val LocalPlayerConnection = staticCompositionLocalOf<PlayerConnection?> { error("No PlayerConnection provided") }
val LocalPlayerAwareWindowInsets = compositionLocalOf<WindowInsets> { error("No WindowInsets provided") }
val LocalDownloadUtil = staticCompositionLocalOf<DownloadUtil> { error("No DownloadUtil provided") }
val LocalSyncUtils = staticCompositionLocalOf<SyncUtils> { error("No SyncUtils provided") }
val LocalSnackbarHostState = staticCompositionLocalOf<Any?> { error("No SnackbarHostState provided") }
