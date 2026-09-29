package com.metro.statusbar.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import com.metro.statusbar.TrayIconFlags
import com.metro.statusbar.TrayLayout
import com.metro.statusbar.TrayLayoutSlot

/**
 * Icons-tab preview — same [TrayPreview] / [StatusTray] renderer as customise and configure.
 */
@Composable
fun TrayIconsPreview(
    flags: TrayIconFlags,
    modifier: Modifier = Modifier,
    clockText: String = remember {
        LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm"))
    },
    layout: List<TrayLayoutSlot> = TrayLayout.DEFAULT,
) {
    TrayPreview(
        flags = flags,
        clockText = clockText,
        layout = layout,
        modifier = modifier,
    )
}
