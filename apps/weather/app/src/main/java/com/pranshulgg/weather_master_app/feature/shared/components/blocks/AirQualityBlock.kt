package com.pranshulgg.weather_master_app.feature.shared.components.blocks

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pranshulgg.weather_master_app.R
import com.pranshulgg.weather_master_app.core.model.domain.airquality.AirQuality
import com.pranshulgg.weather_master_app.core.model.weather.airquality.toName
import com.pranshulgg.weather_master_app.core.ui.components.Gap
import com.pranshulgg.weather_master_app.core.ui.components.Symbol
import com.pranshulgg.weather_master_app.core.ui.theme.ShadowElevation
import com.pranshulgg.weather_master_app.core.utils.weather.airquality.AirQualityColors

@Composable
fun AirQualityBlock(airQuality: AirQuality?, context: Context, onClickBlock: () -> Unit) {


    val aqi = airQuality!!.getAqi()
    val level = airQuality.getAqiLevel(aqi)
    val aqiBarProgress = airQuality.getAqiBarValue(aqi)

    Surface(
        color = MetroTheme.colors.secondarySurface,
        onClick = onClickBlock
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .aspectRatio(1f)
        ) {
            Column(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                MetroText(
                    aqi.toString(),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                    color = MetroTheme.colors.primaryText,
                    style = MetroTextStyle.HubTitle
                )
                LinearProgressIndicator(
                    progress = { aqiBarProgress },
                    color = AirQualityColors.getColors(level),
                    trackColor = MetroTheme.colors.secondarySurface,
                    modifier = Modifier.height(8.dp)

                )
                Gap(3.dp)
                MetroText(
                    level.toName(context),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                    color = MetroTheme.colors.secondaryText,
                    style = MetroTextStyle.Body
                )
            }

            Box(Modifier.align(Alignment.TopStart)) {
                Header()
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
        modifier = Modifier.padding(top = 16.dp, start = 12.dp, end = 12.dp)
    ) {
        Symbol(
            R.drawable.airwave_24px,
            color = MetroTheme.colors.primaryText.copy(alpha = 0.9f),
        )
        MetroText(
            stringResource(R.string.weather_air_quality),
            style = MetroTextStyle.SectionHeader,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MetroTheme.colors.primaryText.copy(alpha = 0.9f)

        )
    }
}
