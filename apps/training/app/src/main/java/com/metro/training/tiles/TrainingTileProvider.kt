package com.metro.training.tiles

import com.metro.system.MetroTileData
import com.metro.system.MetroTileProvider
import kotlinx.coroutines.runBlocking

/** Exports the Training Live Tile via the shared Metro tile contract (authority com.metro.training.tiles). */
class TrainingTileProvider : MetroTileProvider() {
    override fun buildTileData(tileId: String): MetroTileData? {
        val ctx = context ?: return null
        return runBlocking { TrainingTileDataSource(ctx).buildTileData(tileId) }
    }
}
