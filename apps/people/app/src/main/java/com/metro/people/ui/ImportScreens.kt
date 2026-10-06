package com.metro.people.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metro.people.R
import com.metro.people.data.PeopleContactsLogic
import com.metro.people.data.import.ClassifiedImport
import com.metro.people.data.import.DuplicateClass
import com.metro.people.data.import.ImportError
import com.metro.people.data.import.ImportPreview
import com.metro.people.data.import.ImportProgress
import com.metro.people.data.import.ImportResult
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroCircleIconButton
import com.metro.ui.MetroLoadingDots
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

/** Dispatches the import subpage to the right phase. */
@Composable
fun ImportScreen(
    state: PeopleState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val importState = state.importState
    val blocking = importState is ImportUiState.Parsing || importState is ImportUiState.Importing
    BackHandler(enabled = blocking) { /* swallow Back during parse/write */ }

    when (importState) {
        ImportUiState.Idle -> Unit
        ImportUiState.Parsing -> ImportParsingScreen(modifier = modifier)
        is ImportUiState.Preview -> ImportPreviewScreen(
            preview = importState.preview,
            onImport = state::confirmImport,
            onCancel = onBack,
            modifier = modifier,
        )
        is ImportUiState.Importing -> ImportProgressScreen(
            progress = ImportProgress(importState.done, importState.total),
            modifier = modifier,
        )
        is ImportUiState.Done -> ImportResultScreen(
            result = importState.result,
            onDone = state::finishImport,
            onViewPeople = state::finishImport,
            modifier = modifier,
        )
        is ImportUiState.Error -> ImportErrorScreen(
            error = importState.error,
            onClose = { state.cancelImport() },
            modifier = modifier,
        )
    }
}

@Composable
private fun ImportParsingScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(160.dp))
        MetroText(text = stringResource(R.string.import_parsing), style = MetroTextStyle.HubTitle)
        Spacer(modifier = Modifier.height(24.dp))
        MetroLoadingDots()
    }
}

@Composable
private fun ImportPreviewScreen(
    preview: ImportPreview,
    onImport: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val grouped = remember(preview) { groupForPreview(preview.classified) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        item {
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MetroCircleIconButton(
                    type = MetroSystemIconType.Back,
                    onClick = onCancel,
                    contentDescription = stringResource(R.string.import_cancel),
                )
            }
        }
        item {
            MetroText(
                text = stringResource(R.string.import_contacts).uppercase(),
                style = MetroTextStyle.SectionHeader,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
            )
        }
        item {
            ImportCountRow(stringResource(R.string.import_contacts_found), preview.total)
        }
        item {
            ImportCountRow(stringResource(R.string.import_new), preview.newCount)
        }
        item {
            ImportCountRow(stringResource(R.string.import_already), preview.exactCount)
        }
        item {
            ImportCountRow(stringResource(R.string.import_possible), preview.possibleCount)
        }
        if (preview.warningCount > 0) {
            item {
                MetroText(
                    text = stringResource(R.string.import_warning_count, preview.warningCount),
                    style = MetroTextStyle.ListItemSubtitle,
                    color = MetroTheme.colors.secondaryText,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
        item {
            MetroBorderButton(
                text = stringResource(R.string.import_action),
                onClick = onImport,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
            )
        }
        grouped.forEach { (letter, contacts) ->
            item {
                MetroText(
                    text = letter.toString(),
                    style = MetroTextStyle.SectionHeader,
                    color = MetroTheme.colors.secondaryText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
            items(contacts, key = { keyFor(it) }) { classified ->
                ImportContactRow(classified)
            }
        }
        item { Spacer(modifier = Modifier.height(96.dp)) }
    }
}

@Composable
private fun ImportCountRow(label: String, value: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MetroText(
            text = label,
            style = MetroTextStyle.ListItemTitle,
            modifier = Modifier.weight(1f),
        )
        MetroText(text = value.toString(), style = MetroTextStyle.HubTitle)
    }
}

@Composable
private fun ImportContactRow(classified: ClassifiedImport) {
    val name = classified.contact.resolvedDisplayName().ifEmpty {
        stringResource(R.string.import_no_name)
    }
    val subtitle = when (classified.classification) {
        DuplicateClass.EXACT_DUPLICATE -> stringResource(R.string.import_duplicate_exact)
        DuplicateClass.POSSIBLE_DUPLICATE -> stringResource(R.string.import_duplicate_possible)
        DuplicateClass.NEW -> classified.contact.phones.firstOrNull()?.number
            ?: classified.contact.emails.firstOrNull()?.address
            ?: ""
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        MetroText(
            text = name,
            style = MetroTextStyle.ListItemTitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (subtitle.isNotEmpty()) {
            MetroText(
                text = subtitle,
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ImportProgressScreen(
    progress: ImportProgress,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(160.dp))
        MetroText(text = stringResource(R.string.import_importing), style = MetroTextStyle.HubTitle)
        Spacer(modifier = Modifier.height(12.dp))
        MetroText(
            text = stringResource(R.string.import_progress, progress.done, progress.total),
            style = MetroTextStyle.PageTitle,
        )
        Spacer(modifier = Modifier.height(24.dp))
        MetroLoadingDots()
    }
}

@Composable
private fun ImportResultScreen(
    result: ImportResult,
    onDone: () -> Unit,
    onViewPeople: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 12.dp),
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        MetroText(
            text = stringResource(R.string.import_done_title).uppercase(),
            style = MetroTextStyle.SectionHeader,
        )
        Spacer(modifier = Modifier.height(24.dp))
        MetroText(text = stringResource(R.string.import_added, result.added), style = MetroTextStyle.ListItemTitle)
        if (result.skippedDuplicates > 0) {
            MetroText(
                text = stringResource(R.string.import_existing, result.skippedDuplicates),
                style = MetroTextStyle.ListItemTitle,
            )
        }
        if (result.possibleAdded > 0) {
            MetroText(
                text = stringResource(R.string.import_possible_added, result.possibleAdded),
                style = MetroTextStyle.ListItemTitle,
            )
        }
        if (result.failed > 0) {
            MetroText(
                text = stringResource(R.string.import_failed, result.failed),
                style = MetroTextStyle.ListItemTitle,
                color = MetroTheme.colors.accent,
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        MetroBorderButton(text = stringResource(R.string.import_done), onClick = onDone)
        Spacer(modifier = Modifier.height(12.dp))
        MetroBorderButton(text = stringResource(R.string.import_view_people), onClick = onViewPeople)
    }
}

@Composable
private fun ImportErrorScreen(
    error: ImportError,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 12.dp),
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        MetroText(
            text = stringResource(R.string.import_error_title).uppercase(),
            style = MetroTextStyle.SectionHeader,
        )
        Spacer(modifier = Modifier.height(24.dp))
        MetroText(
            text = stringResource(error.messageRes()),
            style = MetroTextStyle.ListItemTitle,
            color = MetroTheme.colors.primaryText,
        )
        Spacer(modifier = Modifier.height(32.dp))
        MetroBorderButton(text = stringResource(R.string.import_done), onClick = onClose)
    }
}

private fun ImportError.messageRes(): Int = when (this) {
    ImportError.WRITE_PERMISSION_DENIED -> R.string.import_error_write
    ImportError.READ_PERMISSION_REQUIRED -> R.string.import_error_read
    ImportError.FILE_UNREADABLE -> R.string.import_error_unreadable
    ImportError.EMPTY_FILE -> R.string.import_error_empty
    ImportError.NOT_A_VCARD -> R.string.import_error_not_vcard
    ImportError.TOO_MANY_CONTACTS -> R.string.import_error_too_many
    ImportError.IMPORT_FAILED -> R.string.import_error_generic
}

private fun groupForPreview(
    classified: List<ClassifiedImport>,
): Map<Char, List<ClassifiedImport>> =
    classified
        .sortedWith(
            compareBy<ClassifiedImport> {
                PeopleContactsLogic.sortKeyFor(it.contact.resolvedDisplayName(), sortByLastName = false)
            }.thenBy { it.contact.resolvedDisplayName().lowercase() },
        )
        .groupBy { PeopleContactsLogic.sortKeyFor(it.contact.resolvedDisplayName(), sortByLastName = false) }
        .toSortedMap()

private fun keyFor(classified: ClassifiedImport): String =
    "${classified.classification}:${classified.contact.resolvedDisplayName()}:${classified.contact.phones.firstOrNull()?.number}:${classified.contact.emails.firstOrNull()?.address}"
