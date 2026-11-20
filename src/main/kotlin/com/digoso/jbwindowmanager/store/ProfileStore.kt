package com.digoso.jbwindowmanager.store

import com.digoso.jbwindowmanager.model.CURRENT_VERSION
import com.digoso.jbwindowmanager.model.Profile
import com.digoso.jbwindowmanager.model.ProfileFile
import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import com.intellij.openapi.diagnostic.Logger
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

private val LOG = Logger.getInstance(ProfileStore::class.java)

/**
 * Manages the profiles in a JSON file. If the file is unreadable it is left untouched:
 * [loadError] is set, [profiles] is empty and every write throws [IllegalStateException].
 * Names are unique ignoring case and are trimmed.
 */
class ProfileStore(private val file: Path) {
    var loadError: String? = null
        private set
    private var list: List<Profile> = load()

    fun profiles(): List<Profile> = list

    fun save(profile: Profile) = change {
        val name = validName(profile.name)
        val i = indexOf(name)
        val p = profile.copy(name = name)
        if (i < 0) this + p else toMutableList().also { it[i] = p }
    }

    fun rename(oldName: String, newName: String) = change {
        val name = validName(newName)
        val i = indexOf(oldName)
        require(i >= 0) { "Profile not found: $oldName" }
        require(indexOf(name).let { it < 0 || it == i }) { "Profile already exists: $name" }
        toMutableList().also { it[i] = it[i].copy(name = name) }
    }

    fun delete(name: String) = change {
        val i = indexOf(name)
        require(i >= 0) { "Profile not found: $name" }
        toMutableList().also { it.removeAt(i) }
    }

    private fun List<Profile>.indexOf(name: String) = indexOfFirst { it.name.equals(name.trim(), ignoreCase = true) }

    private fun validName(name: String) = name.trim().also { require(it.isNotEmpty()) { "Name must not be empty" } }

    /** Computes the new list, writes it, and only then adopts it (on error the old state stays). */
    private fun change(transform: List<Profile>.() -> List<Profile>) {
        check(loadError == null) { "Profiles unreadable: $loadError" }
        val next = list.transform()
        write(next)
        list = next
    }

    private fun load(): List<Profile> {
        if (!Files.exists(file)) return emptyList()
        return try {
            ProfileJson.fromJson(Files.readString(file)).profiles
        } catch (e: IllegalArgumentException) {
            loadFailed(e)
        } catch (e: IOException) {
            loadFailed(e)
        }
    }

    private fun loadFailed(e: Exception): List<Profile> {
        loadError = "${file.fileName}: ${e.message}"
        LOG.warn("Profiles unreadable, file left untouched: $file", e)
        return emptyList()
    }

    /** IO errors become [IllegalStateException] so callers can show them as a dialog. */
    private fun write(profiles: List<Profile>) {
        try {
            Files.createDirectories(file.parent)
            val tmp = file.resolveSibling(file.fileName.toString() + ".tmp")
            Files.writeString(tmp, ProfileJson.toJson(ProfileFile(CURRENT_VERSION, profiles)))
            try {
                Files.move(tmp, file, REPLACE_EXISTING, ATOMIC_MOVE)
            } catch (e: AtomicMoveNotSupportedException) {
                Files.move(tmp, file, REPLACE_EXISTING)
            }
        } catch (e: IOException) {
            throw IllegalStateException("Saving failed: ${e.message}", e)
        }
    }
}

object ProfileJson {
    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun toJson(f: ProfileFile): String = gson.toJson(f)

    /** Throws [IllegalArgumentException] for empty text, invalid JSON, a missing profile list or a newer version. */
    fun fromJson(s: String): ProfileFile {
        require(s.isNotBlank()) { "File is empty" }
        val file = try {
            gson.fromJson(s, ProfileFile::class.java)
        } catch (e: JsonParseException) {
            throw IllegalArgumentException("Invalid JSON: ${e.message}", e)
        }
        // Gson sets missing fields to null despite the Kotlin type (and "null" list elements stay null)
        @Suppress("SENSELESS_COMPARISON", "USELESS_IS_CHECK")
        require(file != null && file.profiles != null) { "Profile list missing" }
        @Suppress("SENSELESS_COMPARISON")
        require(
            file.profiles.all { p ->
                p != null && p.name != null && p.toolWindows != null &&
                    p.toolWindows.all { s -> s != null && s.id != null && s.anchor != null && s.type != null }
            },
        ) { "Profile incomplete (name, toolWindows, id, anchor or type missing)" }
        require(file.version <= CURRENT_VERSION) { "File version ${file.version} is newer than supported ($CURRENT_VERSION)" }
        return file
    }
}
