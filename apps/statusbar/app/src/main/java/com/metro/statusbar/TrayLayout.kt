package com.metro.statusbar

import java.util.UUID

/**
 * Icons that can appear in the configure-page layout (icons-tab glyphs + clock).
 * Network is one slot (cellular bars + data label).
 */
enum class TrayLayoutIcon(val storageKey: String) {
    Network("network"),
    Wifi("wifi"),
    Mute("mute"),
    Notifications("notifications"),
    Hotspot("hotspot"),
    BluetoothAudio("bluetooth"),
    Battery("battery"),
    Clock("clock"),
    ;

    fun isEnabled(flags: TrayIconFlags): Boolean = when (this) {
        Network -> flags.network
        Wifi -> flags.wifi
        Mute -> flags.mute
        Notifications -> flags.notifications
        Hotspot -> flags.hotspot
        BluetoothAudio -> flags.bluetoothAudio
        Battery -> flags.battery
        Clock -> true
    }

    companion object {
        fun fromStorage(key: String): TrayLayoutIcon? =
            entries.firstOrNull { it.storageKey == key }
    }
}

/**
 * One slot in the configurable tray row — an icon or a blank spacer used to clear
 * notch / privacy-mic regions.
 */
sealed class TrayLayoutSlot {
    abstract val id: String

    data class Icon(val kind: TrayLayoutIcon) : TrayLayoutSlot() {
        override val id: String get() = kind.storageKey
    }

    data class Spacer(
        override val id: String,
        val widthDp: Int = TrayLayout.DEFAULT_SPACER_WIDTH_DP,
    ) : TrayLayoutSlot()
}

object TrayLayout {
    const val DEFAULT_SPACER_WIDTH_DP = 40

    /**
     * Absolute spacer ceiling. Practical max is lower when most icons are enabled —
     * see [maxSpacers].
     */
    const val MAX_SPACERS = 3

    /** Icons the user can toggle off (clock is always shown). */
    val TOGGLEABLE_ICONS: List<TrayLayoutIcon> =
        TrayLayoutIcon.entries.filter { it != TrayLayoutIcon.Clock }

    /** WP-style default: all icons L→R, no spacers. */
    val DEFAULT: List<TrayLayoutSlot> = TrayLayoutIcon.entries.map { TrayLayoutSlot.Icon(it) }

    fun spacerCount(slots: List<TrayLayoutSlot>): Int =
        slots.count { it is TrayLayoutSlot.Spacer }

    fun disabledToggleableCount(flags: TrayIconFlags): Int =
        TOGGLEABLE_ICONS.count { !it.isEnabled(flags) }

    /**
     * Spacers allowed for the current icons-tab set: **1** when every toggleable icon is
     * on (full tray; more wraps the clock), plus one more per disabled icon, capped at
     * [MAX_SPACERS].
     */
    fun maxSpacers(flags: TrayIconFlags): Int =
        (1 + disabledToggleableCount(flags)).coerceIn(1, MAX_SPACERS)

    fun canAddSpacer(slots: List<TrayLayoutSlot>, flags: TrayIconFlags): Boolean =
        spacerCount(slots) < maxSpacers(flags)

    /**
     * Drops trailing excess spacers when icons are re-enabled and the allowance shrinks.
     * Keeps earlier spacers (left-to-right) so notch placement stays put.
     */
    fun trimSpacersToMax(
        slots: List<TrayLayoutSlot>,
        flags: TrayIconFlags,
    ): List<TrayLayoutSlot> {
        val max = maxSpacers(flags)
        if (spacerCount(slots) <= max) return slots
        var kept = 0
        return slots.mapNotNull { slot ->
            when (slot) {
                is TrayLayoutSlot.Icon -> slot
                is TrayLayoutSlot.Spacer -> {
                    if (kept < max) {
                        kept++
                        slot
                    } else {
                        null
                    }
                }
            }
        }
    }

    fun serialize(slots: List<TrayLayoutSlot>): String =
        slots.joinToString(",") { slot ->
            when (slot) {
                is TrayLayoutSlot.Icon -> slot.kind.storageKey
                is TrayLayoutSlot.Spacer -> "spacer:${slot.id}:${slot.widthDp}"
            }
        }

    /**
     * Parses a stored layout string. Unknown tokens are skipped; missing icons from the
     * default set are appended so upgrades never drop a new glyph slot.
     */
    fun parse(raw: String?): List<TrayLayoutSlot> {
        if (raw.isNullOrBlank()) return DEFAULT
        val parsed = mutableListOf<TrayLayoutSlot>()
        val seenIcons = mutableSetOf<TrayLayoutIcon>()
        for (token in raw.split(',')) {
            val part = token.trim()
            if (part.isEmpty()) continue
            if (part.startsWith("spacer:")) {
                val bits = part.split(':')
                if (bits.size < 2) continue
                val id = bits[1].ifBlank { newSpacerId() }
                val width = bits.getOrNull(2)?.toIntOrNull()?.coerceIn(16, 120)
                    ?: DEFAULT_SPACER_WIDTH_DP
                parsed += TrayLayoutSlot.Spacer(id = id, widthDp = width)
            } else {
                val icon = TrayLayoutIcon.fromStorage(part) ?: continue
                if (icon in seenIcons) continue
                seenIcons += icon
                parsed += TrayLayoutSlot.Icon(icon)
            }
        }
        for (icon in TrayLayoutIcon.entries) {
            if (icon !in seenIcons) {
                parsed += TrayLayoutSlot.Icon(icon)
            }
        }
        if (parsed.none { it is TrayLayoutSlot.Icon && it.kind == TrayLayoutIcon.Clock }) {
            parsed += TrayLayoutSlot.Icon(TrayLayoutIcon.Clock)
        }
        return parsed
    }

    /** Visible configure / tray slots for the current icon flags; spacers trimmed to [maxSpacers]. */
    fun visible(slots: List<TrayLayoutSlot>, flags: TrayIconFlags): List<TrayLayoutSlot> =
        trimSpacersToMax(
            slots = slots.filter { slot ->
                when (slot) {
                    is TrayLayoutSlot.Spacer -> true
                    is TrayLayoutSlot.Icon -> slot.kind.isEnabled(flags)
                }
            },
            flags = flags,
        )

    /**
     * Slots that occupy space in the live / preview tray: every icons-tab–enabled icon plus
     * spacers (within [maxSpacers]). Live telemetry does **not** drop a slot — unavailable
     * glyphs stay reserved and are drawn invisible via [isGlyphVisible] so the justified
     * layout stays stable.
     */
    fun liveOccupying(
        slots: List<TrayLayoutSlot>,
        flags: TrayIconFlags,
    ): List<TrayLayoutSlot> = visible(slots, flags)

    /**
     * Whether the glyph for [kind] should paint. When false the slot still occupies layout
     * space (blank reservation) so disconnecting Wi-Fi / clearing mute does not reshuffle
     * neighbors.
     */
    fun isGlyphVisible(
        kind: TrayLayoutIcon,
        wifiConnected: Boolean,
        ringerMuted: Boolean,
        hotspotActive: Boolean,
        bluetoothAudio: BluetoothAudioKind?,
        notificationPackage: String?,
        batteryPresent: Boolean,
    ): Boolean = when (kind) {
        TrayLayoutIcon.Clock,
        TrayLayoutIcon.Network,
        -> true
        TrayLayoutIcon.Wifi -> wifiConnected
        TrayLayoutIcon.Mute -> ringerMuted
        TrayLayoutIcon.Notifications -> !notificationPackage.isNullOrBlank()
        TrayLayoutIcon.Hotspot -> hotspotActive
        TrayLayoutIcon.BluetoothAudio -> bluetoothAudio != null
        TrayLayoutIcon.Battery -> batteryPresent
    }

    /** Index of the rightmost icon slot in [slots], or -1 when none. */
    fun rightmostIconIndex(slots: List<TrayLayoutSlot>): Int =
        slots.indexOfLast { it is TrayLayoutSlot.Icon }

    /**
     * Icons that stagger on expand/collapse — every occupying icon except the persistent
     * rightmost.
     */
    fun animatingIcons(slots: List<TrayLayoutSlot>): List<TrayLayoutIcon> {
        val icons = slots.mapNotNull { (it as? TrayLayoutSlot.Icon)?.kind }
        if (icons.isEmpty()) return emptyList()
        return icons.dropLast(1)
    }

    fun move(slots: List<TrayLayoutSlot>, fromIndex: Int, toIndex: Int): List<TrayLayoutSlot> {
        if (fromIndex == toIndex) return slots
        if (fromIndex !in slots.indices || toIndex !in slots.indices) return slots
        val mutable = slots.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        return mutable
    }

    fun addSpacer(slots: List<TrayLayoutSlot>, flags: TrayIconFlags): List<TrayLayoutSlot> {
        if (!canAddSpacer(slots, flags)) return slots
        return slots + TrayLayoutSlot.Spacer(id = newSpacerId())
    }

    fun removeSpacer(slots: List<TrayLayoutSlot>, spacerId: String): List<TrayLayoutSlot> =
        slots.filterNot { it is TrayLayoutSlot.Spacer && it.id == spacerId }

    fun newSpacerId(): String = UUID.randomUUID().toString().take(8)
}
