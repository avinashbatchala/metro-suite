package com.metro.training.domain.equipment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EquipmentMathTest {

    private val plates = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)

    @Test
    fun plateMath_solvesPerSide() {
        assertEquals(listOf(25.0, 15.0), PlateMath.platesPerSide(100.0, 20.0, plates))
        assertEquals(listOf(20.0), PlateMath.platesPerSide(60.0, 20.0, plates))
        assertEquals(listOf(25.0, 15.0, 1.25), PlateMath.platesPerSide(102.5, 20.0, plates))
    }

    @Test
    fun plateMath_nullWhenNotAchievable() {
        assertNull(PlateMath.platesPerSide(61.0, 20.0, plates))
        assertNull(PlateMath.platesPerSide(10.0, 20.0, plates))
    }

    @Test
    fun warmup_rampsToWorkingWeight() {
        val sets = WarmupCalculator.suggest(100.0, 20.0, 2.5)
        assertEquals(listOf(40.0, 60.0, 80.0, 90.0), sets.map { it.loadKg })
        assertEquals(listOf(5, 3, 2, 1), sets.map { it.reps })
    }

    @Test
    fun warmup_emptyWhenLoadAtOrBelowBar() {
        assertEquals(emptyList<WarmupSet>(), WarmupCalculator.suggest(20.0, 20.0, 2.5))
    }
}
