package com.pranshulgg.weather_master_app.feature.main.ui.layouts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroDimens
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

/**
 * Active-location overline shown above the Bing Weather panorama headings.
 *
 * Uppercase, flush-left at the 12dp screen margin, and one step stronger than the shared
 * secondary labels so the place reads as page chrome rather than a caption.
 */
@Composable
fun WeatherPanoramaHeader(
    locationName: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        MetroText(
            text = locationName.uppercase(),
            style = MetroTextStyle.ListItemSubtitle,
            color = MetroTheme.colors.primaryText,
            maxLines = 1,
            modifier = Modifier.padding(
                start = MetroDimens.ScreenHorizontalMargin,
                top = 8.dp,
                bottom = 2.dp,
            ),
        )
    }
}
