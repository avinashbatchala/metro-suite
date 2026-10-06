package com.metro.dialer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.dialer.R
import com.metro.dialer.data.DialerCallLogic
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroNavBarPadding

@Composable
fun SaveContactScreen(
    number: String,
    onNewContact: (String) -> Unit,
    onAddToExisting: (String) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val display = DialerCallLogic.formatDisplayNumber(number)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .metroNavBarPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 72.dp),
        ) {
            MetroAppTitle(title = stringResource(R.string.save))
            Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                MetroText(
                    text = stringResource(R.string.save_number_title),
                    style = MetroTextStyle.ListItemSubtitle,
                    color = MetroTheme.colors.secondaryText,
                    modifier = Modifier.padding(top = 8.dp),
                )
                MetroText(
                    text = display,
                    style = MetroTextStyle.ListItemTitle,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Spacer(modifier = Modifier.height(24.dp))
                MetroBorderButton(
                    text = stringResource(R.string.new_contact),
                    onClick = { onNewContact(number) },
                )
                Spacer(modifier = Modifier.height(12.dp))
                MetroBorderButton(
                    text = stringResource(R.string.add_to_existing),
                    onClick = { onAddToExisting(number) },
                )
            }
        }

        MetroAppBar(
            icons = listOf(
                MetroAppBarIcon(
                    type = MetroSystemIconType.Close,
                    label = stringResource(R.string.cancel),
                    onClick = onBack,
                ),
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
