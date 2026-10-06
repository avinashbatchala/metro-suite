package com.metro.clock.tiles

import com.metro.system.MetroTileData
import com.metro.system.MetroTileProvider
import kotlinx.coroutines.runBlocking

/** Exports Clock Live Tiles via the shared Metro tile contract (authority com.metro.clock.tiles). */
class ClockTileProvider : MetroTileProvider() {
    override fun buildTileData(tileId: String): MetroTileData? {
        val ctx = context ?: return null
        return runBlocking { ClockTileDataSource(ctx).buildTileData(tileId) }
    }
}
