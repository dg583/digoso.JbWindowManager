package com.digoso.jbwindowmanager.store

import com.digoso.jbwindowmanager.model.Bounds
import com.digoso.jbwindowmanager.model.Profile
import com.digoso.jbwindowmanager.model.ProfileFile
import com.digoso.jbwindowmanager.model.ToolWindowState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files

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

    // --- ProfileStore ---

    @get:Rule
    val tmp = TemporaryFolder()

    private fun file() = tmp.root.toPath().resolve("sub").resolve("profiles.json")
    private fun profile(name: String, id: String = "Explorer") =
        Profile(name, listOf(ToolWindowState(id, "left", false, "DOCKED", false, true, 0, 0.25f, null)))

    @Test
    fun missingFileGivesEmptyList() {
        val store = ProfileStore(file())
        assertTrue(store.profiles().isEmpty())
        assertNull(store.loadError)
    }

    @Test
    fun saveCreatesFileAndReadsBack() {
        ProfileStore(file()).save(profile("A"))
        assertEquals(listOf(profile("A")), ProfileStore(file()).profiles())
    }

    @Test
    fun saveSameNameReplaces() {
        val store = ProfileStore(file())
        store.save(profile("Debug", "Old"))
        store.save(profile("debug", "New"))
        assertEquals(listOf(profile("debug", "New")), store.profiles())
    }

    @Test
    fun blankNameIsRejected() {
        val store = ProfileStore(file())
        assertThrows(IllegalArgumentException::class.java) { store.save(profile("  ")) }
        assertTrue(store.profiles().isEmpty())
    }

    @Test
    fun renameToExistingNameFails() {
        val store = ProfileStore(file())
        store.save(profile("A", "x"))
        store.save(profile("B", "y"))
        assertThrows(IllegalArgumentException::class.java) { store.rename("A", "b") }
        assertEquals(listOf(profile("A", "x"), profile("B", "y")), store.profiles())
    }

    @Test
    fun renameAndDeleteWork() {
        val store = ProfileStore(file())
        store.save(profile("A", "x"))
        store.save(profile("B", "y"))
        store.rename("A", " C ")
        assertEquals(listOf("C", "B"), store.profiles().map { it.name })
        store.delete("b")
        assertEquals(listOf("C"), ProfileStore(file()).profiles().map { it.name })
    }

    @Test
    fun corruptFileIsNotOverwritten() {
        val f = file()
        Files.createDirectories(f.parent)
        Files.writeString(f, "{kaputt")
        val store = ProfileStore(f)
        assertNotNull(store.loadError)
        assertTrue(store.profiles().isEmpty())
        assertThrows(IllegalStateException::class.java) { store.save(profile("A")) }
        assertEquals("{kaputt", Files.readString(f))
    }

    @Test
    fun newerVersionFileIsNotOverwritten() {
        val f = file()
        Files.createDirectories(f.parent)
        val text = """{"version":2,"profiles":[]}"""
        Files.writeString(f, text)
        val store = ProfileStore(f)
        assertNotNull(store.loadError)
        assertThrows(IllegalStateException::class.java) { store.delete("A") }
        assertEquals(text, Files.readString(f))
    }

    // --- Fixes aus dem Review ---

    @Test
    fun loadErrorNamesTheFile() {
        val f = file()
        Files.createDirectories(f.parent)
        Files.writeString(f, "{kaputt")
        assertTrue(ProfileStore(f).loadError!!.contains("profiles.json"))
    }

    @Test
    fun profileWithoutToolWindowsIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { ProfileJson.fromJson("""{"version":1,"profiles":[{"name":"A"}]}""") }
    }

    @Test
    fun nullProfileIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { ProfileJson.fromJson("""{"version":1,"profiles":[null]}""") }
    }

    @Test
    fun toolWindowWithoutAnchorIsRejected() {
        val json = """{"version":1,"profiles":[{"name":"A","toolWindows":[{"id":"X","type":"DOCKED"}]}]}"""
        assertThrows(IllegalArgumentException::class.java) { ProfileJson.fromJson(json) }
    }

    @Test
    fun writeFailureBecomesIllegalStateAndKeepsOldState() {
        val blocker = tmp.newFile("sub") // regular file where the directory would have to go
        val store = ProfileStore(blocker.toPath().resolve("profiles.json"))
        assertThrows(IllegalStateException::class.java) { store.save(profile("A")) }
        assertTrue(store.profiles().isEmpty())
    }
}
