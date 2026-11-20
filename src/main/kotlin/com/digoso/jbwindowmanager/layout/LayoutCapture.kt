package com.digoso.jbwindowmanager.layout

import com.digoso.jbwindowmanager.model.Bounds
import com.digoso.jbwindowmanager.model.Profile
import com.digoso.jbwindowmanager.model.ToolWindowState
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ex.ToolWindowManagerEx

/** Reads the current state of all tool windows of the project (EDT only). */
fun captureProfile(project: Project, name: String): Profile {
    val infos = ToolWindowManagerEx.getInstanceEx(project).getLayout().getInfos().values
    val states = infos.mapNotNull { i ->
        val id = i.id ?: return@mapNotNull null
        ToolWindowState(
            id = id,
            anchor = anchorName(i.anchor),
            split = i.isSplit,
            type = i.type.name,
            autoHide = i.isAutoHide,
            visible = i.isVisible,
            order = i.order,
            weight = i.weight,
            bounds = i.floatingBounds?.let { Bounds(it.x, it.y, it.width, it.height) },
        )
    }.sortedWith(compareBy({ it.anchor }, { it.split }, { it.order }, { it.id }))
    return Profile(name, states)
}
