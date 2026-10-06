package com.metro.people.ui

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.metro.people.R
import com.metro.people.data.AccountOption
import com.metro.people.data.ContactsRepository
import com.metro.people.data.PeopleContactsLogic
import com.metro.people.data.PeopleFilter
import com.metro.people.data.PersonDetail
import com.metro.people.data.PersonSummary
import com.metro.people.data.WhatsAppLink
import com.metro.people.data.import.ContactImportRepository
import com.metro.people.data.import.ImportError
import com.metro.people.data.import.ImportPreview
import com.metro.people.data.import.ImportResult
import com.metro.people.data.import.ParseResult
import com.metro.people.data.import.toImportError
import com.metro.people.tiles.PeopleTileLogic
import com.metro.system.MetroIntents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class PeopleRoute {
    data object Hub : PeopleRoute()
    data object Filter : PeopleRoute()
    data object Settings : PeopleRoute()
    data object AddContacts : PeopleRoute()
    data class Detail(val contactId: Long) : PeopleRoute()
    data object Import : PeopleRoute()
}

/** Distinct phases of the VCF import subpage. */
sealed interface ImportUiState {
    data object Idle : ImportUiState
    data object Parsing : ImportUiState
    data class Preview(val preview: ImportPreview) : ImportUiState
    data class Importing(val done: Int, val total: Int) : ImportUiState
    data class Done(val result: ImportResult) : ImportUiState
    data class Error(val error: ImportError) : ImportUiState
}

class PeopleState(context: Context) {
    private val repository = ContactsRepository(context)
    private val importRepository = ContactImportRepository(context)
    internal val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** Bumped on every mutation so Compose recomposes. */
    var generation by mutableIntStateOf(0)
        private set

    private fun notifyChanged() {
        generation++
    }

    var hasContactsPermission: Boolean = false
        private set

    var hasWriteContactsPermission: Boolean = false
        private set

    /** False until the first [refreshPermission] completes — avoids flashing the permission gate. */
    var permissionsChecked: Boolean = false
        private set

    var route: PeopleRoute = PeopleRoute.Hub
        private set

    var allContacts: List<PersonSummary> = emptyList()
        private set

    var filter: PeopleFilter = PeopleFilter(hideWithoutPhone = true)
        private set

    var searchQuery: String = ""
        private set

    var searchVisible: Boolean = false
        private set

    var jumpListVisible: Boolean = false
        private set

    var selectedDetail: PersonDetail? = null
        private set

    // ---- Import ------------------------------------------------------------

    var importState by mutableStateOf<ImportUiState>(ImportUiState.Idle)
        private set

    /** Set by the Activity so [PeopleState] can request WRITE_CONTACTS at import time only. */
    var requestWriteContactsPermission: (() -> Unit)? = null

    /** Set by the Activity so [PeopleState] can request CALL_PHONE on the first direct call. */
    var requestCallPhonePermission: (() -> Unit)? = null

    private var pendingImportUri: Uri? = null

    val accountOptions: List<AccountOption> = repository.accountOptions()

    private var knownAccounts: Set<String> = emptySet()

    val visibleContacts: List<PersonSummary>
        get() {
            val filtered = PeopleContactsLogic.applyFilter(allContacts, filter, knownAccounts)
            val query = searchQuery.trim().lowercase()
            if (query.isEmpty()) return filtered
            return filtered.filter { it.displayName.lowercase().contains(query) }
        }

    val groupedContacts: Map<Char, List<PersonSummary>>
        get() = PeopleContactsLogic.groupBySortKey(visibleContacts)

    val filterLabel: String
        get() = PeopleContactsLogic.filterLabel(filter)

    fun refreshPermission(context: Context) {
        hasContactsPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS,
        ) == PackageManager.PERMISSION_GRANTED
        hasWriteContactsPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CONTACTS,
        ) == PackageManager.PERMISSION_GRANTED
        permissionsChecked = true
        notifyChanged()
    }

    fun onPermissionResult(granted: Boolean) {
        hasContactsPermission = granted
        if (granted) {
            reloadContacts()
            resumePendingImport()
        }
        notifyChanged()
    }

    fun reloadContacts() {
        if (!hasContactsPermission) return
        allContacts = repository.loadContacts()
        knownAccounts = repository.discoverAccounts(allContacts)
        if (filter.visibleAccounts.isEmpty()) {
            filter = filter.copy(visibleAccounts = knownAccounts)
        }
        notifyChanged()
    }

    fun openFilter() {
        dismissSearch()
        route = PeopleRoute.Filter
        notifyChanged()
    }

    fun openSettings() {
        dismissSearch()
        route = PeopleRoute.Settings
        notifyChanged()
    }

    fun openAddContacts() {
        route = PeopleRoute.AddContacts
        notifyChanged()
    }

    /** Inline Metro status message (replaces Android Toasts). */
    var statusMessage: String? by mutableStateOf(null)
        private set

    fun clearStatus() {
        statusMessage = null
        notifyChanged()
    }

    /** Opens the platform account-setup surface for adding contact sources. */
    fun addAccount() {
        val intent = Intent(android.provider.Settings.ACTION_ADD_ACCOUNT).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val ok = runCatching { appContext.startActivity(intent) }.isSuccess
        if (!ok) {
            statusMessage = appContext.getString(R.string.add_account_unavailable)
            notifyChanged()
        }
    }

    /** Opens the platform contact editor (device-local contacts are valid on Android). */
    fun newContact() {
        val intent = Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val ok = runCatching { appContext.startActivity(intent) }.isSuccess
        if (!ok) {
            statusMessage = appContext.getString(R.string.contact_editor_unavailable)
            notifyChanged()
        }
    }

    fun closeOverlay() {
        if (route == PeopleRoute.Import) {
            importState = ImportUiState.Idle
            pendingImportUri = null
        }
        route = PeopleRoute.Hub
        jumpListVisible = false
        notifyChanged()
    }

    fun openDetail(contactId: Long) {
        dismissSearch()
        selectedDetail = repository.loadDetail(contactId)
        route = PeopleRoute.Detail(contactId)
        notifyChanged()
    }

    fun openSearch() {
        jumpListVisible = false
        searchVisible = true
        notifyChanged()
    }

    fun dismissSearch() {
        if (!searchVisible && searchQuery.isEmpty()) return
        searchVisible = false
        searchQuery = ""
        notifyChanged()
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
        notifyChanged()
    }

    fun saveFilter(newFilter: PeopleFilter) {
        filter = newFilter
        notifyChanged()
    }

    // ---- Import actions ----------------------------------------------------

    /** Entry point from the picker result or an incoming ACTION_VIEW intent. */
    fun onVcfPicked(uri: Uri) {
        pendingImportUri = uri
        dismissSearch()
        resumePendingImport()
    }

    fun onWritePermissionResult(granted: Boolean) {
        hasWriteContactsPermission = granted
        if (granted) {
            resumePendingImport()
        } else {
            route = PeopleRoute.Import
            importState = ImportUiState.Error(ImportError.WRITE_PERMISSION_DENIED)
            notifyChanged()
        }
    }

    private fun resumePendingImport() {
        if (pendingImportUri == null) return
        if (!hasContactsPermission) return
        if (!hasWriteContactsPermission) {
            requestWriteContactsPermission?.invoke()
            return
        }
        beginImport(pendingImportUri!!)
    }

    private fun beginImport(uri: Uri) {
        importState = ImportUiState.Parsing
        route = PeopleRoute.Import
        notifyChanged()
        scope.launch {
            val fileName = withContext(Dispatchers.IO) { importRepository.displayName(uri) }
            val parsed = withContext(Dispatchers.IO) { importRepository.parse(uri) }
            importState = when (parsed) {
                is ParseResult.Failure -> ImportUiState.Error(parsed.reason.toImportError())
                is ParseResult.Success -> {
                    val preview = withContext(Dispatchers.IO) {
                        importRepository.buildPreview(parsed.contacts, parsed.warningCount, fileName)
                    }
                    ImportUiState.Preview(preview)
                }
            }
            notifyChanged()
        }
    }

    fun confirmImport() {
        val current = importState
        if (current !is ImportUiState.Preview) return
        val importable = current.preview.importable
        importState = ImportUiState.Importing(0, importable.size)
        notifyChanged()
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                importRepository.import(importable, current.preview.exactCount) { done, total ->
                    scope.launch {
                        if (importState is ImportUiState.Importing) {
                            importState = ImportUiState.Importing(done, total)
                            notifyChanged()
                        }
                    }
                }
            }
            pendingImportUri = null
            importState = ImportUiState.Done(result)
            reloadContacts()
            com.metro.people.tiles.PeopleTileRefresh.request(appContext)
            notifyChanged()
        }
    }

    fun finishImport() {
        importState = ImportUiState.Idle
        pendingImportUri = null
        route = PeopleRoute.Hub
        reloadContacts()
        notifyChanged()
    }

    fun cancelImport() {
        importState = ImportUiState.Idle
        pendingImportUri = null
        route = PeopleRoute.Hub
        notifyChanged()
    }

    // ---- Existing actions --------------------------------------------------

    fun callContact(person: PersonSummary) {
        val number = person.defaultPhone
        if (number.isNullOrBlank()) {
            openDetail(person.id)
            return
        }
        val hasCall = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.CALL_PHONE,
        ) == PackageManager.PERMISSION_GRANTED
        // WP8.1 taps the primary number directly; request CALL_PHONE contextually, else DIAL.
        if (!hasCall) requestCallPhonePermission?.invoke()
        val action = if (hasCall) Intent.ACTION_CALL else Intent.ACTION_DIAL
        val intent = Intent(action, Uri.parse("tel:$number")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { appContext.startActivity(intent) }.onFailure {
            val dial = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { appContext.startActivity(dial) }
        }
    }

    fun addToSpeedDial(person: PersonSummary) {
        val number = person.defaultPhone
        if (number.isNullOrBlank()) {
            statusMessage = appContext.getString(R.string.speed_dial_needs_phone)
            notifyChanged()
            return
        }
        MetroIntents.requestAddSpeedDial(
            context = appContext,
            displayName = person.displayName,
            phoneNumber = number,
            contactId = person.id,
        )
    }

    fun pinToStart(person: PersonSummary) {
        MetroIntents.requestPinTile(
            context = appContext,
            packageName = MetroIntents.PACKAGE_PEOPLE,
            tileId = PeopleTileLogic.contactTileId(person.id),
        )
    }

    fun openDeepLink(uri: Uri?) {
        val contactId = PeopleTileLogic.parseContactDeepLink(uri) ?: return
        openDetail(contactId)
    }

    fun textContact(person: PersonSummary) {
        val number = person.defaultPhone ?: return
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(intent)
    }

    fun whatsAppCall(link: WhatsAppLink) {
        val dataId = link.voiceCallDataId ?: return
        openWhatsAppData(dataId, link.packageName)
    }

    fun whatsAppText(link: WhatsAppLink) {
        val dataId = link.messageDataId
        if (dataId != null) {
            openWhatsAppData(dataId, link.packageName)
            return
        }
        val digits = link.phoneDigits ?: return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digits")).apply {
            setPackage(link.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (runCatching { appContext.startActivity(intent) }.isFailure) {
            statusMessage = appContext.getString(R.string.whatsapp_unavailable)
            notifyChanged()
        }
    }

    private fun openWhatsAppData(dataId: Long, packageName: String) {
        val uri = ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, dataId)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setData(uri)
            setPackage(packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (runCatching { appContext.startActivity(intent) }.isFailure) {
            statusMessage = appContext.getString(R.string.whatsapp_unavailable)
            notifyChanged()
        }
    }

    fun emailContact(address: String) {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$address"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(intent)
    }

    fun toggleJumpList() {
        if (searchVisible) return
        jumpListVisible = !jumpListVisible
        notifyChanged()
    }

    fun dismissJumpList() {
        jumpListVisible = false
        notifyChanged()
    }

    fun knownAccounts(): Set<String> =
        if (allContacts.isEmpty()) {
            setOf(appContext.getString(com.metro.people.R.string.account_device))
        } else {
            allContacts.map { it.sourceLabel }.toSet()
        }

    companion object {
        private const val TAG = "PeopleState"
    }
}
