package com.example.layoutmanager.layout

import com.example.layoutmanager.model.Bounds
import com.example.layoutmanager.model.Profile
import com.example.layoutmanager.model.ToolWindowState
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ex.ToolWindowManagerEx

/** Liest den Ist-Zustand aller Toolfenster des Projekts (nur im EDT). */
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
