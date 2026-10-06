package com.metro.dialer.telecom

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.PhoneNumberUtils
import androidx.core.content.ContextCompat

/**
 * The single outgoing-call gateway.
 *
 * Real cellular/PSTN calls always go through [TelecomManager.placeCall] to the enabled SIM/carrier
 * account — Metro never routes them through its own connection provider. When Metro is not the
 * default dialer (or lacks CALL_PHONE), the intent is handed to the system default dialer instead.
 */
object MetroTelecomBridge {

    fun isDefaultDialer(context: Context): Boolean {
        val telecom = context.getSystemService(TelecomManager::class.java) ?: return false
        return telecom.defaultDialerPackage == context.packageName
    }

    fun hasCallPhonePermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED

    fun hasReadPhoneStatePermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
            PackageManager.PERMISSION_GRANTED

    /** Real call-capable SIM/account handles, in platform order. */
    @SuppressLint("MissingPermission")
    fun callCapableAccounts(context: Context): List<PhoneAccountHandle> {
        val telecom = context.getSystemService(TelecomManager::class.java) ?: return emptyList()
        return runCatching { telecom.callCapablePhoneAccounts }.getOrDefault(emptyList())
    }

    fun hasMultipleCallCapableAccounts(context: Context): Boolean =
        callCapableAccounts(context).size > 1

    /** Default account Telecom will use for a plain [TelecomManager.placeCall]. */
    @SuppressLint("MissingPermission")
    fun defaultOutgoingAccount(context: Context): PhoneAccountHandle? {
        val telecom = context.getSystemService(TelecomManager::class.java) ?: return null
        return runCatching { telecom.getDefaultOutgoingPhoneAccount("tel") }.getOrNull()
    }

    fun isEmergencyNumber(number: String): Boolean =
        runCatching { PhoneNumberUtils.isEmergencyNumber(number) }.getOrDefault(false)

    /**
     * Place a call.
     *
     * @param accountHandle explicit SIM selection (multi-SIM chooser). Null lets Telecom pick the
     *   default enabled cellular account.
     */
    @SuppressLint("MissingPermission")
    fun placeOutgoingCall(
        context: Context,
        phoneNumber: String,
        displayName: String? = null,
        accountHandle: PhoneAccountHandle? = null,
    ) {
        val trimmed = phoneNumber.trim()
        if (trimmed.isEmpty()) return

        val canPlaceDirectly = isDefaultDialer(context) && hasCallPhonePermission(context)
        if (!canPlaceDirectly) {
            handOffToSystemDialer(context, trimmed)
            return
        }

        val uri = Uri.fromParts("tel", trimmed, null)
        val extras = Bundle().apply {
            if (accountHandle != null && !isEmergencyNumber(trimmed)) {
                putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, accountHandle)
            }
        }
        val telecom = context.getSystemService(TelecomManager::class.java) ?: return
        runCatching { telecom.placeCall(uri, extras) }
            .onFailure { handOffToSystemDialer(context, trimmed) }
    }

    /** Hand the number to the current default dialer without ever showing Metro call UI. */
    fun handOffToSystemDialer(context: Context, phoneNumber: String) {
        val trimmed = phoneNumber.trim()
        if (trimmed.isEmpty()) return
        val intent = Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", trimmed, null)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    /** Handle an inbound `tel:` / ACTION_CALL / ACTION_DIAL intent. */
    fun handleCallIntent(
        context: Context,
        uri: Uri,
        displayName: String? = null,
        accountHandle: PhoneAccountHandle? = null,
    ) {
        val number = uri.schemeSpecificPart?.trim().orEmpty()
        if (number.isEmpty()) return
        placeOutgoingCall(context, number, displayName, accountHandle)
    }
}
