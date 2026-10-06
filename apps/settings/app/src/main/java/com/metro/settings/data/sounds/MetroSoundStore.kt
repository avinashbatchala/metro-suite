package com.metro.settings.data.sounds

import android.content.Context
import com.metro.system.MetroSoundRole

/**
 * App-private persistence for the Metro sound pack: installed MediaStore URIs, pack version,
 * semantic role selections and system-default backups.
 *
 * Sound ids are stored separately from MediaStore URIs so a URI can be repaired/reinstalled
 * without losing the user's semantic selection.
 */
class MetroSoundStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ---- Pack version ------------------------------------------------------

    fun installedPackVersion(): Int? =
        prefs.getInt(KEY_PACK_VERSION, -1).takeIf { it >= 0 }

    fun setInstalledPackVersion(version: Int) {
        prefs.edit().putInt(KEY_PACK_VERSION, version).apply()
    }

    // ---- Installed URIs ----------------------------------------------------

    fun installedUri(soundId: String): String? =
        prefs.getString(uriKey(soundId), null)?.takeIf { it.isNotBlank() }

    fun setInstalledUri(soundId: String, uri: String) {
        prefs.edit().putString(uriKey(soundId), uri).apply()
    }

    fun removeInstalledUri(soundId: String) {
        prefs.edit().remove(uriKey(soundId)).apply()
    }

    fun installedIds(): Set<String> =
        prefs.all.keys
            .filter { it.startsWith(URI_PREFIX) }
            .map { it.removePrefix(URI_PREFIX) }
            .toSet()

    // ---- Role selections ---------------------------------------------------

    fun roleSelection(role: MetroSoundRole): String? =
        prefs.getString(roleKey(role), null)?.takeIf { it.isNotBlank() }

    fun roleSelections(): Map<MetroSoundRole, String> = buildMap {
        MetroSoundRole.entries.forEach { role ->
            roleSelection(role)?.let { put(role, it) }
        }
    }

    fun setRoleSelection(role: MetroSoundRole, soundId: String?) {
        val editor = prefs.edit()
        if (soundId.isNullOrBlank()) editor.remove(roleKey(role)) else editor.putString(roleKey(role), soundId)
        editor.apply()
    }

    // ---- Preferences -------------------------------------------------------

    var vibrate: Boolean
        get() = prefs.getBoolean(KEY_VIBRATE, true)
        set(value) { prefs.edit().putBoolean(KEY_VIBRATE, value).apply() }

    // ---- Previous system defaults (reversibility) --------------------------

    fun priorDefault(ringtoneType: Int): String? =
        prefs.getString(priorKey(ringtoneType), null)?.takeIf { it.isNotBlank() }

    fun rememberPriorDefault(ringtoneType: Int, uri: String?) {
        if (uri.isNullOrBlank()) return
        if (prefs.contains(priorKey(ringtoneType))) return
        prefs.edit().putString(priorKey(ringtoneType), uri).apply()
    }

    fun clearPriorDefault(ringtoneType: Int) {
        prefs.edit().remove(priorKey(ringtoneType)).apply()
    }

    private fun uriKey(id: String) = "$URI_PREFIX$id"
    private fun roleKey(role: MetroSoundRole) = "$ROLE_PREFIX${role.name}"
    private fun priorKey(type: Int) = "$PRIOR_PREFIX$type"

    private companion object {
        const val PREFS = "metro_sounds"
        const val KEY_PACK_VERSION = "pack_version"
        const val KEY_VIBRATE = "vibrate"
        const val URI_PREFIX = "uri_"
        const val ROLE_PREFIX = "role_"
        const val PRIOR_PREFIX = "prior_"
    }
}
