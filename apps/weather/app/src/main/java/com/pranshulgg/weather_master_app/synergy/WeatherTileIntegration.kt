package com.pranshulgg.weather_master_app.synergy

import android.content.Context
import com.metro.system.MetroIntents
import com.metro.system.MetroPreferences
import com.metro.system.MetroTileContract
import com.metro.system.MetroTileData
import com.metro.system.MetroTilePeek
import com.metro.system.MetroTileProvider
import com.metro.system.MetroTileUpdates
import com.metro.system.MetroTileWidgetFace
import com.metro.system.MetroTileWidgetFaceKind
import com.pranshulgg.weather_master_app.core.model.domain.location.Location
import com.pranshulgg.weather_master_app.core.model.domain.weather.Weather
import com.pranshulgg.weather_master_app.core.model.domain.weather.WeatherUnits
import com.pranshulgg.weather_master_app.core.model.weather.TemperatureUnit
import com.pranshulgg.weather_master_app.core.model.weather.toLabel
import com.pranshulgg.weather_master_app.core.utils.formatters.toWeekdayString
import com.pranshulgg.weather_master_app.data.repository.WeatherContextRepository
import com.pranshulgg.weather_master_app.data.repository.WeatherUnitsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import kotlin.math.roundToInt

/** Hilt access for the tile ContentProvider (which is not an @AndroidEntryPoint). */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WeatherTileEntryPoint {
    fun weatherContextRepository(): WeatherContextRepository
    fun weatherUnitsRepository(): WeatherUnitsRepository
}

/**
 * Exports one live tile per saved location as a MetroSuite Start tile.
 *
 * Tile id is the location id; the launcher's default id ([MetroTileContract.DEFAULT_TILE_ID])
 * resolves to the default location. Discovered via manifest metadata
 * [MetroTileContract.METADATA_TILE_PROVIDER] on authority `{applicationId}.tiles`.
 */
class WeatherTileProvider : MetroTileProvider() {
    override fun buildTileData(tileId: String): MetroTileData? {
        val ctx = context ?: return null
        return WeatherTileDataSource(ctx).buildTileData(tileId)
    }
}

/**
 * Builds a [MetroTileData] from the cached weather for the requested location. Reads the Room
 * database synchronously (the provider is called on a binder thread by the launcher).
 */
class WeatherTileDataSource(context: Context) {
    private val appContext = context.applicationContext

    fun buildTileData(tileId: String): MetroTileData? {
        val entryPoint = EntryPointAccessors.fromApplication(
            appContext,
            WeatherTileEntryPoint::class.java,
        )
        val contextRepository = entryPoint.weatherContextRepository()
        val units = runBlocking { entryPoint.weatherUnitsRepository().getUnitsOnce() }
            ?: WeatherUnits.getDefault()
        val locations = runBlocking { contextRepository.getLocationsOnce() }
        val location = if (tileId == MetroTileContract.DEFAULT_TILE_ID) {
            locations.firstOrNull { it.isDefault } ?: locations.firstOrNull()
        } else {
            locations.firstOrNull { it.id == tileId }
        } ?: return null
        val weather = runBlocking {
            runCatching { contextRepository.getWeatherForLocation(location.id) }.getOrNull()
        } ?: return null
        return weather.toTileData(appContext, units)
    }
}

/**
 * Exports a cycling Start tile (no app icon): face 1 is the current conditions, then the next
 * three days (weekday + hi/lo + condition). Uses the PEEK_CYCLE widget face so the launcher
 * rotates the faces.
 */
private fun Weather.toTileData(context: Context, units: WeatherUnits): MetroTileData {
    val placeLabel = location.customName ?: location.name
    val timezone = location.timezone
    val accentHex = MetroPreferences(context).accentColorHex

    fun temp(value: Double?): Int? =
        TemperatureUnit.CELSIUS.convert(value, units.tempUnit)?.roundToInt()

    val currentFace = MetroTilePeek(
        title = placeLabel,
        subtitle = temp(current.temperature)?.let { "$it°" } ?: "--",
        body = current.weatherCondition.toLabel(context).takeIf { it.isNotBlank() },
        footer = "Weather",
        packageName = context.packageName,
    )

    val dayFaces = daily.drop(1).take(3).map { day ->
        MetroTilePeek(
            title = toWeekdayString(day.time, timezone),
            subtitle = listOfNotNull(
                temp(day.temperatureMax)?.let { "H $it°" },
                temp(day.temperatureMin)?.let { "L $it°" },
            ).joinToString("   ").takeIf { it.isNotBlank() },
            body = day.weatherCondition.toLabel(context).takeIf { it.isNotBlank() },
            footer = placeLabel,
            packageName = context.packageName,
        )
    }

    return MetroTileData(
        title = placeLabel,
        backgroundColorHex = accentHex,
        widgetFace = MetroTileWidgetFace(kind = MetroTileWidgetFaceKind.PEEK_CYCLE),
        peeks = (listOf(currentFace) + dayFaces).filter { it.hasContent },
    )
}

/** Tells the launcher to re-read a tile (e.g. after a weather refresh). */
object WeatherTileSync {
    fun request(context: Context, tileId: String? = null) {
        MetroTileUpdates.requestUpdate(
            context.applicationContext,
            context.packageName,
            tileId ?: MetroTileContract.DEFAULT_TILE_ID,
        )
    }
}

/**
 * Asks the MetroSuite launcher to pin a location's weather tile to Start.
 * Replaces the old `com.ab.action.PIN_WEATHER_TILE` bridge to the Win10 launcher.
 */
object WeatherTilePin {
    fun pin(context: Context, location: Location, size: String? = null): Boolean {
        WeatherTileSync.request(context, location.id)
        MetroIntents.requestPinTile(
            context = context.applicationContext,
            packageName = context.packageName,
            tileId = location.id,
            size = size,
        )
        return true
    }
}
