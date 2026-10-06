package com.metro.people.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.metro.people.R
import com.metro.ui.MetroListItem
import com.metro.ui.MetroPageHeader
import com.metro.ui.MetroTextStyle

/** WP8.1 People Settings. */
@Composable
fun PeopleSettingsScreen(
    state: PeopleState,
    onBack: () -> Unit,
    onAddContacts: () -> Unit,
    onImportContacts: () -> Unit,
) {
    @Suppress("UNUSED_VARIABLE")
    val generation = state.generation
    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        MetroPageHeader(title = stringResource(R.string.settings))
        MetroListItem(
            title = stringResource(R.string.settings_add_contacts),
            subtitle = stringResource(R.string.settings_add_contacts_hint),
            onClick = onAddContacts,
        )
        MetroListItem(
            title = stringResource(R.string.settings_filter),
            subtitle = stringResource(R.string.settings_filter_hint),
            onClick = state::openFilter,
        )
        MetroListItem(
            title = stringResource(R.string.import_contacts),
            onClick = onImportContacts,
        )
    }
}

/** `add contacts` — real Android account setup + VCF import (no fake provider rows). */
@Composable
fun AddContactsScreen(
    onBack: () -> Unit,
    onAddAccount: () -> Unit,
    onImportContacts: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        MetroPageHeader(title = stringResource(R.string.settings_add_contacts))
        MetroListItem(
            title = stringResource(R.string.add_account),
            onClick = onAddAccount,
        )
        MetroListItem(
            title = stringResource(R.string.import_contacts),
            onClick = onImportContacts,
        )
    }
}
