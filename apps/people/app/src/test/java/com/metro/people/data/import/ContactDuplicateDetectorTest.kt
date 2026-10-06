package com.metro.people.data.import

import org.junit.Assert.assertEquals
import org.junit.Test

class ContactDuplicateDetectorTest {

    private val existing = ExistingContactIndex(
        phoneDigits = listOf("49555100"),
        emails = setOf("known.person@example.com"),
        names = setOf("jane doe"),
    )

    private val detector = ContactDuplicateDetector(existing)

    private fun contact(
        name: String? = null,
        phones: List<String> = emptyList(),
        emails: List<String> = emptyList(),
    ) = ImportContact(
        displayName = name,
        givenName = null,
        middleName = null,
        familyName = null,
        prefix = null,
        suffix = null,
        phones = phones.map { ImportPhone(it) },
        emails = emails.map { ImportEmail(it) },
    )

    @Test
    fun `identical phone is an exact duplicate`() {
        assertEquals(DuplicateClass.EXACT_DUPLICATE, detector.classify(contact(phones = listOf("+49 555 100"))))
    }

    @Test
    fun `formatted variant of the same phone is an exact duplicate`() {
        assertEquals(DuplicateClass.EXACT_DUPLICATE, detector.classify(contact(phones = listOf("+49-555-100"))))
    }

    @Test
    fun `email match ignores case`() {
        assertEquals(
            DuplicateClass.EXACT_DUPLICATE,
            detector.classify(contact(emails = listOf("Known.Person@Example.com"))),
        )
    }

    @Test
    fun `same name only is a possible duplicate`() {
        assertEquals(
            DuplicateClass.POSSIBLE_DUPLICATE,
            detector.classify(contact(name = "Jane   Doe")),
        )
    }

    @Test
    fun `unrelated contact is new`() {
        assertEquals(
            DuplicateClass.NEW,
            detector.classify(contact(name = "Sam Smith", phones = listOf("+1 555 9999"))),
        )
    }
}
