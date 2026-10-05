package com.pranshulgg.weather_master_app.data.provider

import com.pranshulgg.weather_master_app.core.model.sources.Source
import com.pranshulgg.weather_master_app.core.network.sources.weather.dwd.DwdRepository
import com.pranshulgg.weather_master_app.core.network.sources.weather.metnorway.MetNorwayRepository
import com.pranshulgg.weather_master_app.core.network.sources.weather.nws.NwsRepository
import com.pranshulgg.weather_master_app.core.network.sources.weather.openmeteo.OpenMeteoRepository
import com.pranshulgg.weather_master_app.core.network.sources.weather.smhi.SmhiRepository
import com.pranshulgg.weather_master_app.data.repository.airquality.AirQualityRepository
import com.pranshulgg.weather_master_app.data.repository.alerts.AlertRepository
import com.pranshulgg.weather_master_app.data.repository.weather.WeatherRepository
import javax.inject.Inject

class SourceRepositoryProvider @Inject constructor(
    private val openMeteoRepository: OpenMeteoRepository,
    private val nwsRepository: NwsRepository,
    private val metNorwayRepository: MetNorwayRepository,
    private val smhiRepository: SmhiRepository,
    private val dwdRepository: DwdRepository
) {

    val repositories = listOf(
        openMeteoRepository,
        nwsRepository,
        metNorwayRepository,
        smhiRepository,
        dwdRepository
    )

    fun getWeatherRepository(source: Source): WeatherRepository {
        return repositories.firstOrNull {
            it.weatherSource == source
        } ?: openMeteoRepository
    }

    fun getAlertRepository(source: Source): AlertRepository? {
        return repositories.firstOrNull {
            it.alertSource == source
        }
    }

    fun getAirQualityRepository(source: Source): AirQualityRepository? {
        return repositories.firstOrNull {
            it.airQualitySource == source
        }
    }

}
