package com.example.layoutmanager.store

import com.example.layoutmanager.model.CURRENT_VERSION
import com.example.layoutmanager.model.Profile
import com.example.layoutmanager.model.ProfileFile
import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

/**
 * Verwaltet die Profile in einer JSON-Datei. Ist die Datei nicht lesbar, bleibt sie unangetastet:
 * [loadError] ist gesetzt, [profiles] leer und jede Schreiboperation wirft [IllegalStateException].
 * Namen sind eindeutig ohne Beachtung der Groß-/Kleinschreibung und werden getrimmt.
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
        require(i >= 0) { "Profil nicht gefunden: $oldName" }
        require(indexOf(name).let { it < 0 || it == i }) { "Profil existiert bereits: $name" }
        toMutableList().also { it[i] = it[i].copy(name = name) }
    }

    fun delete(name: String) = change {
        val i = indexOf(name)
        require(i >= 0) { "Profil nicht gefunden: $name" }
        toMutableList().also { it.removeAt(i) }
    }

    private fun List<Profile>.indexOf(name: String) = indexOfFirst { it.name.equals(name.trim(), ignoreCase = true) }

    private fun validName(name: String) = name.trim().also { require(it.isNotEmpty()) { "Name darf nicht leer sein" } }

    /** Berechnet die neue Liste, schreibt sie, übernimmt sie erst danach (bei Fehler bleibt der alte Stand). */
    private fun change(transform: List<Profile>.() -> List<Profile>) {
        check(loadError == null) { "Profile nicht lesbar: $loadError" }
        val next = list.transform()
        write(next)
        list = next
    }

    private fun load(): List<Profile> {
        if (!Files.exists(file)) return emptyList()
        return try {
            ProfileJson.fromJson(Files.readString(file)).profiles
        } catch (e: IllegalArgumentException) {
            loadError = e.message
            emptyList()
        } catch (e: IOException) {
            loadError = e.message
            emptyList()
        }
    }

    private fun write(profiles: List<Profile>) {
        Files.createDirectories(file.parent)
        val tmp = file.resolveSibling(file.fileName.toString() + ".tmp")
        Files.writeString(tmp, ProfileJson.toJson(ProfileFile(CURRENT_VERSION, profiles)))
        try {
            Files.move(tmp, file, REPLACE_EXISTING, ATOMIC_MOVE)
        } catch (e: AtomicMoveNotSupportedException) {
            Files.move(tmp, file, REPLACE_EXISTING)
        }
    }
}

object ProfileJson {
    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun toJson(f: ProfileFile): String = gson.toJson(f)

    /** Wirft [IllegalArgumentException] bei leerem Text, ungültigem JSON, fehlender Profilliste oder neuerer Version. */
    fun fromJson(s: String): ProfileFile {
        require(s.isNotBlank()) { "Datei ist leer" }
        val file = try {
            gson.fromJson(s, ProfileFile::class.java)
        } catch (e: JsonParseException) {
            throw IllegalArgumentException("Ungültiges JSON: ${e.message}", e)
        }
        // Gson setzt fehlende Felder trotz Kotlin-Typ auf null
        @Suppress("SENSELESS_COMPARISON")
        require(file != null && file.profiles != null) { "Profilliste fehlt" }
        require(file.version <= CURRENT_VERSION) { "Dateiversion ${file.version} ist neuer als unterstützt ($CURRENT_VERSION)" }
        return file
    }
}
