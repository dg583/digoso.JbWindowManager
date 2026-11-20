package com.digoso.jbwindowmanager.layout

import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowType

// Platform types only. Kept apart from Geometry.kt so that file stays free of the IDE (CI compiles it without Rider).

internal val ANCHORS = listOf(ToolWindowAnchor.LEFT, ToolWindowAnchor.RIGHT, ToolWindowAnchor.BOTTOM, ToolWindowAnchor.TOP)

internal fun parseAnchor(s: String): ToolWindowAnchor? = ANCHORS.firstOrNull { anchorName(it) == s }

internal fun anchorName(a: ToolWindowAnchor): String = a.toString()

internal fun parseType(s: String): ToolWindowType? = ToolWindowType.entries.firstOrNull { it.name == s }

internal fun isHorizontalSide(a: ToolWindowAnchor): Boolean = a == ToolWindowAnchor.LEFT || a == ToolWindowAnchor.RIGHT
