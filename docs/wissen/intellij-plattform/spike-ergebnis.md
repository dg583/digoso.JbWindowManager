# Spike-Ergebnis: Toolfenster lesen und anwenden in Rider 2026.3

Stand: 2026-11-19 · Methode: Wegwerf-Aktionen `Tools > Spike: …` in der `runIde`-Sandbox (Rider 2026.3, Build 263), Nutzer hat manuell geprüft, Belege aus `idea.log` (Zeilen `SNAP`, `APPLY`, `AFTER-PUBLIC`, `AFTER-SETLAYOUT`). Ein Projekt, 16 Einträge im Layout.

## Antworten auf die offenen Fragen
1. **Setter-Reihenfolge (öffentliche API):** `setAnchor` → `setSplitMode` → `setType` → `setAutoHide` → `show/hide` stellt Seite, Split (z. B. Project links oben, Commit links unten) und offen/zu korrekt wieder her. 39 Aufrufe ohne Fehler, 0 Ausnahmen.
2. **Fensterposition (`floatingBounds`):** Die öffentliche API stellt sie **nicht** wieder her. Nach `AFTER-PUBLIC` blieb ein verschobenes Floating-Fenster (Database) an der neuen Position (x=1529/1300 statt 1014). **`ToolWindowManagerEx.setLayout(DesktopLayout)` stellt sie wieder her** (`AFTER-SETLAYOUT`: x=1014,y=1794 wie gemerkt).
3. **Rider:** Verhält sich wie IntelliJ, nichts Rider-Spezifisches aufgefallen.
4. **Interne API nutzbar:** `ToolWindowManagerEx.getInstanceEx(project).getLayout()/setLayout()`, `DesktopLayout.copy()/getInfos()`, `WindowInfoImpl` kompilieren gegen Rider 2026.3 und laufen im EDT ohne Fehler.

## Weitere Beobachtungen
- `getLayout().getInfos()` enthält auch Toolfenster, die im geöffneten Projekt **nicht registriert** sind (Gradle, Maven, Problems). `ToolWindowManager.getToolWindow(id)` liefert dafür `null`: der Überspringen-Pfad trägt.
- `floatingBounds` ist nur bei `FLOATING`/`WINDOWED` gesetzt, sonst `null`.
- `order` kann `-1` sein (Database.Output). `weight` ist Anteil (0 bis 1) der Breite/Höhe.
- IDs sind keine Anzeigenamen (z. B. „Explorer“ = `Project`).

## Entscheidung für `LayoutApply`
Anwenden über **`setLayout`**: Kopie des aktuellen Layouts (`getLayout().copy()`) holen, pro Profileintrag, dessen ID in der Kopie existiert, die Felder von `WindowInfoImpl` setzen (`anchor`, `isSplit`, `type`, `isAutoHide`, `isVisible`, `order`, `weight`, `floatingBounds`), dann `setLayout(kopie)`. Einträge im Profil ohne Gegenstück werden übersprungen, Fenster, die das Profil nicht kennt, bleiben unverändert.
Ungeprüft: Ob `setLayout` mit **geänderten** Infos (statt unveränderter Kopie, wie im Spike) alles sauber umsetzt. Das wird in Task 6 manuell am Beispiel „Project links oben, Commit links unten“ geprüft; fällt es durch, Rückfall auf öffentliche API plus `setLayout` nur für Bounds.
Risiko: `DesktopLayout`/`WindowInfoImpl` sind interne Klassen und können sich zwischen Versionen ändern (`verifyPlugin` in Task 8).
