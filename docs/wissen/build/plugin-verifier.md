# Plugin Verifier (`verifyPlugin`)

Stand: 2026-11-19 · Beleg: Lauf gegen die lokale Rider 2026.3 (Build 263.7307.258), Plugin Verifier 1.411.

## Ergebnis
- **Keine Kompatibilitätsprobleme.**
- **5 Internal-API-Usages:** `DesktopLayout` (Klasse, `getInfos()`, `copy()`), genutzt in `LayoutCapture` und `LayoutApply`. Bewusst akzeptiert, siehe `docs/wissen/intellij-plattform/spike-ergebnis.md` (nur `setLayout` stellt `floatingBounds` wieder her).
- Dynamic Plugin Eligibility: Plugin kann vermutlich ohne IDE-Neustart aktiviert/deaktiviert werden.

## Konfiguration in `build.gradle.kts`
- `failureLevel` = `COMPATIBILITY_PROBLEMS` und `INVALID_PLUGIN`: interne API lässt die Prüfung nicht scheitern, echte Brüche schon.
- `freeArgs = -mute ForbiddenPluginIdPrefix`: Platzhalter-ID `com.example.*` ist laut Verifier nicht erlaubt. Entfernen, sobald die echte Plugin-ID gesetzt ist.
- Prüfziel: dieselbe Plattform wie der Build (`riderPath` oder Download).

## Eigenheiten
- Die Plugin-Beschreibung muss mit lateinischen Zeichen beginnen und mindestens 40 Zeichen lang sein (sonst „Invalid plugin descriptor 'description'“), daher englisch.
- Der Lauf lädt ca. 370 MB Abhängigkeiten (Plugins, die Rider-Module mitbringt) in den Cache und dauert etwa 1,5 Minuten.
- Parallel zu einer laufenden `runIde`-Sandbox brach ein Gradle-Lauf einmal ab („daemon disappeared“); danach lief derselbe Befehl durch.
