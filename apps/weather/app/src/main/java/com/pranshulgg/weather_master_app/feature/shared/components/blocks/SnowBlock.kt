package com.pranshulgg.weather_master_app.feature.shared.components.blocks

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.sp
import com.pranshulgg.weather_master_app.R
import com.pranshulgg.weather_master_app.core.model.domain.weather.WeatherUnits
import com.pranshulgg.weather_master_app.core.model.weather.toName
import com.pranshulgg.weather_master_app.core.ui.components.Gap
import com.pranshulgg.weather_master_app.core.ui.components.Symbol
import com.pranshulgg.weather_master_app.core.ui.theme.ShadowElevation
import com.pranshulgg.weather_master_app.core.utils.formatters.formatLocalizedNumber
import com.pranshulgg.weather_master_app.core.utils.locale.getCurrentAppLocale

@Composable
fun SnowBlock(
    snowForTheDay: Double,
    context: Context,
    units: WeatherUnits,
    onClickBlock: () -> Unit
) {


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
                    .padding(bottom = 16.dp)
            ) {

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MetroText(
                        formatLocalizedNumber(
                            getCurrentAppLocale(),
                            snowForTheDay,
                            1
                        ),
                        modifier = Modifier.alignByBaseline(),
                        textAlign = TextAlign.End,
                        color = MetroTheme.colors.primaryText,

                        style = MetroTextStyle.HubTitle
                    )
                    Gap(horizontal = 2.dp)
                    MetroText(
                        units.precipitationUnit.toName(context, true),
                        modifier = Modifier
                            .alignByBaseline()
                            .padding(end = 16.dp),
                        textAlign = TextAlign.End,
                        style = MetroTextStyle.SectionHeader,
                        color = MetroTheme.colors.secondaryText
                    )
                }

                MetroText(
                    stringResource(R.string.weather_total_snow_day),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp),
                    textAlign = TextAlign.End,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
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
            R.drawable.snowflake_24px,
            color = MetroTheme.colors.primaryText.copy(alpha = 0.9f),
        )
        MetroText(
            stringResource(R.string.weather_snow_block),
            style = MetroTextStyle.SectionHeader,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MetroTheme.colors.primaryText.copy(alpha = 0.9f)

        )
    }
}
