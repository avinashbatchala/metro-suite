package com.pranshulgg.weather_master_app.feature.locations.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroDimens
import com.metro.ui.MetroListItem
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.pranshulgg.weather_master_app.R
import com.pranshulgg.weather_master_app.core.ui.components.SettingsTileIcon

@Composable
fun LocationScreenSheetContent(
    locationName: String,
    onDelete: () -> Unit,
    onSetAsDefault: () -> Unit,
    onEdit: () -> Unit,
    onPinToStart: () -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        MetroText(
            text = locationName,
            style = MetroTextStyle.SectionHeader,
            color = MetroTheme.colors.accent,
            modifier = Modifier.padding(
                start = MetroDimens.ScreenHorizontalMargin,
                top = 12.dp,
                bottom = 4.dp,
            )
        )
        MetroListItem(
            title = stringResource(R.string.location_edit),
            leading = { SettingsTileIcon(R.drawable.edit_24px) },
            onClick = onEdit
        )
        MetroListItem(
            title = stringResource(R.string.action_delete),
            leading = { SettingsTileIcon(R.drawable.delete_24px) },
            onClick = onDelete
        )
        MetroListItem(
            title = stringResource(R.string.action_set_default),
            leading = { SettingsTileIcon(R.drawable.home_pin_24px) },
            onClick = onSetAsDefault
        )
        MetroListItem(
            title = "pin to start",
            leading = { SettingsTileIcon(R.drawable.home_pin_24px) },
            onClick = onPinToStart
        )
    }
}
