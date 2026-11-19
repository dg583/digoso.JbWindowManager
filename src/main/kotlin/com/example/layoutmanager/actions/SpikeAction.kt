package com.example.layoutmanager.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ToolWindowType
import com.intellij.openapi.wm.ex.ToolWindowManagerEx
import com.intellij.openapi.wm.impl.DesktopLayout
import java.awt.Rectangle

// SPIKE (Task 2): Wegwerfcode, wird in Task 5 entfernt.
private val LOG = Logger.getInstance("LayoutSpike")

private data class Snap(
    val id: String, val anchor: ToolWindowAnchor, val split: Boolean, val type: ToolWindowType,
    val autoHide: Boolean, val visible: Boolean, val order: Int, val weight: Float, val bounds: Rectangle?,
)

private var remembered: List<Snap> = emptyList()
private var rememberedLayout: DesktopLayout? = null

private fun readSnaps(e: AnActionEvent): List<Snap> {
    val project = e.project ?: return emptyList()
    val layout = ToolWindowManagerEx.getInstanceEx(project).getLayout()
    return layout.getInfos().values.mapNotNull { i ->
        val id = i.id ?: return@mapNotNull null
        Snap(id, i.anchor, i.isSplit, i.type, i.isAutoHide, i.isVisible, i.order, i.weight, i.floatingBounds)
    }.sortedWith(compareBy({ it.anchor.toString() }, { it.split }, { it.order }))
}

class SpikeRememberAction : AnAction("Spike: Layout merken + Dump") {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        remembered = readSnaps(e)
        rememberedLayout = ToolWindowManagerEx.getInstanceEx(project).getLayout().copy()
        remembered.forEach { LOG.warn("SNAP $it") }
        Messages.showInfoMessage(project, "${remembered.size} Toolfenster gemerkt (Details in idea.log, Zeilen 'SNAP').", "Spike")
    }
}

class SpikeRestorePublicAction : AnAction("Spike: Wiederherstellen (öffentliche API)") {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val twm = ToolWindowManager.getInstance(project)
        for (s in remembered) {
            val tw = twm.getToolWindow(s.id) ?: run { LOG.warn("APPLY skip (fehlt): ${s.id}"); continue }
            try {
                tw.setAnchor(s.anchor, null)
                tw.setSplitMode(s.split, null)
                tw.setType(s.type, null)
                tw.setAutoHide(s.autoHide)
                if (s.visible) tw.show(null) else tw.hide(null)
                LOG.warn("APPLY ok: ${s.id}")
            } catch (t: Throwable) {
                LOG.warn("APPLY fail: ${s.id}", t)
            }
        }
        // Nachlesen, um zu sehen, was wirklich ankam
        readSnaps(e).forEach { LOG.warn("AFTER-PUBLIC $it") }
    }
}

class SpikeRestoreLayoutAction : AnAction("Spike: Wiederherstellen (setLayout)") {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val layout = rememberedLayout ?: return
        ToolWindowManagerEx.getInstanceEx(project).setLayout(layout.copy())
        readSnaps(e).forEach { LOG.warn("AFTER-SETLAYOUT $it") }
    }
}
