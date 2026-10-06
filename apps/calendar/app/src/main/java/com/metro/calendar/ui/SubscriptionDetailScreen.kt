package com.metro.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.calendar.R
import com.metro.calendar.data.subscription.SubscriptionUrl
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroCircleIconButton
import com.metro.ui.MetroDimens
import com.metro.ui.MetroMessageDialog
import com.metro.ui.MetroPageHeader
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.MetroToggleSwitch
import com.metro.ui.metroNavBarPadding
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Detail/actions for one subscription: enable, manual sync, and confirmed removal. */
@Composable
fun SubscriptionDetailScreen(
    state: CalendarState,
    subscriptionId: String,
    onBack: () -> Unit,
    onRemoved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    @Suppress("UNUSED_VARIABLE")
    val generation = state.generation
    val subscription = state.subscriptions.firstOrNull { it.id == subscriptionId }
    var confirmRemove by remember { mutableStateOf(false) }

    if (subscription == null) {
        LaunchedBack(onBack)
        return
    }

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
                    contentDescription = stringResource(R.string.calendars),
                )
            }
            MetroPageHeader(title = subscription.name)

            DetailRow(
                label = stringResource(R.string.subscription_url_label),
                value = SubscriptionUrl.mask(subscription.url),
            )
            DetailRow(
                label = stringResource(R.string.subscription_last_synced),
                value = formatTimestamp(subscription.lastSuccessMillis),
            )
            subscription.lastError?.let { error ->
                DetailRow(
                    label = stringResource(R.string.subscription_last_error),
                    value = error,
                )
            }

            MetroToggleSwitch(
                checked = subscription.enabled,
                onCheckedChange = { state.setSubscriptionEnabled(subscription.id, it) },
                label = stringResource(R.string.subscription_show_events),
                modifier = Modifier.padding(
                    horizontal = MetroDimens.ScreenHorizontalMargin,
                    vertical = 12.dp,
                ),
            )

            MetroBorderButton(
                text = stringResource(R.string.subscription_sync_now),
                onClick = { state.syncSubscription(subscription.id) },
                enabled = !state.syncing,
                modifier = Modifier.padding(
                    horizontal = MetroDimens.ScreenHorizontalMargin,
                    vertical = 4.dp,
                ),
            )
            Spacer(modifier = Modifier.height(8.dp))
            MetroBorderButton(
                text = stringResource(R.string.subscription_remove),
                onClick = { confirmRemove = true },
                modifier = Modifier.padding(
                    horizontal = MetroDimens.ScreenHorizontalMargin,
                    vertical = 4.dp,
                ),
            )
        }
    }

    if (confirmRemove) {
        MetroMessageDialog(
            title = stringResource(R.string.remove_calendar_title),
            body = stringResource(R.string.remove_calendar_body),
            confirmLabel = stringResource(R.string.remove),
            onConfirm = {
                confirmRemove = false
                state.removeSubscription(subscription.id)
                onRemoved()
            },
            dismissLabel = stringResource(R.string.cancel),
            onDismiss = { confirmRemove = false },
            onDismissRequest = { confirmRemove = false },
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = MetroDimens.ScreenHorizontalMargin,
                end = MetroDimens.ScreenHorizontalMargin,
                top = 12.dp,
            ),
    ) {
        MetroText(
            text = label,
            style = MetroTextStyle.SectionHeader,
            color = MetroTheme.colors.secondaryText,
        )
        MetroText(
            text = value,
            style = MetroTextStyle.ListItemTitle,
            color = MetroTheme.colors.primaryText,
        )
    }
}

@Composable
private fun LaunchedBack(onBack: () -> Unit) {
    androidx.compose.runtime.LaunchedEffect(Unit) { onBack() }
}

private fun formatTimestamp(millis: Long?): String {
    if (millis == null) return "—"
    val formatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")
    return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(formatter)
}
