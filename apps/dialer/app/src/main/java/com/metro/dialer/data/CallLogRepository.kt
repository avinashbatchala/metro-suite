package com.metro.dialer.data

import android.content.Context
import android.database.Cursor
import android.provider.CallLog

/**
 * Read access to the platform call log. No contact resolution here (the ViewModel enriches entries
 * from a single contacts-cache query) and no threading decisions.
 */
class CallLogRepository(
    private val context: Context,
) {
    fun loadRecentCalls(limit: Int = 200): List<CallEntry> {
        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            CallLog.Calls.NUMBER_PRESENTATION,
        )
        val cursor = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            null,
            null,
            "${CallLog.Calls.DATE} DESC",
        ) ?: return emptyList()

        cursor.use {
            val entries = mutableListOf<CallEntry>()
            while (cursor.moveToNext() && entries.size < limit) {
                entries.add(cursor.toCallEntry())
            }
            return entries
        }
    }

    private fun Cursor.toCallEntry(): CallEntry {
        val id = getLong(getColumnIndexOrThrow(CallLog.Calls._ID))
        val rawNumber = getString(getColumnIndexOrThrow(CallLog.Calls.NUMBER)).orEmpty()
        val cachedName = getString(getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME))
        val type = getInt(getColumnIndexOrThrow(CallLog.Calls.TYPE))
        val date = getLong(getColumnIndexOrThrow(CallLog.Calls.DATE))
        val duration = getInt(getColumnIndexOrThrow(CallLog.Calls.DURATION))
        val presentation = getInt(getColumnIndexOrThrow(CallLog.Calls.NUMBER_PRESENTATION))

        val trimmed = rawNumber.trim()
        val isPrivate = presentation == CallLog.Calls.PRESENTATION_RESTRICTED ||
            presentation == CallLog.Calls.PRESENTATION_PAYPHONE
        val isUnknown = presentation == CallLog.Calls.PRESENTATION_UNKNOWN ||
            (trimmed.isEmpty() && !isPrivate) ||
            trimmed == "-1" || trimmed == "-2" || trimmed == "-3"

        return CallEntry(
            id = id,
            phoneNumber = if (isPrivate || isUnknown) "" else trimmed,
            normalizedNumber = DialerCallLogic.normalizeNumber(trimmed),
            type = DialerCallLogic.mapCallType(type),
            timestamp = date,
            durationSeconds = duration,
            contactName = cachedName?.takeIf { it.isNotBlank() },
            presentation = presentation,
            isPrivate = isPrivate,
            isUnknown = isUnknown,
        )
    }

    /** Delete exact call-log rows by id. Returns true when the provider accepted the delete. */
    fun deleteCalls(ids: Collection<Long>): Boolean {
        if (ids.isEmpty()) return true
        val selection = ids.joinToString(" OR ") { "${CallLog.Calls._ID}=?" }
        return runCatching {
            context.contentResolver.delete(
                CallLog.Calls.CONTENT_URI,
                "($selection)",
                ids.map { it.toString() }.toTypedArray(),
            )
        }.isSuccess
    }
}
