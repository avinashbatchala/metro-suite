package com.pranshulgg.weather_master_app.feature.blocks.screens.precipitation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.pranshulgg.weather_master_app.R
import com.pranshulgg.weather_master_app.core.ui.components.Gap
import com.pranshulgg.weather_master_app.core.ui.components.LargeTopBarScaffold
import com.pranshulgg.weather_master_app.core.ui.components.NavigateUpBtn
import com.pranshulgg.weather_master_app.core.utils.formatters.getCurrentTimeFor
import com.pranshulgg.weather_master_app.core.utils.formatters.toDateString
import com.pranshulgg.weather_master_app.core.utils.weather.forecast.findMatchingHourly
import com.pranshulgg.weather_master_app.feature.blocks.BlocksScreenViewModel
import com.pranshulgg.weather_master_app.feature.blocks.components.AboutCard
import com.pranshulgg.weather_master_app.feature.blocks.components.AboutCardText
import com.pranshulgg.weather_master_app.feature.blocks.screens.precipitation.components.RainHourlyCard

@Composable
fun RainScreen(navController: NavController, index: Int = 0, locationId: String) {

    val viewModel: BlocksScreenViewModel = hiltViewModel()

    val weather = viewModel.weather.collectAsState().value.weather
    val hourly = weather?.hourly ?: return
    val context = LocalContext.current
    val units = viewModel.units.collectAsState().value.units
    val zoneId = weather.location.timezone
    val time =
        if (index != 0) weather.daily[index].time else getCurrentTimeFor(weather.location.timezone)


    val data = findMatchingHourly(
        hourly,
        time,
        weather.location.source,
        weather.location.timezone,
        keepPastHour = index == 0


    )
    val date = toDateString(weather.daily[index].time, weather.location.timezone)


    val isOnlyPrecipitation = !weather.location.source.providesSnowFall()


    LargeTopBarScaffold(
        title = stringResource(if (isOnlyPrecipitation) R.string.weather_precipitation else R.string.weather_rain_block),
        navigationIcon = { NavigateUpBtn(navController) },
        actions = {
            MetroText(
                text = date,
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.padding(end = 16.dp)
            )
        }
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MetroTheme.colors.background)
                    .verticalScroll(rememberScrollState())
                    .padding(paddingValues)
        ) {
            RainHourlyCard(data, zoneId, units.precipitationUnit, context)
            Gap(14.dp)
            AboutCard { AboutCardText(stringResource(R.string.weather_about_precipitation)) }
            Gap(WindowInsets.systemBars.asPaddingValues().calculateBottomPadding() + 30.dp)
        }


    }
}
