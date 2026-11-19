package com.digoso.jbwindowmanager.model

const val CURRENT_VERSION = 1

data class Bounds(val x: Int, val y: Int, val width: Int, val height: Int)

/** Zustand eines Toolfensters. `anchor` und `type` sind Strings, damit unbekannte Werte nicht beim Lesen scheitern. */
data class ToolWindowState(
    val id: String,
    val anchor: String, // left | right | bottom | top
    val split: Boolean,
    val type: String, // DOCKED | FLOATING | SLIDING | WINDOWED
    val autoHide: Boolean,
    val visible: Boolean,
    val order: Int,
    val weight: Float,
    val bounds: Bounds?,
)

data class Profile(val name: String, val toolWindows: List<ToolWindowState>)

data class ProfileFile(val version: Int, val profiles: List<Profile>)
