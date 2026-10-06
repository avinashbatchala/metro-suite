package com.metro.widgets.data

import org.junit.Assert.assertEquals
import org.junit.Test

class WorldClockSelectionLogicTest {

    @Test
    fun addsUpToThree() {
        var ids = emptyList<String>()
        ids = WorldClockSelectionLogic.toggle(ids, "london")
        ids = WorldClockSelectionLogic.toggle(ids, "new-york")
        ids = WorldClockSelectionLogic.toggle(ids, "tokyo")
        assertEquals(listOf("london", "new-york", "tokyo"), ids)
    }

    @Test
    fun ignoresFourthCity() {
        val ids = listOf("london", "new-york", "tokyo")
        assertEquals(ids, WorldClockSelectionLogic.toggle(ids, "berlin"))
        assertEquals(true, WorldClockSelectionLogic.isFull(ids))
    }

    @Test
    fun removesSelectedCity() {
        val ids = listOf("london", "new-york", "tokyo")
        assertEquals(listOf("london", "tokyo"), WorldClockSelectionLogic.toggle(ids, "new-york"))
    }

    @Test
    fun deduplicatesAndDropsInvalid() {
        val ids = listOf("london", "london", "atlantis", "tokyo")
        assertEquals(listOf("london", "tokyo"), WorldClockSelectionLogic.sanitize(ids))
        assertEquals(listOf("london", "tokyo"), WorldClockSelectionLogic.toggle(ids, "atlantis"))
    }

    @Test
    fun emptyFallsBackToDefaults() {
        val defaults = WorldClockSelectionLogic.withDefaults(emptyList())
        assertEquals(3, defaults.size)
    }

    @Test
    fun oneTwoThreeSelections() {
        assertEquals(listOf("london"), WorldClockSelectionLogic.sanitize(listOf("london")))
        assertEquals(
            listOf("london", "tokyo"),
            WorldClockSelectionLogic.sanitize(listOf("london", "tokyo")),
        )
        assertEquals(
            listOf("london", "new-york", "tokyo"),
            WorldClockSelectionLogic.sanitize(listOf("london", "new-york", "tokyo")),
        )
    }
}
