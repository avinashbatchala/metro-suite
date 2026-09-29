package com.metro.statusbar

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Detects connected Bluetooth audio and, when possible, whether it is a headset/headphones
 * or a speaker/loudspeaker for the tray glyph.
 */
object BluetoothAudioSource {
    fun current(context: Context): BluetoothAudioKind? {
        val app = context.applicationContext
        if (!hasBluetoothAudioOutput(app)) return null
        return classifyConnectedDevice(app) ?: BluetoothAudioKind.Headset
    }

    private fun hasBluetoothAudioOutput(context: Context): Boolean {
        val audio = context.getSystemService(AudioManager::class.java) ?: return false
        val devices = audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        return devices.any { device ->
            device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    device.type == AudioDeviceInfo.TYPE_BLE_HEADSET)
        }
    }

    private fun classifyConnectedDevice(context: Context): BluetoothAudioKind? {
        if (!hasConnectPermission(context)) return null
        val adapter = bluetoothAdapter(context) ?: return null
        if (!adapter.isEnabled) return null
        val a2dpConnected = runCatching {
            adapter.getProfileConnectionState(BluetoothProfile.A2DP) ==
                BluetoothAdapter.STATE_CONNECTED
        }.getOrDefault(false)
        val headsetConnected = runCatching {
            adapter.getProfileConnectionState(BluetoothProfile.HEADSET) ==
                BluetoothAdapter.STATE_CONNECTED
        }.getOrDefault(false)
        if (!a2dpConnected && !headsetConnected) return null

        return runCatching {
            var sawHeadset = false
            var sawSpeaker = false
            for (device in adapter.bondedDevices.orEmpty()) {
                val deviceClass = device.bluetoothClass?.deviceClass ?: continue
                when (deviceClass) {
                    BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES,
                    BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET,
                    BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE,
                    -> sawHeadset = true
                    BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER,
                    BluetoothClass.Device.AUDIO_VIDEO_HIFI_AUDIO,
                    BluetoothClass.Device.AUDIO_VIDEO_PORTABLE_AUDIO,
                    BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO,
                    -> sawSpeaker = true
                }
            }
            when {
                sawSpeaker && !sawHeadset -> BluetoothAudioKind.Speaker
                sawHeadset || headsetConnected -> BluetoothAudioKind.Headset
                else -> null
            }
        }.getOrNull()
    }

    private fun bluetoothAdapter(context: Context): BluetoothAdapter? {
        val manager = context.getSystemService(BluetoothManager::class.java)
        return manager?.adapter
    }

    fun hasConnectPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT,
        ) == PackageManager.PERMISSION_GRANTED
    }
}
