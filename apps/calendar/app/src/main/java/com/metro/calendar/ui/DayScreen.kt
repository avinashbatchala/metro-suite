package com.metro.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metro.calendar.R
import com.metro.calendar.data.CalendarEvent
import com.metro.calendar.data.CalendarLogic
import com.metro.calendar.data.CalendarWeather
import com.metro.calendar.data.TimelineEvent
import com.metro.ui.MetroCircleIconButton
import com.metro.ui.MetroDimens
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroTextBox
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroClickable
import java.time.LocalTime

private val HourHeight = 60.dp
private val TimeGutter = 64.dp

/**
 * WP8.1 day view: a real time grid. Each appointment renders **once** at its absolute position
 * and true duration, with side-by-side lanes for overlaps. Empty hours accept a Quick Event.
 */
@Composable
fun DayScreen(
    allDayEvents: List<CalendarEvent>,
    timeline: List<TimelineEvent>,
    weather: CalendarWeather?,
    loadFailed: Boolean,
    use24Hour: Boolean,
    scrollToNow: Boolean,
    scrollRequestId: Int,
    onEventClick: (CalendarEvent) -> Unit,
    onEventLongClick: (CalendarEvent) -> Unit,
    onQuickEvent: (startMinute: Int, title: String) -> Boolean,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    var quickMinute by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(scrollToNow, scrollRequestId) {
        if (scrollToNow) {
            val now = LocalTime.now()
            val offset = ((now.hour * 60 + now.minute) / 60f * HourHeight.value).toInt() - 180
            scrollState.animateScrollTo(offset.coerceAtLeast(0))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (weather != null) {
            Row(
                modifier = Modifier.padding(
                    start = MetroDimens.ScreenHorizontalMargin,
                    top = 2.dp,
                    bottom = 4.dp,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MetroText(
                    text = weather.temperature,
                    style = MetroTextStyle.ListItemTitle,
                    color = MetroTheme.colors.primaryText,
                )
                weather.condition?.let { condition ->
                    Spacer(Modifier.width(8.dp))
                    MetroText(
                        text = condition,
                        style = MetroTextStyle.ListItemSubtitle,
                        color = MetroTheme.colors.secondaryText,
                    )
                }
            }
        }

        allDayEvents.forEach { event ->
            MetroText(
                text = event.title,
                style = MetroTextStyle.ListItemTitle,
                color = eventAccent(event),
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .metroClickable(
                        onClick = { onEventClick(event) },
                        onLongClick = { onEventLongClick(event) },
                    )
                    .padding(horizontal = MetroDimens.ScreenHorizontalMargin, vertical = 4.dp),
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val laneArea = maxWidth - TimeGutter
            Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(HourHeight * 24),
                ) {
                    // Hour lines + labels.
                    for (hour in 0..23) {
                        val lineColor = MetroTheme.colors.secondaryText.copy(alpha = 0.22f)
                        MetroText(
                            text = CalendarLogic.formatHourLabel(hour, use24Hour),
                            style = MetroTextStyle.ListItemSubtitle,
                            color = MetroTheme.colors.secondaryText,
                            modifier = Modifier
                                .offset(y = HourHeight * hour + 2.dp)
                                .width(TimeGutter)
                                .padding(start = 6.dp),
                        )
                        Box(
                            modifier = Modifier
                                .offset(y = HourHeight * hour)
                                .padding(start = TimeGutter)
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(lineColor),
                        )
                        // Empty-hour Quick Event band (behind event blocks).
                        Box(
                            modifier = Modifier
                                .offset(y = HourHeight * hour)
                                .padding(start = TimeGutter)
                                .fillMaxWidth()
                                .height(HourHeight)
                                .metroClickable { quickMinute = hour * 60 },
                        )
                    }

                    // Appointments.
                    timeline.forEach { item ->
                        val top = HourHeight * (item.startMinute / 60f)
                        val height = HourHeight * ((item.endMinute - item.startMinute) / 60f)
                        val laneWidth = laneArea / item.laneCount
                        val accent = eventAccent(item.event)
                        Box(
                            modifier = Modifier
                                .offset(x = TimeGutter + laneWidth * item.lane, y = top)
                                .width(laneWidth)
                                .height(height)
                                .padding(end = 2.dp, bottom = 1.dp)
                                .background(accent.copy(alpha = 0.9f))
                                .metroClickable(
                                    onClick = { onEventClick(item.event) },
                                    onLongClick = { onEventLongClick(item.event) },
                                )
                                .padding(4.dp),
                        ) {
                            MetroText(
                                text = item.event.title,
                                style = MetroTextStyle.ListItemSubtitle,
                                color = Color.White,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    // Quick Event inline editor.
                    quickMinute?.let { minute ->
                        val top = HourHeight * (minute / 60f)
                        var text by remember(minute) { mutableStateOf("") }
                        Row(
                            modifier = Modifier
                                .offset(y = top)
                                .fillMaxWidth()
                                .padding(start = TimeGutter)
                                .background(MetroTheme.colors.background)
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            MetroTextBox(
                                value = text,
                                onValueChange = { text = it },
                                placeholder = stringResource(R.string.event_subject_label),
                                modifier = Modifier.weight(1f),
                            )
                            MetroCircleIconButton(
                                type = MetroSystemIconType.Check,
                                onClick = {
                                    if (text.isNotBlank() && onQuickEvent(minute, text.trim())) {
                                        quickMinute = null
                                    }
                                },
                                contentDescription = stringResource(R.string.save),
                            )
                        }
                    }

                    // Now marker.
                    if (scrollToNow) {
                        val now = LocalTime.now()
                        val y = HourHeight * ((now.hour * 60 + now.minute) / 60f)
                        Box(
                            modifier = Modifier
                                .offset(y = y)
                                .fillMaxWidth()
                                .padding(start = TimeGutter - 6.dp)
                                .height(2.dp)
                                .background(MetroTheme.colors.accent),
                        )
                    }
                }
            }
        }
    }
}
