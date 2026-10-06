package com.metro.people.data.import

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class VCardParserTest {

    private fun parseFixture(name: String): ParseResult =
        VCardParser.parse(javaClass.getResourceAsStream("/vcf/$name")!!)

    private fun success(name: String): ParseResult.Success =
        parseFixture(name) as ParseResult.Success

    @Test
    fun `parses vCard 2_1 with structured name and typed fields`() {
        val result = success("single_v21.vcf")
        assertEquals(1, result.contacts.size)
        val contact = result.contacts.first()

        assertEquals("John Smith", contact.displayName)
        assertEquals("John", contact.givenName)
        assertEquals("Quincy", contact.middleName)
        assertEquals("Smith", contact.familyName)
        assertEquals("Mr.", contact.prefix)
        assertEquals("Jr.", contact.suffix)

        assertEquals(1, contact.phones.size)
        assertEquals("+1 555 0100", contact.phones[0].number)
        assertEquals(ContactLabel.HOME, contact.phones[0].label)

        assertEquals(1, contact.emails.size)
        assertEquals("john.smith@example.com", contact.emails[0].address)
    }

    @Test
    fun `parses vCard 3_0 organization and labels`() {
        val contact = success("single_v30.vcf").contacts.single()

        assertEquals("Alice Brown", contact.displayName)
        assertEquals("Brown", contact.familyName)
        assertEquals("Alice", contact.givenName)
        assertEquals(ContactLabel.MOBILE, contact.phones.single().label)
        assertEquals(ContactLabel.WORK, contact.emails.single().label)
        assertEquals("Example Corp", contact.organization)
        assertEquals("Engineering", contact.department)
        assertEquals("Engineer", contact.jobTitle)
    }

    @Test
    fun `parses vCard 4_0 birthday url and note`() {
        val contact = success("single_v40.vcf").contacts.single()

        assertEquals("Bob Jones", contact.displayName)
        assertEquals("1985-05-04", contact.birthday)
        assertTrue(contact.websites.contains("https://example.com/bob"))
        assertTrue(contact.note!!.contains("conference"))
        assertEquals(ContactLabel.MOBILE, contact.phones.single().label)
        assertEquals("4.0", contact.vcardVersion)
    }

    @Test
    fun `parses multiple contacts in one file`() {
        val result = success("multiple_contacts.vcf")
        assertEquals(3, result.contacts.size)
        assertEquals(3, result.totalCards)
    }

    @Test
    fun `preserves multiple phone numbers and labels`() {
        val contact = success("multiple_phones.vcf").contacts.single()
        assertEquals(4, contact.phones.size)
        assertEquals(
            listOf(ContactLabel.HOME, ContactLabel.WORK, ContactLabel.MOBILE, ContactLabel.FAX),
            contact.phones.map { it.label },
        )
    }

    @Test
    fun `preserves multiple emails`() {
        val contact = success("multiple_emails.vcf").contacts.single()
        assertEquals(2, contact.emails.size)
        assertEquals(ContactLabel.HOME, contact.emails[0].label)
        assertEquals(ContactLabel.WORK, contact.emails[1].label)
    }

    @Test
    fun `maps structured postal addresses`() {
        val contact = success("address.vcf").contacts.single()
        assertEquals(2, contact.addresses.size)
        val home = contact.addresses.first { it.label == ContactLabel.HOME }
        assertEquals("123 Maple Street", home.street)
        assertEquals("Springfield", home.city)
        assertEquals("IL", home.region)
        assertEquals("62704", home.postalCode)
        assertEquals("United States", home.country)
    }

    @Test
    fun `maps organization and website`() {
        val contact = success("organization.vcf").contacts.single()
        assertEquals("Acme Widgets", contact.organization)
        assertEquals("Research and Development", contact.department)
        assertEquals("Senior Analyst", contact.jobTitle)
        assertEquals("https://acme.example.com/kevin", contact.websites.single())
    }

    @Test
    fun `maps birthday`() {
        val contact = success("birthday.vcf").contacts.single()
        assertEquals("1990-02-14", contact.birthday)
    }

    @Test
    fun `imports embedded base64 photo`() {
        val contact = success("photo.vcf").contacts.single()
        assertNotNull(contact.photoBytes)
        assertTrue(contact.photoBytes!!.isNotEmpty())
    }

    @Test
    fun `handles unicode names`() {
        val contact = success("unicode.vcf").contacts.single()
        assertEquals("José Müller", contact.displayName)
        assertEquals("東京商事", contact.organization)
    }

    @Test
    fun `decodes quoted printable vCard 2_1`() {
        val contact = success("quoted_printable.vcf").contacts.single()
        assertEquals("Jörg Müller", contact.displayName)
    }

    @Test
    fun `falls back to structured name when FN missing`() {
        val contact = success("missing_fn.vcf").contacts.single()
        assertEquals("Ngozi Adaeze Okafor", contact.resolvedDisplayName())
    }

    @Test
    fun `skips unusable card but imports the rest and unfolds notes`() {
        val result = success("malformed_one_of_many.vcf")
        assertEquals(3, result.totalCards)
        assertEquals(2, result.contacts.size)
        assertTrue(result.warningCount >= 1)
        val peter = result.contacts.first { it.displayName == "Peter Quinn" }
        assertTrue(peter.note!!.contains("onto a second physical line"))
    }

    @Test
    fun `rejects non vCard content`() {
        val result = parseFixture("invalid.vcf")
        assertTrue(result is ParseResult.Failure)
        assertEquals(ParseFailure.NOT_A_VCARD, (result as ParseResult.Failure).reason)
    }

    @Test
    fun `reports empty input`() {
        val result = VCardParser.parse(ByteArrayInputStream(ByteArray(0)))
        assertTrue(result is ParseResult.Failure)
        assertEquals(ParseFailure.EMPTY_FILE, (result as ParseResult.Failure).reason)
    }
}
