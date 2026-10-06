package com.metro.dialer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.metro.dialer.data.ContactsLookup
import com.metro.dialer.data.DialerCallLogic
import com.metro.dialer.data.SpeedDialEntry
import com.metro.dialer.data.SpeedDialStore
import com.metro.system.MetroIntents

/**
 * Receives [MetroIntents.ACTION_ADD_SPEED_DIAL] from People (and other Metro apps).
 *
 * Guarded by the signature-level [MetroIntents.PERMISSION_INTERNAL] permission (declared in the
 * manifest) so arbitrary third-party apps cannot inject speed-dial entries.
 */
class AddSpeedDialReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != MetroIntents.ACTION_ADD_SPEED_DIAL) return
        val phoneNumber = intent.getStringExtra(MetroIntents.EXTRA_PHONE_NUMBER)?.trim().orEmpty()
        if (phoneNumber.isEmpty()) return
        val displayName = intent.getStringExtra(MetroIntents.EXTRA_DISPLAY_NAME)
            ?.trim()
            .orEmpty()
            .ifEmpty { DialerCallLogic.formatDisplayNumber(phoneNumber) }

        var lookupKey = intent.getStringExtra(MetroIntents.EXTRA_CONTACT_LOOKUP_KEY)
        val contactId = intent.getLongExtra(MetroIntents.EXTRA_CONTACT_ID, -1L).takeIf { it >= 0 }
        if (lookupKey == null && contactId != null) {
            lookupKey = runCatching { resolveLookupKey(context, contactId) }.getOrNull()
        }

        val store = SpeedDialStore(context.applicationContext)
        store.add(
            SpeedDialEntry(
                id = System.currentTimeMillis().toString(),
                displayName = displayName,
                phoneNumber = phoneNumber,
                contactLookupKey = lookupKey,
                normalizedNumberFallback = DialerCallLogic.normalizeNumber(phoneNumber),
            ),
        )
    }

    private fun resolveLookupKey(context: Context, contactId: Long): String? {
        val uri = android.content.ContentUris.withAppendedId(
            android.provider.ContactsContract.Contacts.CONTENT_URI,
            contactId,
        )
        context.contentResolver.query(
            uri,
            arrayOf(android.provider.ContactsContract.Contacts.LOOKUP_KEY),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getString(0)
        }
        return null
    }
}
