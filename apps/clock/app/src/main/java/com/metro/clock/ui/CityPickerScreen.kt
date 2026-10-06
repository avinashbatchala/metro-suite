package com.metro.clock.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.clock.R
import com.metro.ui.MetroDimens
import com.metro.ui.MetroListItem
import com.metro.ui.MetroPageHeader
import com.metro.ui.MetroTextBox

/**
 * Clock-specific add-city flow: one invocation adds one chosen city and returns. (The Widgets
 * multi-select configuration is a separate surface.)
 */
@Composable
fun CityPickerScreen(
    state: ClockState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    @Suppress("UNUSED_VARIABLE")
    val generation = state.generation
    var query by remember { mutableStateOf("") }
    val results = remember(query, generation) { state.searchCities(query) }

    Column(modifier = modifier.fillMaxSize()) {
        MetroPageHeader(title = stringResource(R.string.city_picker_title))
        MetroTextBox(
            value = query,
            onValueChange = { query = it },
            placeholder = stringResource(R.string.city_search_placeholder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MetroDimens.ScreenHorizontalMargin, vertical = 8.dp),
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(results, key = { it.id }) { city ->
                MetroListItem(
                    title = city.name,
                    subtitle = city.region,
                    enabled = !state.isCitySelected(city.id),
                    onClick = {
                        if (!state.isCitySelected(city.id)) {
                            state.addCity(city.id)
                            onBack()
                        }
                    },
                )
            }
        }
    }
}
