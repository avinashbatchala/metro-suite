package com.metro.clock.tiles

import android.content.Context
import com.metro.system.MetroTileContract
import com.metro.system.MetroTileUpdates

/** Requests a launcher tile refresh — called only on meaningful state changes, never per tick. */
object ClockTileRefresh {
    fun request(context: Context, tileId: String = MetroTileContract.DEFAULT_TILE_ID) {
        MetroTileUpdates.requestUpdate(context.applicationContext, context.packageName, tileId)
    }

    fun requestAll(context: Context, tileIds: Collection<String>) {
        tileIds.forEach { request(context, it) }
    }
}
