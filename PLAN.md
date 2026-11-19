# LayoutManager – Plan & Kontext

Stand: 2026-11-19 · Status: Projektgerüst steht, Kernanforderung geklärt (Profilverwaltung), Spezifikation läuft noch (Brainstorming), kein Code.

Weitere Erkenntnisse und Recherche-Ergebnisse liegen nach Thema sortiert unter `docs/wissen/` (siehe `CLAUDE.md`).

## Ziel
JetBrains-Plugin für **Rider**: Das aktuelle IDE-Layout (Toolfenster, Docking, Größen, ggf. Editor-Splits) unter einem **Namen speichern**. Es sollen **mehrere Layouts** speicherbar sein, und gespeicherte Layouts sollen **wieder geladen** werden können.

## Entscheidungen
- **Kotlin + Gradle** (IntelliJ Platform Gradle Plugin). Ein reines .NET-Plugin ist nicht möglich: Layouts liegen im JVM-Frontend der IDE (z. B. `ToolWindowManager`). Das .NET-/ReSharper-Backend hat darauf keinen Zugriff.
- Kein .NET-Projekt, solange es keinen Zweck hat (z. B. Solution-/Code-Analyse-Wissen im Backend).
- Ziel-IDE: Rider 2026.3 (`sinceBuild = 263`), JDK 21.
- Entwicklungs-IDE: IntelliJ IDEA (Community reicht). Rider zum Testen über `runIde` (Sandbox).
- Git-Repo, Branch `main`. `.gitignore` deckt .NET, Gradle/Kotlin, JetBrains und VS Code ab.

## Kernanforderung (geklärt 2026-11-19)
Riders `Window > Layouts` reicht nicht: Dort lassen sich nur einzelne Layouts speichern/laden, aber **keine Profile verwalten**. Das Plugin soll daher **Profile verwalten** können (anlegen, benennen, umbenennen, löschen, wechseln; ggf. duplizieren, exportieren/importieren). Details der Profilverwaltung (Umfang, Ablage, Bedienung) sind noch zu spezifizieren.

## Geklärt: Umfang, Ablage, Bedienung (Details: `docs/wissen/profile/umfang-und-ablage.md`)
- **Profil = Toolfenster-Zustand:** je Toolfenster Seite (left/right/bottom/top), Split-Position (z. B. left/top vs. left/bottom), offen/zu, ViewMode, bei abgedockten Fenstern Position. Keine Editor-Splits/offenen Dateien.
- **Ablage:** global.
- **Bedienung:** Einstieg in einem bestehenden Menü, z. B. `Window`.

## Offen / zu klären
- Teilen im Team (Export/Import), Shortcuts: zunächst nicht nötig.

## Entschieden
- Format **JSON**, Ablage global.
- Fehlende Toolfenster beim Laden **überspringen**.
- Bedienung: Untermenü **`Window > Profile`** (Profile wechseln, „Speichern als…“, „Verwalten…“ mit Umbenennen/Löschen).

## Machbarkeit (Details: `docs/wissen/intellij-plattform/toolwindow-api-machbarkeit.md`)
- Seite, Split, ViewMode, Sichtbarkeit: lesen und setzen über öffentliche `ToolWindow`-API.
- Fensterposition, Größe, Reihenfolge: nur über interne `DesktopLayout`/`ToolWindowManagerEx` (Risiko: nicht stabil).
- Alles per Quellcode belegt (Branch 252 und 263, die genutzten Signaturen sind identisch), **nicht in der IDE getestet**. Nächster Schritt: Spike in `runIde`.
- Echte IDs/Platzhalter: Paketname `com.example.layoutmanager`, Plugin-ID, Vendor `TBD`.

## Zu prüfen beim ersten Sync
- Versionen bestätigt (Details: `docs/wissen/build/gradle-setup.md`): Gradle-Plugin 2.20.1, Kotlin 2.4.0, Gradle 9.8.0, Rider 2026.3, JDK 21.
- Gradle-Wrapper ist erzeugt (Gradle 9.8.0), JDK 21 (Temurin) liegt in `~/.jdks`. `./gradlew test buildPlugin` läuft grün (lokale Rider über `riderPath` in `~/.gradle/gradle.properties`).

## Struktur
```
build.gradle.kts / settings.gradle.kts / gradle.properties
src/main/kotlin/com/example/layoutmanager/   (leer)
src/main/resources/META-INF/plugin.xml       (Minimal-Manifest)
```
Build: `gradlew buildPlugin` · Sandbox-Test: `gradlew runIde`

## Nächste Schritte
1. Offene Fragen klären, Ansätze abwägen, Design-Doc schreiben (`docs/superpowers/specs/YYYY-MM-DD-<topic>-design.md`) und freigeben lassen.
2. Danach Implementierungsplan (superpowers:writing-plans).
3. Erst dann Code. Das Gerüst wurde auf Wunsch des Nutzers vorab angelegt, das Design ist aber noch nicht freigegeben.

## Arbeitsweise
- Sprache der Zusammenarbeit: Deutsch.
- Ponytail-Modus aktiv: kleinste funktionierende Lösung, keine spekulativen Abstraktionen.
