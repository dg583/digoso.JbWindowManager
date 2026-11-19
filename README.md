# LayoutManager

JetBrains-Plugin für Rider (Kotlin): Die Anordnung der Toolfenster als benannte **Profile** speichern, verwalten und wechseln.

## Bedienung
Menü **`Window > Profile`**:
- Profil in der Liste anklicken: Anordnung wird angewendet.
- **Speichern als…**: aktuelle Anordnung unter einem Namen speichern (vorhandener Name wird nach Rückfrage überschrieben).
- **Verwalten…**: Profile umbenennen oder löschen.

Ein Profil enthält je Toolfenster Seite, Split-Position, offen/zu, ViewMode, Größe und bei Float/Window die Fensterposition. Gespeichert wird global in `<IDE-Config>/layoutmanager/profiles.json`.

## Entwicklung
- `gradlew test buildPlugin`: Tests und Plugin-Zip (`build/distributions`)
- `gradlew runIde`: Sandbox-Rider mit dem Plugin
- `gradlew verifyPlugin`: Kompatibilitätsprüfung gegen Rider 2026.3
- Lokale Rider statt Download: `riderPath=<Pfad zur Rider-Installation>` in `~/.gradle/gradle.properties`
- Mehr: `PLAN.md`, `docs/wissen/`
