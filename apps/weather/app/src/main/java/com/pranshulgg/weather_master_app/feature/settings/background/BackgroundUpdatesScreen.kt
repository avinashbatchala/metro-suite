package com.pranshulgg.weather_master_app.feature.settings.background

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.pranshulgg.weather_master_app.R
import com.pranshulgg.weather_master_app.core.prefs.LocalAppPrefs
import com.pranshulgg.weather_master_app.core.ui.components.SettingSection
import com.pranshulgg.weather_master_app.core.ui.components.SettingTile
import com.pranshulgg.weather_master_app.core.ui.components.SettingsTileIcon
import com.pranshulgg.weather_master_app.core.ui.components.WeatherPageHeader
import com.pranshulgg.weather_master_app.core.ui.components.tiles.DialogOption


@Composable
fun BackgroundUpdatesScreen() {
    val viewModel: BackgroundUpdatesViewModel = hiltViewModel()

    val prefs = LocalAppPrefs.current
    val intervals = mapOf(
        60 to stringResource(R.string.time_hour, "1"),
        90 to stringResource(R.string.time_hours, "1.5"),
        120 to stringResource(R.string.time_hours, "2"),
        180 to stringResource(R.string.time_hours, "3"),
        240 to stringResource(R.string.time_hours, "4"),
        300 to stringResource(R.string.time_hours, "5"),
        360 to stringResource(R.string.time_hours, "6")
    )

    val intervalOptions = intervals.map { DialogOption(it.key.toString(), it.value) }

    Column(modifier = Modifier.fillMaxSize()) {
        WeatherPageHeader(title = stringResource(R.string.setting_background_updates))

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SettingSection(
                title = stringResource(R.string.setting_updates),
                primarySwitch = true,
                tiles = listOf(
                    SettingTile.SwitchTile(
                        title = stringResource(R.string.setting_background_updates),
                        checked = prefs.backgroundUpdatesEnabled,
                        onCheckedChange = {
                            prefs.setBackgroundUpdates(it)
                            if (it) {
                                viewModel.scheduleWeatherUpdates(prefs.backgroundUpdatesInterval)
                            } else {
                                viewModel.disableWeatherUpdates()
                            }
                        }
                    )
                )
            )

            SettingSection(
                title = stringResource(R.string.setting_additional),
                tiles = listOf(
                    SettingTile.DialogOptionTile(
                        leading = { SettingsTileIcon(R.drawable.update_24px) },
                        title = stringResource(R.string.setting_update_interval),
                        options = intervalOptions,
                        selectedOption = prefs.backgroundUpdatesInterval.toString(),
                        onOptionSelected = {
                            prefs.setBackgroundUpdatesInterval(it.toInt())

                            if (prefs.backgroundUpdatesEnabled) {
                                viewModel.scheduleWeatherUpdates(it.toInt())
                            }
                        }
                    )
                )
            )
        }
    }
}
