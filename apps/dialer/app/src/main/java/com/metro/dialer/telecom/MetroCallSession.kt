package com.metro.dialer.telecom

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.VideoProfile
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.metro.dialer.data.ContactsLookup
import com.metro.system.MetroLockscreen

/**
 * The Metro call session.
 *
 * Owns observation of every real [Call] supplied by [MetroInCallService]. The session represents
 * *all* calls simultaneously (multi-call, call waiting, hold, conference) — it never overwrites a
 * call with the next one and never infers connection state from elapsed time.
 *
 * Android Telecom owns the actual call lifecycle; Metro only presents it.
 */
object MetroCallSession {
    private val _state = mutableStateOf(MetroCallSessionState())
    val state: State<MetroCallSessionState> = _state

    private var service: InCallService? = null
    private var appContext: Context? = null
    private var onSessionEnded: (() -> Unit)? = null

    private val calls = LinkedHashMap<String, Call>()
    private val callbacks = HashMap<String, Call.Callback>()
    private val connectTimes = HashMap<String, Long>()
    private val callerInfo = HashMap<String, MetroCallerInfo>()

    private val mainHandler = Handler(Looper.getMainLooper())

    // ---- service binding -------------------------------------------------

    fun bindService(service: InCallService) {
        this.service = service
        appContext = service.applicationContext
    }

    fun unbindService(service: InCallService) {
        if (this.service === service) {
            this.service = null
        }
    }

    fun setOnSessionEndedListener(listener: (() -> Unit)?) {
        onSessionEnded = listener
    }

    // ---- call observation ------------------------------------------------

    fun onCallAdded(call: Call, info: MetroCallerInfo) {
        val id = idOf(call)
        if (calls.containsKey(id)) {
            updateCall(call)
            return
        }
        calls[id] = call
        callerInfo[id] = info
        if (call.state == Call.STATE_ACTIVE) {
            connectTimes.putIfAbsent(id, System.currentTimeMillis())
        }
        val callback = object : Call.Callback() {
            override fun onStateChanged(call: Call, state: Int) = updateCall(call)
            override fun onDetailsChanged(call: Call, details: Call.Details) = updateCall(call)
            override fun onCallDestroyed(call: Call) = Unit
        }
        callbacks[id] = callback
        runCatching { call.registerCallback(callback) }
        syncIncomingRingtone()
        recompute()
    }

    /** Apply asynchronously-resolved caller presentation without disturbing call state. */
    fun applyCallerInfo(call: Call, info: MetroCallerInfo) {
        val id = idOf(call)
        if (!calls.containsKey(id)) return
        callerInfo[id] = info
        recompute()
    }

    fun onCallRemoved(call: Call) {
        val id = idOf(call)
        callbacks.remove(id)?.let { cb -> runCatching { call.unregisterCallback(cb) } }
        calls.remove(id)
        callerInfo.remove(id)
        connectTimes.remove(id)
        if (calls.isEmpty()) {
            IncomingRingtonePlayer.stop()
            appContext?.let {
                MetroLockscreen.requestSuppress(it, false)
                IncomingCallNotifier.stop(it)
                ActiveCallNotifier.stop(it)
            }
            _state.value = MetroCallSessionState()
            onSessionEnded?.invoke()
        } else {
            recompute()
        }
    }

    /** Re-read platform state for [call]; called from per-call callbacks and audio callbacks. */
    fun updateCall(call: Call) {
        val id = idOf(call)
        if (!calls.containsKey(id)) return
        val mapped = mapState(call.state)
        if (mapped == MetroCallState.ACTIVE && !connectTimes.containsKey(id)) {
            connectTimes[id] = System.currentTimeMillis()
        }
        if (mapped == MetroCallState.DISCONNECTED) {
            // Telecom will follow with onCallRemoved; keep presenting the ended call briefly.
        }
        syncIncomingRingtone()
        recompute()
    }

    // ---- audio -----------------------------------------------------------

    fun onLegacyAudioStateChanged(audioState: CallAudioState?) {
        if (audioState == null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
        val mapped = MetroCallAudioMapper.mapLegacy(
            route = audioState.route,
            supportedRouteMask = audioState.supportedRouteMask,
            muted = audioState.isMuted,
        )
        _state.value = _state.value.copy(audio = mapped)
    }

    fun onEndpointsChanged(
        current: MetroCallEndpoint?,
        available: List<MetroCallEndpoint>,
        muted: Boolean,
    ) {
        _state.value = _state.value.copy(
            audio = MetroCallAudioState(
                muted = muted,
                currentEndpoint = current,
                availableEndpoints = available,
            ),
        )
    }

    fun onMuteChanged(muted: Boolean) {
        _state.value = _state.value.copy(audio = _state.value.audio.copy(muted = muted))
    }

    fun refreshCanAddCall() {
        val can = service?.canAddCall() ?: false
        if (can != _state.value.canAddCall) {
            _state.value = _state.value.copy(canAddCall = can)
        }
    }

    // ---- actions ---------------------------------------------------------

    fun answer(callId: String? = null) {
        val call = resolveCall(callId) ?: return
        IncomingRingtonePlayer.stop()
        appContext?.let { IncomingCallNotifier.stop(it) }
        runCatching { call.answer(VideoProfile.STATE_AUDIO_ONLY) }
        updateCall(call)
    }

    fun reject(callId: String? = null, message: String? = null) {
        val call = resolveCall(callId) ?: return
        IncomingRingtonePlayer.stop()
        appContext?.let { IncomingCallNotifier.stop(it) }
        runCatching {
            if (message.isNullOrBlank()) call.reject(false, null) else call.reject(true, message)
        }
        updateCall(call)
    }

    fun end(callId: String? = null, context: Context? = null) {
        val call = resolveCall(callId) ?: return
        IncomingRingtonePlayer.stop()
        runCatching { call.disconnect() }
        updateCall(call)
    }

    fun endAll(context: Context? = null) {
        calls.values.toList().forEach { call ->
            IncomingRingtonePlayer.stop()
            runCatching { call.disconnect() }
        }
    }

    fun hold(callId: String? = null, held: Boolean = true) {
        val call = resolveCall(callId) ?: return
        runCatching { if (held) call.hold() else call.unhold() }
        updateCall(call)
    }

    fun swap() {
        val active = _state.value.activeCall ?: return
        val held = _state.value.heldCalls.firstOrNull() ?: return
        val activePlatform = resolvePlatform(active.id) ?: return
        val heldPlatform = resolvePlatform(held.id) ?: return
        runCatching {
            activePlatform.hold()
            heldPlatform.unhold()
        }
        updateCall(activePlatform)
        updateCall(heldPlatform)
    }

    fun merge() {
        val mergeable = _state.value.calls.filter { it.canMerge && !it.state.isDisconnected }
        if (mergeable.size < 2) return
        val first = resolvePlatform(mergeable[0].id) ?: return
        val second = resolvePlatform(mergeable[1].id) ?: return
        runCatching { first.conference(second) }
        recompute()
    }

    /** Pull one participant out of a conference into a private call. */
    fun splitCall(callId: String) {
        val call = resolvePlatform(callId) ?: return
        runCatching { call.splitFromConference() }
        recompute()
    }

    fun playDtmf(digit: Char, callId: String? = null) {
        val call = resolveCall(callId) ?: return
        runCatching {
            call.playDtmfTone(digit)
            mainHandler.postDelayed({ runCatching { call.stopDtmfTone() } }, 160L)
        }
    }

    fun setMuted(muted: Boolean) {
        service?.setMuted(muted)
        onMuteChanged(muted)
    }

    fun selectAudioEndpoint(endpoint: MetroCallEndpoint) {
        val activeService = service ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            MetroAudioRouteController.requestEndpoint(activeService, endpoint, platformEndpoints)
        } else {
            @Suppress("DEPRECATION")
            activeService.setAudioRoute(legacyRouteFor(endpoint.type))
        }
        refreshCanAddCall()
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private var platformEndpoints: List<android.telecom.CallEndpoint> = emptyList()

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun updatePlatformEndpoints(endpoints: List<android.telecom.CallEndpoint>) {
        platformEndpoints = endpoints
    }

    // ---- state helpers ---------------------------------------------------

    fun hasActiveCall(): Boolean = calls.isNotEmpty()

    fun primaryCall(): MetroTelecomCall? = _state.value.primaryCall

    fun currentState(): MetroCallSessionState = _state.value

    // ---- internals -------------------------------------------------------

    private fun recompute() {
        val mapped = calls.entries.mapNotNull { (id, call) ->
            runCatching { buildModel(id, call) }.getOrNull()
        }
        val selection = MetroCallSelectionLogic.select(mapped)
        _state.value = _state.value.copy(
            calls = mapped,
            primaryCallId = selection.primaryCallId,
            ringingCallId = selection.ringingCallId,
            heldCallIds = selection.heldCallIds,
            conferenceCallId = selection.conferenceCallId,
            conferenceChildIds = selection.conferenceChildIds,
            canAddCall = service?.canAddCall() ?: false,
        )
    }

    private fun buildModel(id: String, call: Call): MetroTelecomCall {
        val details = call.details
        val info = callerInfo[id] ?: MetroCallerInfo()
        val number = details.handle?.schemeSpecificPart?.trim()?.takeIf { it.isNotEmpty() }
        val direction = resolveDirection(call)
        val children = runCatching { call.children }.getOrNull().orEmpty()
        val parent = runCatching { call.parent }.getOrNull()
        val capabilities = details.callCapabilities
        val canHold = (capabilities and Call.Details.CAPABILITY_SUPPORT_HOLD) != 0 ||
            (capabilities and Call.Details.CAPABILITY_HOLD) != 0
        val canMerge = (capabilities and Call.Details.CAPABILITY_MERGE_CONFERENCE) != 0

        return MetroTelecomCall(
            id = id,
            direction = direction,
            state = mapState(call.state),
            phoneNumber = number,
            displayName = info.displayName ?: details.callerDisplayName?.toString(),
            phoneLabel = info.phoneLabel,
            photoUri = info.photoUri,
            contactLookupKey = info.contactLookupKey,
            contactId = info.contactId,
            connectTimeMillis = connectTimes[id],
            carrierLabel = info.carrierLabel,
            canHold = canHold,
            canMerge = canMerge,
            isConference = children.isNotEmpty(),
            parentId = parent?.let { idOf(it) },
            childIds = children.map { idOf(it) },
        )
    }

    private fun resolveCall(callId: String?): Call? {
        val id = callId ?: _state.value.primaryCallId ?: return null
        return resolvePlatform(id)
    }

    private fun resolvePlatform(id: String): Call? = calls[id]

    private fun mapState(state: Int): MetroCallState = when (state) {        Call.STATE_NEW -> MetroCallState.NEW
        Call.STATE_CONNECTING -> MetroCallState.CONNECTING
        Call.STATE_DIALING -> MetroCallState.DIALING
        Call.STATE_RINGING -> MetroCallState.RINGING
        Call.STATE_ACTIVE -> MetroCallState.ACTIVE
        Call.STATE_HOLDING -> MetroCallState.HOLDING
        Call.STATE_DISCONNECTING -> MetroCallState.DISCONNECTING
        Call.STATE_DISCONNECTED -> MetroCallState.DISCONNECTED
        Call.STATE_PULLING_CALL -> MetroCallState.CONNECTING
        Call.STATE_AUDIO_PROCESSING -> MetroCallState.ACTIVE
        Call.STATE_SELECT_PHONE_ACCOUNT -> MetroCallState.NEW
        else -> MetroCallState.UNKNOWN
    }

    private fun idOf(call: Call): String = "call-${System.identityHashCode(call)}"

    private fun syncIncomingRingtone() {
        val context = appContext ?: return
        val ringing = calls.values.any { call ->
            call.state == Call.STATE_RINGING && resolveDirection(call) == MetroCallDirection.INCOMING
        }
        if (ringing) IncomingRingtonePlayer.start(context) else IncomingRingtonePlayer.stop()
    }

    private fun resolveDirection(call: Call): MetroCallDirection {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return when (call.details.callDirection) {
                Call.Details.DIRECTION_INCOMING -> MetroCallDirection.INCOMING
                Call.Details.DIRECTION_OUTGOING -> MetroCallDirection.OUTGOING
                else -> if (call.state == Call.STATE_RINGING) {
                    MetroCallDirection.INCOMING
                } else {
                    MetroCallDirection.UNKNOWN
                }
            }
        }
        return if (call.state == Call.STATE_RINGING) {
            MetroCallDirection.INCOMING
        } else {
            MetroCallDirection.UNKNOWN
        }
    }

    private fun legacyRouteFor(type: MetroCallEndpointType): Int = when (type) {
        MetroCallEndpointType.SPEAKER -> CallAudioState.ROUTE_SPEAKER
        MetroCallEndpointType.BLUETOOTH -> CallAudioState.ROUTE_BLUETOOTH
        MetroCallEndpointType.WIRED_HEADSET -> CallAudioState.ROUTE_WIRED_HEADSET
        else -> CallAudioState.ROUTE_EARPIECE
    }

    /** Called by the background notifier; keeps the process-local app context warm. */
    fun attachContext(context: Context) {
        appContext = appContext ?: context.applicationContext
    }

    /** Resolve caller presentation directly via ContactsContract (no call-history scan). */
    fun resolveCallerPresentation(context: Context, number: String?): MetroCallerInfo {
        val trimmed = number?.trim().orEmpty()
        if (trimmed.isEmpty()) return MetroCallerInfo()
        return runCatching { ContactsLookup(context).resolveCaller(trimmed) }
            .getOrElse { MetroCallerInfo() }
    }
}
