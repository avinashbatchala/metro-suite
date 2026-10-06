package com.metro.dialer.data

import android.provider.CallLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DialerCallLogicTest {

    @Test
    fun normalizeNumber_stripsFormatting() {
        assertEquals("+15551234567", DialerCallLogic.normalizeNumber("+1 (555) 123-4567"))
    }

    @Test
    fun canonicalDigits_dropsPlusAndSeparators() {
        assertEquals("15551234567", DialerCallLogic.canonicalDigits("+1 (555) 123-4567"))
    }

    @Test
    fun areSameNumber_ignoresTrunkAndCountryPrefix() {
        assertTrue(DialerCallLogic.areSameNumber("+15551234567", "5551234567"))
        assertTrue(DialerCallLogic.areSameNumber("09876543210", "9876543210"))
    }

    @Test
    fun mapCallType_coversEverySupportedConstant() {
        assertEquals(CallType.INCOMING, DialerCallLogic.mapCallType(CallLog.Calls.INCOMING_TYPE))
        assertEquals(CallType.OUTGOING, DialerCallLogic.mapCallType(CallLog.Calls.OUTGOING_TYPE))
        assertEquals(CallType.MISSED, DialerCallLogic.mapCallType(CallLog.Calls.MISSED_TYPE))
        assertEquals(CallType.REJECTED, DialerCallLogic.mapCallType(CallLog.Calls.REJECTED_TYPE))
        assertEquals(CallType.BLOCKED, DialerCallLogic.mapCallType(CallLog.Calls.BLOCKED_TYPE))
        assertEquals(CallType.VOICEMAIL, DialerCallLogic.mapCallType(CallLog.Calls.VOICEMAIL_TYPE))
        assertEquals(
            CallType.ANSWERED_EXTERNALLY,
            DialerCallLogic.mapCallType(CallLog.Calls.ANSWERED_EXTERNALLY_TYPE),
        )
        assertEquals(CallType.UNKNOWN, DialerCallLogic.mapCallType(9999))
    }

    @Test
    fun groupCalls_sameNumberSameDayCollapses() {
        val now = nowAt(2024, 6, 10, 12)
        val entries = listOf(
            callEntry("5551112222", CallType.OUTGOING, now - 1_000),
            callEntry("5551112222", CallType.INCOMING, now - 2_000),
        )
        val groups = DialerCallLogic.groupCalls(entries, now)
        assertEquals(1, groups.size)
        assertEquals(2, groups.first().callCount)
        assertEquals(CallType.OUTGOING, groups.first().latestType)
    }

    @Test
    fun groupCalls_sameNumberDifferentDayStaysSeparate() {
        val day1 = nowAt(2024, 6, 10, 12)
        val day2 = nowAt(2024, 6, 9, 12)
        val entries = listOf(
            callEntry("5551112222", CallType.MISSED, day1),
            callEntry("5551112222", CallType.MISSED, day2),
        )
        val groups = DialerCallLogic.groupCalls(entries, day1)
        assertEquals(2, groups.size)
        assertEquals(listOf(1, 1), groups.map { it.callCount })
    }

    @Test
    fun groupCalls_sameContactDifferentFormattingSharesIdentity() {
        val now = nowAt(2024, 6, 10, 12)
        val entries = listOf(
            callEntry("+1 (555) 123-4567", CallType.INCOMING, now - 1_000),
            callEntry("5551234567", CallType.OUTGOING, now - 2_000),
        )
        val groups = DialerCallLogic.groupCalls(entries, now)
        assertEquals(1, groups.size)
        assertEquals(2, groups.first().callCount)
    }

    @Test
    fun groupCalls_privateAndUnknownAreNotAllCollapsed() {
        val now = nowAt(2024, 6, 10, 12)
        val entries = listOf(
            callEntryRaw("", CallType.MISSED, now - 1_000, isPrivate = true),
            callEntryRaw("", CallType.MISSED, now - 2_000, isUnknown = true),
        )
        val groups = DialerCallLogic.groupCalls(entries, now)
        assertEquals(2, groups.size)
        assertNotEquals(groups[0].displayName, groups[1].displayName)
    }

    @Test
    fun groupIntoSections_labelsTodayAndYesterday() {
        val today = nowAt(2024, 6, 10, 12)
        val yesterday = today - 24L * 60 * 60 * 1000
        val groups = DialerCallLogic.groupCalls(
            listOf(
                callEntry("5551112222", CallType.INCOMING, today - 1_000),
                callEntry("5559998888", CallType.INCOMING, yesterday),
            ),
            today,
        )
        val sections = DialerCallLogic.groupIntoSections(groups, today)
        assertEquals(2, sections.size)
        assertEquals("today", sections[0].label)
        assertEquals("yesterday", sections[1].label)
    }

    @Test
    fun dateSectionLabel_respectsTimeZoneBoundary() {
        val utc = TimeZone.getTimeZone("UTC")
        val tokyo = TimeZone.getTimeZone("Asia/Tokyo")
        // 2024-06-10 22:00 UTC == 2024-06-11 07:00 Tokyo.
        val instant = nowAt(2024, 6, 10, 22, utc)
        val utcBucket = DialerCallLogic.localDayBucket(instant, utc)
        val tokyoBucket = DialerCallLogic.localDayBucket(instant, tokyo)
        assertNotEquals(utcBucket, tokyoBucket)
    }

    @Test
    fun primaryLabel_showsCountWhenGrouped() {
        val group = CallGroup(
            id = "x",
            identityKey = "555",
            phoneNumber = "555",
            displayName = "Alice",
            latestType = CallType.INCOMING,
            latestTimestamp = 0L,
            callCount = 3,
            calls = emptyList(),
            dateBucket = 0L,
        )
        assertEquals("Alice (3)", DialerCallLogic.primaryLabel(group))
    }

    @Test
    fun filterGroups_matchesNameOrNumber() {
        val groups = listOf(
            testGroup("5551", "555-1", "Alice"),
            testGroup("5552", "555-2", "Bob"),
        )
        assertEquals(1, DialerCallLogic.filterGroups(groups, "ali").size)
        assertEquals(1, DialerCallLogic.filterGroups(groups, "555-2").size)
        assertEquals(1, DialerCallLogic.filterGroups(groups, "5552").size)
    }

    @Test
    fun matchesNumberPrefix_matchesStoredInternationalShape() {
        assertTrue(DialerCallLogic.matchesNumberPrefix("+1 (555) 123-4567", "555"))
        assertTrue(DialerCallLogic.matchesNumberPrefix("+91 9876543210", "98765"))
        assertFalse(DialerCallLogic.matchesNumberPrefix("+1 (555) 123-4567", "999"))
    }

    @Test
    fun contactSuggestions_prefersNumberMatchThenName() {
        val contacts = listOf(
            ContactSuggestion("Kell", "444-1111", "4441111"),
            ContactSuggestion("Alice", "555-9999", "5559999"),
        )
        val matches = DialerCallLogic.contactSuggestions("555", contacts)
        assertEquals(1, matches.size)
        assertEquals("Alice", matches.first().displayName)
    }

    @Test
    fun contactSuggestions_matchesT9NamePrefixWhenEnabled() {
        val contacts = listOf(
            ContactSuggestion("Andrew Hill", "5550001", "5550001"),
            ContactSuggestion("Arturo Lopez", "5550005", "5550005"),
            ContactSuggestion("Chris Sells", "5550003", "5550003"),
            ContactSuggestion("Dana", "4449999", "4449999"),
        )
        val matches = DialerCallLogic.contactSuggestions("2", contacts)
        assertEquals(3, matches.size)
        assertEquals(
            listOf("Andrew Hill", "Arturo Lopez", "Chris Sells"),
            matches.map { it.displayName },
        )
    }

    @Test
    fun matchesT9_matchesEachWordPrefix() {
        assertTrue(DialerCallLogic.matchesT9("Chris Sells", "2"))
        assertTrue(DialerCallLogic.matchesT9("Chris Sells", "73557"))
        assertFalse(DialerCallLogic.matchesT9("Chris Sells", "3"))
    }

    @Test
    fun formatDuration_formatsMinutesAndSeconds() {
        assertEquals("1:05", DialerCallLogic.formatDuration(65))
    }

    private fun testGroup(identity: String, number: String, name: String) = CallGroup(
        id = identity,
        identityKey = identity,
        phoneNumber = number,
        displayName = name,
        latestType = CallType.INCOMING,
        latestTimestamp = 0L,
        callCount = 1,
        calls = emptyList(),
        dateBucket = 0L,
    )

    private fun nowAt(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): Long {
        val cal = Calendar.getInstance(timeZone)
        cal.set(year, month - 1, day, hour, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun callEntry(number: String, type: CallType, timestamp: Long): CallEntry =
        callEntryRaw(number, type, timestamp, contactName = "Alice")

    private fun callEntryRaw(
        number: String,
        type: CallType,
        timestamp: Long,
        contactName: String? = null,
        isPrivate: Boolean = false,
        isUnknown: Boolean = false,
    ): CallEntry {
        return CallEntry(
            id = timestamp,
            phoneNumber = number,
            normalizedNumber = DialerCallLogic.normalizeNumber(number),
            type = type,
            timestamp = timestamp,
            durationSeconds = 30,
            contactName = contactName,
            isPrivate = isPrivate,
            isUnknown = isUnknown,
        )
    }
}
