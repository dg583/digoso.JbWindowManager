package com.digoso.jbwindowmanager.model

const val CURRENT_VERSION = 2

/** Absolute bounds in pixels. Only read from version 1 files; version 2 stores [Rel]. */
data class Bounds(val x: Int, val y: Int, val width: Int, val height: Int)

/** Position and size as fractions of the main frame's screen (values outside 0..1 are allowed). */
data class Rel(val x: Double, val y: Double, val w: Double, val h: Double)

/** Main window state. `rel` is only meaningful when neither fullscreen nor maximized. */
data class FrameState(val fullscreen: Boolean, val maximized: Boolean, val rel: Rel?)

/** State of one tool window. `anchor` and `type` are strings so unknown values do not fail while reading. */
data class ToolWindowState(
    val id: String,
    val anchor: String, // left | right | bottom | top
    val split: Boolean,
    val type: String, // DOCKED | FLOATING | SLIDING | WINDOWED
    val autoHide: Boolean,
    val visible: Boolean,
    val order: Int,
    val weight: Float,
    val bounds: Bounds?, // legacy absolute bounds (version 1), no longer written
    val rel: Rel? = null, // FLOATING/WINDOWED position relative to the main frame's screen
)

/** `frame` is null for version 1 profiles: applying them leaves the main window untouched. */
data class Profile(val name: String, val toolWindows: List<ToolWindowState>, val frame: FrameState? = null)

data class ProfileFile(val version: Int, val profiles: List<Profile>)
