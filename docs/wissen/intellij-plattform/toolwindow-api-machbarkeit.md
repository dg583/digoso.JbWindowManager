# Toolfenster-API: Was lässt sich lesen und setzen?

Stand: 2026-11-19 · Methode: Quellcode von `JetBrains/intellij-community`, Branch `252` (2025.2) und Stichprobe auf `263` (= Rider 2026.3, Ziel des Plugins: `ToolWindow.setAnchor/setSplitMode/setType`, `ToolWindowManagerEx.getLayout/setLayout`, `DesktopLayout.getInfos` unverändert vorhanden), per `gh api` gelesen. **Nicht in einer laufenden IDE getestet.** Rider-Frontend-Verhalten ungeprüft.

## Fazit
Alles, was das Profil braucht, ist **lesbar**. **Setzen** geht für Seite, Split, ViewMode und Sichtbarkeit über die öffentliche API. Für **Position/Größe und Reihenfolge** gibt es keine öffentlichen Setter, nur den internen Weg über `DesktopLayout`.

## Öffentliche API (stabil)
`com.intellij.openapi.wm.ToolWindow` (`platform/ide-core/.../wm/ToolWindow.java`) und `ToolWindowManager` (`platform/platform-api/.../wm/ToolWindowManager.kt`):

| Profilfeld | Lesen | Setzen |
|---|---|---|
| Seite (left/right/bottom/top) | `getAnchor()` | `setAnchor(anchor, runnable)` |
| Split (oberer/unterer Bereich) | `isSplitMode()` | `setSplitMode(split, runnable)` |
| ViewMode | `getType()`: `DOCKED`, `FLOATING`, `SLIDING`, `WINDOWED` | `setType(type, runnable)` |
| Auto-Hide (Unpinned) | `isAutoHide()` | `setAutoHide(bool)` |
| offen/zu | `isVisible()` | `show(runnable)` / `hide(runnable)` |
| Stripe-Button sichtbar | `isShowStripeButton()` | `setShowStripeButton(bool)` |
| Alle IDs / Zugriff | `ToolWindowManager.toolWindowIds`, `getToolWindow(id)` (liefert `null` für Fehlende, also leicht zu überspringen) | – |

Hinweis: ViewMode im UI ist eine Kombination aus `type` und `isAutoHide` (Dock Pinned = `DOCKED`, Dock Unpinned = `DOCKED` + autoHide, Undock = `SLIDING`, Float = `FLOATING`, Window = `WINDOWED`). **Zuordnung ungeprüft.**

## Lücke der öffentlichen API
- **Fensterposition/-größe (floatingBounds):** nur indirekt über `setDefaultState(anchor, type, floatingBounds)` (nur Default, wirkt nicht zuverlässig auf bereits registrierte Fenster – ungeprüft). Kein Getter.
- **Größe angedockter Fenster (`weight`, `sideWeight`):** nur `stretchWidth/stretchHeight` (`ToolWindowEx`, `platform-impl`).
- **Reihenfolge im Stripe (`order`):** kein öffentlicher Zugriff.

## Interner Weg (deckt alles ab)
- `ToolWindowManagerEx.getInstanceEx(project)` (`platform/platform-impl/.../wm/ex/ToolWindowManagerEx.kt`) bietet `getLayout(): DesktopLayout` und `setLayout(DesktopLayout)`. Beide nur im **EDT**.
- `DesktopLayout.getInfos(): Map<String, WindowInfoImpl>`, `copy()`.
- `WindowInfoImpl` / Interface `WindowInfo` (`platform-api`) enthalten: `id`, `anchor`, `isSplit`, `order`, `weight`, `sideWeight`, `isVisible`, `type`, `internalType`, `isAutoHide`, `floatingBounds`, `isMaximized`, `isShowStripeButton`, `toolWindowPaneId`.
- Risiko: `ToolWindowManagerImpl` ist `@ApiStatus.Internal`, `DesktopLayout` liegt in `platform-impl`. Kann sich zwischen Versionen ändern (Plugin Verifier meldet das).

## Ähnliches in der Plattform: benannte Layouts
`ToolWindowDefaultLayoutManager` (`platform/platform-impl/.../toolWindow/`, Datei existiert auf 252) speichert benannte Layouts (Aktive Layout, Speichern, Wiederherstellen, Umbenennen, Löschen) und ist vermutlich die Basis von `Window > Layouts`. Methoden gelesen auf `master`; **auf 252 nicht verifiziert**. Für unser Ziel (eigene JSON-Profile, eigene Verwaltung) nicht nötig, aber als Vorbild nützlich.

## Empfehlung fürs Design
1. **Lesen:** `getLayout().getInfos()` (alle Felder inkl. Bounds/Weight/Order), nur für IDs, die es gibt.
2. **Eigenes JSON-Format** mit den Feldern oben (nicht `WindowInfoImpl` direkt serialisieren, um von Interna entkoppelt zu bleiben).
3. **Anwenden:** Zuerst über die öffentliche API (anchor, split, type, autoHide, visible). Falls Position/Größe/Reihenfolge so nicht wiederherstellbar sind, ergänzend über eine `DesktopLayout`-Kopie + `setLayout`. Entscheidung per **Spike in `runIde`**.

## Noch zu prüfen (Spike)
- Wirkt `setAnchor` + `setSplitMode` + `setType` in der richtigen Reihenfolge so, dass „Explorer left/top, Commit left/bottom“ exakt entsteht?
- Lässt sich `floatingBounds` für `FLOATING`/`WINDOWED` wirklich wiederherstellen?
- Verhält sich Rider (Frontend, ggf. Split-Mode mit Backend) wie IntelliJ?
- Kompiliert und verifiziert `ToolWindowManagerEx`/`DesktopLayout` gegen Rider 2026.3?
