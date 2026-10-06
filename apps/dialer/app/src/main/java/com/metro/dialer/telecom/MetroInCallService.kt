package com.metro.dialer.telecom

import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.CallEndpoint
import android.telecom.InCallService
import android.telecom.TelecomManager
import androidx.annotation.RequiresApi
import com.metro.dialer.InCallActivity
import com.metro.dialer.data.ContactsLookup

/**
 * Owns Telecom call observation for the Metro default dialer.
 *
 * Telecom delivers every real `Call` here. We build the Metro session, resolve caller presentation
 * directly from ContactsContract (never by scanning call history), and open the WP8.1 call UI.
 */
class MetroInCallService : InCallService() {

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        MetroCallSession.bindService(this)
    }

    override fun onDestroy() {
        MetroCallSession.unbindService(this)
        super.onDestroy()
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        val number = call.details.handle?.schemeSpecificPart?.trim()
        val placeholder = MetroCallerInfo(carrierLabel = resolveCarrierLabel(call))
        MetroCallSession.onCallAdded(call, placeholder)

        Thread {
            val info = runCatching { ContactsLookup(applicationContext).resolveCaller(number) }
                .getOrDefault(MetroCallerInfo())
                .copy(carrierLabel = resolveCarrierLabel(call))
            mainHandler.post { MetroCallSession.applyCallerInfo(call, info) }
        }.apply { name = "metro-caller-lookup" }.start()

        if (call.state != Call.STATE_DISCONNECTED) {
            openCallUi(call)
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        MetroCallSession.onCallRemoved(call)
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        super.onCallAudioStateChanged(audioState)
        MetroCallSession.onLegacyAudioStateChanged(audioState)
    }

    override fun onBringToForeground(showDialpad: Boolean) {
        super.onBringToForeground(showDialpad)
        if (MetroCallSession.hasActiveCall()) {
            startActivity(
                Intent(this, InCallActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                },
            )
        }
    }

    override fun onSilenceRinger() {
        super.onSilenceRinger()
        // WP8.1 ringer silence is handled by the user-facing Ignore action; stopping the
        // Metro ringtone here mirrors what Telecom expects when the ringer is silenced.
        IncomingRingtonePlayer.stop()
    }

    override fun onCanAddCallChanged(canAddCall: Boolean) {
        super.onCanAddCallChanged(canAddCall)
        MetroCallSession.refreshCanAddCall()
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCallEndpointChanged(endpoint: CallEndpoint) {
        super.onCallEndpointChanged(endpoint)
        publishAudioState()
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onAvailableCallEndpointsChanged(endpoints: List<CallEndpoint>) {
        super.onAvailableCallEndpointsChanged(endpoints)
        availableEndpoints = endpoints
        MetroCallSession.updatePlatformEndpoints(endpoints)
        publishAudioState()
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onMuteStateChanged(isMuted: Boolean) {
        super.onMuteStateChanged(isMuted)
        MetroCallSession.onMuteChanged(isMuted)
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun publishAudioState() {
        val mapped = com.metro.dialer.telecom.MetroCallAudioMapper.mapEndpoints(
            current = currentCallEndpoint,
            available = availableEndpoints,
            muted = MetroCallSession.state.value.audio.muted,
        )
        MetroCallSession.onEndpointsChanged(mapped.currentEndpoint, mapped.availableEndpoints, mapped.muted)
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private var availableEndpoints: List<CallEndpoint> = emptyList()

    /**
     * Show the Metro call UI.
     *
     * - Unlocked / interactive: start [InCallActivity] directly (no Android heads-up).
     * - Locked / screen-off: post the full-screen-intent notification so the activity can appear.
     */
    private fun openCallUi(call: Call) {
        val isIncoming = isIncomingCall(call) && call.state != Call.STATE_ACTIVE
        if (isIncoming) {
            com.metro.system.MetroLockscreen.requestSuppress(this, true)
        }
        val needsFsi = isIncoming && IncomingCallNotifier.needsFullScreenIntent(this)
        if (needsFsi) {
            MetroCallSession.primaryCall()?.let { IncomingCallNotifier.show(this, it) }
        } else {
            runCatching {
                startActivity(
                    Intent(this, InCallActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    },
                )
            }
        }
    }

    private fun isIncomingCall(call: Call): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return call.details.callDirection == Call.Details.DIRECTION_INCOMING
        }
        return call.state == Call.STATE_RINGING
    }

    private fun resolveCarrierLabel(call: Call): String? {
        val handle = call.details.accountHandle ?: return null
        val telecom = getSystemService(TelecomManager::class.java) ?: return null
        return runCatching {
            telecom.getPhoneAccount(handle)?.label?.toString()?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }
}
