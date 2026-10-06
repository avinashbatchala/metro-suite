package com.metro.dialer.telecom

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telecom.TelecomManager

/**
 * Default-dialer role acquisition.
 *
 * Metro deliberately does **not** register its own cellular [android.telecom.PhoneAccount] or
 * [android.telecom.ConnectionService]: Android Telecom owns real cellular calling. The only setup
 * Metro needs for cellular is the [RoleManager.ROLE_DIALER] role plus an [android.telecom.InCallService].
 */
object MetroTelecomSetup {

    fun holdsDialerRole(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java) ?: return false
            return roleManager.isRoleAvailable(RoleManager.ROLE_DIALER) &&
                roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
        }
        return MetroTelecomBridge.isDefaultDialer(context)
    }

    fun createDefaultDialerRequestIntent(context: Context): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java) ?: return null
            if (!roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) return null
            if (roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) return null
            roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
        } else {
            val telecomManager = context.getSystemService(TelecomManager::class.java) ?: return null
            if (telecomManager.defaultDialerPackage == context.packageName) return null
            Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
            }
        }
    }
}
