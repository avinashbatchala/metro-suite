package com.pranshulgg.weather_master_app.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pranshulgg.weather_master_app.R
import com.pranshulgg.weather_master_app.core.prefs.LocalAppPrefs
import com.pranshulgg.weather_master_app.core.ui.components.SettingSection
import com.pranshulgg.weather_master_app.core.ui.components.SettingTile
import com.pranshulgg.weather_master_app.core.ui.components.SettingsTileIcon
import com.pranshulgg.weather_master_app.core.ui.components.WeatherPageHeader

/**
 * Slim Metro Settings: units, 24-hour time, background updates and about.
 */
@Composable
fun SettingsScreen(
    onOpenUnits: () -> Unit,
    onOpenBackgroundUpdates: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val prefs = LocalAppPrefs.current

    Column(modifier = Modifier.fillMaxSize()) {
        WeatherPageHeader(title = stringResource(R.string.settings))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 4.dp)
        ) {
            SettingSection(
                staggerStartIndex = 1,
                tiles = listOf(
                    SettingTile.ActionTile(
                        leading = { SettingsTileIcon(R.drawable.linear_scale_24px) },
                        title = stringResource(R.string.setting_units),
                        onClick = onOpenUnits
                    ),
                    SettingTile.SwitchTile(
                        leading = { SettingsTileIcon(R.drawable.schedule_48px) },
                        title = stringResource(R.string.settings_24hr_time),
                        checked = prefs.is24HrTimeFormat,
                        onCheckedChange = { prefs.set24HrTimeFormat(it) }
                    ),
                    SettingTile.ActionTile(
                        leading = { SettingsTileIcon(R.drawable.sync_24px) },
                        title = stringResource(R.string.setting_background_updates),
                        description = stringResource(R.string.setting_background_updates_secondary),
                        onClick = onOpenBackgroundUpdates
                    ),
                    SettingTile.ActionTile(
                        leading = { SettingsTileIcon(R.drawable.info_24px) },
                        title = stringResource(R.string.setting_about_app),
                        description = stringResource(R.string.setting_about_app_secondary),
                        onClick = onOpenAbout
                    )
                )
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
