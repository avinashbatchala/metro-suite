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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pranshulgg.weather_master_app.R
import com.pranshulgg.weather_master_app.core.model.domain.weather.WeatherUnits
import com.pranshulgg.weather_master_app.core.model.domain.weather.Weather
import com.pranshulgg.weather_master_app.core.model.weather.WindSpeedUnit
import com.pranshulgg.weather_master_app.core.model.weather.toName
import com.pranshulgg.weather_master_app.core.model.weather.wind.WindDirection
import com.pranshulgg.weather_master_app.core.ui.components.Gap
import com.pranshulgg.weather_master_app.core.ui.components.Symbol
import com.pranshulgg.weather_master_app.core.ui.theme.ShadowElevation
import com.pranshulgg.weather_master_app.core.ui.theme.ShapeRadius
import kotlin.math.roundToInt

@Composable
fun WindBlock(
    weather: Weather,
    context: Context,
    isDaily: Boolean,
    dailyIndex: Int,
    units: WeatherUnits,
    onClickBlock: () -> Unit
) {

    val windDirection = if (isDaily) {
        weather.daily[dailyIndex].windDirection
    } else {
        weather.current.windDirection
    }

    val windSpeed = if (isDaily) weather.daily[dailyIndex].windSpeed else weather.current.windSpeed

    val windSpeedFormatted = WindSpeedUnit.KPH.convert(windSpeed, units.windUnit)?.roundToInt()

    Surface(
        color = MetroTheme.colors.secondarySurface,
        onClick = onClickBlock
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .aspectRatio(1f)
        ) {
            if (windDirection != null) {
                Image(
                    painter = painterResource(id = R.drawable.weather_wind_arrow_dominant),
                    contentDescription = "",
                    modifier = Modifier
                        .matchParentSize()
                        .rotate(WindDirection.toDegrees(windDirection)?.toFloat() ?: 0f),
                    colorFilter = ColorFilter.tint(MetroTheme.colors.accent)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Gap(28.dp)
                Header()

                Row(
                ) {
                    MetroText(
                        windSpeedFormatted.toString(),
                        style = MetroTextStyle.HubTitle,
                        modifier = Modifier.alignByBaseline(),
                        color = MetroTheme.colors.primaryText,
                    )
                    Gap(horizontal = 2.dp)
                    MetroText(
                        units.windUnit.toName(context, true),
                        modifier = Modifier.alignByBaseline(),
                        style = MetroTextStyle.SectionHeader,
                        color = MetroTheme.colors.primaryText
                    )
                }


                if (windDirection != null) {
                    MetroText(
                        stringResource(R.string.weather_wind_direction_from, "$windDirection"),
                        style = MetroTextStyle.Body,
                    )
                }
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
            R.drawable.air_24px,
            color = MetroTheme.colors.primaryText.copy(alpha = 0.9f),
        )
        MetroText(
            stringResource(R.string.weather_wind),
            style = MetroTextStyle.SectionHeader,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MetroTheme.colors.primaryText.copy(alpha = 0.9f)

        )
    }
}
