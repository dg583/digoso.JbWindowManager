# Design: Toolfenster-Profile für Rider

Stand: 2026-11-19 · Status: **Entwurf, wartet auf Freigabe**
Grundlagen: `PLAN.md`, `docs/wissen/profile/umfang-und-ablage.md`, `docs/wissen/intellij-plattform/toolwindow-api-machbarkeit.md`

## Ziel
Der Nutzer speichert die aktuelle Anordnung der Toolfenster als benanntes **Profil**, verwaltet mehrere Profile und wechselt per Klick zwischen ihnen. `Window > Layouts` kann das nicht (keine Profilverwaltung).

## Umfang
**Im Profil, je Toolfenster:** Seite (left/right/bottom/top), Split (oben/unten), offen/zu, ViewMode (`type` + `autoHide`), Position/Größe bei abgedockten Fenstern (`floatingBounds`), Größe/Reihenfolge der angedockten Fenster (`weight`, `order`), soweit der Spike sie als wiederherstellbar bestätigt.
**Nicht im Umfang:** Editor-Splits, offene Dateien, Hauptfenstergröße, Export/Import, Shortcuts, Teilen im Team.

## Bedienung
Untermenü **`Window > Profile`**:
- Liste der gespeicherten Profile: Klick wendet das Profil an.
- **Speichern als…**: Namen eingeben, aktuellen Zustand speichern. Existiert der Name schon, wird nach Bestätigung überschrieben.
- **Verwalten…**: Dialog mit Liste, Aktionen **Umbenennen** und **Löschen**.

## Ablage
Eine Datei `<IDE-Config>/layoutmanager/profiles.json` (global, über `PathManager.getConfigPath()`). Inhalt:

```json
{
  "version": 1,
  "profiles": [
    {
      "name": "Debugging",
      "toolWindows": [
        { "id": "Explorer", "anchor": "left", "split": false, "type": "DOCKED",
          "autoHide": false, "visible": true, "order": 0, "weight": 0.25,
          "bounds": null }
      ]
    }
  ]
}
```
`bounds` (`x, y, width, height`) nur bei `FLOATING`/`WINDOWED`. Das Format ist bewusst vom internen `WindowInfoImpl` entkoppelt.

## Bausteine
| Baustein | Aufgabe |
|---|---|
| `Profile`, `ToolWindowState` | Datenklassen für das JSON-Modell |
| `ProfileStore` | Application-Service: laden, speichern, hinzufügen/ersetzen, umbenennen, löschen (liest/schreibt die JSON-Datei) |
| `LayoutCapture` | Liest den Ist-Zustand aller Toolfenster des Projekts (Weg laut Spike: `ToolWindowManagerEx.getLayout().getInfos()`) |
| `LayoutApply` | Wendet ein Profil an: pro vorhandenem Toolfenster Seite → Split → ViewMode → Sichtbarkeit setzen. Unbekannte IDs werden **übersprungen**. |
| Aktionen | `ProfilesActionGroup` (dynamische Kinder unter `Window > Profile`), `SaveProfileAction`, `ManageProfilesAction` (+ Dialog) |

JSON per Gson (von der Plattform mitgeliefert, Verfügbarkeit im Rider-Classpath **ungeprüft**; sonst Alternative wählen). Alle Zugriffe auf Toolfenster im EDT.

## Fehlerbehandlung
- Ungültige oder nicht lesbare `profiles.json`: nicht überschreiben, Fehler loggen, dem Nutzer eine Benachrichtigung zeigen und mit leerer Liste weiterarbeiten.
- Schreiben atomar (temporäre Datei, dann umbenennen), damit kein halber Stand bleibt.
- Anwenden ist best effort: ein Toolfenster, das sich nicht setzen lässt, bricht das Profil nicht ab (Fehler loggen, weiter).

## Tests
- Automatisch: ein Test für JSON-Roundtrip und `ProfileStore` (hinzufügen, ersetzen, umbenennen, löschen, fehlerhafte Datei).
- Manuell in `runIde`: Speichern und Anwenden mit dem Beispiel „Explorer left/top, Commit left/bottom“, offen/zu, Float/Window mit Position.

## Spike (vor der Umsetzung)
Klärt, was Anwenden wirklich leisten kann (siehe Machbarkeitsdoku):
1. Ergibt die Reihenfolge `setAnchor` → `setSplitMode` → `setType` → `show/hide` das gewünschte Ergebnis?
2. Ist `floatingBounds` wiederherstellbar (öffentlich oder nur über `DesktopLayout` + `setLayout`)?
3. Verhält sich Rider wie IntelliJ?
4. Sind `ToolWindowManagerEx`/`DesktopLayout` gegen Rider 2026.3 nutzbar?
Ergebnis steuert, ob `LayoutApply` rein öffentliche API nutzt oder zusätzlich `setLayout`. Eingeschränkte Wiederherstellung (z. B. ohne Fensterposition) wird dann bewusst dokumentiert.

## Voraussetzungen
JDK 21 und Gradle-Wrapper fehlen noch (siehe `PLAN.md`). Plugin-ID, Paketname und Vendor sind noch Platzhalter.
