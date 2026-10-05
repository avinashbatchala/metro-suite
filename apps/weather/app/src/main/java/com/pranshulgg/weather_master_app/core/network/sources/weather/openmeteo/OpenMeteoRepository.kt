package com.pranshulgg.weather_master_app.core.network.sources.weather.openmeteo

import com.pranshulgg.weather_master_app.core.model.domain.location.Location
import com.pranshulgg.weather_master_app.core.model.domain.weather.Weather
import com.pranshulgg.weather_master_app.core.model.sources.Source
import com.pranshulgg.weather_master_app.core.model.weather.FinishedWeatherResult
import com.pranshulgg.weather_master_app.core.model.weather.WeatherDataPack
import com.pranshulgg.weather_master_app.core.network.calls.safeApiCall
import com.pranshulgg.weather_master_app.data.local.dao.weather.WeatherContextDao
import com.pranshulgg.weather_master_app.data.local.dao.weather.WeatherDao
import com.pranshulgg.weather_master_app.data.local.mapper.weather.sources.openmeteo.toDomain
import com.pranshulgg.weather_master_app.data.local.mapper.weather.toCurrentWeatherEntity
import com.pranshulgg.weather_master_app.data.local.mapper.weather.toDailyWeatherEntity
import com.pranshulgg.weather_master_app.data.local.mapper.weather.toHourlyWeatherEntity
import com.pranshulgg.weather_master_app.data.repository.capability.AirQualityCapability
import com.pranshulgg.weather_master_app.data.repository.capability.AlertCapability
import com.pranshulgg.weather_master_app.data.repository.capability.WeatherCapability
import com.pranshulgg.weather_master_app.data.repository.data.BaseRepository
import com.pranshulgg.weather_master_app.data.repository.weather.CacheModel
import javax.inject.Inject

class OpenMeteoRepository @Inject constructor(
    val dao: WeatherContextDao,
    val weatherDao: WeatherDao,
    val api: OpenMeteoApi
) : BaseRepository() {

    override val weatherSource = Source.OPEN_METEO
    override val airQualitySource = Source.OPEN_METEO
    override val alertSource = Source.NONE


    override fun weatherCapability(): WeatherCapability {
        return object : WeatherCapability {

            override suspend fun fetchAndProcess(
                location: Location,
                isManualRefresh: Boolean,
                isForceRefresh: Boolean,
                cacheModel: CacheModel
            ): WeatherDataPack {

                val response = safeApiCall {
                    api.fetchWeather(
                        location.latitude,
                        location.longitude,
                        location.timezone,
                        model = location.openMeteoModel.modelId
                    )
                }.getOrThrow()

                val domain = response.toDomain(location)

                return WeatherDataPack(domain)
            }

            override suspend fun saveToDb(data: WeatherDataPack, cacheModel: CacheModel) {
                weatherDao.insertWeather(
                    data.weather.current.toCurrentWeatherEntity(data.weather.location.id),
                    data.weather.hourly.toHourlyWeatherEntity(data.weather.location),
                    data.weather.daily.toDailyWeatherEntity(data.weather.location.id),
                    data.weather.location.id
                )
            }

            override fun finishedResult(data: Weather): FinishedWeatherResult {
                return FinishedWeatherResult(weather = data)
            }
        }
    }


    override fun airQualityCapability(): AirQualityCapability? = null

    override fun alertCapability(): AlertCapability? = null
}