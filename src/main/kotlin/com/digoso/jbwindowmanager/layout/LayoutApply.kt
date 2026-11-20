package com.digoso.jbwindowmanager.layout

import com.digoso.jbwindowmanager.model.FrameState
import com.digoso.jbwindowmanager.model.Profile
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ToolWindowType
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.wm.ex.ToolWindowEx
import java.awt.Frame
import java.awt.Rectangle
import javax.swing.SwingUtilities
import javax.swing.Timer
import kotlin.math.roundToInt

private val LOG = Logger.getInstance("digoso.JbWindowManager.Apply")
private const val SETTLE_TIMEOUT_MS = 3000L
private const val POLL_MS = 100
private const val MAX_SIZE_PASSES = 5

/**
 * Applies [profile] (EDT only, public API only). Returns immediately; the steps run asynchronously, each after the
 * previous one has settled: main frame, tool windows, floating/windowed window positions, docked sizes.
 * Best effort: a failure on one tool window is logged and does not abort the profile.
 */
fun applyProfile(project: Project, profile: Profile) {
    applyFrame(project, profile.frame) {
        applyToolWindows(project, profile)
        // Sizes only make sense once every window sits on its side with its type; the platform applies these asynchronously.
        waitUntil(toolWindowsStable(project, profile)) {
            applyWindowBounds(project, profile)
            ApplicationManager.getApplication().invokeLater { applySizes(project, profile, 1, SizeState()) }
        }
    }
}

/** Settled and, in addition, the docked window sizes are unchanged between two consecutive polls (the layout has caught up). */
private fun toolWindowsStable(project: Project, profile: Profile): () -> Boolean {
    val manager = ToolWindowManager.getInstance(project)
    var last: List<Pair<Int, Int>>? = null
    return {
        val settled = toolWindowsSettled(project, profile)
        val sizes = if (settled) {
            profile.toolWindows.mapNotNull { s ->
                manager.getToolWindow(s.id)?.takeIf { s.visible && it.type == ToolWindowType.DOCKED }?.component?.let { it.width to it.height }
            }
        } else {
            null
        }
        val same = settled && sizes == last
        last = sizes
        same
    }
}

/** True once every registered window of the profile has the saved side, split, type and visibility (and is laid out if docked and visible). */
private fun toolWindowsSettled(project: Project, profile: Profile): Boolean {
    val manager = ToolWindowManager.getInstance(project)
    return profile.toolWindows.all { s ->
        val tw = manager.getToolWindow(s.id) ?: return@all true
        val anchor = parseAnchor(s.anchor) ?: return@all true
        val type = parseType(s.type) ?: return@all true
        val placed = tw.anchor == anchor && tw.isSplitMode == s.split && tw.type == type && tw.isVisible == s.visible
        val laidOut = !(s.visible && type == ToolWindowType.DOCKED) || (tw.component.isShowing && tw.component.width > 0 && tw.component.height > 0)
        placed && laidOut
    }
}

/** Per apply run: last measured size per window and windows whose size does not react to stretching. */
private class SizeState {
    val last = HashMap<String, Int>()
    val stuck = HashSet<String>()
}

/** Polls on the EDT until [condition] holds or the timeout passes, then runs [then]. No fixed sleeps. */
private fun waitUntil(condition: () -> Boolean, then: () -> Unit) {
    val start = System.currentTimeMillis()
    Timer(POLL_MS) { e ->
        val ok = condition()
        if (ok || System.currentTimeMillis() - start > SETTLE_TIMEOUT_MS) {
            (e.source as Timer).stop()
            if (!ok) LOG.warn("Timed out waiting for the window state to settle, continuing")
            then()
        }
    }.start()
}

/** True once the frame bounds are unchanged between two consecutive polls. */
private fun stable(frame: Frame): () -> Boolean {
    var last: Rectangle? = null
    return {
        val now = frame.bounds
        val same = now == last
        last = now
        same
    }
}

private fun applyFrame(project: Project, f: FrameState?, then: () -> Unit) {
    val windowManager = WindowManager.getInstance()
    val frame = windowManager.getFrame(project)
    if (f == null || frame == null) {
        then()
        return
    }
    fun isFullScreen() = windowManager.getIdeFrame(project)?.isInFullScreen == true
    if (isFullScreen() != f.fullscreen) {
        val action = ActionManager.getInstance().getAction("ToggleFullScreen")
        if (action == null) LOG.warn("Action ToggleFullScreen not found") else ActionUtil.invokeAction(action, frame.rootPane, "ProfileApply", null, null)
    }
    waitUntil({ isFullScreen() == f.fullscreen }) {
        if (!f.fullscreen) {
            try {
                frame.extendedState = if (f.maximized) Frame.MAXIMIZED_BOTH else Frame.NORMAL
                val rel = f.rel
                if (!f.maximized && rel != null) frame.bounds = rel.toRect(frame.graphicsConfiguration.bounds)
            } catch (t: Throwable) {
                LOG.warn("Failed to apply the main frame state", t)
            }
        }
        waitUntil(stable(frame), then)
    }
}

private fun applyToolWindows(project: Project, profile: Profile) {
    val manager = ToolWindowManager.getInstance(project)
    for (s in applyOrder(profile.toolWindows)) {
        val tw = manager.getToolWindow(s.id)
        if (tw == null) {
            LOG.info("Skipped (missing): ${s.id}")
            continue
        }
        val anchor = parseAnchor(s.anchor)
        val type = parseType(s.type)
        if (anchor == null || type == null) {
            LOG.warn("Skipped (unknown value anchor=${s.anchor}, type=${s.type}): ${s.id}")
            continue
        }
        try {
            tw.setAnchor(anchor, null)
            tw.setSplitMode(s.split, null)
            tw.setType(type, null)
            tw.isAutoHide = s.autoHide
            if (s.visible) tw.show(null) else tw.hide(null)
        } catch (t: Throwable) {
            LOG.warn("Failed to apply: ${s.id}", t)
        }
    }
}

private fun applyWindowBounds(project: Project, profile: Profile) {
    val manager = ToolWindowManager.getInstance(project)
    val screen = WindowManager.getInstance().getFrame(project)?.graphicsConfiguration?.bounds
    for (s in profile.toolWindows) {
        val tw = manager.getToolWindow(s.id) ?: continue
        if (tw.type != ToolWindowType.FLOATING && tw.type != ToolWindowType.WINDOWED) continue
        // version 2: relative to the current screen; version 1 fallback: absolute bounds
        val rect = s.rel?.let { r -> screen?.let { r.toRect(it) } }
            ?: s.bounds?.let { Rectangle(it.x, it.y, it.width, it.height) }
            ?: continue
        try {
            SwingUtilities.getWindowAncestor(tw.component)?.bounds = rect
        } catch (t: Throwable) {
            LOG.warn("Failed to set the window position: ${s.id}", t)
        }
    }
}

/**
 * Converging passes: one window per pass (windows in one column share their width), re-measure after the layout has
 * settled, until every docked window is within tolerance or [MAX_SIZE_PASSES] is reached.
 */
private fun applySizes(project: Project, profile: Profile, pass: Int, state: SizeState) {
    val manager = ToolWindowManager.getInstance(project)
    val frame = WindowManager.getInstance().getFrame(project)
    var changed = false
    if (frame != null) {
        for (s in profile.toolWindows) {
            if (s.weight <= 0f || !s.visible || s.id in state.stuck) continue
            val tw = manager.getToolWindow(s.id) as? ToolWindowEx ?: continue
            if (tw.type != ToolWindowType.DOCKED || !tw.isVisible) continue
            val anchor = parseAnchor(s.anchor) ?: continue
            val horizontal = isHorizontalSide(anchor)
            val target = (s.weight * (if (horizontal) frame.width else frame.height)).roundToInt()
            val actual = if (horizontal) tw.component.width else tw.component.height
            val delta = nextStretch(target, actual) ?: continue
            if (state.last[s.id] == actual) {
                // The last stretch changed nothing: do not let this window block the others.
                LOG.warn("Size does not react, skipped: ${s.id} (${s.anchor}, split=${s.split}) target=$target actual=$actual")
                state.stuck.add(s.id)
                continue
            }
            state.last[s.id] = actual
            LOG.info("Size pass $pass: ${s.id} (${s.anchor}, split=${s.split}) target=$target actual=$actual delta=$delta frame=${frame.width}x${frame.height}")
            try {
                if (horizontal) tw.stretchWidth(delta) else tw.stretchHeight(delta)
            } catch (t: Throwable) {
                LOG.warn("Failed to resize: ${s.id}", t)
            }
            changed = true
            break
        }
    }
    if (changed && pass < MAX_SIZE_PASSES * 4) {
        ApplicationManager.getApplication().invokeLater { applySizes(project, profile, pass + 1, state) }
    } else if (changed) {
        LOG.warn("Docked sizes did not converge within ${MAX_SIZE_PASSES * 4} passes")
    }
}
