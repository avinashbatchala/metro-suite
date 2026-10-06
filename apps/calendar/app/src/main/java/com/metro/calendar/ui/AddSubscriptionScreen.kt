package com.metro.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.calendar.R
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroCircleIconButton
import com.metro.ui.MetroDimens
import com.metro.ui.MetroListPicker
import com.metro.ui.MetroPageHeader
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTextBox
import com.metro.ui.MetroTheme
import com.metro.ui.metroNavBarPadding

/** Add-a-calendar-URL form. Validates that the URL is HTTPS (or webcal, normalized) before saving. */
@Composable
fun AddSubscriptionScreen(
    state: CalendarState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var colorIndex by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    val invalidUrl = stringResource(R.string.subscription_invalid_url)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background)
            .statusBarsPadding()
            .metroNavBarPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier.padding(
                    start = MetroDimens.ScreenHorizontalMargin - 8.dp,
                    top = 8.dp,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MetroCircleIconButton(
                    type = MetroSystemIconType.Back,
                    onClick = onBack,
                    contentDescription = stringResource(R.string.add_calendar),
                )
            }
            MetroPageHeader(title = stringResource(R.string.add_calendar))

            MetroTextBox(
                value = name,
                onValueChange = { name = it },
                placeholder = stringResource(R.string.subscription_name_placeholder),
                modifier = Modifier.padding(
                    horizontal = MetroDimens.ScreenHorizontalMargin,
                    vertical = 8.dp,
                ),
            )
            MetroTextBox(
                value = url,
                onValueChange = {
                    url = it
                    error = null
                },
                placeholder = stringResource(R.string.subscription_url_placeholder),
                modifier = Modifier.padding(
                    horizontal = MetroDimens.ScreenHorizontalMargin,
                    vertical = 8.dp,
                ),
            )
            MetroListPicker(
                options = SubscriptionColors.labels,
                selectedOptionIndex = colorIndex,
                onSelectOption = { colorIndex = it },
                label = stringResource(R.string.subscription_color_label),
                modifier = Modifier.padding(
                    horizontal = MetroDimens.ScreenHorizontalMargin,
                    vertical = 8.dp,
                ),
            )

            error?.let { message ->
                MetroText(
                    text = message,
                    style = MetroTextStyle.Body,
                    color = MetroTheme.colors.accent,
                    modifier = Modifier.padding(
                        horizontal = MetroDimens.ScreenHorizontalMargin,
                        vertical = 4.dp,
                    ),
                )
            }

            MetroBorderButton(
                text = stringResource(R.string.subscription_add),
                onClick = {
                    val ok = state.addSubscription(
                        name = name,
                        url = url,
                        colorHex = SubscriptionColors.palette[colorIndex].second,
                    )
                    if (ok) onBack() else error = invalidUrl
                },
                modifier = Modifier.padding(
                    horizontal = MetroDimens.ScreenHorizontalMargin,
                    vertical = 12.dp,
                ),
            )
        }
    }
}
