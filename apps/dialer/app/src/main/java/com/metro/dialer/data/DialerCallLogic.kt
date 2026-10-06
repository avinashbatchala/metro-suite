package com.metro.dialer.data

import android.provider.CallLog
import android.telephony.PhoneNumberUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Pure call-log / dialing logic. No Android side effects beyond platform number utilities, so it is
 * unit-testable under Robolectric.
 */
object DialerCallLogic {

    // ---- number normalisation -------------------------------------------

    /** Digits (and a leading `+`) of [raw], separator-free. */
    fun normalizeNumber(raw: String): String {
        val normalized = runCatching { PhoneNumberUtils.normalizeNumber(raw.trim()) }
            .getOrDefault(raw.trim())
        val builder = StringBuilder()
        normalized.forEachIndexed { index, char ->
            when {
                char.isDigit() -> builder.append(char)
                char == '+' && index == 0 -> builder.append(char)
            }
        }
        return builder.toString()
    }

    /** Digits only, no leading `+`. */
    fun canonicalDigits(raw: String): String = normalizeNumber(raw).filter { it.isDigit() }

    /** Platform-aware number equality (handles country-code / trunk-prefix differences). */
    fun areSameNumber(a: String, b: String): Boolean {
        val da = canonicalDigits(a)
        val db = canonicalDigits(b)
        if (da.isEmpty() || db.isEmpty()) return da == db && da.isNotEmpty()
        if (da == db) return true
        return runCatching { PhoneNumberUtils.compare(a, b) }.getOrDefault(false)
    }

    fun formatDisplayNumber(number: String): String {
        val trimmed = number.trim()
        if (trimmed.isEmpty()) return trimmed
        return runCatching {
            PhoneNumberUtils.formatNumber(trimmed, null) ?: trimmed
        }.getOrDefault(trimmed)
    }

    // ---- call types ------------------------------------------------------

    fun mapCallType(androidType: Int): CallType = when (androidType) {
        CallLog.Calls.INCOMING_TYPE -> CallType.INCOMING
        CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
        CallLog.Calls.MISSED_TYPE -> CallType.MISSED
        CallLog.Calls.REJECTED_TYPE -> CallType.REJECTED
        CallLog.Calls.BLOCKED_TYPE -> CallType.BLOCKED
        CallLog.Calls.VOICEMAIL_TYPE -> CallType.VOICEMAIL
        CallLog.Calls.ANSWERED_EXTERNALLY_TYPE -> CallType.ANSWERED_EXTERNALLY
        else -> CallType.UNKNOWN
    }

    // ---- grouping --------------------------------------------------------

    fun groupCalls(
        entries: List<CallEntry>,
        now: Long = System.currentTimeMillis(),
    ): List<CallGroup> {
        if (entries.isEmpty()) return emptyList()
        val sorted = entries.sortedByDescending { it.timestamp }
        val groups = mutableListOf<MutableGroup>()

        sorted.forEach { entry ->
            val bucket = localDayBucket(entry.timestamp)
            val existing = groups.firstOrNull { group ->
                group.dateBucket == bucket && sameIdentity(group, entry)
            }
            if (existing != null) {
                existing.calls.add(entry)
            } else {
                groups.add(MutableGroup(dateBucket = bucket, identityKey = identityKey(entry), calls = mutableListOf(entry)))
            }
        }

        return groups
            .map { it.toCallGroup() }
            .sortedByDescending { it.latestTimestamp }
    }

    fun groupIntoSections(
        groups: List<CallGroup>,
        now: Long = System.currentTimeMillis(),
    ): List<HistorySection> = groups
        .groupBy { it.dateBucket }
        .toSortedMap(compareByDescending { it })
        .map { (bucket, bucketGroups) ->
            HistorySection(
                dateBucket = bucket,
                label = dateSectionLabel(bucket, now),
                groups = bucketGroups.sortedByDescending { it.latestTimestamp },
            )
        }

    private fun sameIdentity(group: MutableGroup, entry: CallEntry): Boolean {
        val first = group.calls.first()
        if (first.isPrivate || first.isUnknown) {
            // Private/unknown callers are grouped only with the same raw presentation.
            return group.identityKey == identityKey(entry)
        }
        if (first.contactLookupKey != null && first.contactLookupKey == entry.contactLookupKey) {
            return true
        }
        return areSameNumber(first.phoneNumber, entry.phoneNumber)
    }

    private fun identityKey(entry: CallEntry): String = when {
        entry.isPrivate -> "private"
        entry.isUnknown -> "unknown"
        entry.contactLookupKey != null -> "lc:${entry.contactLookupKey}"
        else -> canonicalDigits(entry.phoneNumber).ifEmpty { "raw:${entry.phoneNumber}" }
    }

    private class MutableGroup(
        val dateBucket: Long,
        val identityKey: String,
        val calls: MutableList<CallEntry>,
    ) {
        fun toCallGroup(): CallGroup {
            val latest = calls.maxByOrNull { it.timestamp }!!
            val first = calls.first()
            val named = calls.firstNotNullOfOrNull { it.contactName?.takeIf(String::isNotBlank) }
            val displayName = named
                ?: when {
                    first.isPrivate -> PRIVATE_LABEL
                    first.isUnknown || first.phoneNumber.isBlank() -> UNKNOWN_LABEL
                    else -> formatDisplayNumber(latest.phoneNumber)
                }
            return CallGroup(
                id = "$identityKey@$dateBucket",
                identityKey = identityKey,
                phoneNumber = latest.phoneNumber,
                displayName = displayName,
                latestType = latest.type,
                latestTimestamp = latest.timestamp,
                callCount = calls.size,
                calls = calls.sortedByDescending { it.timestamp },
                contactLookupKey = calls.firstNotNullOfOrNull { it.contactLookupKey },
                contactId = calls.firstNotNullOfOrNull { it.contactId },
                dateBucket = dateBucket,
                isPrivate = first.isPrivate,
                isUnknown = first.isUnknown,
            )
        }
    }

    fun primaryLabel(group: CallGroup): String =
        if (group.callCount > 1) "${group.displayName} (${group.callCount})" else group.displayName

    fun filterGroups(groups: List<CallGroup>, query: String): List<CallGroup> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return groups
        val lower = trimmed.lowercase(Locale.getDefault())
        val queryDigits = canonicalDigits(trimmed)
        return groups.filter { group ->
            group.displayName.lowercase(Locale.getDefault()).contains(lower) ||
                (queryDigits.isNotEmpty() &&
                    canonicalDigits(group.phoneNumber).contains(queryDigits))
        }
    }

    // ---- date helpers ----------------------------------------------------

    fun localDayBucket(timestamp: Long, timeZone: TimeZone = TimeZone.getDefault()): Long {
        val cal = Calendar.getInstance(timeZone)
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun dateSectionLabel(
        bucket: Long,
        now: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault(),
        locale: Locale = Locale.getDefault(),
    ): String {
        val today = localDayBucket(now, timeZone)
        val dayMs = TimeUnit.DAYS.toMillis(1)
        return when (bucket) {
            today -> "today"
            today - dayMs -> "yesterday"
            else -> {
                val withinWeek = bucket >= today - dayMs * 6
                val pattern = if (withinWeek) "EEEE" else "MMMM d, yyyy"
                SimpleDateFormat(pattern, locale).apply { this.timeZone = timeZone }
                    .format(Date(bucket))
                    .lowercase(locale)
            }
        }
    }

    fun relativeTime(timestamp: Long, now: Long = System.currentTimeMillis()): String {
        val delta = (now - timestamp).coerceAtLeast(0)
        return when {
            delta < TimeUnit.MINUTES.toMillis(1) -> "just now"
            delta < TimeUnit.HOURS.toMillis(1) -> {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(delta)
                "$minutes min ago"
            }
            delta < TimeUnit.DAYS.toMillis(1) -> {
                val hours = TimeUnit.MILLISECONDS.toHours(delta)
                "$hours h ago"
            }
            delta < TimeUnit.DAYS.toMillis(7) -> {
                val days = TimeUnit.MILLISECONDS.toDays(delta)
                "$days d ago"
            }
            else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
        }
    }

    fun formatDuration(seconds: Int): String {
        if (seconds <= 0) return "0:00"
        val minutes = seconds / 60
        val remainder = seconds % 60
        return "$minutes:${remainder.toString().padStart(2, '0')}"
    }

    fun formatTimestamp(timestamp: Long): String {
        val formatter = SimpleDateFormat("EEE, MMM d · h:mm a", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }

    // ---- smart dial (MetroSuite extension) ------------------------------

    fun t9Key(digit: Char): String = when (digit) {
        '2' -> "abc"
        '3' -> "def"
        '4' -> "ghi"
        '5' -> "jkl"
        '6' -> "mno"
        '7' -> "pqrs"
        '8' -> "tuv"
        '9' -> "wxyz"
        else -> ""
    }

    fun matchesT9(name: String, digits: String): Boolean {
        val t9Digits = digits.filter { it in '2'..'9' }
        if (t9Digits.isEmpty()) return false
        val words = name.lowercase(Locale.getDefault())
            .split(Regex("[^a-z]+"))
            .filter { it.isNotEmpty() }
        return words.any { wordMatchesT9Prefix(it, t9Digits) }
    }

    private fun wordMatchesT9Prefix(letters: String, digits: String): Boolean {
        if (digits.length > letters.length) return false
        digits.forEachIndexed { index, digit ->
            val keyLetters = t9Key(digit)
            if (keyLetters.isEmpty() || !keyLetters.contains(letters[index])) return false
        }
        return true
    }

    fun contactSuggestions(
        queryDigits: String,
        contacts: List<ContactSuggestion>,
        limit: Int = 3,
    ): List<ContactSuggestion> {
        if (queryDigits.isEmpty()) return emptyList()
        val t9Digits = queryDigits.filter { it in '2'..'9' }
        return contacts.mapNotNull { contact ->
            val numberMatch = matchesNumberPrefix(contact.phoneNumber, queryDigits)
            val nameMatch = t9Digits.isNotEmpty() && matchesT9(contact.displayName, t9Digits)
            when {
                numberMatch -> contact to 0
                nameMatch -> contact to 1
                else -> null
            }
        }
            .sortedWith(
                compareBy<Pair<ContactSuggestion, Int>> { it.second }
                    .thenBy { it.first.displayName.lowercase(Locale.getDefault()) },
            )
            .map { it.first }
            .take(limit)
    }

    fun mergeContactSuggestions(
        primary: List<ContactSuggestion>,
        secondary: List<ContactSuggestion>,
        limit: Int = 3,
    ): List<ContactSuggestion> {
        val seen = linkedSetOf<String>()
        val merged = mutableListOf<ContactSuggestion>()
        for (contact in primary + secondary) {
            val key = "${canonicalDigits(contact.phoneNumber)}|${contact.displayName.lowercase(Locale.getDefault())}"
            if (seen.add(key)) merged.add(contact)
            if (merged.size >= limit) break
        }
        return merged
    }

    /** Locale-safe digit-prefix match — the query appears in the stored number. */
    fun matchesNumberPrefix(number: String, query: String): Boolean {
        val n = canonicalDigits(number)
        val q = canonicalDigits(query)
        if (q.isEmpty() || n.isEmpty()) return false
        return n.startsWith(q) || n.contains(q)
    }

    const val PRIVATE_LABEL = "private number"
    const val UNKNOWN_LABEL = "unknown"
    const val VOICEMAIL_LABEL = "voicemail"
}
