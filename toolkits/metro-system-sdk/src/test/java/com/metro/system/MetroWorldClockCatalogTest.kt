package com.metro.system

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class MetroWorldClockCatalogTest {

    @Test
    fun allZonesAreValidIanaIds() {
        MetroWorldClockCatalog.cities.forEach { city ->
            runCatching { ZoneId.of(city.zoneId) }
                .onFailure { fail("Invalid zoneId for ${city.id}: ${city.zoneId}") }
        }
    }

    @Test
    fun idsAndNamesAreStableAndUnique() {
        val ids = MetroWorldClockCatalog.cities.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(MetroWorldClockCatalog.cities.all { it.name.isNotBlank() && it.region.isNotBlank() })
    }

    @Test
    fun resolveKeepsOrderAndSkipsUnknown() {
        val resolved = MetroWorldClockCatalog.resolve(listOf("tokyo", "nope", "london"))
        assertEquals(listOf("tokyo", "london"), resolved.map { it.id })
    }

    @Test
    fun defaultIdsResolve() {
        val defaults = MetroWorldClockCatalog.resolve(MetroWorldClockCatalog.DEFAULT_IDS)
        assertEquals(3, defaults.size)
    }

    @Test
    fun searchMatchesCityCountryZoneAndAlias() {
        assertTrue(MetroWorldClockCatalog.search("tokyo").any { it.id == "tokyo" })
        assertTrue(MetroWorldClockCatalog.search("japan").any { it.id == "tokyo" })
        assertTrue(MetroWorldClockCatalog.search("asia/").isNotEmpty())
        assertTrue(MetroWorldClockCatalog.search("nyc").any { it.id == "new-york" })
    }

    @Test
    fun byIdReturnsNullForUnknown() {
        assertNotNull(MetroWorldClockCatalog.byId("london"))
        assertTrue(MetroWorldClockCatalog.byId("atlantis") == null)
    }

    private fun fail(message: String): Nothing = throw AssertionError(message)
}
