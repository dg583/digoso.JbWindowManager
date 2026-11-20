package com.digoso.jbwindowmanager.layout

import com.digoso.jbwindowmanager.model.Rel
import com.digoso.jbwindowmanager.model.ToolWindowState
import java.awt.Rectangle
import kotlin.math.abs
import kotlin.math.roundToInt

// Pure logic, no IDE types: CI compiles and tests this file without Rider.

/** [r] as fractions of [screen]; null for a screen without area (no sensible fraction). */
internal fun relOf(r: Rectangle, screen: Rectangle): Rel? {
    if (screen.width <= 0 || screen.height <= 0) return null
    return Rel(
        (r.x - screen.x).toDouble() / screen.width,
        (r.y - screen.y).toDouble() / screen.height,
        r.width.toDouble() / screen.width,
        r.height.toDouble() / screen.height,
    )
}

internal fun Rel.toRect(screen: Rectangle): Rectangle = Rectangle(
    screen.x + (x * screen.width).roundToInt(),
    screen.y + (y * screen.height).roundToInt(),
    (w * screen.width).roundToInt(),
    (h * screen.height).roundToInt(),
)

/** Pixels to stretch to get from [actual] to [target]; null when already within [tolerance]. */
internal fun nextStretch(target: Int, actual: Int, tolerance: Int = 2): Int? =
    (target - actual).takeIf { abs(it) > tolerance }

/** Apply order: per side and split in saved order, so appending reproduces the order. */
internal fun applyOrder(states: List<ToolWindowState>): List<ToolWindowState> =
    states.sortedWith(compareBy({ it.anchor }, { it.split }, { it.order }, { it.id }))
