package com.pranshulgg.weather_master_app.core.di.weather

import com.pranshulgg.weather_master_app.core.network.sources.weather.dwd.DwdApi
import com.pranshulgg.weather_master_app.core.network.sources.weather.dwd.DwdRepository
import com.pranshulgg.weather_master_app.core.network.sources.weather.metnorway.MetNorwayApi
import com.pranshulgg.weather_master_app.core.network.sources.weather.metnorway.MetNorwayRepository
import com.pranshulgg.weather_master_app.core.network.sources.weather.nws.NwsApi
import com.pranshulgg.weather_master_app.core.network.sources.weather.nws.NwsRepository
import com.pranshulgg.weather_master_app.core.network.sources.weather.openmeteo.OpenMeteoApi
import com.pranshulgg.weather_master_app.core.network.sources.weather.openmeteo.OpenMeteoRepository
import com.pranshulgg.weather_master_app.core.network.sources.weather.smhi.SmhiApi
import com.pranshulgg.weather_master_app.core.network.sources.weather.smhi.SmhiRepository
import com.pranshulgg.weather_master_app.data.local.dao.alerts.AlertsDao
import com.pranshulgg.weather_master_app.data.local.dao.weather.WeatherContextDao
import com.pranshulgg.weather_master_app.data.local.dao.weather.WeatherDao
import com.pranshulgg.weather_master_app.data.local.dao.weather.nws.NwsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WeatherRepositoryModule {
    @Provides
    @Singleton
    fun provideOpenMeteoRepository(
        dao: WeatherContextDao,
        api: OpenMeteoApi,
        weatherDao: WeatherDao
    ): OpenMeteoRepository = OpenMeteoRepository(dao, weatherDao, api)


    @Provides
    @Singleton
    fun provideNwsRepository(
        api: NwsApi,
        dao: WeatherContextDao,
        weatherDao: WeatherDao,
        nwsDao: NwsDao,
        alertsDao: AlertsDao
    ): NwsRepository = NwsRepository(dao, weatherDao, nwsDao, api, alertsDao)

    @Provides
    @Singleton
    fun provideMetNorwayRepository(
        dao: WeatherContextDao,
        api: MetNorwayApi,
        weatherDao: WeatherDao
    ): MetNorwayRepository = MetNorwayRepository(dao, weatherDao, api)

    @Provides
    @Singleton
    fun provideSmhiRepository(
        dao: WeatherContextDao,
        api: SmhiApi,
        weatherDao: WeatherDao
    ): SmhiRepository = SmhiRepository(dao, weatherDao, api)

    @Provides
    @Singleton
    fun provideDwdRepository(
        dao: WeatherContextDao,
        api: DwdApi,
        weatherDao: WeatherDao
    ): DwdRepository = DwdRepository(dao, weatherDao, api)

}
