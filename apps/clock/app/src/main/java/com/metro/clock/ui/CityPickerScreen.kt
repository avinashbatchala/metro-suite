package com.metro.clock.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.clock.R
import com.metro.ui.MetroCheckBox
import com.metro.ui.MetroCircleIconButton
import com.metro.ui.MetroListItem
import com.metro.ui.MetroPageHeader
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroTextBox

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
        Row(
            modifier = Modifier.padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MetroCircleIconButton(
                type = MetroSystemIconType.Back,
                onClick = onBack,
                contentDescription = stringResource(R.string.back),
            )
        }
        MetroPageHeader(title = stringResource(R.string.city_picker_title))
        MetroTextBox(
            value = query,
            onValueChange = { query = it },
            placeholder = stringResource(R.string.city_search_placeholder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(results, key = { it.id }) { city ->
                val selected = state.isCitySelected(city.id)
                MetroListItem(
                    title = city.name,
                    subtitle = city.region,
                    trailing = {
                        MetroCheckBox(
                            checked = selected,
                            onCheckedChange = { checked ->
                                if (checked) state.addCity(city.id) else state.removeCity(city.id)
                            },
                        )
                    },
                    onClick = {
                        if (selected) state.removeCity(city.id) else state.addCity(city.id)
                    },
                )
            }
        }
    }
}
