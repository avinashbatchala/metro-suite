package com.metro.dialer

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import com.metro.dialer.data.PhonePreferences
import com.metro.dialer.telecom.ActiveCallNotifier
import com.metro.dialer.telecom.IncomingCallNotifier
import com.metro.dialer.telecom.MetroCallEndpoint
import com.metro.dialer.telecom.MetroCallEndpointType
import com.metro.dialer.telecom.MetroCallSession
import com.metro.dialer.telecom.ProximityScreenController
import com.metro.dialer.ui.InCallScreen
import com.metro.dialer.ui.IncomingCallScreen
import com.metro.system.MetroLockscreen
import com.metro.ui.MetroStatusBarFullscreenEffect
import com.metro.ui.MetroSystemTheme

class InCallActivity : ComponentActivity() {
    private val proximityScreen by lazy { ProximityScreenController(this) }
    private var proximityWanted = false
    private lateinit var phonePreferences: PhonePreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        phonePreferences = PhonePreferences(this)
        MetroLockscreen.requestSuppress(this, true)
        super.onCreate(savedInstanceState)
        enableShowWhenLocked()
        enableEdgeToEdge()

        if (!MetroCallSession.hasActiveCall()) {
            MetroLockscreen.requestSuppress(this, false)
            finish()
            return
        }

        val lockedAtLaunch = IncomingCallNotifier.needsFullScreenIntent(this)

        MetroCallSession.setOnSessionEndedListener {
            runOnUiThread { finish() }
        }

        setContent {
            val session by MetroCallSession.state
            val context = this

            LaunchedEffect(session.isEmpty) {
                if (session.isEmpty) finish()
            }

            val ringing = session.ringingCall
            val primaryActive = session.primaryCall?.state?.isConnected == true
            val endpoint = session.audio.currentEndpoint
            val privateAudio = endpoint?.type == MetroCallEndpointType.EARPIECE ||
                endpoint == null

            LaunchedEffect(primaryActive, endpoint?.type, ringing?.id) {
                proximityWanted = primaryActive && privateAudio && ringing == null
                syncProximityScreen()
            }

            MetroSystemTheme {
                MetroStatusBarFullscreenEffect(active = !session.isEmpty)
                if (ringing != null) {
                    IncomingCallScreen(
                        call = ringing,
                        textReplies = phonePreferences.textReplies(),
                        textReplyEnabled = phonePreferences.textReplyEnabled,
                        lockedReveal = lockedAtLaunch,
                        onAnswer = { MetroCallSession.answer(ringing.id) },
                        onIgnore = { MetroCallSession.reject(ringing.id) },
                        onTextReply = { message -> MetroCallSession.reject(ringing.id, message) },
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    InCallScreen(
                        session = session,
                        onEndCall = { callId -> MetroCallSession.end(callId, context) },
                        onHold = { callId, held -> MetroCallSession.hold(callId, held) },
                        onSwap = { MetroCallSession.swap() },
                        onMerge = { MetroCallSession.merge() },
                        onSplit = { callId -> MetroCallSession.splitCall(callId) },
                        onAnswerCall = { callId -> MetroCallSession.answer(callId) },
                        onAddCall = {
                            startActivity(
                                Intent(context, MainActivity::class.java).apply {
                                    addFlags(
                                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                            Intent.FLAG_ACTIVITY_SINGLE_TOP,
                                    )
                                    action = "com.metro.dialer.action.ADD_CALL"
                                },
                            )
                        },
                        onSelectEndpoint = { endpointValue: MetroCallEndpoint ->
                            MetroCallSession.selectAudioEndpoint(endpointValue)
                        },
                        onToggleMute = {
                            MetroCallSession.setMuted(!MetroCallSession.state.value.audio.muted)
                        },
                        onMinimize = { finish() },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (!MetroCallSession.hasActiveCall()) {
            finish()
            return
        }
        MetroLockscreen.requestSuppress(this, true)
        IncomingCallNotifier.stop(this)
        ActiveCallNotifier.stop(this)
    }

    override fun onResume() {
        super.onResume()
        syncProximityScreen()
    }

    override fun onPause() {
        proximityScreen.setEnabled(false)
        super.onPause()
    }

    override fun onStop() {
        val session = MetroCallSession.currentState()
        if (!session.isEmpty && !isFinishing) {
            val ringing = session.ringingCall
            if (ringing != null && IncomingCallNotifier.needsFullScreenIntent(this)) {
                MetroLockscreen.requestSuppress(this, true)
                IncomingCallNotifier.show(this, ringing)
            } else {
                MetroLockscreen.requestSuppress(this, false)
                session.primaryCall?.let { ActiveCallNotifier.start(this, it) }
            }
        }
        super.onStop()
    }

    override fun onDestroy() {
        proximityWanted = false
        proximityScreen.setEnabled(false)
        MetroCallSession.setOnSessionEndedListener(null)
        super.onDestroy()
    }

    private fun syncProximityScreen() {
        val resumeReady = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        proximityScreen.setEnabled(proximityWanted && resumeReady)
    }

    private fun enableShowWhenLocked() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
    }
}
