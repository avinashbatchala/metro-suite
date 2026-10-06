package com.metro.dialer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import com.metro.dialer.R
import com.metro.dialer.data.DialerCallLogic
import com.metro.dialer.data.PhoneContact
import com.metro.dialer.data.SpeedDialEntry
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroEmptyState
import com.metro.ui.MetroMessageDialog
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroNavBarPadding

@Composable
fun SpeedDialAddScreen(
    state: DialerViewModel,
    onRequestPermissions: (Array<String>) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    LaunchedEffect(Unit) {
        if (!state.hasContactsPermission) {
            onRequestPermissions(arrayOf(android.Manifest.permission.READ_CONTACTS))
        }
        state.loadChooserContacts()
    }

    var numberPicker by remember { mutableStateOf<PhoneContact?>(null) }

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
            MetroAppTitle(title = stringResource(R.string.add))
            if (state.chooserContacts.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    MetroEmptyState(
                        message = stringResource(R.string.no_contacts),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (!state.hasContactsPermission) {
                        com.metro.ui.MetroBorderButton(
                            text = stringResource(R.string.allow_access),
                            onClick = {
                                onRequestPermissions(arrayOf(android.Manifest.permission.READ_CONTACTS))
                            },
                            modifier = Modifier.padding(top = 16.dp),
                        )
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                    items(state.chooserContacts, key = { it.contactId }) { contact ->
                        AddContactRow(
                            contact = contact,
                            onClick = {
                                if (contact.numbers.size > 1) {
                                    numberPicker = contact
                                } else {
                                    contact.numbers.firstOrNull()?.let { number ->
                                        state.addSpeedDialEntry(
                                            SpeedDialEntry(
                                                id = System.currentTimeMillis().toString(),
                                                displayName = contact.displayName,
                                                phoneNumber = number.number,
                                                contactLookupKey = contact.lookupKey,
                                                phoneDataId = number.dataId,
                                                normalizedNumberFallback =
                                                    DialerCallLogic.normalizeNumber(number.number),
                                            ),
                                        )
                                    }
                                    onBack()
                                }
                            },
                        )
                    }
                }
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

    numberPicker?.let { contact ->
        NumberPickerDialog(
            contact = contact,
            onSelect = { number ->
                state.addSpeedDialEntry(
                    SpeedDialEntry(
                        id = System.currentTimeMillis().toString(),
                        displayName = contact.displayName,
                        phoneNumber = number.number,
                        contactLookupKey = contact.lookupKey,
                        phoneDataId = number.dataId,
                        normalizedNumberFallback = DialerCallLogic.normalizeNumber(number.number),
                    ),
                )
                numberPicker = null
                onBack()
            },
            onDismiss = { numberPicker = null },
        )
    }
}

@Composable
private fun AddContactRow(
    contact: PhoneContact,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 14.dp),
    ) {
        MetroText(text = contact.displayName, style = MetroTextStyle.ListItemTitle)
        MetroText(
            text = contact.defaultNumber?.number.orEmpty(),
            style = MetroTextStyle.ListItemSubtitle,
            color = MetroTheme.colors.secondaryText,
        )
    }
}

@Composable
private fun NumberPickerDialog(
    contact: PhoneContact,
    onSelect: (com.metro.dialer.data.PhoneContactNumber) -> Unit,
    onDismiss: () -> Unit,
) {
    MetroMessageDialog(
        title = contact.displayName,
        onDismissRequest = onDismiss,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            contact.numbers.forEach { number ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelect(number) },
                        )
                        .padding(vertical = 12.dp),
                ) {
                    Column {
                        number.label?.let {
                            MetroText(
                                text = it,
                                style = MetroTextStyle.ListItemSubtitle,
                                color = MetroTheme.colors.secondaryText,
                            )
                        }
                        MetroText(text = number.number, style = MetroTextStyle.ListItemTitle)
                    }
                }
            }
        }
    }
}
