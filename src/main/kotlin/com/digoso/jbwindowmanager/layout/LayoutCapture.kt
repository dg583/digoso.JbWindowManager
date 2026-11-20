package com.digoso.jbwindowmanager.layout

import com.digoso.jbwindowmanager.model.FrameState
import com.digoso.jbwindowmanager.model.Profile
import com.digoso.jbwindowmanager.model.ToolWindowState
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowType
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerEx
import java.awt.Frame
import java.awt.Rectangle
import javax.swing.SwingUtilities

/** Reads the current state of all tool windows and the main window of the project (EDT only, public API only). */
fun captureProfile(project: Project, name: String): Profile {
    val windowManager = WindowManager.getInstance()
    val frame = windowManager.getFrame(project)
    val screen: Rectangle? = frame?.graphicsConfiguration?.bounds
    val manager = ToolWindowManagerEx.getInstanceEx(project)
    val order = HashMap<String, Int>()
    for (a in ANCHORS) manager.getIdsOn(a).forEachIndexed { i, id -> if (id != null) order[id] = i }

    val states = manager.toolWindows.mapNotNull { tw ->
        val id = tw.id ?: return@mapNotNull null
        val type = tw.type
        val floating = type == ToolWindowType.FLOATING || type == ToolWindowType.WINDOWED
        val rel = if (floating && screen != null) {
            SwingUtilities.getWindowAncestor(tw.component)?.bounds?.let { relOf(it, screen) }
        } else {
            null
        }
        // Docked size as a fraction of the main frame (width for left/right, height for top/bottom); 0 = not applicable.
        val weight = if (type == ToolWindowType.DOCKED && tw.isVisible && frame != null) {
            val horizontal = isHorizontalSide(tw.anchor)
            val size = if (horizontal) tw.component.width else tw.component.height
            val total = if (horizontal) frame.width else frame.height
            if (total > 0) size.toFloat() / total else 0f
        } else {
            0f
        }
        ToolWindowState(
            id = id,
            anchor = anchorName(tw.anchor),
            split = tw.isSplitMode,
            type = type.name,
            autoHide = tw.isAutoHide,
            visible = tw.isVisible,
            order = order[id] ?: -1,
            weight = weight,
            bounds = null,
            rel = rel,
        )
    }.sortedWith(compareBy({ it.anchor }, { it.split }, { it.order }, { it.id }))

    val frameState = frame?.let {
        FrameState(
            fullscreen = windowManager.getIdeFrame(project)?.isInFullScreen == true,
            maximized = (it.extendedState and Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH,
            rel = screen?.let { sc -> relOf(it.bounds, sc) },
        )
    }
    return Profile(name, states, frameState)
}
