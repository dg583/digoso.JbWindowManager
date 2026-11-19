# CLAUDE.md

JetBrains-Plugin für Rider (Kotlin + Gradle), das IDE-Layouts als **Profile** verwaltet. Kontext und Entscheidungen: `PLAN.md`.

## Arbeitsweise
- Sprache der Zusammenarbeit: Deutsch.
- Kleinste funktionierende Lösung, keine spekulativen Abstraktionen.
- Kein Code, bevor das Design in `docs/superpowers/specs/` freigegeben ist.

## Wissensablage (Pflicht)
Neue Erkenntnisse **sofort** im Ordner `docs/wissen/` festhalten, **nach Thema** gegliedert. Nicht in Chat-Antworten, `PLAN.md` oder Commit-Messages versanden lassen.

Struktur:
```
docs/wissen/
  <thema>/            # ein Ordner je Thema, kebab-case
    <unterthema>.md   # eine Datei je abgegrenzter Erkenntnis
  README.md           # Index: ein Eintrag je Datei (Link + eine Zeile)
```

Themenordner (bei Bedarf neue anlegen, vorher prüfen, ob ein bestehender passt):
- `intellij-plattform/` – APIs und Verhalten der IntelliJ Platform (ToolWindowManager, WindowManager, Layout-Persistenz, Services, Actions)
- `rider/` – Rider-Besonderheiten (Frontend/Backend-Split, `Window > Layouts`)
- `profile/` – Design und Entscheidungen zur Profilverwaltung (Datenmodell, Ablage, Format, Bedienung)
- `build/` – Gradle, IntelliJ Platform Gradle Plugin, Versionen, `runIde`, Signierung/Veröffentlichung
- `testen/` – Teststrategie, Sandbox-Erkenntnisse

Regeln:
1. **Vor dem Schreiben** `docs/wissen/README.md` und den passenden Themenordner prüfen. Bestehende Datei aktualisieren statt Duplikat anlegen.
2. Pro Datei: Überschrift, Erkenntnis in wenigen Sätzen, **Quelle/Beleg** (Doku-Link, Klasse, Test), Datum (`YYYY-MM-DD`). Unsicheres als „ungeprüft“ markieren.
3. Neue oder geänderte Datei → Eintrag in `docs/wissen/README.md` ergänzen.
4. Falsch gewordene Erkenntnisse korrigieren oder löschen, nicht stehen lassen.
5. `PLAN.md` bleibt der knappe Überblick (Ziel, Entscheidungen, nächste Schritte) und verweist auf `docs/wissen/`. Details gehören dorthin.
6. Nur festhalten, was nicht schon aus Code oder Git-Historie ersichtlich ist.
