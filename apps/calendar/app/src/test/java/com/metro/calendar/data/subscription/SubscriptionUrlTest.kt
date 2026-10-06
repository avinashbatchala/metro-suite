package com.metro.calendar.data.subscription

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionUrlTest {

    @Test
    fun `normalizes webcal to https`() {
        assertEquals(
            "https://example.com/calendar.ics",
            SubscriptionUrl.normalize("webcal://example.com/calendar.ics"),
        )
    }

    @Test
    fun `keeps https url`() {
        assertEquals(
            "https://example.com/private/token.ics",
            SubscriptionUrl.normalize("https://example.com/private/token.ics"),
        )
    }

    @Test
    fun `rejects plain http`() {
        assertNull(SubscriptionUrl.normalize("http://example.com/calendar.ics"))
        assertFalse(SubscriptionUrl.isValid("http://example.com/calendar.ics"))
    }

    @Test
    fun `rejects blank and non-urls`() {
        assertNull(SubscriptionUrl.normalize(""))
        assertNull(SubscriptionUrl.normalize("   "))
        assertNull(SubscriptionUrl.normalize("not a url"))
        assertNull(SubscriptionUrl.normalize("ftp://example.com/calendar.ics"))
    }

    @Test
    fun `rejects https without host`() {
        assertNull(SubscriptionUrl.normalize("https:///calendar.ics"))
    }

    @Test
    fun `masks path and query`() {
        val masked = SubscriptionUrl.mask("https://example.com/private/secret-token.ics?key=abc")
        assertTrue(masked.startsWith("https://example.com/"))
        assertFalse(masked.contains("secret-token"))
        assertFalse(masked.contains("abc"))
    }
}
