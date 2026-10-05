package com.pranshulgg.weather_master_app.core.prefs

import com.pranshulgg.weather_master_app.core.model.sources.SearchSource


data class AppPrefsState(
    val searchSource: SearchSource,
    val setSearchSource: (SearchSource) -> Unit,

    val backgroundUpdatesEnabled: Boolean,
    val setBackgroundUpdates: (Boolean) -> Unit,

    val backgroundUpdatesInterval: Int,
    val setBackgroundUpdatesInterval: (Int) -> Unit,

    val is24HrTimeFormat: Boolean,
    val set24HrTimeFormat: (Boolean) -> Unit,
)
