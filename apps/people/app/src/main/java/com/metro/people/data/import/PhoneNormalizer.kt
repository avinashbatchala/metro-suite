package com.metro.people.data.import

/**
 * Deterministic, locale-agnostic phone matching used only for duplicate detection.
 *
 * Formatting is stripped, then numbers are compared allowing a missing country/trunk prefix:
 * `+49 170 1234567`, `0170 1234567`, and `+49-170-1234567` all match.
 */
object PhoneNormalizer {
    private val NON_DIGITS = Regex("[^0-9]")
    private const val MIN_SIGNIFICANT_DIGITS = 7

    fun digitsOnly(raw: String?): String = raw?.replace(NON_DIGITS, "").orEmpty()

    fun sameNumber(a: String, b: String): Boolean {
        val left = digitsOnly(a)
        val right = digitsOnly(b)
        if (left.isEmpty() || right.isEmpty()) return false
        if (left == right) return true
        if (left.length < MIN_SIGNIFICANT_DIGITS || right.length < MIN_SIGNIFICANT_DIGITS) return false
        val leftSignificant = left.trimStart('0')
        val rightSignificant = right.trimStart('0')
        if (leftSignificant == rightSignificant) return true
        return leftSignificant.endsWith(rightSignificant) || rightSignificant.endsWith(leftSignificant)
    }
}
