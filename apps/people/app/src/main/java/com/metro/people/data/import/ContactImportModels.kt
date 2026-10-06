package com.metro.people.data.import

/**
 * Internal, provider-independent representation of a parsed vCard contact.
 *
 * Nothing here is written to storage while parsing. The map onto `ContactsContract` happens later
 * in [ContactWriter] so parsing stays pure and testable.
 */
data class ImportContact(
    val displayName: String?,
    val givenName: String?,
    val middleName: String?,
    val familyName: String?,
    val prefix: String?,
    val suffix: String?,
    val phones: List<ImportPhone> = emptyList(),
    val emails: List<ImportEmail> = emptyList(),
    val addresses: List<ImportAddress> = emptyList(),
    val organization: String? = null,
    val department: String? = null,
    val jobTitle: String? = null,
    /** Birthday as Android-friendly text: `yyyy-MM-dd` or `--MM-dd` (year unknown). */
    val birthday: String? = null,
    val note: String? = null,
    val websites: List<String> = emptyList(),
    val photoBytes: ByteArray? = null,
    val vcardVersion: String? = null,
) {
    /** Name derived from the structured components (`N`), when present. */
    val structuredName: String?
        get() = listOfNotNull(prefix, givenName, middleName, familyName, suffix)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(" ")
            .ifEmpty { null }

    /**
     * Display-name fallback chain (blueprint §Name handling):
     * FN → formatted N → organization → first phone → first email → empty.
     */
    fun resolvedDisplayName(): String {
        listOf(
            displayName,
            structuredName,
            organization,
            phones.firstOrNull()?.number,
            emails.firstOrNull()?.address,
        ).forEach { candidate ->
            val trimmed = candidate?.trim()
            if (!trimmed.isNullOrEmpty()) return trimmed
        }
        return ""
    }
}

enum class ContactLabel { HOME, WORK, MOBILE, FAX, PAGER, OTHER, CUSTOM }

data class ImportPhone(
    val number: String,
    val label: ContactLabel = ContactLabel.OTHER,
    val customLabel: String? = null,
)

data class ImportEmail(
    val address: String,
    val label: ContactLabel = ContactLabel.OTHER,
    val customLabel: String? = null,
)

data class ImportAddress(
    val formatted: String?,
    val street: String?,
    val city: String?,
    val region: String?,
    val postalCode: String?,
    val country: String?,
    val label: ContactLabel = ContactLabel.OTHER,
    val customLabel: String? = null,
)

enum class ParseFailure {
    EMPTY_FILE,
    NOT_A_VCARD,
    UNREADABLE,
    TOO_MANY_CONTACTS,
}

sealed interface ParseResult {
    data class Success(
        val contacts: List<ImportContact>,
        val totalCards: Int,
        val warningCount: Int,
    ) : ParseResult

    data class Failure(val reason: ParseFailure) : ParseResult
}

enum class DuplicateClass { NEW, EXACT_DUPLICATE, POSSIBLE_DUPLICATE }

data class ClassifiedImport(
    val contact: ImportContact,
    val classification: DuplicateClass,
)

data class ImportPreview(
    val classified: List<ClassifiedImport>,
    val warningCount: Int,
    val fileName: String?,
) {
    val total: Int get() = classified.size
    val newCount: Int get() = classified.count { it.classification == DuplicateClass.NEW }
    val exactCount: Int get() = classified.count { it.classification == DuplicateClass.EXACT_DUPLICATE }
    val possibleCount: Int get() = classified.count { it.classification == DuplicateClass.POSSIBLE_DUPLICATE }

    /** Exact duplicates are skipped; possible duplicates are imported as separate contacts. */
    val importable: List<ClassifiedImport>
        get() = classified.filter { it.classification != DuplicateClass.EXACT_DUPLICATE }
}

data class ImportProgress(val done: Int, val total: Int)

data class ImportResult(
    val added: Int,
    val possibleAdded: Int,
    val skippedDuplicates: Int,
    val failed: Int,
) {
    val imported: Int get() = added + possibleAdded
}

enum class ImportError {
    WRITE_PERMISSION_DENIED,
    READ_PERMISSION_REQUIRED,
    FILE_UNREADABLE,
    EMPTY_FILE,
    NOT_A_VCARD,
    TOO_MANY_CONTACTS,
    IMPORT_FAILED,
}

internal fun ParseFailure.toImportError(): ImportError = when (this) {
    ParseFailure.EMPTY_FILE -> ImportError.EMPTY_FILE
    ParseFailure.NOT_A_VCARD -> ImportError.NOT_A_VCARD
    ParseFailure.UNREADABLE -> ImportError.FILE_UNREADABLE
    ParseFailure.TOO_MANY_CONTACTS -> ImportError.TOO_MANY_CONTACTS
}
