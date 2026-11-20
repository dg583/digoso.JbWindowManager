package com.digoso.jbwindowmanager.layout

import com.digoso.jbwindowmanager.model.Profile
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowType
import com.intellij.openapi.wm.ex.ToolWindowManagerEx
import java.awt.Rectangle

private val LOG = Logger.getInstance("digoso.JbWindowManager.Apply")

/**
 * Applies [profile] (EDT only). Approach per the spike: modify a copy of the current layout and set it via `setLayout`,
 * because only that also restores `floatingBounds` (see docs/knowledge/intellij-platform/spike-result.md).
 * Tool windows missing from the current layout, or with an unknown `anchor`/`type`, are skipped.
 */
fun applyProfile(project: Project, profile: Profile) {
    val manager = ToolWindowManagerEx.getInstanceEx(project)
    val layout = manager.getLayout().copy()
    val infos = layout.getInfos()
    for (s in profile.toolWindows) {
        val info = infos[s.id]
        if (info == null) {
            LOG.info("Skipped (missing): ${s.id}")
            continue
        }
        try {
            val anchor = parseAnchor(s.anchor)
            val type = parseType(s.type)
            if (anchor == null || type == null) {
                LOG.warn("Skipped (unknown value anchor=${s.anchor}, type=${s.type}): ${s.id}")
                continue
            }
            info.anchor = anchor
            info.isSplit = s.split
            info.type = type
            // WindowInfoImpl.setType (internal) keeps internalType in sync: only DOCKED and SLIDING are "internal"
            if (type == ToolWindowType.DOCKED || type == ToolWindowType.SLIDING) info.internalType = type
            info.isAutoHide = s.autoHide
            info.isVisible = s.visible
            info.order = s.order
            if (s.weight > 0f) info.weight = s.weight // 0 = field was missing in the file
            info.floatingBounds = s.bounds?.let { Rectangle(it.x, it.y, it.width, it.height) }
        } catch (t: Throwable) {
            LOG.warn("Failed to apply: ${s.id}", t)
        }
    }
    manager.setLayout(layout)
}
