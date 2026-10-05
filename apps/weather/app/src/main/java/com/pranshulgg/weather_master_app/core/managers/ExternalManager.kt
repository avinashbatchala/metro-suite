package com.pranshulgg.weather_master_app.core.managers

import android.content.Context
import com.pranshulgg.weather_master_app.data.repository.WeatherContextRepository
import com.pranshulgg.weather_master_app.data.store.WeatherStore
import com.pranshulgg.weather_master_app.data.store.WeatherUnitsStore
import com.pranshulgg.weather_master_app.data.worker.WeatherBackgroundUpdateScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

// UPDATES NOTIFICATION/WIDGETS
// APP SIDE ONLY!!!
@Singleton
class ExternalManager @Inject constructor(
    private val weatherStore: WeatherStore,
    private val weatherUnitsStore: WeatherUnitsStore,
    private val weatherContextRepository: WeatherContextRepository,
    @ApplicationContext val context: Context
) {


    suspend fun refreshLiveTiles() {
        val weather = weatherStore.data.value.weather
        if (weather != null && weather.location.isDefault) {
            val locations = weatherContextRepository.getLocationsOnce()
            WeatherBackgroundUpdateScheduler.refreshLiveTiles(
                context = context,
                locations = locations
            )
        }
    }

    fun refreshNotifications() {
        // Notifications are no longer part of the trimmed Metro weather experience.
        // Kept as a no-op so the shared weather refresh pipeline stays intact.
    }

}

