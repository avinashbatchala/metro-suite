package com.metro.calendar.data.subscription

import com.metro.calendar.data.CalendarEvent
import com.metro.calendar.data.CalendarSourceType

/**
 * Bridges stored ICS subscriptions to the normalized [CalendarEvent] stream. Networking and caching
 * are handled here; failures never discard the last successfully downloaded payload.
 */
class IcsCalendarSource(
    private val store: CalendarSubscriptionStore,
    private val client: IcsCalendarClient = IcsCalendarClient(),
) {
    sealed interface SyncOutcome {
        data object NotModified : SyncOutcome
        data object Success : SyncOutcome
        data class Failure(val message: String) : SyncOutcome
    }

    fun subscriptions(): List<CalendarSubscription> = store.loadAll()

    fun add(name: String, url: String, colorHex: String, nowMillis: Long): CalendarSubscription? {
        val normalized = SubscriptionUrl.normalize(url) ?: return null
        val subscription = CalendarSubscription(
            id = store.newId(),
            name = name.trim().ifBlank { "Calendar" },
            url = normalized,
            colorHex = colorHex,
            lastSyncMillis = nowMillis,
        )
        store.upsert(subscription)
        return subscription
    }

    fun remove(id: String) = store.delete(id)

    fun setEnabled(id: String, enabled: Boolean) = store.setEnabled(id, enabled)

    /** Fetches one subscription and updates its cache/metadata. Keeps old cache on failure. */
    fun sync(id: String, nowMillis: Long = System.currentTimeMillis()): SyncOutcome {
        val subscription = store.get(id)
            ?: return SyncOutcome.Failure("Calendar not found")
        return when (val result = client.fetch(subscription.url, subscription.etag, subscription.lastModified)) {
            is IcsCalendarClient.FetchResult.NotModified -> {
                store.recordSuccess(id, result.etag, result.lastModified, nowMillis)
                SyncOutcome.NotModified
            }

            is IcsCalendarClient.FetchResult.Success -> {
                store.writeCache(id, result.body)
                store.recordSuccess(id, result.etag, result.lastModified, nowMillis)
                SyncOutcome.Success
            }

            is IcsCalendarClient.FetchResult.Failure -> {
                store.recordError(id, result.message, nowMillis)
                SyncOutcome.Failure(result.message)
            }
        }
    }

    /** Syncs every enabled subscription; returns the number that failed. */
    fun syncAll(nowMillis: Long = System.currentTimeMillis()): Int =
        subscriptions().filter { it.enabled }.count { sync(it.id, nowMillis) is SyncOutcome.Failure }

    /** Parsed events from every enabled, cached subscription within the window. */
    fun loadEvents(windowStartMillis: Long, windowEndMillis: Long): List<CalendarEvent> {
        val result = ArrayList<CalendarEvent>()
        for (subscription in subscriptions()) {
            if (!subscription.enabled) continue
            val body = store.readCache(subscription.id) ?: continue
            val parsed = IcsCalendarParser.parse(body, windowStartMillis, windowEndMillis)
            for (event in parsed) {
                result += CalendarEvent(
                    id = stableId(subscription.id, event.uid, event.startMillis),
                    title = event.title,
                    startMillis = event.startMillis,
                    endMillis = event.endMillis,
                    allDay = event.allDay,
                    calendarColorHex = subscription.colorHex,
                    calendarName = subscription.name,
                    location = event.location,
                    sourceType = CalendarSourceType.SUBSCRIPTION,
                    sourceId = subscription.id,
                )
            }
        }
        return result
    }

    private fun stableId(subscriptionId: String, uid: String, startMillis: Long): Long {
        var hash = FNV_OFFSET
        val key = "sub|$subscriptionId|uid|$uid|start|$startMillis"
        for (ch in key) {
            hash = hash xor ch.code.toLong()
            hash *= FNV_PRIME
        }
        return hash
    }

    private companion object {
        const val FNV_OFFSET = -3750763034362895579L
        const val FNV_PRIME = 1099511628211L
    }
}
