package com.metro.settings.data.sounds

import com.metro.system.MetroSoundCategory
import com.metro.system.MetroSoundRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MetroSoundManifestTest {

    private val validJson = """
        {
          "packId": "metro.original.v1",
          "name": "Metro Original Sound Pack",
          "version": 3,
          "roles": {
            "PHONE_RINGTONE": "ringtones/metro_beacon.ogg",
            "ALT_RINGTONE_1": "ringtones/metro_orbit.ogg",
            "MESSAGE": "notifications/metro_message.ogg",
            "MAIL": "notifications/metro_mail.ogg",
            "CALENDAR": "notifications/metro_calendar.ogg",
            "REMINDER": "notifications/metro_reminder.ogg",
            "SYSTEM_NOTIFICATION": "notifications/metro_system.ogg",
            "ALARM": "alarms/metro_dawn.ogg",
            "ALT_ALARM_1": "alarms/metro_rise.ogg",
            "TIMER": "alarms/metro_urgent.ogg",
            "UI_TAP": "ui/metro_tap.ogg"
          }
        }
    """.trimIndent()

    @Test
    fun parsesPackMetadataAndAssets() {
        val parsed = MetroSoundManifest.parse(validJson)
        assertEquals("metro.original.v1", parsed.pack.packId)
        assertEquals(3, parsed.pack.version)
        assertTrue(parsed.warnings.isEmpty())
        assertEquals(11, parsed.assets.size)
    }

    @Test
    fun derivesIdTitleAndCategory() {
        val parsed = MetroSoundManifest.parse(validJson)
        val beacon = parsed.assets.first { it.id == "metro_beacon" }
        assertEquals("Metro Beacon", beacon.title)
        assertEquals(MetroSoundCategory.RINGTONE, beacon.category)
        assertEquals(MetroSoundRole.PHONE_RINGTONE, beacon.role)

        val timer = parsed.assets.first { it.id == "metro_urgent" }
        assertEquals(MetroSoundCategory.ALARM, timer.category)
        assertEquals(MetroSoundRole.TIMER, timer.role)
    }

    @Test
    fun alternateAssetsHaveNoSemanticRole() {
        val parsed = MetroSoundManifest.parse(validJson)
        assertNull(parsed.assets.first { it.id == "metro_orbit" }.role)
        assertNull(parsed.assets.first { it.id == "metro_tap" }.role)
    }

    @Test
    fun detectsAllRequiredRolesPresent() {
        val parsed = MetroSoundManifest.parse(validJson)
        assertTrue(MetroSoundManifest.missingRequiredRoles(parsed).isEmpty())
    }

    @Test
    fun detectsMissingRequiredRole() {
        val parsed = MetroSoundManifest.parse(
            """
            { "version": 1, "roles": { "MESSAGE": "notifications/metro_message.ogg" } }
            """.trimIndent(),
        )
        val missing = MetroSoundManifest.missingRequiredRoles(parsed)
        assertTrue(missing.contains(MetroSoundRole.ALARM))
        assertTrue(missing.contains(MetroSoundRole.PHONE_RINGTONE))
        assertTrue(!missing.contains(MetroSoundRole.MESSAGE))
    }

    @Test
    fun unsupportedRoleAndCategoryFailSafelyWithWarnings() {
        val parsed = MetroSoundManifest.parse(
            """
            {
              "version": 1,
              "roles": {
                "PHONE_RINGTONE": "ringtones/metro_beacon.ogg",
                "BOGUS": "ringtones/metro_orbit.ogg",
                "MESSAGE": "video/metro_message.ogg"
              }
            }
            """.trimIndent(),
        )
        // BOGUS still installs (unknown role → null role); "video/..." has unknown category → skipped.
        assertEquals(2, parsed.assets.size)
        assertTrue(parsed.warnings.any { it.contains("video/metro_message.ogg") })
        assertNull(parsed.assets.first { it.id == "metro_orbit" }.role)
    }

    @Test
    fun duplicateAssetIdDeduplicatesAndWarns() {
        val parsed = MetroSoundManifest.parse(
            """
            {
              "version": 1,
              "roles": {
                "PHONE_RINGTONE": "ringtones/metro_beacon.ogg",
                "MESSAGE": "ringtones/metro_beacon.ogg"
              }
            }
            """.trimIndent(),
        )
        assertEquals(1, parsed.assets.size)
        assertTrue(parsed.warnings.any { it.contains("duplicate") })
    }

    @Test
    fun titleFormatting() {
        assertEquals("Metro Beacon", MetroSoundManifest.titleForId("metro_beacon"))
        assertEquals("Metro Dawn", MetroSoundManifest.titleForId("metro-dawn"))
    }
}
