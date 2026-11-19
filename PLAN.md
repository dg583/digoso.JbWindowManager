# LayoutManager – Plan & Kontext

Stand: 2026-11-19 · Status: Funktion umgesetzt und in Rider 2026.3 (Sandbox) manuell bestätigt: Profile speichern, wechseln, umbenennen, löschen. Offen: Abschlusstests (Task 8), Review, echte Plugin-ID/Vendor.

Weitere Erkenntnisse und Recherche-Ergebnisse liegen nach Thema sortiert unter `docs/wissen/` (siehe `CLAUDE.md`). Design: `docs/superpowers/specs/2026-11-19-profile-design.md`, Umsetzungsplan: `docs/superpowers/plans/2026-11-19-toolwindow-profile.md`.

## Ziel
JetBrains-Plugin für **Rider**: Die Anordnung der Toolfenster als benanntes **Profil** speichern, mehrere Profile verwalten (anlegen, wechseln, umbenennen, löschen). Riders `Window > Layouts` reicht dafür nicht, weil man dort keine Profile verwalten kann.

## Entscheidungen
- **Kotlin + Gradle** (IntelliJ Platform Gradle Plugin). Ein reines .NET-Plugin ist nicht möglich: Layouts liegen im JVM-Frontend der IDE (z. B. `ToolWindowManager`). Das .NET-/ReSharper-Backend hat darauf keinen Zugriff.
- Kein .NET-Projekt, solange es keinen Zweck hat.
- Ziel-IDE: Rider 2026.3 (`sinceBuild = 263`), JDK 21. Versionen bestätigt: `docs/wissen/build/gradle-setup.md`.
- Entwicklungs-IDE: IntelliJ IDEA. Rider zum Testen über `runIde` (Sandbox).
- **Profil = Toolfenster-Zustand:** je Toolfenster Seite (left/right/bottom/top), Split-Position (z. B. left/top vs. left/bottom), offen/zu, ViewMode (`type` + `autoHide`), Größe/Reihenfolge, bei Float/Window die Position. Keine Editor-Splits, keine offenen Dateien.
- **Ablage:** global, JSON, `<IDE-Config>/layoutmanager/profiles.json`.
- **Bedienung:** Untermenü `Window > Profile` (Profile wechseln, „Speichern als…“, „Verwalten…“ mit Umbenennen/Löschen).
- Fehlende Toolfenster beim Anwenden werden übersprungen.
- **Anwenden über `ToolWindowManagerEx.setLayout`** (interne API `DesktopLayout`): Nur so wird die Fensterposition wiederhergestellt (Spike: `docs/wissen/intellij-plattform/spike-ergebnis.md`). Risiko: interne API kann sich zwischen Versionen ändern. `verifyPlugin` meldet sie (5 Internal-API-Usages) und ist darauf eingestellt, nur echte Brüche als Fehler zu werten.

## Struktur
```
build.gradle.kts / settings.gradle.kts / gradle.properties
src/main/kotlin/com/example/layoutmanager/
  model/Profile.kt            Datenmodell (Profile, ToolWindowState, Bounds)
  store/ProfileStore.kt       JSON lesen/schreiben, Verwaltung (reine Logik)
  store/ProfileService.kt     App-Service mit globalem Pfad
  layout/LayoutCapture.kt     Ist-Zustand lesen
  layout/LayoutApply.kt       Profil anwenden
  actions/                    Menü, Speichern, Verwalten-Dialog
src/main/resources/META-INF/plugin.xml
src/test/kotlin/...           Tests für Store/JSON und Parsing
```
Build und Test: `gradlew test buildPlugin` · Sandbox: `gradlew runIde` · Prüfung: `gradlew verifyPlugin`.
Lokale Rider statt Download: `riderPath` in `~/.gradle/gradle.properties`.

## Offen
- **Echte Plugin-ID, Paketname und Vendor:** Platzhalter `com.example.layoutmanager` / `TBD`. `verifyPlugin` stummschaltet die Meldung `ForbiddenPluginIdPrefix`, bis das geklärt ist.
- Nicht im Umfang, bei Bedarf später: Export/Import, Shortcuts, Teilen im Team, Editor-Splits.

## Arbeitsweise
- Sprache der Zusammenarbeit: Deutsch.
- Ponytail-Modus aktiv: kleinste funktionierende Lösung, keine spekulativen Abstraktionen.
