package com.example.layoutmanager.store

import com.example.layoutmanager.model.Bounds
import com.example.layoutmanager.model.Profile
import com.example.layoutmanager.model.ProfileFile
import com.example.layoutmanager.model.ToolWindowState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ProfileStoreTest {
    private val sample = ProfileFile(
        1,
        listOf(
            Profile(
                "Debugging",
                listOf(
                    ToolWindowState("Explorer", "left", false, "DOCKED", false, true, 0, 0.25f, null),
                    ToolWindowState("Commit", "left", true, "FLOATING", false, false, 1, 0.3f, Bounds(10, 20, 300, 400)),
                ),
            ),
        ),
    )

    @Test
    fun roundtripKeepsAllFields() {
        assertEquals(sample, ProfileJson.fromJson(ProfileJson.toJson(sample)))
    }

    @Test
    fun emptyTextIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { ProfileJson.fromJson("  ") }
    }

    @Test
    fun invalidJsonIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { ProfileJson.fromJson("{kaputt") }
    }

    @Test
    fun newerVersionIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { ProfileJson.fromJson("""{"version":2,"profiles":[]}""") }
    }
}
