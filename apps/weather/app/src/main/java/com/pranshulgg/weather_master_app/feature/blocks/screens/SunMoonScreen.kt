package com.pranshulgg.weather_master_app.feature.blocks.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.metro.ui.MetroColors
import com.metro.ui.MetroDimens
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.pranshulgg.weather_master_app.R
import com.pranshulgg.weather_master_app.core.prefs.LocalAppPrefs
import com.pranshulgg.weather_master_app.core.ui.components.Gap
import com.pranshulgg.weather_master_app.core.ui.components.LargeTopBarScaffold
import com.pranshulgg.weather_master_app.core.ui.components.NavigateUpBtn
import com.pranshulgg.weather_master_app.core.utils.formatters.to12HourTimeString
import com.pranshulgg.weather_master_app.core.utils.formatters.to24HourTimeString
import com.pranshulgg.weather_master_app.core.utils.formatters.toDateString
import com.pranshulgg.weather_master_app.feature.blocks.BlocksScreenViewModel
import com.pranshulgg.weather_master_app.feature.blocks.components.AboutCard
import com.pranshulgg.weather_master_app.feature.blocks.components.AboutCardText
import com.pranshulgg.weather_master_app.feature.shared.components.blocks.MoonBlock
import com.pranshulgg.weather_master_app.feature.shared.components.blocks.SunBlock
import java.util.concurrent.TimeUnit


@Composable
fun SunMoonScreen(navController: NavController, index: Int, locationId: String) {
    val viewModel: BlocksScreenViewModel = hiltViewModel()

    val weather = viewModel.weather.collectAsState().value.weather
    val daily = weather?.daily ?: return
    val prefs = LocalAppPrefs.current
    val is24hr = prefs.is24HrTimeFormat

    val date = toDateString(daily[index].time, weather.location.timezone)

    val duskFormatted = if (is24hr) to24HourTimeString(
        daily[index].dusk!!,
        weather.location.timezone
    ) else to12HourTimeString(
        daily[index].dusk!!,
        weather.location.timezone,
        "hh:mm a"
    )
    val dawnFormatted = if (is24hr) to24HourTimeString(
        daily[index].dawn!!,
        weather.location.timezone
    ) else to12HourTimeString(
        daily[index].dawn!!,
        weather.location.timezone,
        "hh:mm a"
    )


    val dayLength = daily[index].sunset!!.minus(daily[index].sunrise!!)
    val dayLengthHrs = TimeUnit.MILLISECONDS.toHours(dayLength)


    LargeTopBarScaffold(
        title = stringResource(R.string.weather_sun_moon),
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MetroDimens.ScreenHorizontalMargin)
                    .background(MetroTheme.colors.secondarySurface, RectangleShape)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextHeader(
                            header = stringResource(R.string.text_dawn),
                            text = dawnFormatted
                        )
                        TextHeader(
                            header = stringResource(R.string.text_dusk),
                            text = duskFormatted
                        )
                        TextHeader(
                            header = stringResource(R.string.text_day_length),
                            text = stringResource(R.string.day_length_hr, "$dayLengthHrs")
                        )
                    }
                    Box(Modifier.size(160.dp)) {
                        SunBlock(weather, index, prefs) {}
                    }
                }
            }
            Gap(14.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MetroDimens.ScreenHorizontalMargin)
                    .background(MetroTheme.colors.secondarySurface, RectangleShape)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        TextHeader(
                            header = stringResource(R.string.moon_phase),
                            text = stringResource(daily[index].moonPhase.displayName)
                        )
                        Gap(4.dp)
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(MetroTheme.colors.accent, RectangleShape)
                        ) {
                            Image(
                                painter = painterResource(daily[index].moonPhase.icon),
                                contentDescription = "",
                                colorFilter = ColorFilter.tint(
                                    MetroColors.tileContentColor(MetroTheme.colors.accent)
                                )
                            )
                        }
                    }
                    Box(Modifier.size(160.dp)) {
                        MoonBlock(weather, index, prefs) {}
                    }
                }
            }

            Gap(14.dp)
            AboutCard {
                AboutCardText(stringResource(R.string.weather_about_sun_moon_rise_set))
                AboutCardText(stringResource(R.string.weather_about_dawn_dusk))
            }

            Gap(WindowInsets.systemBars.asPaddingValues().calculateBottomPadding() + 30.dp)
        }
    }
}

@Composable
private fun TextHeader(header: String, text: String) {
    Column() {
        MetroText(
            text = header,
            style = MetroTextStyle.SectionHeader,
            color = MetroTheme.colors.accent
        )
        MetroText(
            text = text,
            style = MetroTextStyle.Body,
            color = MetroTheme.colors.primaryText
        )
    }
}
