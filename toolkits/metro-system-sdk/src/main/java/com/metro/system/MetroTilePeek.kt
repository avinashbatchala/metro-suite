package com.metro.system

import org.json.JSONArray
import org.json.JSONObject

/**
 * One notification peek face for [MetroTileWidgetFaceKind.PEEK_CYCLE] Start tiles.
 * Launcher cycles these without showing the host app icon.
 */
data class MetroTilePeek(
    val title: String? = null,
    val subtitle: String? = null,
    val body: String? = null,
    /** Source app name (Start-style footer), not the host tile title. */
    val footer: String? = null,
    /** Package of the notifying app — Start opens this on peek-cycle tap. */
    val packageName: String? = null,
    /**
     * Optional structured temporal state (timer/stopwatch/world clock/alarm). When present the
     * launcher renders the value locally and ticks without provider refreshes. Absent → text-only
     * peek (fully backwards compatible).
     */
    val temporal: MetroTileTemporalState? = null,
) {
    val hasContent: Boolean
        get() = !title.isNullOrBlank() || !subtitle.isNullOrBlank() || !body.isNullOrBlank() ||
            temporal?.hasContent == true
}

internal object MetroTilePeekCodec {
    fun encode(peeks: List<MetroTilePeek>?): String? {
        if (peeks.isNullOrEmpty()) return null
        val array = JSONArray()
        peeks.forEach { peek ->
            if (!peek.hasContent) return@forEach
            array.put(
                JSONObject().apply {
                    peek.title?.let { put("title", it) }
                    peek.subtitle?.let { put("subtitle", it) }
                    peek.body?.let { put("body", it) }
                    peek.footer?.let { put("footer", it) }
                    peek.packageName?.takeIf { it.isNotBlank() }?.let { put("package", it) }
                    encodeTemporal(peek.temporal)?.let { put("temporal", it) }
                },
            )
        }
        return array.takeIf { it.length() > 0 }?.toString()
    }

    fun decode(raw: String?): List<MetroTilePeek>? {
        if (raw.isNullOrBlank()) return null
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val peek = MetroTilePeek(
                        title = obj.optString("title").takeIf { it.isNotBlank() },
                        subtitle = obj.optString("subtitle").takeIf { it.isNotBlank() },
                        body = obj.optString("body").takeIf { it.isNotBlank() },
                        footer = obj.optString("footer").takeIf { it.isNotBlank() },
                        packageName = obj.optString("package").takeIf { it.isNotBlank() },
                        temporal = decodeTemporal(obj.optJSONObject("temporal")),
                    )
                    if (peek.hasContent) add(peek)
                }
            }.takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        }
    }

    private fun encodeTemporal(state: MetroTileTemporalState?): JSONObject? {
        if (state == null || !state.hasContent) return null
        return JSONObject().apply {
            put("kind", state.kind)
            state.itemId?.let { put("item_id", it) }
            state.label?.let { put("label", it) }
            if (state.running) put("running", true)
            if (state.finished) put("finished", true)
            state.targetElapsedRealtimeMillis?.let { put("target_elapsed", it) }
            state.targetEpochMillis?.let { put("target_epoch", it) }
            state.pausedRemainingMillis?.let { put("paused_remaining", it) }
            state.accumulatedElapsedMillis?.let { put("accumulated", it) }
            state.runningSinceElapsedRealtimeMillis?.let { put("running_since_elapsed", it) }
            state.runningSinceEpochMillis?.let { put("running_since_epoch", it) }
            state.zoneId?.let { put("zone_id", it) }
        }
    }

    private fun decodeTemporal(obj: JSONObject?): MetroTileTemporalState? {
        if (obj == null) return null
        val kind = obj.optString("kind").takeIf { it.isNotBlank() } ?: return null
        return MetroTileTemporalState(
            kind = kind,
            itemId = obj.optString("item_id").takeIf { it.isNotBlank() },
            label = obj.optString("label").takeIf { it.isNotBlank() },
            running = obj.optBoolean("running", false),
            finished = obj.optBoolean("finished", false),
            targetElapsedRealtimeMillis = obj.optLongOrNull("target_elapsed"),
            targetEpochMillis = obj.optLongOrNull("target_epoch"),
            pausedRemainingMillis = obj.optLongOrNull("paused_remaining"),
            accumulatedElapsedMillis = obj.optLongOrNull("accumulated"),
            runningSinceElapsedRealtimeMillis = obj.optLongOrNull("running_since_elapsed"),
            runningSinceEpochMillis = obj.optLongOrNull("running_since_epoch"),
            zoneId = obj.optString("zone_id").takeIf { it.isNotBlank() },
        )
    }

    private fun JSONObject.optLongOrNull(key: String): Long? =
        if (has(key) && !isNull(key)) optLong(key) else null
}
