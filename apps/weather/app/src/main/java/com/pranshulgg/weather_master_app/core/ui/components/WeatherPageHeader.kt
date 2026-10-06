package com.pranshulgg.weather_master_app.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroPageHeader

/**
 * House subpage chrome: the small `WEATHER` app-title overline plus the large lowercase page
 * title. The bottom [com.metro.ui.MetroAppBar] is owned by the shell, so subpages render this
 * header directly instead of a top app bar.
 */
@Composable
fun WeatherPageHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        MetroAppTitle(title = "WEATHER")
        MetroPageHeader(title = title.lowercase())
    }
}
