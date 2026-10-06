/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.utils

import com.music.vivi.constants.DataSaverKey
import com.music.vivi.utils.ViviPrefCache
import timber.log.Timber

/**
 * Resizes a Google CDN or YouTube thumbnail URL to the requested dimensions.
 * Uses domain-independent and parameter-based matching to maximize quality
 * and ensure fetching logic does not break if YouTube changes hostnames or paths.
 */
fun String.resize(
    width: Int? = null,
    height: Int? = null,
): String {
    val isGoogleCdn = this.contains("googleusercontent.com") ||
        this.contains("ggpht.com") ||
        this.contains(Regex("=[wshd]\\d+"))

    val isYtimg = this.contains("ytimg") || this.contains("youtube.com") || this.contains("/vi/")

    val isDataSaverEnabled = ViviPrefCache.get(DataSaverKey) == true

    return when {
        isGoogleCdn -> resizeGoogleCdn(width, height, isDataSaverEnabled)
        isYtimg -> resizeYtimg(width, height, isDataSaverEnabled)
        else -> this
    }
}

private fun String.resizeGoogleCdn(width: Int?, height: Int?, isDataSaver: Boolean): String {
    val w = if (isDataSaver) {
        (width ?: height ?: 150).coerceAtMost(150)
    } else {
        (width ?: height ?: 1200).coerceAtLeast(544)
    }
    val h = if (isDataSaver) {
        (height ?: width ?: 150).coerceAtMost(150)
    } else {
        (height ?: width ?: 1200).coerceAtLeast(544)
    }

    return if (contains(Regex("w\\d+-h\\d+"))) {
        replace(Regex("w\\d+-h\\d+"), "w$w-h$h")
    } else {
        val base = split(Regex("=[wshd]"), limit = 2)[0]
        "$base=w$w-h$h-p-l90-rj"
    }
}

private fun String.resizeYtimg(width: Int?, height: Int?, isDataSaver: Boolean): String {
    val videoId = Regex("/vi(?:_webp)?/([^/]+)/").find(this)?.groupValues?.get(1)
    if (videoId == null) {
        Timber.w("[Thumbnail] resizeYtimg: could not extract videoId from: $this")
        return this
    }

    return if (isDataSaver) {
        val w = width ?: height ?: 150
        val url = if (w >= 800) {
            "https://i.ytimg.com/vi_webp/$videoId/mqdefault.webp"
        } else {
            "https://i.ytimg.com/vi_webp/$videoId/default.webp"
        }
        Timber.d("[Thumbnail] resizeYtimg (dataSaver): id=$videoId w=$w → $url")
        url
    } else {
        val w = width ?: height ?: 1200
        val url = when {
            w >= 800 -> "https://i.ytimg.com/vi_webp/$videoId/hqdefault.webp"
            w >= 320 -> "https://i.ytimg.com/vi_webp/$videoId/mqdefault.webp"
            else -> "https://i.ytimg.com/vi_webp/$videoId/default.webp"
        }
        Timber.d("[Thumbnail] resizeYtimg: id=$videoId w=$w → $url")
        url
    }
}
