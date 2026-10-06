package com.metro.dialer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.dialer.R
import com.metro.dialer.data.PhonePreferences
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroTextBox
import com.metro.ui.metroNavBarPadding

@Composable
fun EditRepliesScreen(
    preferences: PhonePreferences,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val initial = remember { preferences.textReplies().toMutableList() }
    var replies by remember {
        mutableStateOf(
            List(4) { index -> initial.getOrElse(index) { "" } },
        )
    }

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
                .padding(bottom = 72.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            MetroAppTitle(title = stringResource(R.string.edit_replies))
            Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                replies.forEachIndexed { index, value ->
                    MetroTextBox(
                        value = value,
                        onValueChange = { updated ->
                            replies = replies.toMutableList().also { it[index] = updated }
                        },
                        placeholder = stringResource(R.string.reply_hint, index + 1),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    )
                }
            }
        }

        MetroAppBar(
            icons = listOf(
                MetroAppBarIcon(
                    type = MetroSystemIconType.Check,
                    label = stringResource(R.string.save),
                    onClick = {
                        preferences.setTextReplies(replies)
                        onBack()
                    },
                ),
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
