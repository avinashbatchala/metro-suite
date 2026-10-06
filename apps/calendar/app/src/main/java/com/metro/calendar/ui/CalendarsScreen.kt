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
import com.metro.calendar.data.subscription.SubscriptionUrl
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarTextButton
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

/** Subscription + device calendar management root. */
@Composable
fun CalendarsScreen(
    state: CalendarState,
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
                    contentDescription = stringResource(R.string.calendars),
                )
            }
            MetroPageHeader(title = stringResource(R.string.calendars))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item { SectionHeader(stringResource(R.string.calendars_section_device)) }
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
                items(state.writableCalendars, key = { "cal-${it.id}" }) { calendar ->
                    Column {
                        MetroListItem(
                            title = calendar.displayName,
                            leading = {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(
                                            runCatching {
                                                Color(android.graphics.Color.parseColor(calendar.colorHex))
                                            }.getOrDefault(MetroTheme.colors.accent),
                                        )
                                        .metroClickable {
                                            colorEditingId = if (colorEditingId == calendar.id) null else calendar.id
                                        },
                                )
                            },
                            trailing = {
                                MetroToggleSwitch(
                                    checked = calendar.isVisible,
                                    onCheckedChange = { visible ->
                                        state.setCalendarVisible(calendar.id, visible)
                                    },
                                    showStatus = false,
                                )
                            },
                        )
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
                                                state.setCalendarColor(calendar.id, hex)
                                                colorEditingId = null
                                            },
                                    )
                                }
                            }
                        }
                    }
                }

                item { SectionHeader(stringResource(R.string.calendars_section_subscriptions)) }
                if (state.subscriptions.isEmpty()) {
                    item {
                        MetroText(
                            text = stringResource(R.string.calendars_empty),
                            style = MetroTextStyle.Body,
                            color = MetroTheme.colors.secondaryText,
                            modifier = Modifier.padding(
                                start = MetroDimens.ScreenHorizontalMargin,
                                end = MetroDimens.ScreenHorizontalMargin,
                                top = 8.dp,
                            ),
                        )
                    }
                } else {
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

/** WP8.1 calendar accent palette for per-calendar colour. */
private val CalendarColorPresets = listOf(
    "#1BA1E2", "#E51400", "#339933", "#F09609", "#8CBF26", "#A05000",
    "#E671B8", "#A200FF", "#0050EF", "#76608A", "#647687", "#60A917",
)

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
