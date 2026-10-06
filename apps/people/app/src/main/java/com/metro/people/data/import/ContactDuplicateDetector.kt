package com.metro.people.data.import

/** Snapshot of the contacts already on the device, pre-normalized for fast comparison. */
data class ExistingContactIndex(
    val phoneDigits: List<String>,
    val emails: Set<String>,
    val names: Set<String>,
) {
    companion object {
        val EMPTY = ExistingContactIndex(emptyList(), emptySet(), emptySet())
    }
}

/**
 * Deterministic duplicate classification. Phone and email are the primary signals; a matching
 * display name alone is only a [DuplicateClass.POSSIBLE_DUPLICATE]. No fuzzy/ML matching.
 */
class ContactDuplicateDetector(private val existing: ExistingContactIndex) {

    fun classify(contact: ImportContact): DuplicateClass {
        val incomingPhones = contact.phones
            .map { PhoneNormalizer.digitsOnly(it.number) }
            .filter { it.length >= MIN_PHONE_DIGITS }
        if (incomingPhones.isNotEmpty() && existing.phoneDigits.isNotEmpty()) {
            for (incoming in incomingPhones) {
                for (known in existing.phoneDigits) {
                    if (PhoneNormalizer.sameNumber(known, incoming)) return DuplicateClass.EXACT_DUPLICATE
                }
            }
        }

        val incomingEmails = contact.emails
            .map { normalizeEmail(it.address) }
            .filter { it.isNotEmpty() }
            .toSet()
        if (incomingEmails.isNotEmpty() && incomingEmails.any { it in existing.emails }) {
            return DuplicateClass.EXACT_DUPLICATE
        }

        val name = normalizeName(contact.resolvedDisplayName())
        if (name.isNotEmpty() && name in existing.names) {
            return DuplicateClass.POSSIBLE_DUPLICATE
        }

        return DuplicateClass.NEW
    }

    companion object {
        private const val MIN_PHONE_DIGITS = 7

        fun normalizeEmail(value: String): String = value.trim().lowercase()

        fun normalizeName(value: String): String =
            value.trim().lowercase().replace(Regex("\\s+"), " ")
    }
}
