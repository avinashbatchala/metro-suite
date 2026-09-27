package com.metro.music

import com.metro.music.data.MusicDirectoryLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicDirectoryLogicTest {

    @Test
    fun directoryId_prefersRelativePath() {
        assertEquals(
            "Music/Albums",
            MusicDirectoryLogic.directoryId("Music/Albums/", "/storage/emulated/0/Music/Albums/a.mp3"),
        )
    }

    @Test
    fun directoryId_fallsBackToParentOfAbsolute() {
        assertEquals(
            "/storage/emulated/0/Music",
            MusicDirectoryLogic.directoryId(null, "/storage/emulated/0/Music/track.mp3"),
        )
    }

    @Test
    fun directoryId_nullWhenMissing() {
        assertNull(MusicDirectoryLogic.directoryId(null, null))
        assertNull(MusicDirectoryLogic.directoryId("  ", ""))
    }

    @Test
    fun displayTitle_relativeAndAbsolute() {
        assertEquals("Music/Albums", MusicDirectoryLogic.displayTitle("Music/Albums/"))
        assertEquals("0/Music", MusicDirectoryLogic.displayTitle("/storage/emulated/0/Music"))
        assertEquals("Music", MusicDirectoryLogic.displayTitle("/Music"))
    }

    @Test
    fun aggregate_countsAndSorts() {
        val dirs = MusicDirectoryLogic.aggregate(
            listOf("Music", "Download", "Music", "Podcasts", "Download"),
        )
        assertEquals(listOf("Download", "Music", "Podcasts"), dirs.map { it.id })
        assertEquals(2, dirs.first { it.id == "Download" }.songCount)
        assertEquals(2, dirs.first { it.id == "Music" }.songCount)
        assertEquals(1, dirs.first { it.id == "Podcasts" }.songCount)
    }

    @Test
    fun filterExcluded_defaultsIncludeUnknown() {
        assertTrue(MusicDirectoryLogic.filterExcluded(null, setOf("Music")))
        assertTrue(MusicDirectoryLogic.filterExcluded("Download", setOf("Music")))
        assertFalse(MusicDirectoryLogic.filterExcluded("Music", setOf("Music")))
    }
}
