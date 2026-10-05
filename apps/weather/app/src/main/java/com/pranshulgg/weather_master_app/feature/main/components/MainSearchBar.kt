package com.pranshulgg.weather_master_app.feature.main.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.metro.ui.MetroDimens
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.pranshulgg.weather_master_app.core.model.domain.location.Location
import com.pranshulgg.weather_master_app.core.utils.weather.location.getFullLocationName

/**
 * Thin location-name wrapper. Search / settings / refresh live in the bottom
 * [com.metro.ui.MetroAppBar]; this composable no longer paints a Material search pill.
 */
@Composable
fun MainSearchBar(
    isFroggyLayout: Boolean = false,
    paddingValues: PaddingValues,
    navController: NavController,
    activeLocation: Location?,
    onEditLocation: () -> Unit,
    layoutDirection: LayoutDirection,
    onRefresh: () -> Unit = {}
) {
    val startPadding = paddingValues.calculateStartPadding(layoutDirection)
    val endPadding = paddingValues.calculateEndPadding(layoutDirection)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = paddingValues.calculateTopPadding() + 4.dp,
                start = startPadding + MetroDimens.ScreenHorizontalMargin,
                end = endPadding + MetroDimens.ScreenHorizontalMargin
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MetroText(
            text = getFullLocationName(activeLocation),
            style = MetroTextStyle.ListItemTitle,
            color = MetroTheme.colors.primaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
