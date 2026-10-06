package com.metro.training.domain.workout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SetTypeTest {

    @Test
    fun onlyWorkFeedsProgression() {
        assertTrue(SetType.WORK.feedsProgression)
        assertFalse(SetType.WARMUP.feedsProgression)
        assertFalse(SetType.FAILURE.feedsProgression)
        assertFalse(SetType.DROP.feedsProgression)
        assertFalse(SetType.BACKOFF.feedsProgression)
        assertFalse(SetType.MYOREP.feedsProgression)
    }

    @Test
    fun dropAndMyoRepSuppressRest() {
        assertTrue(SetType.DROP.suppressesRest)
        assertTrue(SetType.MYOREP.suppressesRest)
        assertFalse(SetType.WORK.suppressesRest)
        assertFalse(SetType.WARMUP.suppressesRest)
        assertFalse(SetType.FAILURE.suppressesRest)
    }

    @Test
    fun perSideSetIsDetected() {
        val single = set(repsLeft = null, repsRight = null)
        val perSide = set(repsLeft = 8, repsRight = 8)
        assertFalse(single.isPerSide)
        assertTrue(perSide.isPerSide)
        assertEquals(SetType.WORK, perSide.setType)
    }

    private fun set(repsLeft: Int?, repsRight: Int?) = WorkoutSet(
        id = "s",
        exerciseSessionId = "e",
        setIndex = 1,
        setType = SetType.WORK,
        load = 80.0,
        repsCompleted = 8,
        rir = 2,
        quality = SetQuality.NORMAL,
        completed = true,
        repsLeft = repsLeft,
        repsRight = repsRight,
    )
}
