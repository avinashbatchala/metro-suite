package com.pranshulgg.weather_master_app.core.model.sources

import com.pranshulgg.weather_master_app.R


// TODO: implement alerts / air quality for sources that support it
enum class Source(
    val displayName: String,
    val hourlyAggregationLimitHours: Int = 24, // stupid and should be removed
    val displayLink: String,
    val fullName: String,
    val countryNameRes: Int? = null,
    val requiresUserApiKey: Boolean = false, // Source must not be selectable until the user has provided their API key
    val signupLink: String? = null, // Shown instead of displayLink on the API key entry screen, when set
    val apiKeyNote: String? = null, // Optional hint shown under the link on the API key entry screen
    val regionalButWorldwideSupport: Boolean = false,
    val capabilities: Set<Capability>,
) {
    OPEN_METEO(
        displayName = "Open Meteo",
        fullName = "Open Meteo",
        displayLink = "https://open-meteo.com/",
        capabilities = setOf(Capability.WEATHER)
    ),
    NWS(
        displayName = "NWS",
        fullName = "National Weather Service",
        displayLink = "https://www.weather.gov/documentation/services-web-api",
        countryNameRes = R.string.country_usa,
        capabilities = setOf(Capability.WEATHER, Capability.ALERTS)
    ),
    SMHI(
        displayName = "SMHI",
        fullName = "Swedish Meteorological and Hydrological Institute",
        displayLink = "https://opendata.smhi.se",
        countryNameRes = R.string.country_sweden,
        capabilities = setOf(Capability.WEATHER)
    ),
    DWD(
        displayName = "DWD",
        fullName = "Bright Sky DWD",
        displayLink = "https://brightsky.dev",
        countryNameRes = R.string.country_germany,
        capabilities = setOf(Capability.WEATHER)
    ),
    MET_NORWAY(
        displayName = "Met Norway",
        fullName = "Met Norway",
        displayLink = "https://api.met.no/",
        regionalButWorldwideSupport = true,
        capabilities = setOf(Capability.WEATHER)
    ),
    NONE(
        displayName = "None",
        fullName = "",
        displayLink = "",
        capabilities = setOf(Capability.ALERTS, Capability.AIR_QUALITY)
    );

    // Sources that provide snow/rain as precipitation
    fun providesSnowFall(): Boolean {
        return when (this) {
            MET_NORWAY -> false
            DWD -> false
            else -> true
        }
    }
}


// WE MAP EVERY WEATHER SOURCE HERE, AS THEY GET ADDED

private val sourcesByCountry = buildMap {
    put("US", listOf(Source.NWS))
    put("SE", listOf(Source.SMHI))
    put("DE", listOf(Source.DWD))
    put("NO", listOf(Source.MET_NORWAY))
}

fun getSourcesForCountry(countryCode: String?): List<Source> {
    return sourcesByCountry[countryCode] ?: emptyList()
}

/**
 * Any source that has global coverage,
 * including regional sources with global data,
 * must be added here.
 */
private val sourcesGlobal = listOf(

    // GLOBAL
    Source.OPEN_METEO,

    // REGIONAL WITH GLOBAL
    Source.MET_NORWAY,

    // Yes, "NONE" is a global source :P
    Source.NONE
)

fun getSourcesGlobal(): List<Source> {
    return sourcesGlobal
}

fun Source.isSourceSupportedFor(countryCode: String?): Boolean {
    val source = sourcesByCountry[countryCode] ?: return false

    return this in source
}

fun Source.isGlobal(): Boolean {
    return this in sourcesGlobal
}

enum class Capability {
    WEATHER,
    ALERTS,
    AIR_QUALITY
}
