package com.metro.statusbar

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Feeds [StatusBarNotificationStore] so the tray can cycle active notification app icons
 * when the icons-tab toggle is on.
 */
class StatusBarNotificationListenerService : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        publishAll()
    }

    override fun onListenerDisconnected() {
        StatusBarNotificationStore.clear()
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        publishAll()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        publishAll()
    }

    private fun publishAll() {
        val active = runCatching { activeNotifications }.getOrNull()
        StatusBarNotificationStore.replaceAll(active)
    }
}
