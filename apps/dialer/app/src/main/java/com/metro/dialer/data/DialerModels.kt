package com.metro.dialer.data

import android.net.Uri

/**
 * Normalised call-log type. Every supported [android.provider.CallLog.Calls] constant maps to a
 * distinct value; unknown types never silently become OUTGOING.
 */
enum class CallType {
    INCOMING,
    OUTGOING,
    MISSED,
    REJECTED,
    BLOCKED,
    VOICEMAIL,
    ANSWERED_EXTERNALLY,
    UNKNOWN,
    ;

    val isMissedFamily: Boolean
        get() = this == MISSED || this == REJECTED || this == BLOCKED
}

data class CallEntry(
    val id: Long,
    val phoneNumber: String,
    val normalizedNumber: String,
    val type: CallType,
    val timestamp: Long,
    val durationSeconds: Int,
    val contactName: String?,
    val contactLookupKey: String? = null,
    val contactId: Long? = null,
    val presentation: Int = 1,
    val isPrivate: Boolean = false,
    val isUnknown: Boolean = false,
)

data class CallGroup(
    /** Stable identity for the list = caller identity + local calendar date. */
    val id: String,
    /** Contact lookup key or normalized number — used for contact handoff. */
    val identityKey: String,
    val phoneNumber: String,
    val displayName: String,
    val latestType: CallType,
    val latestTimestamp: Long,
    val callCount: Int,
    val calls: List<CallEntry>,
    val contactLookupKey: String? = null,
    val contactId: Long? = null,
    /** Local midnight of the group's calendar day. */
    val dateBucket: Long,
    val isPrivate: Boolean = false,
    val isUnknown: Boolean = false,
) {
    val hasCallableNumber: Boolean get() = phoneNumber.isNotBlank() && !isPrivate && !isUnknown
    val hasContact: Boolean get() = contactLookupKey != null || contactId != null
}

/** A dated section of grouped history rows. */
data class HistorySection(
    val dateBucket: Long,
    val label: String,
    val groups: List<CallGroup>,
)

data class ContactSuggestion(
    val displayName: String,
    val phoneNumber: String,
    val normalizedNumber: String,
    val contactLookupKey: String? = null,
    val contactId: Long? = null,
    val phoneDataId: Long? = null,
    val phoneLabel: String? = null,
    val photoUri: Uri? = null,
)

data class SpeedDialEntry(
    val id: String,
    val displayName: String,
    val phoneNumber: String,
    val contactLookupKey: String? = null,
    val phoneDataId: Long? = null,
    val normalizedNumberFallback: String = "",
) {
    val isResolvableByContact: Boolean
        get() = contactLookupKey != null || phoneDataId != null
}

/** A contact used by the speed-dial add chooser / Save-number flow. */
data class PhoneContact(
    val contactId: Long,
    val lookupKey: String?,
    val displayName: String,
    val numbers: List<PhoneContactNumber>,
) {
    val defaultNumber: PhoneContactNumber? get() = numbers.firstOrNull()
}

data class PhoneContactNumber(
    val dataId: Long,
    val number: String,
    val normalizedNumber: String,
    val label: String?,
    val type: Int,
)
