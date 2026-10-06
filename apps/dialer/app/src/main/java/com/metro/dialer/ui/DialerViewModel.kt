package com.metro.dialer.ui

import android.Manifest
import android.app.Application
import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import android.provider.ContactsContract
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.metro.dialer.R
import com.metro.dialer.data.CallEntry
import com.metro.dialer.data.CallGroup
import com.metro.dialer.data.CallLogRepository
import com.metro.dialer.data.ContactSuggestion
import com.metro.dialer.data.ContactsLookup
import com.metro.dialer.data.DialerCallLogic
import com.metro.dialer.data.HistorySection
import com.metro.dialer.data.PhoneContact
import com.metro.dialer.data.PhonePreferences
import com.metro.dialer.data.SpeedDialEntry
import com.metro.dialer.data.SpeedDialStore
import com.metro.dialer.telecom.MetroTelecomBridge
import com.metro.system.MetroIntents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SimOption(val handle: PhoneAccountHandle, val label: String)

data class SimChooserState(
    val number: String,
    val displayName: String?,
    val options: List<SimOption>,
    val isSelected: Boolean = true,
)

data class DialerStatus(val message: String)

class DialerViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val callLogRepository = CallLogRepository(appContext)
    private val contactsLookup = ContactsLookup(appContext)
    private val speedDialStore = SpeedDialStore(appContext)
    val phonePreferences = PhonePreferences(appContext)

    var route by androidx.compose.runtime.mutableStateOf(DialerRoute.Main)
        private set
    var pivot by androidx.compose.runtime.mutableStateOf(PhonePivot.History)
        private set

    var hasCallLogPermission by androidx.compose.runtime.mutableStateOf(false)
        private set
    var hasContactsPermission by androidx.compose.runtime.mutableStateOf(false)
        private set
    var hasCallPhonePermission by androidx.compose.runtime.mutableStateOf(false)
        private set
    var permissionsChecked by androidx.compose.runtime.mutableStateOf(false)
        private set

    var historyLoading by androidx.compose.runtime.mutableStateOf(true)
        private set
    var callGroups by androidx.compose.runtime.mutableStateOf<List<CallGroup>>(emptyList())
        private set
    var historySections by androidx.compose.runtime.mutableStateOf<List<HistorySection>>(emptyList())
        private set

    var speedDialEntries by androidx.compose.runtime.mutableStateOf<List<SpeedDialEntry>>(emptyList())
        private set
    var contactSuggestions by androidx.compose.runtime.mutableStateOf<List<ContactSuggestion>>(emptyList())
        private set
    var chooserContacts by androidx.compose.runtime.mutableStateOf<List<PhoneContact>>(emptyList())
        private set

    var dialString by androidx.compose.runtime.mutableStateOf("")
        private set
    var searchQuery by androidx.compose.runtime.mutableStateOf("")
        private set
    var searchVisible by androidx.compose.runtime.mutableStateOf(false)
        private set

    var selectedGroup by androidx.compose.runtime.mutableStateOf<CallGroup?>(null)
        private set

    var selectionMode by androidx.compose.runtime.mutableStateOf(false)
        private set
    var selectedCallIds by androidx.compose.runtime.mutableStateOf<Set<Long>>(emptySet())
        private set

    var simChooser by androidx.compose.runtime.mutableStateOf<SimChooserState?>(null)
        private set

    var status by androidx.compose.runtime.mutableStateOf<DialerStatus?>(null)
        private set

    var voicemailNumber by androidx.compose.runtime.mutableStateOf<String?>(null)
        private set
    var voicemailLoaded by androidx.compose.runtime.mutableStateOf(false)
        private set

    private var contactIndex: Map<String, ContactSuggestion> = emptyMap()
    private val mainHandler = Handler(Looper.getMainLooper())

    private val callLogObserver = object : ContentObserver(mainHandler) {
        override fun onChange(selfChange: Boolean) {
            viewModelScope.launch { reloadHistory() }
        }
    }
    private val contactsObserver = object : ContentObserver(mainHandler) {
        override fun onChange(selfChange: Boolean) {
            viewModelScope.launch { reloadContacts() }
        }
    }

    val filteredGroups: List<CallGroup>
        get() = DialerCallLogic.filterGroups(callGroups, searchQuery)

    val filteredSections: List<HistorySection>
        get() = DialerCallLogic.groupIntoSections(filteredGroups)

    val t9Suggestions: List<ContactSuggestion>
        get() {
            if (!phonePreferences.smartDialEnabled || !hasContactsPermission) return emptyList()
            val query = dialString.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
            if (query.isEmpty()) return emptyList()
            return DialerCallLogic.contactSuggestions(query, contactSuggestions)
        }

    init {
        refreshPermissions()
        reloadSpeedDial()
        reloadContacts()
        viewModelScope.launch { reloadHistory() }
        loadVoicemailNumber()
        runCatching {
            appContext.contentResolver.registerContentObserver(
                CallLog.Calls.CONTENT_URI,
                true,
                callLogObserver,
            )
            appContext.contentResolver.registerContentObserver(
                ContactsContract.Contacts.CONTENT_URI,
                true,
                contactsObserver,
            )
        }
    }

    override fun onCleared() {
        runCatching {
            appContext.contentResolver.unregisterContentObserver(callLogObserver)
            appContext.contentResolver.unregisterContentObserver(contactsObserver)
        }
        super.onCleared()
    }

    // ---- permissions -----------------------------------------------------

    fun refreshPermissions() {
        hasCallLogPermission = granted(Manifest.permission.READ_CALL_LOG)
        hasContactsPermission = granted(Manifest.permission.READ_CONTACTS)
        hasCallPhonePermission = granted(Manifest.permission.CALL_PHONE)
        permissionsChecked = true
    }
    fun onPermissionResult(callLogGranted: Boolean, contactsGranted: Boolean, callPhoneGranted: Boolean) {
        hasCallLogPermission = callLogGranted
        hasContactsPermission = contactsGranted
        hasCallPhonePermission = callPhoneGranted
        viewModelScope.launch {
            if (contactsGranted) reloadContacts() else {
                contactSuggestions = emptyList()
                contactIndex = emptyMap()
            }
            reloadHistory()
        }
    }

    /** Non-suspend refresh used after a contextual permission result. */
    fun refreshAfterPermissions() {
        refreshPermissions()
        reloadContacts()
        viewModelScope.launch { reloadHistory() }
    }

    private fun granted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED

    // ---- loading ---------------------------------------------------------

    suspend fun reloadHistory() {
        if (!hasCallLogPermission) {
            historyLoading = false
            callGroups = emptyList()
            historySections = emptyList()
            return
        }
        historyLoading = true
        val entries = withContext(Dispatchers.IO) {
            val raw = callLogRepository.loadRecentCalls()
            enrichWithContacts(raw)
        }
        callGroups = withContext(Dispatchers.Default) { DialerCallLogic.groupCalls(entries) }
        historySections = withContext(Dispatchers.Default) {
            DialerCallLogic.groupIntoSections(callGroups)
        }
        historyLoading = false
    }

    fun reloadContacts() {
        if (!hasContactsPermission) {
            contactSuggestions = emptyList()
            contactIndex = emptyMap()
            chooserContacts = emptyList()
            return
        }
        viewModelScope.launch {
            val contacts = withContext(Dispatchers.IO) { contactsLookup.loadPhoneContacts() }
            contactSuggestions = contacts
            contactIndex = buildIndex(contacts)
            // Re-enrich history names now that we have a contacts cache.
            if (hasCallLogPermission) reloadHistory()
        }
    }

    fun reloadSpeedDial() {
        viewModelScope.launch {
            val entries = withContext(Dispatchers.IO) { speedDialStore.load() }
            speedDialEntries = entries
        }
    }

    fun loadChooserContacts(onLoaded: (List<PhoneContact>) -> Unit = {}) {
        viewModelScope.launch {
            val contacts = withContext(Dispatchers.IO) { contactsLookup.loadContactsForChooser() }
            chooserContacts = contacts
            onLoaded(contacts)
        }
    }

    private fun enrichWithContacts(entries: List<CallEntry>): List<CallEntry> {
        if (contactIndex.isEmpty()) return entries
        return entries.map { entry ->
            if (entry.contactName != null || entry.phoneNumber.isBlank()) {
                entry
            } else {
                val match = contactIndex[DialerCallLogic.canonicalDigits(entry.phoneNumber)]
                if (match != null) {
                    entry.copy(
                        contactName = match.displayName,
                        contactLookupKey = match.contactLookupKey,
                        contactId = match.contactId,
                    )
                } else {
                    entry
                }
            }
        }
    }

    private fun buildIndex(contacts: List<ContactSuggestion>): Map<String, ContactSuggestion> {
        val map = HashMap<String, ContactSuggestion>()
        contacts.forEach { contact ->
            val key = DialerCallLogic.canonicalDigits(contact.phoneNumber)
            if (key.isNotEmpty() && !map.containsKey(key)) map[key] = contact
        }
        return map
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun loadVoicemailNumber() {
        viewModelScope.launch {
            val number = withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                runCatching {
                    val tm = appContext.getSystemService(TelephonyManager::class.java)
                    tm?.voiceMailNumber?.takeIf { it.isNotBlank() }
                }.getOrNull()
            }
            voicemailNumber = number
            voicemailLoaded = true
        }
    }

    // ---- navigation ------------------------------------------------------

    fun setPivot(index: Int) {
        pivot = when (index) {
            1 -> PhonePivot.SpeedDial
            2 -> PhonePivot.Voicemail
            else -> PhonePivot.History
        }
    }

    fun openDialPad() {
        route = DialerRoute.DialPad
    }

    fun openCallDetail(group: CallGroup) {
        selectedGroup = group
        route = DialerRoute.CallDetail
    }

    fun openSpeedDialAdd() {
        route = DialerRoute.SpeedDialAdd
    }

    fun openPhoneSettings() {
        route = DialerRoute.PhoneSettings
    }

    fun openEditReplies() {
        route = DialerRoute.EditReplies
    }

    fun openSaveContact() {
        route = DialerRoute.SaveContact
    }

    fun closeSubpage() {
        route = DialerRoute.Main
        selectedGroup = null
    }

    fun handleDialIntent(uri: Uri?) {
        val number = uri?.schemeSpecificPart?.trim().orEmpty()
        if (number.isNotEmpty()) {
            dialString = number
            route = DialerRoute.DialPad
        }
    }

    fun toggleSearch() {
        searchVisible = !searchVisible
        if (!searchVisible) searchQuery = ""
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
    }

    // ---- dial pad --------------------------------------------------------

    fun appendDialChar(char: Char) {
        dialString += char
    }

    fun deleteDialChar() {
        if (dialString.isNotEmpty()) dialString = dialString.dropLast(1)
    }

    fun replaceDialString(value: String) {
        dialString = value
    }

    fun selectSuggestion(suggestion: ContactSuggestion) {
        val alreadySelected = DialerCallLogic.areSameNumber(dialString, suggestion.phoneNumber)
        if (alreadySelected) {
            placeCall(suggestion.phoneNumber, suggestion.displayName)
        } else {
            dialString = suggestion.phoneNumber
        }
    }

    // ---- calls -----------------------------------------------------------

    fun placeCall(number: String, displayName: String? = null) {
        val trimmed = number.trim()
        if (trimmed.isEmpty()) return
        if (MetroTelecomBridge.hasMultipleCallCapableAccounts(appContext) &&
            MetroTelecomBridge.defaultOutgoingAccount(appContext) == null &&
            !MetroTelecomBridge.isEmergencyNumber(trimmed)
        ) {
            val accounts = MetroTelecomBridge.callCapableAccounts(appContext)
            simChooser = SimChooserState(
                number = trimmed,
                displayName = displayName,
                options = accounts.mapIndexed { index, handle ->
                    SimOption(handle, simLabel(handle, index))
                },
            )
            return
        }
        MetroTelecomBridge.placeOutgoingCall(appContext, trimmed, displayName)
    }

    fun confirmSimChoice(option: SimOption) {
        val chooser = simChooser ?: return
        simChooser = null
        MetroTelecomBridge.placeOutgoingCall(
            appContext,
            chooser.number,
            chooser.displayName,
            option.handle,
        )
    }

    fun dismissSimChooser() {
        simChooser = null
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun simLabel(handle: PhoneAccountHandle, index: Int): String {
        val telecom = appContext.getSystemService(TelecomManager::class.java)
        val label = runCatching { telecom?.getPhoneAccount(handle)?.label?.toString() }.getOrNull()
        return label?.takeIf { it.isNotBlank() } ?: "SIM ${index + 1}"
    }

    fun callVoicemail() {
        val number = voicemailNumber
        if (number.isNullOrBlank()) {
            status = DialerStatus(appContext.getString(R.string.voicemail_unavailable))
            return
        }
        placeCall(number, appContext.getString(R.string.voicemail))
    }

    // ---- history actions -------------------------------------------------

    fun enterSelectionMode() {
        selectionMode = true
        selectedCallIds = emptySet()
    }

    fun exitSelectionMode() {
        selectionMode = false
        selectedCallIds = emptySet()
    }

    fun toggleSelection(callId: Long) {
        selectedCallIds = if (callId in selectedCallIds) {
            selectedCallIds - callId
        } else {
            selectedCallIds + callId
        }
    }

    fun selectAll() {
        selectedCallIds = callGroups.flatMap { group -> group.calls.map { it.id } }.toSet()
    }

    fun deleteSelected() {
        val ids = selectedCallIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { callLogRepository.deleteCalls(ids) }
            if (!ok) {
                status = DialerStatus(appContext.getString(R.string.delete_failed))
            }
            exitSelectionMode()
            reloadHistory()
        }
    }

    fun deleteGroup(group: CallGroup) {
        val ids = group.calls.map { it.id }
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { callLogRepository.deleteCalls(ids) }
            if (!ok) status = DialerStatus(appContext.getString(R.string.delete_failed))
            selectedGroup = null
            reloadHistory()
        }
    }

    fun blockNumber(group: CallGroup) {
        val number = group.phoneNumber.trim()
        if (number.isEmpty()) {
            status = DialerStatus(appContext.getString(R.string.block_unavailable))
            return
        }
        // Call screening is owned by Android Telecom. Open the platform blocked-numbers surface;
        // Metro never keeps a private list that Telecom would ignore.
        val blockedIntent = android.content.Intent(BLOCKED_NUMBERS_SETTINGS)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        val launched = runCatching { appContext.startActivity(blockedIntent) }.isSuccess
        if (!launched) {
            status = DialerStatus(appContext.getString(R.string.block_unavailable))
        }
    }

    fun addToSpeedDial(group: CallGroup) {
        val entry = SpeedDialEntry(
            id = System.currentTimeMillis().toString(),
            displayName = group.displayName,
            phoneNumber = group.phoneNumber,
            contactLookupKey = group.contactLookupKey,
            normalizedNumberFallback = DialerCallLogic.normalizeNumber(group.phoneNumber),
        )
        viewModelScope.launch {
            val updated = withContext(Dispatchers.IO) { speedDialStore.add(entry) }
            speedDialEntries = updated
        }
    }

    fun addSpeedDialEntry(entry: SpeedDialEntry) {
        viewModelScope.launch {
            val updated = withContext(Dispatchers.IO) { speedDialStore.add(entry) }
            speedDialEntries = updated
        }
    }

    fun removeSpeedDial(entry: SpeedDialEntry) {
        viewModelScope.launch {
            val updated = withContext(Dispatchers.IO) { speedDialStore.removeById(entry.id) }
            speedDialEntries = updated
        }
    }

    fun sendMessage(number: String) {
        val trimmed = number.trim()
        if (trimmed.isEmpty()) return
        val intent = android.content.Intent(
            android.content.Intent.ACTION_SENDTO,
            Uri.parse("smsto:$trimmed"),
        ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            appContext.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            status = DialerStatus(appContext.getString(R.string.messaging_unavailable))
        }
    }

    // ---- People / contact handoff ---------------------------------------

    fun launchPeople() {
        val intent = appContext.packageManager.getLaunchIntentForPackage(PEOPLE_PACKAGE)
        if (intent == null) {
            status = DialerStatus(appContext.getString(R.string.people_unavailable))
            return
        }
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { appContext.startActivity(intent) }.onFailure {
            status = DialerStatus(appContext.getString(R.string.people_unavailable))
        }
    }

    fun openContact(group: CallGroup) {
        val contactId = group.contactId
        if (contactId != null) {
            launchPeopleContact(contactId)
        } else {
            // Unknown number → Save contact flow.
            dialString = group.phoneNumber
            route = DialerRoute.SaveContact
        }
    }

    fun openContact(entry: SpeedDialEntry) {
        val contactId = resolveContactId(entry.phoneNumber)
        if (contactId != null) launchPeopleContact(contactId) else launchPeople()
    }

    private fun launchPeopleContact(contactId: Long) {
        val uri = Uri.parse("metro://people/contact/$contactId")
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
            .setPackage(PEOPLE_PACKAGE)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        val launched = runCatching { appContext.startActivity(intent) }.isSuccess
        if (!launched) launchPeople()
    }

    fun saveNumberAsNewContact(number: String) {
        val trimmed = number.trim()
        if (trimmed.isEmpty()) {
            status = DialerStatus(appContext.getString(R.string.save_contact_empty))
            return
        }
        val intent = android.content.Intent(
            android.content.Intent.ACTION_INSERT,
            ContactsContract.Contacts.CONTENT_URI,
        ).apply {
            putExtra(ContactsContract.Intents.Insert.PHONE, trimmed)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val launched = runCatching { appContext.startActivity(intent) }.isSuccess
        if (!launched) {
            // Fall back to People.
            val peopleIntent = appContext.packageManager.getLaunchIntentForPackage(PEOPLE_PACKAGE)
            if (peopleIntent != null) {
                peopleIntent.putExtra(EXTRA_PREFILL_PHONE, trimmed)
                peopleIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                appContext.startActivity(peopleIntent)
            } else {
                status = DialerStatus(appContext.getString(R.string.people_unavailable))
            }
        }
    }

    fun addNumberToExistingContact(number: String) {
        val trimmed = number.trim()
        if (trimmed.isEmpty()) return
        val intent = android.content.Intent(
            android.content.Intent.ACTION_INSERT_OR_EDIT,
            ContactsContract.Contacts.CONTENT_URI,
        ).apply {
            type = ContactsContract.Contacts.CONTENT_ITEM_TYPE
            putExtra(ContactsContract.Intents.Insert.PHONE, trimmed)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val launched = runCatching { appContext.startActivity(intent) }.isSuccess
        if (!launched) status = DialerStatus(appContext.getString(R.string.people_unavailable))
    }

    fun pinToStart(entry: SpeedDialEntry) {
        val contactId = resolveContactId(entry.phoneNumber)
        if (contactId == null) {
            status = DialerStatus(appContext.getString(R.string.pin_needs_contact))
            return
        }
        MetroIntents.requestPinTile(
            context = appContext,
            packageName = MetroIntents.PACKAGE_PEOPLE,
            tileId = "contact:$contactId",
        )
    }

    private fun resolveContactId(phoneNumber: String): Long? {
        if (!hasContactsPermission) return null
        return runCatching { contactsLookup.resolveContactId(phoneNumber) }.getOrNull()
    }

    fun dismissStatus() {
        status = null
    }

    fun photoUriFor(number: String): Uri? = contactIndex[DialerCallLogic.canonicalDigits(number)]?.photoUri

    fun contactFor(number: String): ContactSuggestion? =
        contactIndex[DialerCallLogic.canonicalDigits(number)]

    companion object {
        const val PEOPLE_PACKAGE = "com.metro.people"
        const val EXTRA_PREFILL_PHONE = "com.metro.dialer.extra.PREFILL_PHONE"

        /** AOSP blocked-numbers settings surface (no public [android.provider.Settings] constant). */
        const val BLOCKED_NUMBERS_SETTINGS = "android.settings.BLOCKED_NUMBERS_SETTINGS"
    }
}
