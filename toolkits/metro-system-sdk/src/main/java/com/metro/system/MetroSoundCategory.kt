package com.metro.system

/** Pack bucket a sound belongs to; drives MediaStore destination and picker filtering. */
enum class MetroSoundCategory {
    RINGTONE,
    NOTIFICATION,
    ALARM,
    UI,
    ;

    companion object {
        fun fromKey(key: String?): MetroSoundCategory? =
            entries.firstOrNull { it.name.equals(key?.trim(), ignoreCase = true) }
    }
}
