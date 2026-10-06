package com.metro.settings.data.sounds

import androidx.test.core.app.ApplicationProvider
import com.metro.system.MetroSoundRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MetroSoundStoreTest {

    private fun store() = MetroSoundStore(ApplicationProvider.getApplicationContext())

    @Test
    fun installedUrisRoundTrip() {
        val store = store()
        assertNull(store.installedUri("metro_beacon"))
        store.setInstalledUri("metro_beacon", "content://media/audio/1")
        assertEquals("content://media/audio/1", store.installedUri("metro_beacon"))
        assertTrue(store.installedIds().contains("metro_beacon"))
        store.removeInstalledUri("metro_beacon")
        assertNull(store.installedUri("metro_beacon"))
    }

    @Test
    fun roleSelectionPersistsSeparatelyFromUri() {
        val store = store()
        store.setRoleSelection(MetroSoundRole.MESSAGE, "metro_message")
        // No URI installed yet — selection still persists (URI can be repaired later).
        assertEquals("metro_message", store.roleSelection(MetroSoundRole.MESSAGE))
        assertEquals("metro_message", store.roleSelections()[MetroSoundRole.MESSAGE])
        store.setRoleSelection(MetroSoundRole.MESSAGE, null)
        assertNull(store.roleSelection(MetroSoundRole.MESSAGE))
    }

    @Test
    fun packVersionAndVibrate() {
        val store = store()
        assertNull(store.installedPackVersion())
        store.setInstalledPackVersion(1)
        assertEquals(1, store.installedPackVersion())
        assertTrue(store.vibrate)
        store.vibrate = false
        assertFalse(store.vibrate)
    }

    @Test
    fun priorDefaultRememberedOnce() {
        val store = store()
        assertNull(store.priorDefault(1))
        store.rememberPriorDefault(1, "content://old")
        store.rememberPriorDefault(1, "content://new") // ignored — keep first backup
        assertEquals("content://old", store.priorDefault(1))
        store.clearPriorDefault(1)
        assertNull(store.priorDefault(1))
    }
}
