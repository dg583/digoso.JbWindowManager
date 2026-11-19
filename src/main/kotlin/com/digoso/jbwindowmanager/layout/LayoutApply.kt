package com.digoso.jbwindowmanager.layout

import com.digoso.jbwindowmanager.model.Profile
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowType
import com.intellij.openapi.wm.ex.ToolWindowManagerEx
import java.awt.Rectangle

private val LOG = Logger.getInstance("LayoutManager.Apply")

internal fun parseAnchor(s: String): ToolWindowAnchor? =
    listOf(ToolWindowAnchor.LEFT, ToolWindowAnchor.RIGHT, ToolWindowAnchor.BOTTOM, ToolWindowAnchor.TOP).firstOrNull { anchorName(it) == s }

internal fun anchorName(a: ToolWindowAnchor): String = a.toString()

internal fun parseType(s: String): ToolWindowType? = ToolWindowType.entries.firstOrNull { it.name == s }

/**
 * Wendet [profile] an (nur im EDT). Weg laut Spike: Kopie des aktuellen Layouts ändern und per `setLayout` setzen,
 * denn nur so wird auch `floatingBounds` wiederhergestellt (siehe docs/wissen/intellij-plattform/spike-ergebnis.md).
 * Toolfenster, die im aktuellen Layout fehlen oder deren `anchor`/`type` unbekannt ist, werden übersprungen.
 */
fun applyProfile(project: Project, profile: Profile) {
    val manager = ToolWindowManagerEx.getInstanceEx(project)
    val layout = manager.getLayout().copy()
    val infos = layout.getInfos()
    for (s in profile.toolWindows) {
        val info = infos[s.id]
        if (info == null) {
            LOG.info("Übersprungen (fehlt): ${s.id}")
            continue
        }
        try {
            val anchor = parseAnchor(s.anchor)
            val type = parseType(s.type)
            if (anchor == null || type == null) {
                LOG.warn("Übersprungen (unbekannter Wert anchor=${s.anchor}, type=${s.type}): ${s.id}")
                continue
            }
            info.anchor = anchor
            info.isSplit = s.split
            info.type = type
            // WindowInfoImpl.setType (intern) hält internalType synchron: nur DOCKED und SLIDING sind "intern"
            if (type == ToolWindowType.DOCKED || type == ToolWindowType.SLIDING) info.internalType = type
            info.isAutoHide = s.autoHide
            info.isVisible = s.visible
            info.order = s.order
            if (s.weight > 0f) info.weight = s.weight // 0 = Feld fehlte in der Datei
            info.floatingBounds = s.bounds?.let { Rectangle(it.x, it.y, it.width, it.height) }
        } catch (t: Throwable) {
            LOG.warn("Fehler beim Anwenden: ${s.id}", t)
        }
    }
    manager.setLayout(layout)
}
