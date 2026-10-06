package com.metro.people.data.import

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneNormalizerTest {

    @Test
    fun `matches identical numbers`() {
        assertTrue(PhoneNormalizer.sameNumber("+1 555 0100", "+15550100"))
    }

    @Test
    fun `matches formatted variants of the same number`() {
        assertTrue(PhoneNormalizer.sameNumber("+49 170 1234567", "+49-170-1234567"))
    }

    @Test
    fun `matches number with and without international prefix`() {
        assertTrue(PhoneNormalizer.sameNumber("+49 170 1234567", "0170 1234567"))
        assertTrue(PhoneNormalizer.sameNumber("0170 1234567", "+491701234567"))
    }

    @Test
    fun `rejects clearly different numbers`() {
        assertFalse(PhoneNormalizer.sameNumber("+1 555 0100", "+1 555 0199"))
    }

    @Test
    fun `rejects blanks and very short fragments`() {
        assertFalse(PhoneNormalizer.sameNumber("", "+1 555 0100"))
        assertFalse(PhoneNormalizer.sameNumber("123", "456"))
    }
}
