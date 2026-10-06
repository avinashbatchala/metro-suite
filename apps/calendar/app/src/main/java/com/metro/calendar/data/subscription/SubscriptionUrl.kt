package com.metro.calendar.data.subscription

import java.net.URI

/**
 * URL handling for ICS subscriptions. HTTPS only; `webcal://` is normalized to HTTPS. Subscription
 * URLs can behave like secrets (they often embed private tokens), so [mask] hides the path/query.
 */
object SubscriptionUrl {

    /** Returns a normalized HTTPS URL, or null when [raw] is not a valid/acceptable feed URL. */
    fun normalize(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val https = when {
            trimmed.startsWith("webcal://", ignoreCase = true) ->
                "https://" + trimmed.substring("webcal://".length)
            trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            else -> return null
        }
        val uri = runCatching { URI(https) }.getOrNull() ?: return null
        if (!uri.scheme.equals("https", ignoreCase = true)) return null
        if (uri.host.isNullOrBlank()) return null
        return uri.toString()
    }

    fun isValid(raw: String): Boolean = normalize(raw) != null

    /** Masked display form: scheme + host only, the rest replaced with bullets. */
    fun mask(url: String): String {
        val uri = runCatching { URI(url) }.getOrNull() ?: return "https://••••••••"
        val host = uri.host ?: "••••••••"
        return "https://$host/••••••••"
    }
}
