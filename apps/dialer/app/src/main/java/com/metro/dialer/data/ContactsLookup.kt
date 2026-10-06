package com.metro.dialer.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.ContactsContract
import com.metro.dialer.telecom.MetroCallerInfo

/**
 * ContactsContract access. Resolves caller presentation directly via
 * [ContactsContract.PhoneLookup] — never by scanning call history.
 */
class ContactsLookup(
    private val context: Context,
) {
    /** All phone-capable contacts for the smart-dial cache and choosers. */
    fun loadPhoneContacts(limit: Int = 2000): List<ContactSuggestion> {
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone._ID,
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.LABEL,
        )
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        ) ?: return emptyList()

        cursor.use {
            val seen = linkedSetOf<String>()
            val contacts = mutableListOf<ContactSuggestion>()
            while (cursor.moveToNext() && contacts.size < limit) {
                val name = cursor.getString(0)?.trim().orEmpty()
                val number = cursor.getString(1)?.trim().orEmpty()
                if (number.isEmpty()) continue
                val normalized = DialerCallLogic.normalizeNumber(number)
                val displayName = name.ifEmpty { DialerCallLogic.formatDisplayNumber(number) }
                val key = "$normalized|$displayName"
                if (!seen.add(key)) continue
                contacts.add(
                    ContactSuggestion(
                        displayName = displayName,
                        phoneNumber = number,
                        normalizedNumber = normalized,
                        contactLookupKey = cursor.getString(4),
                        contactId = cursor.getLong(3),
                        phoneDataId = cursor.getLong(2),
                        phoneLabel = formatLabel(
                            cursor.getInt(6),
                            cursor.getString(7),
                        ),
                        photoUri = cursor.getString(5)?.let { runCatching { Uri.parse(it) }.getOrNull() },
                    ),
                )
            }
            return contacts
        }
    }

    /** Resolve presentation for a live/incoming call number. */
    fun resolveCaller(phoneNumber: String?): MetroCallerInfo {
        val trimmed = phoneNumber?.trim().orEmpty()
        if (trimmed.isEmpty()) return MetroCallerInfo()
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(trimmed),
        )
        val projection = arrayOf(
            ContactsContract.PhoneLookup.DISPLAY_NAME,
            ContactsContract.PhoneLookup.LOOKUP_KEY,
            ContactsContract.PhoneLookup._ID,
            ContactsContract.PhoneLookup.PHOTO_URI,
            ContactsContract.PhoneLookup.TYPE,
            ContactsContract.PhoneLookup.LABEL,
        )
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return MetroCallerInfo(
                    displayName = cursor.getString(0),
                    contactLookupKey = cursor.getString(1),
                    contactId = cursor.getLong(2),
                    photoUri = cursor.getString(3)?.let { runCatching { Uri.parse(it) }.getOrNull() },
                    phoneLabel = formatLabel(cursor.getInt(4), cursor.getString(5)),
                )
            }
        }
        return MetroCallerInfo()
    }

    /** Contacts with all their numbers, for the speed-dial chooser and Save-number flow. */
    fun loadContactsForChooser(limit: Int = 2000): List<PhoneContact> {
        val suggestions = loadPhoneContacts(limit)
        return suggestions
            .groupBy { it.contactId ?: -1L }
            .mapNotNull { (contactId, numbers) ->
                val first = numbers.first()
                PhoneContact(
                    contactId = contactId,
                    lookupKey = first.contactLookupKey,
                    displayName = first.displayName,
                    numbers = numbers.map { number ->
                        PhoneContactNumber(
                            dataId = number.phoneDataId ?: -1L,
                            number = number.phoneNumber,
                            normalizedNumber = number.normalizedNumber,
                            label = number.phoneLabel,
                            type = 0,
                        )
                    },
                )
            }
            .sortedBy { it.displayName.lowercase() }
    }

    /** Resolve a contacts provider id for pin-to-Start secondary tiles. */
    fun resolveContactId(phoneNumber: String): Long? {
        val normalized = DialerCallLogic.normalizeNumber(phoneNumber)
        if (normalized.isEmpty()) return null
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(normalized),
        )
        context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) return cursor.getLong(0)
            }
        return null
    }

    fun resolvePhotoUri(phoneNumber: String): Uri? = resolveCaller(phoneNumber).photoUri

    fun loadContactPhoto(phoneNumber: String): Bitmap? {
        val photoUri = resolvePhotoUri(phoneNumber) ?: return null
        return runCatching {
            context.contentResolver.openInputStream(photoUri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }.getOrNull()
    }

    fun loadContactPhotoByUri(uri: Uri): Bitmap? = runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        }
    }.getOrNull()

    private fun formatLabel(type: Int, customLabel: String?): String? {
        if (type <= 0) return customLabel?.takeIf { it.isNotBlank() }
        return ContactsContract.CommonDataKinds.Phone.getTypeLabel(
            context.resources,
            type,
            customLabel,
        ).toString().lowercase()
    }
}
