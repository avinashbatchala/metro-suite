package com.metro.system

/**
 * Semantic Metro sound roles. Apps refer to a role (never a filename or Settings asset path) and
 * resolve it through [MetroSoundContract]. Settings owns the actual sound pack.
 *
 * `role` in [MetroSoundDescriptor] is nullable because some pack entries (alternate ringtones/alarms)
 * exist for user choice without a fixed semantic role.
 */
enum class MetroSoundRole {
    PHONE_RINGTONE,
    MESSAGE,
    MAIL,
    CALENDAR,
    REMINDER,
    SYSTEM_NOTIFICATION,
    ALARM,
    TIMER,
    ;

    companion object {
        fun fromKey(key: String?): MetroSoundRole? =
            entries.firstOrNull { it.name.equals(key?.trim(), ignoreCase = true) }
    }
}
