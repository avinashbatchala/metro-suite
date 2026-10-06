package com.metro.clock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.clock.R
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroDimens
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroTextBox
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroNavBarPadding

/** New-timer page: duration wheels + name; Start is an ApplicationBar command. */
@Composable
fun TimerEditScreen(
    state: ClockState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var hours by remember { mutableIntStateOf(0) }
    var minutes by remember { mutableIntStateOf(5) }
    var seconds by remember { mutableIntStateOf(0) }
    var label by remember { mutableStateOf("") }
    val durationMillis = (hours * 3600L + minutes * 60L + seconds) * 1000L

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
            MetroText(
                text = stringResource(R.string.timer).uppercase(),
                style = MetroTextStyle.AppTitle,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.padding(start = MetroDimens.ScreenHorizontalMargin, top = 8.dp),
            )
            MetroText(
                text = stringResource(R.string.new_timer),
                style = MetroTextStyle.PageTitle,
                modifier = Modifier.padding(start = MetroDimens.ScreenHorizontalMargin, bottom = 8.dp),
            )
            MetroDurationPicker(
                hours = hours,
                minutes = minutes,
                seconds = seconds,
                onHours = { hours = it },
                onMinutes = { minutes = it },
                onSeconds = { seconds = it },
                modifier = Modifier.padding(vertical = 8.dp),
            )
            MetroTextBox(
                value = label,
                onValueChange = { label = it },
                placeholder = stringResource(R.string.timer_default_label),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MetroDimens.ScreenHorizontalMargin, vertical = 8.dp),
            )
            if (durationMillis <= 0L) {
                MetroText(
                    text = stringResource(R.string.timer_error_zero),
                    style = MetroTextStyle.ListItemSubtitle,
                    color = MetroTheme.colors.accent,
                    modifier = Modifier.padding(horizontal = MetroDimens.ScreenHorizontalMargin),
                )
            }
            Spacer(modifier = Modifier.height(96.dp))
        }

        MetroAppBar(
            icons = listOf(
                MetroAppBarIcon(
                    type = MetroSystemIconType.Play,
                    label = stringResource(R.string.timer_start),
                    enabled = durationMillis > 0L,
                    onClick = { state.createTimer(label, durationMillis) },
                ),
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
