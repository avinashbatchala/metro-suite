package com.metro.settings.data.sounds

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.metro.system.MetroSoundCategory
import com.metro.system.MetroSoundRole

/**
 * Settings-side hub for the Metro sound pack: install, semantic role selection, system-default
 * application and URI resolution. Also backs [MetroSoundsProvider] for other Metro apps.
 */
class MetroSoundsController(context: Context) {
    private val appContext = context.applicationContext
    val registry = SettingsSoundRegistry(appContext)
    val store = MetroSoundStore(appContext)
    val installer = MetroSoundInstaller(appContext, registry, store)
    private val defaults = MetroRingtoneDefaults(appContext)

    var installedVersion by mutableStateOf(store.installedPackVersion())
        private set
    var selections by mutableStateOf(store.roleSelections())
        private set
    var vibrate by mutableStateOf(store.vibrate)
        private set

    /** A system-default change that is waiting for the user to grant Modify System Settings. */
    var pendingSystemApply by mutableStateOf<Pair<MetroSoundRole, Uri>?>(null)
        private set

    var lastInstallSummary by mutableStateOf<String?>(null)
        private set

    enum class SelectOutcome { APPLIED, PERSISTED_ONLY, NEEDS_WRITE_SETTINGS, NOT_INSTALLED }

    fun refresh() {
        installedVersion = store.installedPackVersion()
        selections = store.roleSelections()
        vibrate = store.vibrate
    }

    fun isPackInstalled(): Boolean = installedVersion != null

    fun installPack(): MetroSoundInstaller.Result {
        val result = installer.install()
        refresh()
        lastInstallSummary = "installed ${result.installed}, reused ${result.reused}, failed ${result.failed}"
        return result
    }

    fun applyVibrate(enabled: Boolean) {
        vibrate = enabled
        store.vibrate = enabled
    }

    // ---- Selection ---------------------------------------------------------

    fun selectedSoundId(role: MetroSoundRole): String? = store.roleSelection(role)

    fun selectionLabel(role: MetroSoundRole): String =
        selectedSoundId(role)?.let { registry.assetTitle(it) } ?: SYSTEM_DEFAULT

    /** Default title for a role's bundled asset (shown on the settings row as a hint). */
    fun roleDefaultTitle(role: MetroSoundRole): String? = registry.roleAsset(role)?.title

    fun choicesForRole(role: MetroSoundRole): List<MetroSoundAsset> = registry.assetsForRole(role)

    fun canWriteSystem(): Boolean = defaults.canWrite()

    fun writeSettingsIntent() = defaults.writeSettingsIntent()

    fun selectSound(role: MetroSoundRole, soundId: String?): SelectOutcome {
        store.setRoleSelection(role, soundId)
        refresh()

        if (soundId.isNullOrBlank()) {
            // "system default": revert a system-linked default if we changed it earlier.
            MetroRingtoneDefaults.systemTypeFor(role)?.let { type ->
                store.priorDefault(type)?.let { prior ->
                    runCatching { Uri.parse(prior) }.getOrNull()?.let { defaults.setDefault(type, it) }
                }
            }
            return SelectOutcome.APPLIED
        }

        val uri = resolveUri(soundId) ?: return SelectOutcome.NOT_INSTALLED
        val type = MetroRingtoneDefaults.systemTypeFor(role) ?: return SelectOutcome.PERSISTED_ONLY
        store.rememberPriorDefault(type, defaults.currentDefault(type)?.toString())
        return if (defaults.canWrite()) {
            if (defaults.setDefault(type, uri)) SelectOutcome.APPLIED else SelectOutcome.PERSISTED_ONLY
        } else {
            pendingSystemApply = role to uri
            SelectOutcome.NEEDS_WRITE_SETTINGS
        }
    }

    /** Re-attempts a pending system-default change (call after returning from Android Settings). */
    fun applyPendingIfPossible(): MetroSoundRole? {
        val pending = pendingSystemApply ?: return null
        if (!defaults.canWrite()) return null
        val type = MetroRingtoneDefaults.systemTypeFor(pending.first) ?: return null
        store.rememberPriorDefault(type, defaults.currentDefault(type)?.toString())
        val ok = defaults.setDefault(type, pending.second)
        pendingSystemApply = null
        return if (ok) pending.first else null
    }

    fun clearPending() {
        pendingSystemApply = null
    }

    // ---- Resolution --------------------------------------------------------

    /** A valid, installed URI for [soundId], repairing the stored entry when possible. */
    fun resolveUri(soundId: String): Uri? {
        val asset = registry.assetById(soundId) ?: return null
        installer.existingUri(asset)?.let { return it }
        return store.installedUri(soundId)?.let { runCatching { Uri.parse(it) }.getOrNull() }
    }

    data class RoleInfo(
        val soundId: String?,
        val title: String,
        val category: MetroSoundCategory,
        val uri: Uri?,
    )

    /** Snapshot used by the provider: selected (or default) sound for [role]. */
    fun roleInfo(role: MetroSoundRole): RoleInfo {
        val soundId = store.roleSelection(role) ?: registry.roleAsset(role)?.id
        val asset = soundId?.let { registry.assetById(it) }
        return RoleInfo(
            soundId = soundId,
            title = asset?.title ?: role.name,
            category = asset?.category ?: SettingsSoundRegistry.categoryForRole(role),
            uri = soundId?.let { resolveUri(it) },
        )
    }

    companion object {
        const val SYSTEM_DEFAULT = "system default"
    }
}
