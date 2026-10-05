package com.pranshulgg.weather_master_app.feature.blocks.screens.visibility

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
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
import com.pranshulgg.weather_master_app.core.model.weather.DistanceUnit
import com.pranshulgg.weather_master_app.core.ui.components.Gap
import com.pranshulgg.weather_master_app.core.ui.components.LargeTopBarScaffold
import com.pranshulgg.weather_master_app.core.ui.components.NavigateUpBtn
import com.pranshulgg.weather_master_app.core.utils.formatters.getCurrentTimeFor
import com.pranshulgg.weather_master_app.core.utils.formatters.toDateString
import com.pranshulgg.weather_master_app.core.utils.weather.forecast.findMatchingHourly
import com.pranshulgg.weather_master_app.feature.blocks.BlocksScreenViewModel
import com.pranshulgg.weather_master_app.feature.blocks.components.AboutCard
import com.pranshulgg.weather_master_app.feature.blocks.components.AboutCardText
import com.pranshulgg.weather_master_app.feature.blocks.components.NoHourlyDataAvailable
import com.pranshulgg.weather_master_app.feature.blocks.components.ScaleCard
import com.pranshulgg.weather_master_app.feature.blocks.screens.visibility.components.VisibilityHourlyCard

private data class VisibilityScaleRange(
    val headlineRes: Int,
    val descriptionRes: Int,
    val kmScale: String,
    val miScale: String,
    val mScale: String,
)

private data class VisibilityScaleInfo(
    val headline: String,
    val description: String,
    val scale: String
)

@Composable
fun VisibilityScreen(navController: NavController, index: Int = 0, locationId: String) {

    val viewModel: BlocksScreenViewModel = hiltViewModel()

    val weather = viewModel.weather.collectAsState().value.weather
    val hourly = weather?.hourly ?: return
    val units = viewModel.units.collectAsState().value.units
    val context = LocalContext.current

    val time =
        if (index != 0) weather.daily[index].time else getCurrentTimeFor(weather.location.timezone)

    val data = findMatchingHourly(
        hourly,
        time,
        weather.location.source,
        weather.location.timezone,
        keepPastHour = index == 0


    )
    val zoneId = weather.location.timezone

    val date = toDateString(
        weather.daily[index].time,
        weather.location.timezone
    )
    val scale = getVisibilityScaleFor(units.distanceUnit)
    val visibility = data.map { it.visibility }



    LargeTopBarScaffold(
        title = stringResource(R.string.weather_visibility),
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
            if (visibility.isNotEmpty() && !visibility.contains(null)) {
                VisibilityHourlyCard(data, zoneId, units.distanceUnit, context)
            } else {
                NoHourlyDataAvailable()
            }
            Gap(14.dp)
            AboutCard {
                AboutCardText(stringResource(R.string.weather_visibility_about))
            }
            Gap(14.dp)
            ScaleCard {
                scale.forEach {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            MetroText(
                                text = it.headline,
                                color = MetroTheme.colors.primaryText,
                                style = MetroTextStyle.ListItemTitle
                            )
                            MetroText(
                                text = it.description,
                                color = MetroTheme.colors.secondaryText,
                                style = MetroTextStyle.ListItemSubtitle
                            )
                        }
                        MetroText(
                            text = it.scale,
                            color = MetroTheme.colors.secondaryText,
                            style = MetroTextStyle.Body
                        )
                    }
                }
            }
            Gap(WindowInsets.systemBars.asPaddingValues().calculateBottomPadding() + 30.dp)

        }
    }
}

private val visibilityRanges = listOf(
    VisibilityScaleRange(
        R.string.text_excellent,
        R.string.visibility_description_excellent,
        "> 50 km",
        "> 30 mi",
        "> 50000 m"
    ),
    VisibilityScaleRange(
        R.string.text_very_good,
        R.string.visibility_description_very_good,
        "20 - 50 km",
        "12 - 30 mi",
        "20000 - 50000 m"
    ),
    VisibilityScaleRange(
        R.string.text_good,
        R.string.visibility_description_good,
        "10 - 20 km",
        "6 - 12 mi",
        "10000 - 20000 m"
    ),
    VisibilityScaleRange(
        R.string.text_moderate,
        R.string.visibility_description_moderate,
        "4 - 10 km",
        "2.5 - 6 mi",
        "4000 - 10000 m"
    ),
    VisibilityScaleRange(
        R.string.text_poor,
        R.string.visibility_description_poor,
        "1 - 4 km",
        "0.6 - 2.5 mi",
        "1000 - 4000 m"
    ),
    VisibilityScaleRange(
        R.string.text_very_poor,
        R.string.visibility_description_very_poor,
        "0.2 - 1 km",
        "0.12 - 0.6 mi",
        "200 - 1000 m"
    ),
    VisibilityScaleRange(
        R.string.text_dense_fog,
        R.string.visibility_description_dense,
        "< 0.2 km",
        "< 0.12 mi",
        "< 200 m"
    )
)

@Composable
private fun getVisibilityScaleFor(unit: DistanceUnit): List<VisibilityScaleInfo> {
    return visibilityRanges.map { range ->
        VisibilityScaleInfo(
            headline = stringResource(range.headlineRes),
            scale = when (unit) {
                DistanceUnit.KM -> range.kmScale
                DistanceUnit.MI -> range.miScale
                DistanceUnit.M -> range.mScale
            },
            description = stringResource(range.descriptionRes)
        )
    }
}
