package com.metro.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.calendar.R
import com.metro.calendar.data.subscription.SubscriptionUrl
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarTextButton
import com.metro.ui.MetroCircleIconButton
import com.metro.ui.MetroDimens
import com.metro.ui.MetroEmptyState
import com.metro.ui.MetroListItem
import com.metro.ui.MetroPageHeader
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.MetroToggleSwitch
import com.metro.ui.metroNavBarPadding

/** Subscription management root: device calendar status + subscribed calendars list. */
@Composable
fun CalendarsScreen(
    state: CalendarState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    @Suppress("UNUSED_VARIABLE")
    val generation = state.generation

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background)
            .statusBarsPadding()
            .metroNavBarPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                    contentDescription = stringResource(R.string.calendars),
                )
            }
            MetroPageHeader(title = stringResource(R.string.calendars))

            if (state.subscriptions.isEmpty()) {
                MetroEmptyState(
                    message = stringResource(R.string.calendars_empty),
                    modifier = Modifier.padding(top = 24.dp),
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        SectionHeader(stringResource(R.string.calendars_section_device))
                    }
                    item {
                        MetroListItem(
                            title = stringResource(R.string.calendars_section_device),
                            subtitle = if (state.hasCalendarPermission) {
                                stringResource(R.string.device_calendar_connected)
                            } else {
                                stringResource(R.string.device_calendar_not_connected)
                            },
                        )
                    }
                    item {
                        SectionHeader(stringResource(R.string.calendars_section_subscriptions))
                    }
                    items(state.subscriptions, key = { it.id }) { subscription ->
                        MetroListItem(
                            title = subscription.name,
                            subtitle = subscriptionSubtitle(subscription),
                            leading = {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(SubscriptionColors.colorFor(subscription.colorHex)),
                                )
                            },
                            trailing = {
                                MetroToggleSwitch(
                                    checked = subscription.enabled,
                                    onCheckedChange = { enabled ->
                                        state.setSubscriptionEnabled(subscription.id, enabled)
                                    },
                                    showStatus = false,
                                )
                            },
                            onClick = { state.openSubscriptionDetail(subscription.id) },
                        )
                    }
                }
            }
        }

        MetroAppBar(
            textButtons = listOf(
                MetroAppBarTextButton(
                    text = stringResource(R.string.add_calendar),
                    onClick = state::openAddSubscription,
                ),
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    MetroText(
        text = text,
        style = MetroTextStyle.SectionHeader,
        color = MetroTheme.colors.secondaryText,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = MetroDimens.ScreenHorizontalMargin,
                end = MetroDimens.ScreenHorizontalMargin,
                top = 20.dp,
                bottom = 4.dp,
            ),
    )
}

private fun subscriptionSubtitle(
    subscription: com.metro.calendar.data.subscription.CalendarSubscription,
): String {
    val masked = SubscriptionUrl.mask(subscription.url)
    val error = subscription.lastError
    return if (error != null) "$masked — $error" else masked
}
