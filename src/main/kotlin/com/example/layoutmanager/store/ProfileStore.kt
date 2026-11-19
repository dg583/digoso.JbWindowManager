package com.example.layoutmanager.store

import com.example.layoutmanager.model.CURRENT_VERSION
import com.example.layoutmanager.model.ProfileFile
import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException

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
