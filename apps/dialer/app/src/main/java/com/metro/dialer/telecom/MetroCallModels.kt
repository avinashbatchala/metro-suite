package com.metro.dialer.telecom

import android.net.Uri

/**
 * Live-call domain model.
 *
 * These types are deliberately independent of Android Telecom constants so the UI never has to
 * branch on `Call.STATE_*` / `CallAudioState.ROUTE_*`. [MetroCallSession] maps platform state into
 * these types and owns the authoritative call lifecycle.
 */

enum class MetroCallDirection {
    INCOMING,
    OUTGOING,
    UNKNOWN,
}

enum class MetroCallState {
    NEW,
    CONNECTING,
    DIALING,
    RINGING,
    ACTIVE,
    HOLDING,
    DISCONNECTING,
    DISCONNECTED,
    UNKNOWN,
    ;

    val isConnected: Boolean get() = this == ACTIVE || this == HOLDING
    val isRinging: Boolean get() = this == RINGING
    val isDisconnected: Boolean get() = this == DISCONNECTED || this == DISCONNECTING
}

enum class MetroCallEndpointType {
    EARPIECE,
    SPEAKER,
    BLUETOOTH,
    WIRED_HEADSET,
    HEARING_AID,
    STREAMING,
    UNKNOWN,
}

data class MetroCallEndpoint(
    val id: String,
    val type: MetroCallEndpointType,
    val label: String,
)

data class MetroCallAudioState(
    val muted: Boolean = false,
    val currentEndpoint: MetroCallEndpoint? = null,
    val availableEndpoints: List<MetroCallEndpoint> = emptyList(),
) {
    val hasMultipleEndpoints: Boolean get() = availableEndpoints.size > 1
}

/** Contact presentation resolved directly from [android.provider.ContactsContract.PhoneLookup]. */
data class MetroCallerInfo(
    val displayName: String? = null,
    val phoneLabel: String? = null,
    val photoUri: Uri? = null,
    val contactLookupKey: String? = null,
    val contactId: Long? = null,
    val carrierLabel: String? = null,
)

/**
 * A single real Android Telecom [android.telecom.Call] presented to the Metro UI.
 */
data class MetroTelecomCall(
    val id: String,
    val direction: MetroCallDirection,
    val state: MetroCallState,
    val phoneNumber: String?,
    val displayName: String?,
    val phoneLabel: String?,
    val photoUri: Uri?,
    val contactLookupKey: String?,
    val contactId: Long?,
    val connectTimeMillis: Long?,
    val carrierLabel: String?,
    val canHold: Boolean,
    val canMerge: Boolean,
    val isConference: Boolean,
    val parentId: String?,
    val childIds: List<String>,
) {
    val isIncomingRinging: Boolean
        get() = direction == MetroCallDirection.INCOMING && state.isRinging

    /** Best-effort label for headers and notifications. */
    val primaryLabel: String
        get() = displayName?.takeIf { it.isNotBlank() }
            ?: phoneNumber?.takeIf { it.isNotBlank() }
            ?: "unknown"
}

data class MetroCallSessionState(
    val calls: List<MetroTelecomCall> = emptyList(),
    val primaryCallId: String? = null,
    val ringingCallId: String? = null,
    val heldCallIds: List<String> = emptyList(),
    val conferenceCallId: String? = null,
    val conferenceChildIds: List<String> = emptyList(),
    val canAddCall: Boolean = false,
    val audio: MetroCallAudioState = MetroCallAudioState(),
) {
    val isEmpty: Boolean get() = calls.isEmpty()

    fun call(id: String?): MetroTelecomCall? = calls.firstOrNull { it.id == id }

    val primaryCall: MetroTelecomCall? get() = call(primaryCallId)
    val ringingCall: MetroTelecomCall? get() = call(ringingCallId)
    val activeCall: MetroTelecomCall? get() = calls.firstOrNull { it.state == MetroCallState.ACTIVE }
    val heldCalls: List<MetroTelecomCall> get() = calls.filter { it.state == MetroCallState.HOLDING }
    val conferenceChildren: List<MetroTelecomCall>
        get() = conferenceChildIds.mapNotNull { call(it) }
}
