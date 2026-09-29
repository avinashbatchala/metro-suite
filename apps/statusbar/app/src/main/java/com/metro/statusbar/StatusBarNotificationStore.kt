package com.metro.statusbar

import android.service.notification.StatusBarNotification
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Active notification packages for the tray notification-app glyph. Fed by
 * [StatusBarNotificationListenerService]; cycles one package at a time in [TrayState].
 */
object StatusBarNotificationStore {
    private val listeners = CopyOnWriteArrayList<() -> Unit>()

    @Volatile
    private var packages: List<String> = emptyList()

    fun packages(): List<String> = packages

    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    fun clear() {
        packages = emptyList()
        notifyListeners()
    }

    fun replaceAll(active: Array<StatusBarNotification>?) {
        packages = aggregate(active)
        notifyListeners()
    }

    internal fun aggregate(active: Array<StatusBarNotification>?): List<String> {
        if (active.isNullOrEmpty()) return emptyList()
        val ordered = LinkedHashSet<String>()
        for (sbn in active) {
            if (sbn.isOngoing) continue
            if (sbn.packageName in SHELL_PACKAGES) continue
            if (sbn.notification?.extras?.getBoolean("android.ongoing", false) == true) continue
            ordered.add(sbn.packageName)
        }
        return ordered.toList()
    }

    private fun notifyListeners() {
        for (listener in listeners) {
            runCatching { listener.invoke() }
        }
    }

    /** Shell packages whose FGS / system notifications should not drive the tray glyph. */
    private val SHELL_PACKAGES = setOf(
        "com.metro.statusbar",
        "com.metro.launcher",
        "com.metro.navbar",
        "com.metro.notifications",
        "com.metro.volume",
        "com.metro.lockscreen",
        "android",
        "com.android.systemui",
    )
}
