package com.example.layoutmanager.layout

import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ParseTest {
    @Test
    fun knownAnchorsAreParsed() {
        assertEquals(ToolWindowAnchor.LEFT, parseAnchor("left"))
        assertEquals(ToolWindowAnchor.RIGHT, parseAnchor("right"))
        assertEquals(ToolWindowAnchor.BOTTOM, parseAnchor("bottom"))
        assertEquals(ToolWindowAnchor.TOP, parseAnchor("top"))
    }

    @Test
    fun unknownAnchorIsNull() {
        assertNull(parseAnchor("bogus"))
    }

    @Test
    fun knownTypesAreParsed() {
        assertEquals(ToolWindowType.FLOATING, parseType("FLOATING"))
        assertEquals(ToolWindowType.WINDOWED, parseType("WINDOWED"))
    }

    @Test
    fun unknownTypeIsNull() {
        assertNull(parseType("x"))
    }

    @Test
    fun anchorRoundtripsThroughName() {
        for (a in listOf(ToolWindowAnchor.LEFT, ToolWindowAnchor.RIGHT, ToolWindowAnchor.BOTTOM, ToolWindowAnchor.TOP)) {
            assertEquals(a, parseAnchor(anchorName(a)))
        }
    }
}
