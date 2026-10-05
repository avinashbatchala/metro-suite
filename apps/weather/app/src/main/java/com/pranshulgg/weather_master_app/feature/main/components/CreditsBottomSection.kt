package com.pranshulgg.weather_master_app.feature.main.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.pranshulgg.weather_master_app.core.model.domain.weather.Weather
import com.pranshulgg.weather_master_app.core.ui.components.Gap
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.ui.res.stringResource
import com.pranshulgg.weather_master_app.R
import com.pranshulgg.weather_master_app.core.model.sources.Source

@Composable
fun CreditsBottomSection(weather: Weather?, onClick: () -> Unit) {

    val source = weather?.location?.source

    val sourceString = source?.displayName
        ?: Source.OPEN_METEO.displayName

    val countryString = buildString {
        if (source?.countryNameRes != null) {
            append(" (${stringResource(weather.location.source.countryNameRes)})")
        }
    }

    MetroText(
        text = stringResource(R.string.source_credits_label, "$sourceString$countryString"),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick
            ),
        style = MetroTextStyle.Body,
        color = MetroTheme.colors.accent,
        textAlign = TextAlign.Center,
    )

    Gap(WindowInsets.systemBars.asPaddingValues().calculateBottomPadding())
}
