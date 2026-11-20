package com.digoso.jbwindowmanager.layout

import com.digoso.jbwindowmanager.model.ToolWindowState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.Rectangle
import kotlin.math.abs

class GeometryTest {
    private val screen = Rectangle(0, 0, 1920, 1200)
    private val ultrawide = Rectangle(-848, -1440, 3440, 1440)

    @Test
    fun relRoundTripOnSameScreen() {
        val r = Rectangle(943, 669, 983, 489)
        assertEquals(r, relOf(r, screen)!!.toRect(screen))
    }

    @Test
    fun scalesToAnotherScreen() {
        val r = Rectangle(943, 669, 983, 489)
        val out = relOf(r, screen)!!.toRect(ultrawide)
        assertEquals(Math.round(983 / 1920.0 * 3440).toInt(), out.width)
        assertEquals(-848 + Math.round(943 / 1920.0 * 3440).toInt(), out.x)
    }

    @Test
    fun negativeAndOutsideFractionsSurvive() {
        val r = Rectangle(-7, 0, 974, 1159)
        val back = relOf(r, screen)!!.toRect(screen)
        assertTrue(abs(back.x - r.x) <= 1 && abs(back.width - r.width) <= 1)
    }

    @Test
    fun zeroSizedScreenGivesNull() {
        assertNull(relOf(Rectangle(0, 0, 10, 10), Rectangle(0, 0, 0, 1200)))
        assertNull(relOf(Rectangle(0, 0, 10, 10), Rectangle(0, 0, 1920, 0)))
    }

    @Test
    fun nextStretchWithinTolerance() {
        assertNull(nextStretch(340, 341))
        assertNull(nextStretch(340, 342))
    }

    @Test
    fun nextStretchGrowAndShrink() {
        assertEquals(179, nextStretch(340, 161))
        assertEquals(-51, nextStretch(340, 391))
    }

    private fun w(id: String, anchor: String, split: Boolean, order: Int) =
        ToolWindowState(id, anchor, split, "DOCKED", false, true, order, 0f, null)

    @Test
    fun applyOrderSortsPerAnchorSplitOrder() {
        val sorted = applyOrder(listOf(w("B", "left", true, 1), w("A", "left", false, 4), w("C", "left", false, 2), w("D", "bottom", false, 0)))
        assertEquals(listOf("D", "C", "A", "B"), sorted.map { it.id })
    }
}
