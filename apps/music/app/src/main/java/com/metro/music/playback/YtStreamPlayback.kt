package com.metro.music.playback

import android.net.Uri
import java.util.concurrent.ConcurrentHashMap

/**
 * Per-stream HTTP identity for googlevideo playback.
 *
 * Innertube mints are bound to a client User-Agent. ExoPlayer must reuse that same UA — a Chrome
 * default against an IOS/ANDROID_VR URL is a common cause of mid-file 403s past the ~1 MiB GVS
 * preview window (playback dies near one minute; seeks past it fail / buffer forever).
 *
 * Keys prefer the stable googlevideo `id=` / `mh=` query params so redirects or minor URI
 * normalisation still resolve the minting UA.
 */
object YtStreamPlayback {
    private val userAgents = ConcurrentHashMap<String, String>()

    fun register(url: String, userAgent: String) {
        if (url.isBlank() || userAgent.isBlank()) return
        for (key in keysFor(Uri.parse(url))) {
            userAgents[key] = userAgent
        }
    }

    fun userAgentFor(uri: Uri?): String? {
        if (uri == null) return null
        for (key in keysFor(uri)) {
            userAgents[key]?.let { return it }
        }
        return null
    }

    fun clear() {
        userAgents.clear()
    }

    private fun keysFor(uri: Uri): List<String> {
        val full = uri.toString()
        val id = uri.getQueryParameter("id")?.takeIf { it.isNotBlank() }
        val mh = uri.getQueryParameter("mh")?.takeIf { it.isNotBlank() }
        return buildList {
            add(full)
            if (id != null) add("id:$id")
            if (mh != null) add("mh:$mh")
        }
    }
}
