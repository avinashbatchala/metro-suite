package com.pranshulgg.weather_master_app.synergy

import android.content.Context
import com.metro.system.MetroIntents
import com.metro.system.MetroPreferences
import com.metro.system.MetroTileContract
import com.metro.system.MetroTileData
import com.metro.system.MetroTileProvider
import com.metro.system.MetroTileUpdates
import com.pranshulgg.weather_master_app.core.model.domain.weather.Weather
import com.pranshulgg.weather_master_app.core.model.domain.weather.WeatherUnits
import com.pranshulgg.weather_master_app.core.model.weather.TemperatureUnit
import com.pranshulgg.weather_master_app.core.model.weather.toIcon
import com.pranshulgg.weather_master_app.core.model.weather.toLabel
import com.pranshulgg.weather_master_app.core.utils.formatters.getCurrentTimeFor
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
 * Exports the current location's weather as a MetroSuite Start live tile.
 *
 * The launcher discovers this via manifest metadata [MetroTileContract.METADATA_TILE_PROVIDER]
 * on authority `{applicationId}.tiles` and reads it through [MetroTileContract.readTile].
 */
class WeatherTileProvider : MetroTileProvider() {
    override fun buildTileData(tileId: String): MetroTileData? {
        if (tileId != MetroTileContract.DEFAULT_TILE_ID) return null
        val ctx = context ?: return null
        return WeatherTileDataSource(ctx).buildTileData() ?: emptyTile(ctx)
    }
}

/**
 * Builds a [MetroTileData] from the cached weather for the default location. Reads the Room
 * database synchronously (the provider is called on a binder thread by the launcher).
 */
class WeatherTileDataSource(context: Context) {
    private val appContext = context.applicationContext

    fun buildTileData(): MetroTileData? {
        val entryPoint = EntryPointAccessors.fromApplication(
            appContext,
            WeatherTileEntryPoint::class.java,
        )
        val contextRepository = entryPoint.weatherContextRepository()
        val units = runBlocking { entryPoint.weatherUnitsRepository().getUnitsOnce() }
            ?: WeatherUnits.getDefault()
        val location = runBlocking { contextRepository.getLocationsOnce() }
            .firstOrNull { it.isDefault } ?: return null
        val weather = runBlocking {
            runCatching { contextRepository.getWeatherForLocation(location.id) }.getOrNull()
        } ?: return null
        return weather.toTileData(appContext, units)
    }
}

private fun Weather.toTileData(context: Context, units: WeatherUnits): MetroTileData {
    val temperature = TemperatureUnit.CELSIUS
        .convert(current.temperature, units.tempUnit)
        ?.roundToInt()
    val condition = current.weatherCondition.toLabel(context)
    val accentHex = MetroPreferences(context).accentColorHex
    val placeLabel = location.customName ?: location.name

    // Resolve the condition icon so the back face can show it; the drawable resource is exported
    // as an android.resource:// URI the launcher can load.
    val iconRes = current.weatherCondition.toIcon(
        targetTimeMilli = getCurrentTimeFor(location.timezone),
        daily = daily.firstOrNull(),
    )

    return MetroTileData(
        title = placeLabel,
        backgroundColorHex = accentHex,
        backFaceTitle = listOfNotNull(
            temperature?.let { "$it°" },
            condition,
        ).joinToString("  "),
        backFaceImageUri = "android.resource://${context.packageName}/$iconRes",
    )
}

private fun emptyTile(context: Context): MetroTileData = MetroTileData(
    title = "weather",
    backgroundColorHex = MetroPreferences(context).accentColorHex,
)

/** Tells the launcher to re-read this app's tile (e.g. after a weather refresh). */
object WeatherTileSync {
    fun request(context: Context) {
        MetroTileUpdates.requestUpdate(context.applicationContext, context.packageName)
    }
}

/**
 * Asks the MetroSuite launcher to pin the weather tile to Start. Replaces the old
 * `com.ab.action.PIN_WEATHER_TILE` bridge to the Win10 launcher.
 */
object WeatherTilePin {
    fun pin(
        context: Context,
        @Suppress("UNUSED_PARAMETER") placeLabel: String? = null,
        @Suppress("UNUSED_PARAMETER") latitude: Double? = null,
        @Suppress("UNUSED_PARAMETER") longitude: Double? = null,
        @Suppress("UNUSED_PARAMETER") timezone: String? = null,
        @Suppress("UNUSED_PARAMETER") locationId: String? = null,
        @Suppress("UNUSED_PARAMETER") size: String = "wide",
    ): Boolean {
        WeatherTileSync.request(context)
        MetroIntents.requestPinTile(context.applicationContext, context.packageName)
        return true
    }
}
