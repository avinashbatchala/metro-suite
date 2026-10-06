package com.metro.people.data.import

import ezvcard.VCard
import ezvcard.io.text.VCardReader
import ezvcard.property.Address
import ezvcard.property.Birthday
import ezvcard.property.Email
import ezvcard.property.Telephone
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.temporal.Temporal

/**
 * Streams a vCard (2.1 / 3.0 / 4.0) input stream and normalizes each card into [ImportContact].
 *
 * The input is treated as untrusted: total bytes, card count, field sizes and decoded photo size
 * are all bounded, and a single malformed card or field never aborts the whole import. Nothing is
 * written to storage here, and no contact data is ever logged.
 */
object VCardParser {

    private const val MAX_VCF_BYTES = 8 * 1024 * 1024
    private const val MAX_CONTACTS = 5_000
    private const val MAX_PER_FIELD = 50
    private const val MAX_URLS = 20
    private const val MAX_TEXT = 10_000
    private const val MAX_NOTE = 10_000
    private const val MAX_NAME = 500
    private const val MAX_PHOTO_BYTES = 2 * 1024 * 1024
    private const val SNIFF_BYTES = 4096
    private const val MAX_CONSECUTIVE_ERRORS = 3

    fun parse(input: InputStream): ParseResult {
        val guarded = GuardedInputStream(input, MAX_VCF_BYTES)
        val reader = VCardReader(InputStreamReader(guarded, Charsets.UTF_8)).apply {
            isCaretDecodingEnabled = true
            defaultQuotedPrintableCharset = Charsets.ISO_8859_1
        }

        val contacts = ArrayList<ImportContact>()
        var totalCards = 0
        var warnings = 0
        var consecutiveErrors = 0

        try {
            while (true) {
                val card = try {
                    reader.readNext()
                } catch (e: Exception) {
                    consecutiveErrors++
                    warnings++
                    if (consecutiveErrors >= MAX_CONSECUTIVE_ERRORS) break else continue
                }
                if (card == null) break
                consecutiveErrors = 0
                totalCards++
                if (totalCards > MAX_CONTACTS) return ParseResult.Failure(ParseFailure.TOO_MANY_CONTACTS)

                val (contact, cardWarnings) = mapCard(card)
                warnings += cardWarnings
                if (contact != null) contacts.add(contact)
            }
        } catch (e: Exception) {
            warnings++
        } finally {
            runCatching { reader.close() }
        }

        if (contacts.isEmpty()) {
            return when {
                guarded.bytesRead == 0L -> ParseResult.Failure(ParseFailure.EMPTY_FILE)
                !guarded.sniffedContainsVCard() -> ParseResult.Failure(ParseFailure.NOT_A_VCARD)
                else -> ParseResult.Failure(ParseFailure.NOT_A_VCARD)
            }
        }
        return ParseResult.Success(contacts, totalCards, warnings)
    }

    private fun mapCard(vcard: VCard): Pair<ImportContact?, Int> {
        var warnings = 0

        val formattedName = clamp(vcard.formattedName?.value, MAX_NAME)
        val n = vcard.structuredName

        val phones = vcard.telephoneNumbers
            .asSequence()
            .mapNotNull { it.toImportPhone() }
            .take(MAX_PER_FIELD)
            .toList()

        val emails = vcard.emails
            .asSequence()
            .mapNotNull { it.toImportEmail() }
            .take(MAX_PER_FIELD)
            .toList()

        val addresses = vcard.addresses
            .asSequence()
            .mapNotNull { it.toImportAddress() }
            .take(MAX_PER_FIELD)
            .toList()

        val org = vcard.organization ?: vcard.organizations.firstOrNull()
        val organization = org?.values?.getOrNull(0)?.let { clamp(it, MAX_NAME) }
        val department = org?.values?.getOrNull(1)?.let { clamp(it, MAX_NAME) }
        val jobTitle = vcard.titles.firstOrNull()?.value?.let { clamp(it, MAX_NAME) }

        val birthday = birthdayText(vcard.birthday)

        val note = vcard.notes
            .asSequence()
            .mapNotNull { clamp(it.value, MAX_NOTE) }
            .take(MAX_PER_FIELD)
            .joinToString("\n")
            .take(MAX_NOTE)
            .ifEmpty { null }

        val websites = vcard.urls
            .asSequence()
            .mapNotNull { clamp(it.value, MAX_TEXT) }
            .take(MAX_URLS)
            .distinct()
            .toList()

        val (photoBytes, photoWarning) = resolvePhoto(vcard)
        if (photoWarning) warnings++

        val contact = ImportContact(
            displayName = formattedName,
            givenName = clamp(n?.given, MAX_NAME),
            middleName = n?.additionalNames?.filter { it.isNotBlank() }?.joinToString(" ")?.let { clamp(it, MAX_NAME) },
            familyName = clamp(n?.family, MAX_NAME),
            prefix = n?.prefixes?.filter { it.isNotBlank() }?.joinToString(" ")?.let { clamp(it, MAX_NAME) },
            suffix = n?.suffixes?.filter { it.isNotBlank() }?.joinToString(" ")?.let { clamp(it, MAX_NAME) },
            phones = phones,
            emails = emails,
            addresses = addresses,
            organization = organization,
            department = department,
            jobTitle = jobTitle,
            birthday = birthday,
            note = note,
            websites = websites,
            photoBytes = photoBytes,
            vcardVersion = vcard.version?.version,
        )

        val usable = contact.resolvedDisplayName().isNotEmpty() ||
            contact.phones.isNotEmpty() ||
            contact.emails.isNotEmpty() ||
            contact.organization != null
        return if (usable) contact to warnings else null to (warnings + 1)
    }

    private fun Telephone.toImportPhone(): ImportPhone? {
        val number = clamp(text ?: uri?.toString(), MAX_NAME) ?: return null
        val (label, custom) = labelFor(types.map { it?.value.orEmpty() }, PHONE_RECOGNIZED, PHONE_IGNORED)
        return ImportPhone(number, label, custom)
    }

    private fun Email.toImportEmail(): ImportEmail? {
        val value = clamp(value, MAX_TEXT) ?: return null
        val (label, custom) = labelFor(types.map { it?.value.orEmpty() }, EMAIL_RECOGNIZED, EMAIL_IGNORED)
        return ImportEmail(value, label, custom)
    }

    private fun Address.toImportAddress(): ImportAddress? {
        val street = clamp(streetAddressFull ?: streetAddress, MAX_TEXT)
        val city = clamp(locality, MAX_NAME)
        val region = clamp(region, MAX_NAME)
        val postal = clamp(postalCode, MAX_NAME)
        val country = clamp(country, MAX_NAME)
        val formatted = clamp(label, MAX_TEXT)
            ?: listOfNotNull(street, city, region, postal, country)
                .filter { it.isNotBlank() }
                .joinToString(", ")
                .ifEmpty { null }
        if (formatted == null && street == null && city == null && region == null && postal == null && country == null) {
            return null
        }
        val (label, custom) = labelFor(types.map { it?.value.orEmpty() }, ADDRESS_RECOGNIZED, ADDRESS_IGNORED)
        return ImportAddress(formatted, street, city, region, postal, country, label, custom)
    }

    private fun resolvePhoto(vcard: VCard): Pair<ByteArray?, Boolean> {
        val photo = vcard.photos.firstOrNull { it.data != null } ?: return null to false
        val data = photo.data ?: return null to false
        if (data.isEmpty()) return null to false
        if (data.size > MAX_PHOTO_BYTES) return null to true
        return data to false
    }

    private fun birthdayText(birthday: Birthday?): String? {
        if (birthday == null) return null
        val partial = birthday.partialDate
        if (partial != null) {
            val month = partial.month
            val day = partial.date
            if (month != null && day != null) {
                val year = partial.year
                return if (year != null) {
                    "%04d-%02d-%02d".format(year, month, day)
                } else {
                    "--%02d-%02d".format(month, day)
                }
            }
            return null
        }
        return formatTemporal(birthday.date)
    }

    private fun formatTemporal(temporal: Temporal?): String? = when (temporal) {
        is LocalDate -> formatDate(temporal)
        is LocalDateTime -> formatDate(temporal.toLocalDate())
        is OffsetDateTime -> formatDate(temporal.toLocalDate())
        is ZonedDateTime -> formatDate(temporal.toLocalDate())
        else -> null
    }

    private fun formatDate(date: LocalDate): String =
        "%04d-%02d-%02d".format(date.year, date.monthValue, date.dayOfMonth)

    private fun labelFor(
        rawValues: List<String>,
        recognized: Map<String, ContactLabel>,
        ignored: Set<String>,
    ): Pair<ContactLabel, String?> {
        val trimmed = rawValues.map { it.trim() }
        val upper = trimmed.map { it.uppercase() }
        for (value in upper) {
            recognized[value]?.let { return it to null }
        }
        val customIndex = upper.indexOfFirst { it.isNotEmpty() && it !in recognized && it !in ignored }
        return if (customIndex >= 0) {
            ContactLabel.CUSTOM to trimmed[customIndex]
        } else {
            ContactLabel.OTHER to null
        }
    }

    private fun clamp(value: String?, max: Int): String? {
        val trimmed = value?.trim() ?: return null
        if (trimmed.isEmpty()) return null
        return if (trimmed.length > max) trimmed.substring(0, max) else trimmed
    }

    private val PHONE_RECOGNIZED = mapOf(
        "HOME" to ContactLabel.HOME,
        "WORK" to ContactLabel.WORK,
        "CELL" to ContactLabel.MOBILE,
        "MOBILE" to ContactLabel.MOBILE,
        "FAX" to ContactLabel.FAX,
        "PAGER" to ContactLabel.PAGER,
        "OTHER" to ContactLabel.OTHER,
    )

    private val PHONE_IGNORED = setOf(
        "PREF", "VOICE", "TEXT", "VIDEO", "MSG", "CAR", "ISDN", "BBS", "MODEM", "PCS", "TEXTPHONE",
    )

    private val EMAIL_RECOGNIZED = mapOf(
        "HOME" to ContactLabel.HOME,
        "WORK" to ContactLabel.WORK,
        "CELL" to ContactLabel.MOBILE,
        "MOBILE" to ContactLabel.MOBILE,
    )

    private val EMAIL_IGNORED = setOf(
        "PREF", "INTERNET", "X400", "AOL", "APPLELINK", "ATTMAIL", "CIS", "EWORLD", "IBMMAIL",
        "MCIMAIL", "POWERSHARE", "PRODIGY", "TLX",
    )

    private val ADDRESS_RECOGNIZED = mapOf(
        "HOME" to ContactLabel.HOME,
        "WORK" to ContactLabel.WORK,
        "OTHER" to ContactLabel.OTHER,
    )

    private val ADDRESS_IGNORED = setOf("PREF", "POSTAL", "PARCEL", "DOM", "INTL")

    /**
     * Reads through to [maxBytes], silently truncating beyond the limit, while remembering the
     * first [SNIFF_BYTES] so an unparseable file can be classified without a second read.
     */
    private class GuardedInputStream(
        private val delegate: InputStream,
        private val maxBytes: Int,
    ) : InputStream() {
        var bytesRead: Long = 0
            private set
        private val sniff = ByteArrayOutputStream()

        fun sniffedContainsVCard(): Boolean =
            sniff.toString(Charsets.ISO_8859_1.name()).contains("BEGIN:VCARD", ignoreCase = true)

        override fun read(): Int {
            if (bytesRead >= maxBytes) return -1
            val b = delegate.read()
            if (b == -1) return -1
            bytesRead++
            if (sniff.size() < SNIFF_BYTES) sniff.write(b)
            return b
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (bytesRead >= maxBytes) return -1
            val allowed = minOf(len, (maxBytes - bytesRead).toInt()).coerceAtLeast(1)
            val n = delegate.read(b, off, allowed)
            if (n > 0) {
                bytesRead += n
                val room = SNIFF_BYTES - sniff.size()
                if (room > 0) sniff.write(b, off, minOf(n, room))
            }
            return n
        }

        override fun close() {
            runCatching { delegate.close() }
        }
    }
}
