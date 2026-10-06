package com.metro.dialer.telecom

/**
 * Deterministic selection of which call is primary / ringing / held / conference.
 *
 * Extracted from [MetroCallSession] so the priority rules are unit-testable without platform
 * [android.telecom.Call] objects.
 *
 * Priority:
 *  - ringing incoming call → primary
 *  - else active call → primary
 *  - else most recently added non-disconnected call → primary
 */
object MetroCallSelectionLogic {

    data class Selection(
        val primaryCallId: String?,
        val ringingCallId: String?,
        val heldCallIds: List<String>,
        val conferenceCallId: String?,
        val conferenceChildIds: List<String>,
    )

    fun select(calls: List<MetroTelecomCall>): Selection {
        if (calls.isEmpty()) return Selection(null, null, emptyList(), null, emptyList())
        val ringing = calls.firstOrNull { it.isIncomingRinging }
        val active = calls.firstOrNull { it.state == MetroCallState.ACTIVE }
        val primary = ringing
            ?: active
            ?: calls.lastOrNull { !it.state.isDisconnected }
            ?: calls.lastOrNull()
        val conference = calls.firstOrNull { it.isConference } ?: calls.firstOrNull { it.childIds.isNotEmpty() }
        return Selection(
            primaryCallId = primary?.id,
            ringingCallId = ringing?.id,
            heldCallIds = calls.filter { it.state == MetroCallState.HOLDING }.map { it.id },
            conferenceCallId = conference?.id,
            conferenceChildIds = conference?.childIds.orEmpty(),
        )
    }
}
