package com.metro.clock.worldclock

import android.content.Context
import com.metro.clock.data.ClockDatabase
import com.metro.clock.data.WorldClockCityEntity
import com.metro.clock.tiles.ClockTileRefresh
import com.metro.system.MetroWorldClockCatalog
import com.metro.system.MetroWorldClockCity

class WorldClockRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dao = ClockDatabase.get(appContext).dao()

    suspend fun cityIds(): List<String> = dao.worldCities().map { it.cityId }

    suspend fun cities(): List<MetroWorldClockCity> = MetroWorldClockCatalog.resolve(cityIds())

    suspend fun has(cityId: String): Boolean = dao.worldCities().any { it.cityId == cityId }

    suspend fun add(cityId: String): Boolean {
        if (MetroWorldClockCatalog.byId(cityId) == null) return false
        if (has(cityId)) return false
        val order = (dao.maxWorldCityOrder() ?: -1) + 1
        dao.upsertWorldCity(WorldClockCityEntity(cityId, order))
        ClockTileRefresh.request(appContext, tileIdFor(cityId))
        ClockTileRefresh.request(appContext)
        return true
    }

    suspend fun remove(cityId: String) {
        dao.deleteWorldCity(cityId)
        ClockTileRefresh.request(appContext, tileIdFor(cityId))
        ClockTileRefresh.request(appContext)
    }

    /** Move a city by [delta] positions (−1 = up, +1 = down), persisting the new `sortOrder`. */
    suspend fun move(cityId: String, delta: Int) {
        val list = dao.worldCities().sortedBy { it.sortOrder }
        val index = list.indexOfFirst { it.cityId == cityId }
        if (index < 0 || delta == 0) return
        val target = (index + delta).coerceIn(0, list.size - 1)
        if (target == index) return
        val reordered = list.toMutableList().apply { add(target, removeAt(index)) }
        dao.upsertWorldCities(reordered.mapIndexed { i, entity -> entity.copy(sortOrder = i) })
        ClockTileRefresh.request(appContext)
    }

    companion object {
        fun tileIdFor(cityId: String): String = "world:$cityId"
    }
}
