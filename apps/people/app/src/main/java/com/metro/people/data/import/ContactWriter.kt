package com.metro.people.data.import

import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Event
import android.provider.ContactsContract.CommonDataKinds.Note
import android.provider.ContactsContract.CommonDataKinds.Organization
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.Photo
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal
import android.provider.ContactsContract.CommonDataKinds.Website
import android.provider.ContactsContract.Data
import android.provider.ContactsContract.RawContacts

/**
 * Writes [ImportContact]s into Android's real `ContactsContract` provider as **device-local**
 * contacts (`ACCOUNT_NAME`/`ACCOUNT_TYPE` = null).
 *
 * Each contact becomes one `RawContacts` row plus typed `Data` rows, applied in bounded
 * `ContentProviderOperation` batches. A failing batch falls back to per-contact batches so one bad
 * card cannot lose an entire import, and [write] reports success per contact.
 */
class ContactWriter(private val resolver: ContentResolver) {

    fun write(
        contacts: List<ImportContact>,
        onProgress: (done: Int, total: Int) -> Unit,
    ): List<Boolean> {
        val total = contacts.size
        if (total == 0) return emptyList()
        val success = BooleanArray(total)
        var done = 0
        contacts.chunked(BATCH_CONTACTS).forEach { chunk ->
            val applied = try {
                resolver.applyBatch(ContactsContract.AUTHORITY, buildOperations(chunk))
                true
            } catch (e: Exception) {
                false
            }
            if (applied) {
                for (i in chunk.indices) success[done + i] = true
            } else {
                chunk.forEachIndexed { i, contact ->
                    success[done + i] = try {
                        resolver.applyBatch(ContactsContract.AUTHORITY, buildOperations(listOf(contact)))
                        true
                    } catch (e: Exception) {
                        false
                    }
                }
            }
            done += chunk.size
            onProgress(done, total)
        }
        return success.toList()
    }

    /** Exposed for unit tests: one RawContacts insert per contact with correct back references. */
    fun buildOperations(contacts: List<ImportContact>): ArrayList<ContentProviderOperation> {
        val ops = ArrayList<ContentProviderOperation>()
        contacts.forEach { appendContact(ops, it) }
        return ops
    }

    private fun appendContact(ops: ArrayList<ContentProviderOperation>, contact: ImportContact) {
        val backRef = ops.size
        ops.add(
            ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
                .withValue(RawContacts.ACCOUNT_NAME, null)
                .withValue(RawContacts.ACCOUNT_TYPE, null)
                .build(),
        )

        appendName(ops, contact, backRef)
        contact.phones.forEach { appendPhone(ops, it, backRef) }
        contact.emails.forEach { appendEmail(ops, it, backRef) }
        contact.addresses.forEach { appendAddress(ops, it, backRef) }
        appendOrganization(ops, contact, backRef)
        appendBirthday(ops, contact, backRef)
        appendNote(ops, contact, backRef)
        appendWebsites(ops, contact, backRef)
        appendPhoto(ops, contact, backRef)
    }

    private fun appendName(ops: ArrayList<ContentProviderOperation>, contact: ImportContact, backRef: Int) {
        val builder = dataInsert(StructuredName.CONTENT_ITEM_TYPE, backRef)
        var added = false
        contact.prefix?.let { builder.withValue(StructuredName.PREFIX, it); added = true }
        contact.givenName?.let { builder.withValue(StructuredName.GIVEN_NAME, it); added = true }
        contact.middleName?.let { builder.withValue(StructuredName.MIDDLE_NAME, it); added = true }
        contact.familyName?.let { builder.withValue(StructuredName.FAMILY_NAME, it); added = true }
        contact.suffix?.let { builder.withValue(StructuredName.SUFFIX, it); added = true }
        if (!added) {
            val fallback = contact.resolvedDisplayName()
            if (fallback.isNotEmpty()) {
                builder.withValue(StructuredName.GIVEN_NAME, fallback)
                added = true
            }
        }
        if (added) ops.add(builder.build())
    }

    private fun appendPhone(ops: ArrayList<ContentProviderOperation>, phone: ImportPhone, backRef: Int) {
        val builder = dataInsert(Phone.CONTENT_ITEM_TYPE, backRef)
            .withValue(Phone.NUMBER, phone.number)
            .withValue(Phone.TYPE, phoneType(phone.label))
        if (phone.label == ContactLabel.CUSTOM) {
            builder.withValue(Phone.LABEL, customLabel(phone.customLabel))
        }
        ops.add(builder.build())
    }

    private fun appendEmail(ops: ArrayList<ContentProviderOperation>, email: ImportEmail, backRef: Int) {
        val builder = dataInsert(Email.CONTENT_ITEM_TYPE, backRef)
            .withValue(Email.ADDRESS, email.address)
            .withValue(Email.TYPE, emailType(email.label))
        if (email.label == ContactLabel.CUSTOM) {
            builder.withValue(Email.LABEL, customLabel(email.customLabel))
        }
        ops.add(builder.build())
    }

    private fun appendAddress(ops: ArrayList<ContentProviderOperation>, address: ImportAddress, backRef: Int) {
        val builder = dataInsert(StructuredPostal.CONTENT_ITEM_TYPE, backRef)
            .withValue(StructuredPostal.TYPE, addressType(address.label))
        address.formatted?.let { builder.withValue(StructuredPostal.FORMATTED_ADDRESS, it) }
        address.street?.let { builder.withValue(StructuredPostal.STREET, it) }
        address.city?.let { builder.withValue(StructuredPostal.CITY, it) }
        address.region?.let { builder.withValue(StructuredPostal.REGION, it) }
        address.postalCode?.let { builder.withValue(StructuredPostal.POSTCODE, it) }
        address.country?.let { builder.withValue(StructuredPostal.COUNTRY, it) }
        if (address.label == ContactLabel.CUSTOM) {
            builder.withValue(StructuredPostal.LABEL, customLabel(address.customLabel))
        }
        ops.add(builder.build())
    }

    private fun appendOrganization(ops: ArrayList<ContentProviderOperation>, contact: ImportContact, backRef: Int) {
        if (contact.organization == null && contact.department == null && contact.jobTitle == null) return
        val builder = dataInsert(Organization.CONTENT_ITEM_TYPE, backRef)
        contact.organization?.let { builder.withValue(Organization.COMPANY, it) }
        contact.department?.let { builder.withValue(Organization.DEPARTMENT, it) }
        contact.jobTitle?.let { builder.withValue(Organization.TITLE, it) }
        ops.add(builder.build())
    }

    private fun appendBirthday(ops: ArrayList<ContentProviderOperation>, contact: ImportContact, backRef: Int) {
        val birthday = contact.birthday ?: return
        ops.add(
            dataInsert(Event.CONTENT_ITEM_TYPE, backRef)
                .withValue(Event.START_DATE, birthday)
                .withValue(Event.TYPE, Event.TYPE_BIRTHDAY)
                .build(),
        )
    }

    private fun appendNote(ops: ArrayList<ContentProviderOperation>, contact: ImportContact, backRef: Int) {
        val note = contact.note ?: return
        ops.add(
            dataInsert(Note.CONTENT_ITEM_TYPE, backRef)
                .withValue(Note.NOTE, note.take(MAX_NOTE_CHARS))
                .build(),
        )
    }

    private fun appendWebsites(ops: ArrayList<ContentProviderOperation>, contact: ImportContact, backRef: Int) {
        contact.websites.forEach { url ->
            ops.add(
                dataInsert(Website.CONTENT_ITEM_TYPE, backRef)
                    .withValue(Website.URL, url)
                    .withValue(Website.TYPE, Website.TYPE_HOMEPAGE)
                    .build(),
            )
        }
    }

    private fun appendPhoto(ops: ArrayList<ContentProviderOperation>, contact: ImportContact, backRef: Int) {
        val bytes = contact.photoBytes ?: return
        if (bytes.isEmpty() || bytes.size > MAX_PHOTO_BYTES) return
        ops.add(
            dataInsert(Photo.CONTENT_ITEM_TYPE, backRef)
                .withValue(Photo.PHOTO, bytes)
                .build(),
        )
    }

    private fun dataInsert(mimeType: String, backRef: Int): ContentProviderOperation.Builder =
        ContentProviderOperation.newInsert(Data.CONTENT_URI)
            .withValue(Data.MIMETYPE, mimeType)
            .withValueBackReference(Data.RAW_CONTACT_ID, backRef)

    private fun phoneType(label: ContactLabel): Int = when (label) {
        ContactLabel.HOME -> Phone.TYPE_HOME
        ContactLabel.WORK -> Phone.TYPE_WORK
        ContactLabel.MOBILE -> Phone.TYPE_MOBILE
        ContactLabel.FAX -> Phone.TYPE_FAX_WORK
        ContactLabel.PAGER -> Phone.TYPE_PAGER
        ContactLabel.OTHER -> Phone.TYPE_OTHER
        ContactLabel.CUSTOM -> Phone.TYPE_CUSTOM
    }

    private fun emailType(label: ContactLabel): Int = when (label) {
        ContactLabel.HOME -> Email.TYPE_HOME
        ContactLabel.WORK -> Email.TYPE_WORK
        ContactLabel.MOBILE -> Email.TYPE_MOBILE
        ContactLabel.OTHER -> Email.TYPE_OTHER
        else -> Email.TYPE_CUSTOM
    }

    private fun addressType(label: ContactLabel): Int = when (label) {
        ContactLabel.HOME -> StructuredPostal.TYPE_HOME
        ContactLabel.WORK -> StructuredPostal.TYPE_WORK
        ContactLabel.OTHER -> StructuredPostal.TYPE_OTHER
        else -> StructuredPostal.TYPE_CUSTOM
    }

    private fun customLabel(value: String?): String =
        value?.trim()?.takeIf { it.isNotEmpty() } ?: "other"

    companion object {
        const val BATCH_CONTACTS = 50
        private const val MAX_NOTE_CHARS = 10_000
        private const val MAX_PHOTO_BYTES = 2 * 1024 * 1024
    }
}
