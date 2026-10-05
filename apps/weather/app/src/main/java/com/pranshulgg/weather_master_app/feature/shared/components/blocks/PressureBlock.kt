package com.pranshulgg.weather_master_app.feature.shared.components.blocks

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pranshulgg.weather_master_app.R
import com.pranshulgg.weather_master_app.core.model.domain.weather.WeatherUnits
import com.pranshulgg.weather_master_app.core.model.domain.weather.Weather
import com.pranshulgg.weather_master_app.core.model.weather.PressureUnit
import com.pranshulgg.weather_master_app.core.model.weather.toName
import com.pranshulgg.weather_master_app.core.ui.components.Gap
import com.pranshulgg.weather_master_app.core.ui.components.Symbol
import com.pranshulgg.weather_master_app.core.ui.theme.ShadowElevation
import com.pranshulgg.weather_master_app.core.ui.theme.ShapeRadius
import com.pranshulgg.weather_master_app.core.utils.formatters.formatLocalizedNumber
import com.pranshulgg.weather_master_app.core.utils.locale.getCurrentAppLocale
import kotlin.collections.average
import kotlin.math.roundToInt

@Composable
fun PressureBlock(
    weather: Weather,
    units: WeatherUnits,
    context: Context,
    isDaily: Boolean,
    dailyIndex: Int,
    onClickBlock: () -> Unit
) {
    val pressure =
        if (isDaily) weather.daily[dailyIndex].pressureMsl else weather.current.pressureMsl

    val pressureConverted = PressureUnit.HPA.convert(pressure!!, units.pressureUnit)
    val pressureHpa = weather.current.pressureMsl!!.roundToInt()

    val progressDrawable = when {
        pressureHpa < 980 -> R.drawable.pressure_progress_low
        pressureHpa in 980..1005 -> R.drawable.pressure_progress_medium
        pressureHpa in 1005..1020 -> R.drawable.pressure_progress_low_medium
        pressureHpa in 1020..1035 -> R.drawable.pressure_progress_high
        else -> R.drawable.pressure_progress_very_high
    }


    Surface(
        color = MetroTheme.colors.secondarySurface,
        onClick = onClickBlock
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .aspectRatio(1f)
        ) {
            Image(
                painter = painterResource(id = R.drawable.pressure_progress_container),
                contentDescription = "",
                modifier = Modifier.matchParentSize(),
                colorFilter = ColorFilter.tint(MetroTheme.colors.secondarySurface)
            )
            Image(
                painter = painterResource(id = progressDrawable),
                contentDescription = "",
                modifier = Modifier.matchParentSize(),
                colorFilter = ColorFilter.tint(MetroTheme.colors.accent)
            )
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Gap(28.dp)
                Header()

                MetroText(
                    formatLocalizedNumber(
                        locale = getCurrentAppLocale(),
                        number = pressureConverted!!,
                        decimalPlaces = 1
                    ),
                    style = MetroTextStyle.HubTitle,
                    color = MetroTheme.colors.primaryText,
                )


                MetroText(
                    units.pressureUnit.toName(true, context),
                    style = MetroTextStyle.Body,
                    color = MetroTheme.colors.secondaryText
                )
                Gap(28.dp)
            }
        }
    }
}


@Composable
private fun Header() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(
            5.dp,
            alignment = Alignment.CenterHorizontally
        ),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp)

    ) {
        Symbol(
            R.drawable.compress_24px,
            color = MetroTheme.colors.primaryText.copy(alpha = 0.9f),
        )
        MetroText(
            stringResource(R.string.weather_pressure),
            style = MetroTextStyle.SectionHeader,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MetroTheme.colors.primaryText.copy(alpha = 0.9f)

        )
    }
}
