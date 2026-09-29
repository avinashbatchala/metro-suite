package com.metro.statusbar

/**
 * Setup **icons** tab toggles — which tray glyphs are allowed to appear when their
 * live condition is met. Clock is always shown and is not part of this set.
 */
data class TrayIconFlags(
    val network: Boolean = true,
    val wifi: Boolean = true,
    val mute: Boolean = true,
    val notifications: Boolean = true,
    val hotspot: Boolean = true,
    val bluetoothAudio: Boolean = true,
    val battery: Boolean = true,
)

/** Connected Bluetooth audio device class for the tray glyph. */
enum class BluetoothAudioKind {
    Headset,
    Speaker,
}
