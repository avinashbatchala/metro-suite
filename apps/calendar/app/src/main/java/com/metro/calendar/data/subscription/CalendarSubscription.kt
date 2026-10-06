package com.metro.calendar.data.subscription

/**
 * A read-only, URL-based iCalendar/ICS subscription. The [url] may embed a private access token,
 * so it must never be logged or shown unmasked (see [SubscriptionUrl.mask]).
 */
data class CalendarSubscription(
    val id: String,
    val name: String,
    val url: String,
    val colorHex: String,
    val enabled: Boolean = true,
    val lastSyncMillis: Long? = null,
    val lastSuccessMillis: Long? = null,
    val lastError: String? = null,
    val etag: String? = null,
    val lastModified: String? = null,
)
