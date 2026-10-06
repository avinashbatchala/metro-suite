package com.metro.widgets.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.system.MetroWorldClockCatalog
import com.metro.ui.MetroCheckBox
import com.metro.ui.MetroCircleIconButton
import com.metro.ui.MetroListItem
import com.metro.ui.MetroPageHeader
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroTextBox
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroNavBarPadding
import com.metro.widgets.R

/**
 * World Clock widget configuration: pick 1–3 cities. Offline, searchable, Metro checkmarks.
 * Selection persists in Widgets app-private state; launcher renders the tile locally.
 */
@Composable
fun WorldClockConfigScreen(
    state: WidgetsState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    val results = remember(query) { MetroWorldClockCatalog.search(query) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .metroNavBarPadding()
            .background(Color.Black),
    ) {
        Row(
            modifier = Modifier.padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MetroCircleIconButton(
                type = MetroSystemIconType.Back,
                onClick = onBack,
                contentDescription = stringResource(R.string.widget_world_clock_back),
            )
        }
        MetroPageHeader(title = stringResource(R.string.widget_world_clock_title))
        MetroText(
            text = stringResource(R.string.widget_world_clock_hint),
            style = MetroTextStyle.ListItemSubtitle,
            color = MetroTheme.colors.secondaryText,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
        MetroTextBox(
            value = query,
            onValueChange = { query = it },
            placeholder = stringResource(R.string.widget_world_clock_search),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(results, key = { it.id }) { city ->
                val selected = state.isWorldClockCitySelected(city.id)
                val enabled = selected || state.canAddMoreWorldClockCities()
                MetroListItem(
                    title = city.name,
                    subtitle = city.region,
                    enabled = enabled,
                    trailing = {
                        MetroCheckBox(
                            checked = selected,
                            enabled = enabled,
                            onCheckedChange = { state.toggleWorldClockCity(city.id) },
                        )
                    },
                    onClick = { if (enabled) state.toggleWorldClockCity(city.id) },
                )
            }
        }
    }
}
