package com.metro.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.calendar.R
import com.metro.ui.MetroCheckBox
import com.metro.ui.MetroCircleIconButton
import com.metro.ui.MetroDimens
import com.metro.ui.MetroListItem
import com.metro.ui.MetroPageHeader
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.MetroToggleSwitch
import com.metro.ui.metroClickable
import com.metro.ui.metroNavBarPadding

private val CalendarColorPresets = listOf(
    "#1BA1E2", "#E51400", "#339933", "#F09609", "#8CBF26", "#A05000",
    "#E671B8", "#A200FF", "#0050EF", "#76608A", "#647687", "#60A917",
)

/** WP8.1 Calendar Settings: device calendars (show/hide + colour) and subscribed calendars. */
@Composable
fun SettingsScreen(
    state: CalendarState,
    onRequestRead: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    @Suppress("UNUSED_VARIABLE")
    val generation = state.generation
    var colorEditingId by remember { mutableStateOf<Long?>(null) }

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
                    contentDescription = stringResource(R.string.settings),
                )
            }
            MetroPageHeader(title = stringResource(R.string.settings))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item { SectionHeader(stringResource(R.string.settings_device)) }

                if (!state.hasReadPermission) {
                    item {
                        MetroListItem(
                            title = stringResource(R.string.allow_device_calendar),
                            onClick = onRequestRead,
                        )
                    }
                }
                items(state.readableCalendars, key = { "cal-${it.id}" }) { calendar ->
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MetroCheckBox(
                                checked = state.isCalendarVisible(calendar),
                                onCheckedChange = { state.setCalendarVisible(calendar, it) },
                                modifier = Modifier.padding(start = MetroDimens.ScreenHorizontalMargin),
                            )
                            MetroListItem(
                                title = calendar.displayName,
                                subtitle = calendar.accountName,
                                leading = {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(
                                                runCatching {
                                                    Color(
                                                        android.graphics.Color.parseColor(
                                                            state.calendarColorOverride(calendar) ?: calendar.colorHex,
                                                        ),
                                                    )
                                                }.getOrDefault(MetroTheme.colors.accent),
                                            )
                                            .metroClickable {
                                                colorEditingId = if (colorEditingId == calendar.id) null else calendar.id
                                            },
                                    )
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (colorEditingId == calendar.id) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = MetroDimens.ScreenHorizontalMargin,
                                        end = MetroDimens.ScreenHorizontalMargin,
                                        bottom = 8.dp,
                                    ),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                CalendarColorPresets.forEach { hex ->
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Color(android.graphics.Color.parseColor(hex)))
                                            .metroClickable {
                                                state.setCalendarColor(calendar, hex)
                                                colorEditingId = null
                                            },
                                    )
                                }
                            }
                        }
                    }
                }

                item { SectionHeader(stringResource(R.string.settings_subscribed)) }
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
                                onCheckedChange = { state.setSubscriptionVisible(subscription, it) },
                                showStatus = false,
                            )
                        },
                        onClick = { state.openSubscriptionDetail(subscription.id) },
                    )
                }
                item {
                    MetroListItem(
                        title = stringResource(R.string.subscribe_to_calendar),
                        onClick = state::openAddSubscription,
                    )
                }
                if (state.hasSubscriptionSource) {
                    item {
                        MetroListItem(
                            title = stringResource(R.string.sync_all_subscriptions),
                            onClick = state::syncAllSubscriptions,
                        )
                    }
                }
            }
        }
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
    val error = subscription.lastError
    return if (error != null) {
        // Concise status only; technical details live on the subscription detail page.
        "couldn't update"
    } else {
        com.metro.calendar.data.subscription.SubscriptionUrl.mask(subscription.url)
    }
}
