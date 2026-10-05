package com.metro.calendar.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.calendar.R
import com.metro.calendar.data.CalendarEvent
import com.metro.calendar.data.CalendarLogic
import com.metro.ui.MetroCircleIconButton
import com.metro.ui.MetroDimens
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

/**
 * Read-only event detail overlay (Blueprint §Event detail). Title, date/time, location and
 * calendar name, with a back affordance. Create/edit stays out of scope in v1.
 */
@Composable
fun EventDetailScreen(
    event: CalendarEvent,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val startDay = CalendarLogic.epochDayFromMillis(event.startMillis)
    val dateLabel = CalendarLogic.dateHeaderLabel(startDay)
    val timeLabel = CalendarLogic.tileTimeRange(event)
    val accent = runCatching { Color(android.graphics.Color.parseColor(event.calendarColorHex)) }
        .getOrDefault(MetroTheme.colors.accent)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = MetroDimens.ScreenHorizontalMargin - 8.dp,
                    end = MetroDimens.ScreenHorizontalMargin,
                    top = 8.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MetroCircleIconButton(
                type = MetroSystemIconType.Back,
                onClick = onBack,
                contentDescription = stringResource(R.string.event_detail_title),
            )
        }

        MetroText(
            text = stringResource(R.string.event_detail_title),
            style = MetroTextStyle.AppTitle,
            color = MetroTheme.colors.primaryText,
            modifier = Modifier.padding(
                start = MetroDimens.ScreenHorizontalMargin,
                top = 4.dp,
            ),
        )

        // Title (accented by the owning calendar).
        MetroText(
            text = event.title,
            style = MetroTextStyle.HubTitle,
            color = accent,
            modifier = Modifier.padding(
                start = MetroDimens.ScreenHorizontalMargin,
                end = MetroDimens.ScreenHorizontalMargin,
                top = 16.dp,
            ),
        )

        Spacer(modifier = Modifier.height(20.dp))
        DetailField(label = stringResource(R.string.event_when_label), value = dateLabel)
        DetailField(label = null, value = timeLabel)

        event.location?.takeIf { it.isNotBlank() }?.let { location ->
            Spacer(modifier = Modifier.height(16.dp))
            DetailField(label = stringResource(R.string.event_location_label), value = location)
        }

        event.calendarName?.takeIf { it.isNotBlank() }?.let { calendar ->
            Spacer(modifier = Modifier.height(16.dp))
            DetailField(label = stringResource(R.string.event_calendar_label), value = calendar)
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
private fun DetailField(label: String?, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = MetroDimens.ScreenHorizontalMargin,
                end = MetroDimens.ScreenHorizontalMargin,
                bottom = 4.dp,
            ),
    ) {
        if (label != null) {
            MetroText(
                text = label,
                style = MetroTextStyle.SectionHeader,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
        MetroText(
            text = value,
            style = MetroTextStyle.ListItemTitle,
            color = MetroTheme.colors.primaryText,
        )
    }
}
