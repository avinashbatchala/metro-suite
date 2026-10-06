package com.metro.system

/**
 * A shared, provider-independent description of a Metro sound. Consumer apps see only this via
 * [MetroSoundContract] — never Settings asset paths or MediaStore internals.
 */
data class MetroSoundDescriptor(
    /** Stable pack id, e.g. `metro_beacon` (never a filename/URI). */
    val id: String,
    /** User-facing title from the pack, e.g. `Metro Beacon`. */
    val title: String,
    /** Semantic role this sound fills, when it is a primary role asset. */
    val role: MetroSoundRole?,
    val category: MetroSoundCategory,
) {
    val hasContent: Boolean
        get() = id.isNotBlank() && title.isNotBlank()
}
