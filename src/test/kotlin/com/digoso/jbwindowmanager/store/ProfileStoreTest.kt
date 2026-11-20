package com.digoso.jbwindowmanager.store

import com.digoso.jbwindowmanager.model.Bounds
import com.digoso.jbwindowmanager.model.CURRENT_VERSION
import com.digoso.jbwindowmanager.model.FrameState
import com.digoso.jbwindowmanager.model.Profile
import com.digoso.jbwindowmanager.model.ProfileFile
import com.digoso.jbwindowmanager.model.Rel
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
        assertThrows(IllegalArgumentException::class.java) { ProfileJson.fromJson("""{"version":3,"profiles":[]}""") }
    }

    @Test
    fun roundtripKeepsFrameAndRel() {
        val f = ProfileFile(
            CURRENT_VERSION,
            listOf(
                Profile(
                    "P",
                    listOf(ToolWindowState("Database", "right", false, "WINDOWED", false, true, 3, 0f, null, Rel(0.49, 0.56, 0.51, 0.41))),
                    FrameState(false, true, Rel(0.1, 0.2, 0.5, 0.6)),
                ),
            ),
        )
        assertEquals(f, ProfileJson.fromJson(ProfileJson.toJson(f)))
    }

    private val version1 = """
        {"version":1,"profiles":[{"name":"Old","toolWindows":[
          {"id":"Commit","anchor":"left","split":true,"type":"FLOATING","autoHide":false,"visible":false,"order":1,"weight":0.3,
           "bounds":{"x":10,"y":20,"width":300,"height":400}}]}]}
    """.trimIndent()

    @Test
    fun version1FileIsReadWithoutFrame() {
        val p = ProfileJson.fromJson(version1).profiles.single()
        assertNull(p.frame)
        assertNull(p.toolWindows[0].rel)
        assertEquals(Bounds(10, 20, 300, 400), p.toolWindows[0].bounds)
    }

    @Test
    fun saveUpgradesVersion1ToCurrent() {
        val path = file()
        Files.createDirectories(path.parent)
        Files.writeString(path, version1)
        ProfileStore(path).save(profile("New"))
        assertEquals(CURRENT_VERSION, ProfileJson.fromJson(Files.readString(path)).version)
    }

    private fun windowJson(extra: String) =
        """{"version":2,"profiles":[{"name":"P","toolWindows":[{"id":"X","anchor":"left","split":false,"type":"WINDOWED","autoHide":false,"visible":true,"order":0,"weight":0.0,$extra}]}]}"""

    @Test
    fun relWithMissingValuesIsRejected() {
        // Gson would turn the missing w/h into 0.0 and the window would be set to a 0x0 rect
        assertThrows(IllegalArgumentException::class.java) { ProfileJson.fromJson(windowJson(""""rel":{"x":0.1,"y":0.2}""")) }
    }

    @Test
    fun relWithNonPositiveSizeIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { ProfileJson.fromJson(windowJson(""""rel":{"x":0.1,"y":0.2,"w":0.0,"h":0.5}""")) }
    }

    @Test
    fun frameRelWithNonPositiveSizeIsRejected() {
        val json = """{"version":2,"profiles":[{"name":"P","toolWindows":[],"frame":{"fullscreen":false,"maximized":false,"rel":{"x":0.0,"y":0.0,"w":0.5,"h":0.0}}}]}"""
        assertThrows(IllegalArgumentException::class.java) { ProfileJson.fromJson(json) }
    }

    @Test
    fun nonFiniteRelOrWeightIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { ProfileJson.fromJson(windowJson(""""rel":{"x":NaN,"y":0.2,"w":0.5,"h":0.5}""")) }
        assertThrows(IllegalArgumentException::class.java) { ProfileJson.fromJson(windowJson(""""weight":NaN""").replace(""""weight":0.0,""", "")) }
    }

    @Test
    fun validRelIsAccepted() {
        val rel = ProfileJson.fromJson(windowJson(""""rel":{"x":-0.1,"y":0.2,"w":0.5,"h":0.5}""")).profiles.single().toolWindows.single().rel
        assertEquals(Rel(-0.1, 0.2, 0.5, 0.5), rel)
    }

    @Test
    fun frameWithoutFlagsIsAccepted() {
        val json = """{"version":2,"profiles":[{"name":"P","toolWindows":[],"frame":{"rel":null}}]}"""
        val frame = ProfileJson.fromJson(json).profiles.single().frame
        assertEquals(FrameState(false, false, null), frame)
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
        val text = """{"version":3,"profiles":[]}"""
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
