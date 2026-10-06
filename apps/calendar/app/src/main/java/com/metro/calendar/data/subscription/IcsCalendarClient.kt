package com.metro.calendar.data.subscription

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit

/**
 * Downloads a read-only iCalendar feed over HTTPS with conditional requests. HTTPS only — the URL
 * is normalized before use and never logged.
 */
class IcsCalendarClient(
    private val client: OkHttpClient = defaultClient(),
) {
    sealed interface FetchResult {
        data class NotModified(val etag: String?, val lastModified: String?) : FetchResult
        data class Success(
            val body: String,
            val etag: String?,
            val lastModified: String?,
        ) : FetchResult

        data class Failure(val message: String) : FetchResult
    }

    fun fetch(url: String, etag: String?, lastModified: String?): FetchResult {
        val normalized = SubscriptionUrl.normalize(url)
            ?: return FetchResult.Failure("invalid or insecure URL")
        val request = Request.Builder()
            .url(normalized)
            .header("Accept", "text/calendar, text/plain, */*")
            .apply {
                if (!etag.isNullOrBlank()) header("If-None-Match", etag)
                if (!lastModified.isNullOrBlank()) header("If-Modified-Since", lastModified)
            }
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val respEtag = response.header("ETag")
                val respLastModified = response.header("Last-Modified")
                when {
                    response.code == 304 ->
                        FetchResult.NotModified(etag ?: respEtag, lastModified ?: respLastModified)

                    response.isSuccessful -> {
                        val body = response.body ?: return FetchResult.Failure("empty response")
                        if (body.contentLength() > MAX_BYTES) return FetchResult.Failure("feed too large")
                        val text = body.byteStream().readAtMost(MAX_BYTES + 1).toString(Charsets.UTF_8)
                        if (text.toByteArray(Charsets.UTF_8).size > MAX_BYTES) {
                            return FetchResult.Failure("feed too large")
                        }
                        if (!looksLikeICalendar(text)) {
                            return FetchResult.Failure("not an iCalendar feed")
                        }
                        FetchResult.Success(text, respEtag, respLastModified)
                    }

                    response.code in 400..499 -> FetchResult.Failure("request failed (${response.code})")
                    response.code in 500..599 -> FetchResult.Failure("server error (${response.code})")
                    else -> FetchResult.Failure("unexpected response (${response.code})")
                }
            }
        } catch (e: IOException) {
            FetchResult.Failure("network error")
        }
    }

    private fun looksLikeICalendar(text: String): Boolean =
        text.take(4096).uppercase().contains("BEGIN:VCALENDAR")

    private fun InputStream.readAtMost(max: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        try {
            while (out.size() < max) {
                val remaining = max - out.size()
                val read = read(buffer, 0, minOf(buffer.size, remaining))
                if (read <= 0) break
                out.write(buffer, 0, read)
            }
        } finally {
            runCatching { close() }
        }
        return out.toByteArray()
    }

    private companion object {
        const val MAX_BYTES = 10 * 1024 * 1024

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
    }
}
