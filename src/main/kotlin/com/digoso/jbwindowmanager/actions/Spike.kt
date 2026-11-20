package com.digoso.jbwindowmanager.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowType
import com.intellij.openapi.wm.ex.ToolWindowEx
import com.intellij.openapi.wm.ex.ToolWindowManagerEx
import java.awt.Rectangle
import javax.swing.SwingUtilities

// THROWAWAY spike: public API only (no DesktopLayout). Delete after the spike.
private val LOG = Logger.getInstance("digoso.JbWindowManager.SPIKE")
private val ANCHORS = listOf(ToolWindowAnchor.LEFT, ToolWindowAnchor.RIGHT, ToolWindowAnchor.BOTTOM, ToolWindowAnchor.TOP)

private data class Snap(
    val id: String, val anchor: ToolWindowAnchor, val split: Boolean, val type: ToolWindowType,
    val autoHide: Boolean, val visible: Boolean, val order: Int, val compW: Int, val compH: Int, val bounds: Rectangle?,
    // floating/windowed bounds as fractions of the main frame's screen: x, y, width, height
    val rel: DoubleArray? = null,
)

private fun mainScreen(project: Project): Rectangle? = WindowManager.getInstance().getFrame(project)?.graphicsConfiguration?.bounds

private fun relative(b: Rectangle?, sc: Rectangle?): DoubleArray? =
    if (b == null || sc == null) null
    else doubleArrayOf((b.x - sc.x).toDouble() / sc.width, (b.y - sc.y).toDouble() / sc.height, b.width.toDouble() / sc.width, b.height.toDouble() / sc.height)

private var snapshot: List<Snap> = emptyList()

// Main frame: fullscreen flag, extended state (maximized), bounds as fractions of the screen the frame is on.
private data class FrameSnap(val fullscreen: Boolean, val extState: Int, val bounds: Rectangle, val screen: Rectangle,
                             val rx: Double, val ry: Double, val rw: Double, val rh: Double)

private var frameSnap: FrameSnap? = null

private fun captureFrame(project: Project): FrameSnap? {
    val wm = WindowManager.getInstance()
    val frame = wm.getFrame(project) ?: return null
    val screen = frame.graphicsConfiguration.bounds
    val b = frame.bounds
    return FrameSnap(wm.getIdeFrame(project)?.isInFullScreen == true, frame.extendedState, b, screen,
        (b.x - screen.x).toDouble() / screen.width, (b.y - screen.y).toDouble() / screen.height,
        b.width.toDouble() / screen.width, b.height.toDouble() / screen.height)
}

/** Applies the frame state, then runs [then] once the frame has settled. */
private fun applyFrame(project: Project, f: FrameSnap?, then: () -> Unit) {
    val wm = WindowManager.getInstance()
    val frame = wm.getFrame(project)
    if (f == null || frame == null) { then(); return }
    val isFull = wm.getIdeFrame(project)?.isInFullScreen == true
    LOG.warn("FRAME fullscreen now=$isFull wanted=${f.fullscreen}")
    if (isFull != f.fullscreen) {
        val action = ActionManager.getInstance().getAction("ToggleFullScreen")
        ActionUtil.invokeAction(action, frame.rootPane, "SpikeApply", null, null)
    }
    if (!f.fullscreen) {
        // The fullscreen toggle is asynchronous: wait, then set state and scaled bounds.
        javax.swing.Timer(1200) {
            try {
                frame.extendedState = f.extState and java.awt.Frame.ICONIFIED.inv()
                if ((f.extState and java.awt.Frame.MAXIMIZED_BOTH) == 0) {
                    val sc = frame.graphicsConfiguration.bounds
                    frame.bounds = Rectangle((sc.x + f.rx * sc.width).toInt(), (sc.y + f.ry * sc.height).toInt(),
                        (f.rw * sc.width).toInt(), (f.rh * sc.height).toInt())
                    LOG.warn("FRAME applied on screen=$sc from screen=${f.screen} -> ${frame.bounds}")
                }
            } catch (t: Throwable) { LOG.warn("FRAME apply failed", t) }
            javax.swing.Timer(800) { then() }.apply { isRepeats = false; start() }
        }.apply { isRepeats = false; start() }
    } else {
        javax.swing.Timer(1500) { then() }.apply { isRepeats = false; start() }
    }
}

private fun windowBounds(tw: ToolWindow): Rectangle? {
    val type = tw.type
    if (type != ToolWindowType.FLOATING && type != ToolWindowType.WINDOWED) return null
    return SwingUtilities.getWindowAncestor(tw.component)?.bounds
}

private fun capture(project: Project): List<Snap> {
    val m = ToolWindowManagerEx.getInstanceEx(project)
    val order = HashMap<String, Int>()
    for (a in ANCHORS) m.getIdsOn(a).forEachIndexed { i, id -> if (id != null) order[id] = i }
    val sc = mainScreen(project)
    return m.toolWindows.mapNotNull { tw ->
        val id = tw.id ?: return@mapNotNull null
        Snap(id, tw.anchor, tw.isSplitMode, tw.type, tw.isAutoHide, tw.isVisible, order[tw.id] ?: -1,
            tw.component.width, tw.component.height, windowBounds(tw), relative(windowBounds(tw), sc))
    }
}

private fun dump(tag: String, project: Project) {
    val m = ToolWindowManagerEx.getInstanceEx(project)
    LOG.warn("$tag FRAME ${captureFrame(project)}")
    for (s in capture(project)) LOG.warn("$tag $s")
    for (a in ANCHORS) LOG.warn("$tag IDS-ON $a = ${m.getIdsOn(a)}")
}

class SpikeSnapshotAction : AnAction("Spike: Snapshot (public API)") {
    override fun getActionUpdateThread() = ActionUpdateThread.EDT
    override fun actionPerformed(e: AnActionEvent) {
        val p = e.project ?: return
        snapshot = capture(p)
        frameSnap = captureFrame(p)
        dump("SNAP", p)
    }
}

class SpikeApplyAction : AnAction("Spike: Apply snapshot (public API)") {
    override fun getActionUpdateThread() = ActionUpdateThread.EDT
    override fun actionPerformed(e: AnActionEvent) {
        val p = e.project ?: return
        applyFrame(p, frameSnap) { applyToolWindows(p) }
    }

    private fun applyToolWindows(p: Project) {
        val m = ToolWindowManagerEx.getInstanceEx(p)
        // Apply in saved order per anchor so that appends could reproduce the order.
        for (s in snapshot.sortedWith(compareBy({ it.anchor.toString() }, { it.split }, { it.order }))) {
            val tw = m.getToolWindow(s.id) ?: run { LOG.warn("APPLY missing ${s.id}"); null } ?: continue
            try {
                tw.setAnchor(s.anchor, null)
                tw.setSplitMode(s.split, null)
                tw.setType(s.type, null)
                tw.isAutoHide = s.autoHide
                if (s.visible) tw.show(null) else tw.hide(null)
            } catch (t: Throwable) {
                LOG.warn("APPLY failed ${s.id}", t)
            }
        }
        // Window position once, then size in converging passes: measure again after each pass (layout settles asynchronously).
        ApplicationManager.getApplication().invokeLater {
            for (s in snapshot) {
                val tw = m.getToolWindow(s.id) ?: continue
                try {
                    val sc = mainScreen(p)
                    val r = s.rel
                    if (r != null && sc != null) {
                        val b = Rectangle((sc.x + r[0] * sc.width).toInt(), (sc.y + r[1] * sc.height).toInt(),
                            (r[2] * sc.width).toInt(), (r[3] * sc.height).toInt())
                        LOG.warn("WINDOW ${s.id} saved=${s.bounds} screen=$sc -> $b")
                        SwingUtilities.getWindowAncestor(tw.component)?.bounds = b
                    }
                } catch (t: Throwable) {
                    LOG.warn("APPLY bounds failed ${s.id}", t)
                }
            }
            sizePass(p, 1)
        }
    }

    private fun sizePass(p: Project, pass: Int) {
        val m = ToolWindowManagerEx.getInstanceEx(p)
        var changed = false
        for (s in snapshot) {
            val tw = m.getToolWindow(s.id) as? ToolWindowEx ?: continue
            if (s.bounds != null || !s.visible || tw.type != ToolWindowType.DOCKED) continue
            val horizontal = s.anchor == ToolWindowAnchor.LEFT || s.anchor == ToolWindowAnchor.RIGHT
            val diff = if (horizontal) s.compW - tw.component.width else s.compH - tw.component.height
            if (kotlin.math.abs(diff) <= 2) continue
            changed = true
            LOG.warn("SIZE pass=$pass ${s.id} diff=$diff")
            try {
                if (horizontal) tw.stretchWidth(diff) else tw.stretchHeight(diff)
            } catch (t: Throwable) {
                LOG.warn("APPLY size failed ${s.id}", t)
            }
            break // one window per pass: windows in one column share the width, re-measure before the next
        }
        ApplicationManager.getApplication().invokeLater {
            if (changed && pass < 5) sizePass(p, pass + 1) else dump("AFTER pass=$pass", p)
        }
    }
}
