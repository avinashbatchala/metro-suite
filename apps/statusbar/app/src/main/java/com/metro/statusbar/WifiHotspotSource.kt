package com.metro.statusbar

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build

/** Soft-AP / Wi-Fi hotspot on/off for the tray hotspot glyph. */
object WifiHotspotSource {
    fun isActive(context: Context): Boolean {
        val wifi = context.applicationContext.getSystemService(WifiManager::class.java)
        if (wifi != null) {
            val reflected = try {
                val method = wifi.javaClass.getMethod("isWifiApEnabled")
                method.invoke(wifi) as? Boolean
            } catch (_: Throwable) {
                null
            }
            if (reflected != null) return reflected
        }
        return tetheredIfacesActive(context)
    }

    private fun tetheredIfacesActive(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) return false
        val cm = context.applicationContext.getSystemService(ConnectivityManager::class.java)
            ?: return false
        return try {
            val method = cm.javaClass.getDeclaredMethod("getTetheredIfaces")
            val ifaces = method.invoke(cm) as? Array<*>
            ifaces != null && ifaces.isNotEmpty()
        } catch (_: Throwable) {
            false
        }
    }
}
