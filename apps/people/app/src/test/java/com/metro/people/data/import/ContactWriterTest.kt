package com.metro.people.data.import

import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Event
import android.provider.ContactsContract.CommonDataKinds.Organization
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.Photo
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal
import android.provider.ContactsContract.CommonDataKinds.Website
import android.provider.ContactsContract.Data
import android.provider.ContactsContract.RawContacts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ContactWriterTest {

    private fun writer(): ContactWriter =
        ContactWriter(RuntimeEnvironment.getApplication().contentResolver)

    private fun valuesOf(operation: ContentProviderOperation): ContentValues {
        val field = ContentProviderOperation::class.java.getDeclaredField("mValues")
        field.isAccessible = true
        return field.get(operation) as ContentValues
    }

    private fun backReferencesOf(operation: ContentProviderOperation): ContentValues {
        val field = ContentProviderOperation::class.java.getDeclaredField("mValuesBackReferences")
        field.isAccessible = true
        return field.get(operation) as ContentValues
    }

    /** Avoids `getUri()`/`isInsert()`, which are absent from some Robolectric runtime jars. */
    private fun uriOf(operation: ContentProviderOperation): Uri? {
        val field = ContentProviderOperation::class.java.getDeclaredField("mUri")
        field.isAccessible = true
        return field.get(operation) as Uri?
    }

    private fun isInsert(operation: ContentProviderOperation): Boolean {
        val field = ContentProviderOperation::class.java.getDeclaredField("mType")
        field.isAccessible = true
        return (field.get(operation) as Int) == TYPE_INSERT
    }

    private fun fullContact() = ImportContact(
        displayName = "Test Person",
        givenName = "Test",
        middleName = null,
        familyName = "Person",
        prefix = null,
        suffix = null,
        phones = listOf(
            ImportPhone("+1 555 0001", ContactLabel.MOBILE),
            ImportPhone("+1 555 0002", ContactLabel.WORK),
        ),
        emails = listOf(ImportEmail("test@example.com", ContactLabel.WORK)),
        addresses = listOf(
            ImportAddress(
                formatted = "1 Test St, Testville",
                street = "1 Test St",
                city = "Testville",
                region = "TS",
                postalCode = "00000",
                country = "Testland",
                label = ContactLabel.HOME,
            ),
        ),
        organization = "Test Co",
        department = "QA",
        jobTitle = "Tester",
        birthday = "1990-01-01",
        note = "A note",
        websites = listOf("https://example.com"),
        photoBytes = byteArrayOf(1, 2, 3, 4),
    )

    @Test
    fun `one raw contact per person with typed data rows`() {
        val ops = writer().buildOperations(listOf(fullContact()))

        val rawInserts = ops.filter { isInsert(it) && uriOf(it) == RawContacts.CONTENT_URI }
        assertEquals(1, rawInserts.size)

        val dataOps = ops.filter { isInsert(it) && uriOf(it) == Data.CONTENT_URI }
        // name + 2 phones + email + address + organization + birthday + note + website + photo
        assertEquals(10, dataOps.size)

        val mimeCounts = dataOps.map { valuesOf(it).getAsString(Data.MIMETYPE) }.groupingBy { it }.eachCount()
        assertEquals(2, mimeCounts[Phone.CONTENT_ITEM_TYPE])
        assertEquals(1, mimeCounts[Email.CONTENT_ITEM_TYPE])
        assertEquals(1, mimeCounts[StructuredName.CONTENT_ITEM_TYPE])
        assertEquals(1, mimeCounts[StructuredPostal.CONTENT_ITEM_TYPE])
        assertEquals(1, mimeCounts[Organization.CONTENT_ITEM_TYPE])
        assertEquals(1, mimeCounts[Event.CONTENT_ITEM_TYPE])
        assertEquals(1, mimeCounts[Photo.CONTENT_ITEM_TYPE])
        assertEquals(1, mimeCounts[Website.CONTENT_ITEM_TYPE])
    }

    @Test
    fun `data rows back-reference their raw contact`() {
        val ops = writer().buildOperations(listOf(fullContact()))
        val dataOps = ops.filter { uriOf(it) == Data.CONTENT_URI }
        assertTrue(dataOps.isNotEmpty())
        dataOps.forEach { operation ->
            assertEquals(0, backReferencesOf(operation).getAsInteger(Data.RAW_CONTACT_ID))
        }
    }

    @Test
    fun `multiple phones map to the correct Android types`() {
        val ops = writer().buildOperations(listOf(fullContact()))
        val phoneOps = ops.filter { valuesOf(it).getAsString(Data.MIMETYPE) == Phone.CONTENT_ITEM_TYPE }
        val types = phoneOps.map { valuesOf(it).getAsInteger(Phone.TYPE) }.toSet()
        assertEquals(setOf(Phone.TYPE_MOBILE, Phone.TYPE_WORK), types)
    }

    @Test
    fun `custom phone label is written`() {
        val contact = fullContact().copy(
            phones = listOf(
                ImportPhone("+1 555 0003", ContactLabel.CUSTOM, customLabel = "Direct Line"),
            ),
        )
        val ops = writer().buildOperations(listOf(contact))
        val phoneValues = ops.first { valuesOf(it).getAsString(Data.MIMETYPE) == Phone.CONTENT_ITEM_TYPE }
            .let { valuesOf(it) }
        assertEquals(Phone.TYPE_CUSTOM, phoneValues.getAsInteger(Phone.TYPE))
        assertEquals("Direct Line", phoneValues.getAsString(Phone.LABEL))
    }

    @Test
    fun `birthday maps to an event row`() {
        val ops = writer().buildOperations(listOf(fullContact()))
        val eventValues = ops.first { valuesOf(it).getAsString(Data.MIMETYPE) == Event.CONTENT_ITEM_TYPE }
            .let { valuesOf(it) }
        assertEquals("1990-01-01", eventValues.getAsString(Event.START_DATE))
        assertEquals(Event.TYPE_BIRTHDAY, eventValues.getAsInteger(Event.TYPE))
    }

    @Test
    fun `no data rows are written for an empty contact beyond its raw row`() {
        val empty = ImportContact(
            displayName = null,
            givenName = null,
            middleName = null,
            familyName = null,
            prefix = null,
            suffix = null,
        )
        val ops = writer().buildOperations(listOf(empty))
        assertEquals(1, ops.size)
        assertEquals(RawContacts.CONTENT_URI, uriOf(ops[0]))
    }

    @Test
    fun `photo bytes are attached when present`() {
        val ops = writer().buildOperations(listOf(fullContact()))
        val photoOp = ops.firstOrNull { valuesOf(it).getAsString(Data.MIMETYPE) == Photo.CONTENT_ITEM_TYPE }
        assertNotNull(photoOp)
        assertNotNull(valuesOf(photoOp!!).getAsByteArray(Photo.PHOTO))
    }

    private companion object {
        const val TYPE_INSERT = 1
    }
}
