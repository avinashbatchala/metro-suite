package com.pranshulgg.weather_master_app.core.prefs


import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import com.pranshulgg.weather_master_app.core.model.sources.SearchSource
import com.pranshulgg.weather_master_app.core.prefs.helper.PreferencesHelper

object AppPrefs {
    private val _searchSource = mutableStateOf(SearchSource.OPEN_METEO)

    private val _backgroundUpdatesEnabled = mutableStateOf(false)

    private val _backgroundUpdatesInterval = mutableIntStateOf(60)

    private val _is24HrTimeFormat = mutableStateOf(true)


    fun initPrefs(context: Context) {
        PreferencesHelper.init(context)

        _searchSource.value = PreferencesHelper.getString("searchSource")
            ?.let { runCatching { SearchSource.valueOf(it) }.getOrNull() }
            ?: SearchSource.OPEN_METEO

        _backgroundUpdatesEnabled.value =
            PreferencesHelper.getBool("backgroundUpdatesEnabled") ?: false


        _backgroundUpdatesInterval.intValue =
            PreferencesHelper.getInt("backgroundUpdatesInterval") ?: 60

        _is24HrTimeFormat.value = PreferencesHelper.getBool("is24HrTimeFormat") ?: true

    }

    @Composable
    fun state(): AppPrefsState = AppPrefsState(

        searchSource = _searchSource.value,
        setSearchSource = {
            _searchSource.value = it
            PreferencesHelper.setString("searchSource", it.name)
        },

        backgroundUpdatesEnabled = _backgroundUpdatesEnabled.value,
        setBackgroundUpdates = {
            _backgroundUpdatesEnabled.value = it
            PreferencesHelper.setBool("backgroundUpdatesEnabled", it)
        },

        backgroundUpdatesInterval = _backgroundUpdatesInterval.intValue,
        setBackgroundUpdatesInterval = {
            _backgroundUpdatesInterval.intValue = it
            PreferencesHelper.setInt("backgroundUpdatesInterval", it)
        },

        is24HrTimeFormat = _is24HrTimeFormat.value,
        set24HrTimeFormat = {
            _is24HrTimeFormat.value = it
            PreferencesHelper.setBool("is24HrTimeFormat", it)
        },
    )
}
