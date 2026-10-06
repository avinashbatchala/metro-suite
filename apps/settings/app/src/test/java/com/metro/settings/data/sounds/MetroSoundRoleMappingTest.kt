package com.metro.settings.data.sounds

import android.media.RingtoneManager
import com.metro.system.MetroSoundCategory
import com.metro.system.MetroSoundRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MetroSoundRoleMappingTest {

    @Test
    fun systemTypeForOnlySystemRoles() {
        assertEquals(
            RingtoneManager.TYPE_RINGTONE,
            MetroRingtoneDefaults.systemTypeFor(MetroSoundRole.PHONE_RINGTONE),
        )
        assertEquals(
            RingtoneManager.TYPE_ALARM,
            MetroRingtoneDefaults.systemTypeFor(MetroSoundRole.ALARM),
        )
        assertEquals(
            RingtoneManager.TYPE_NOTIFICATION,
            MetroRingtoneDefaults.systemTypeFor(MetroSoundRole.SYSTEM_NOTIFICATION),
        )
        assertNull(MetroRingtoneDefaults.systemTypeFor(MetroSoundRole.MESSAGE))
        assertNull(MetroRingtoneDefaults.systemTypeFor(MetroSoundRole.TIMER))
    }

    @Test
    fun categoryForRole() {
        assertEquals(MetroSoundCategory.RINGTONE, SettingsSoundRegistry.categoryForRole(MetroSoundRole.PHONE_RINGTONE))
        assertEquals(MetroSoundCategory.NOTIFICATION, SettingsSoundRegistry.categoryForRole(MetroSoundRole.MESSAGE))
        assertEquals(MetroSoundCategory.NOTIFICATION, SettingsSoundRegistry.categoryForRole(MetroSoundRole.MAIL))
        assertEquals(MetroSoundCategory.ALARM, SettingsSoundRegistry.categoryForRole(MetroSoundRole.ALARM))
        assertEquals(MetroSoundCategory.ALARM, SettingsSoundRegistry.categoryForRole(MetroSoundRole.TIMER))
    }

    @Test
    fun relativePaths() {
        assertEquals("Ringtones/Metro/", SettingsSoundRegistry.relativePathFor(MetroSoundCategory.RINGTONE))
        assertEquals("Notifications/Metro/", SettingsSoundRegistry.relativePathFor(MetroSoundCategory.NOTIFICATION))
        assertEquals("Alarms/Metro/", SettingsSoundRegistry.relativePathFor(MetroSoundCategory.ALARM))
    }

    @Test
    fun displayNameUsesUserTitle() {
        val asset = MetroSoundAsset(
            id = "metro_beacon",
            title = "Metro Beacon",
            assetPath = "ringtones/metro_beacon.ogg",
            category = MetroSoundCategory.RINGTONE,
            role = MetroSoundRole.PHONE_RINGTONE,
        )
        assertEquals("Metro Beacon.ogg", SettingsSoundRegistry.displayNameFor(asset))
        assertEquals("audio/ogg", SettingsSoundRegistry.mimeFor(asset))
    }
}
