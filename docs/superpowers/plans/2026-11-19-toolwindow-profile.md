# Toolfenster-Profile Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rider-Plugin, das die Toolfenster-Anordnung als benannte Profile speichert, verwaltet (umbenennen, löschen) und per `Window > Profile` anwendet.

**Architecture:** Eigenes JSON-Modell (`Profile`/`ToolWindowState`) in einer globalen Datei, verwaltet von `ProfileStore` (reine Logik, ohne IDE-Abhängigkeit, daher unit-testbar). `LayoutCapture` liest den Ist-Zustand, `LayoutApply` wendet ihn an (Weg laut Spike, Task 2). Menüaktionen hängen als dynamische Gruppe an `Window`.

**Tech Stack:** Kotlin 2.4.0, Gradle mit IntelliJ Platform Gradle Plugin 2.20.1, Rider 2026.3 (`sinceBuild = 263`), JDK 21, Gson (JSON), JUnit 4.

**Spec:** `docs/superpowers/specs/2026-11-19-profile-design.md` (Hintergrund: `docs/wissen/intellij-plattform/toolwindow-api-machbarkeit.md`)

## Global Constraints
- Paket `com.example.layoutmanager` (Platzhalter bleibt), Plugin-ID `com.example.layoutmanager`.
- Datei: `<PathManager.getConfigPath()>/layoutmanager/profiles.json`, global, Format `{"version":1,"profiles":[…]}`.
- Menü: Untermenü `Window > Profile` mit Profilliste, „Speichern als…“, „Verwalten…“ (Umbenennen, Löschen).
- Fehlende Toolfenster beim Anwenden **überspringen**; Anwenden ist best effort (ein Fehler bricht nicht ab).
- Alle Toolfenster-Zugriffe im EDT.
- Nicht im Umfang: Editor-Splits, offene Dateien, Hauptfenstergröße, Export/Import, Shortcuts.
- Ponytail: kleinste Lösung, keine spekulativen Abstraktionen. Commits nur nach Rückfrage beim Nutzer.
- Kein Zugriff auf Versionen aus dem Gedächtnis ohne Prüfung: Plugin 2.10.2, Kotlin 2.1.20, Rider 2026.3 sind **ungetestet** und werden in Task 1 bestätigt oder korrigiert.

## Review Focus
1. Profil enthält ein Toolfenster, das im Projekt nicht existiert → wird übersprungen, Rest wird angewendet (Task 5).
2. „Speichern als…“ mit leerem oder nur aus Leerzeichen bestehendem Namen → abgelehnt (Task 4).
3. Umbenennen auf einen bereits vorhandenen Namen (auch abweichende Groß-/Kleinschreibung) → Fehler, nichts geht verloren (Task 4).
4. `profiles.json` leer, kaputt oder halb geschrieben → nicht überschreiben, leere Liste, Fehlermeldung (Task 4).
5. Datei mit `version` > 1 oder unbekanntem `anchor`/`type`-Wert → Datei bleibt unangetastet bzw. der betroffene Eintrag wird beim Anwenden übersprungen (Task 3 und 5).

---

## Dateistruktur
| Datei | Aufgabe |
|---|---|
| `build.gradle.kts` (ändern) | Test-Setup, ggf. Gson-Abhängigkeit |
| `src/main/kotlin/com/example/layoutmanager/model/Profile.kt` | `Profile`, `ToolWindowState`, `Bounds` |
| `.../store/ProfileStore.kt` | JSON lesen/schreiben, Verwaltung (reine Logik) |
| `.../store/ProfileService.kt` | App-Service, der den `ProfileStore` mit dem Config-Pfad bereitstellt |
| `.../layout/LayoutCapture.kt`, `LayoutApply.kt` | Ist-Zustand lesen, Profil anwenden |
| `.../actions/ProfilesActionGroup.kt`, `SaveProfileAction.kt`, `ManageProfilesAction.kt`, `ManageProfilesDialog.kt` | Menü und Dialoge |
| `src/main/resources/META-INF/plugin.xml` (ändern) | Registrierung der Aktionen |
| `src/test/kotlin/com/example/layoutmanager/store/ProfileStoreTest.kt` | Tests für Store und JSON |

---

### Task 1: Build-Setup und erster Lauf

**Files:**
- Modify: `build.gradle.kts`
- Create: Gradle-Wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/*`), `src/test/kotlin/com/example/layoutmanager/SmokeTest.kt`

**Interfaces:**
- Produces: lauffähiges `gradlew test`, `gradlew buildPlugin`, `gradlew runIde`.

- [ ] **Step 1: Voraussetzungen prüfen.** `java -version` muss 21 liefern, `gradle -v` muss Gradle melden. Fehlt eines, den Nutzer bitten, JDK 21 und Gradle zu installieren (z. B. über IntelliJ-Import oder `winget`). Ohne beides nicht weitermachen.
- [ ] **Step 2: Wrapper erzeugen:** `gradle wrapper`. Erwartet: `gradlew.bat` existiert.
- [ ] **Step 3: Testabhängigkeiten in `build.gradle.kts` ergänzen:** `testImplementation("junit:junit:4.13.2")` und in `dependencies { intellijPlatform { … } }` `testFramework(TestFrameworkType.Platform)` (Import `org.jetbrains.intellij.platform.gradle.TestFrameworkType`).
- [ ] **Step 4: Smoke-Test `SmokeTest.kt`:** `@Test fun buildRuns() { assertTrue(true) }` (JUnit 4).
- [ ] **Step 5: Lauf:** `./gradlew test buildPlugin`. Erwartet: `BUILD SUCCESSFUL`. Schlagen die ungetesteten Versionen fehl (Plugin 2.10.2, Kotlin 2.1.20, Rider 2026.3), die Fehlermeldung lesen, auf die tatsächlich verfügbaren Versionen korrigieren und `PLAN.md` anpassen.
- [ ] **Step 6: `./gradlew runIde` einmal starten.** Erwartet: Rider-Sandbox öffnet sich, Plugin „Layout Manager“ ist unter Plugins aufgeführt. Sandbox schließen.
- [ ] **Step 7: Ergebnis (bestätigte Versionen, Eigenheiten) in `docs/wissen/build/gradle-setup.md` festhalten und Index ergänzen.**
- [ ] **Step 8: Commit** (nur nach Rückfrage): `git add . && git commit -m "build: Gradle-Wrapper und Test-Setup"`

---

### Task 2: Spike – Anwenden und Lesen in Rider prüfen

Beantwortet die vier offenen Fragen aus dem Design, bevor die endgültigen Bausteine entstehen. Wegwerfcode, wird in Task 5 ersetzt.

**Files:**
- Create (temporär): `src/main/kotlin/com/example/layoutmanager/actions/SpikeAction.kt`
- Modify (temporär): `src/main/resources/META-INF/plugin.xml`
- Create: `docs/wissen/intellij-plattform/spike-ergebnis.md`

**Interfaces:**
- Produces: dokumentierte Antworten auf die Fragen 1–4 mit Beleg; Entscheidung, welcher Anwendungsweg in Task 5 verwendet wird.

- [ ] **Step 1: `SpikeAction : AnAction` anlegen**, in `plugin.xml` unter `Tools` registrieren (Text „Spike: Dump layout“). `actionPerformed` ruft in `invokeLater`/EDT `ToolWindowManagerEx.getInstanceEx(project).getLayout().getInfos()` und schreibt je Eintrag `id, anchor, isSplit, type, isAutoHide, isVisible, order, weight, sideWeight, floatingBounds` per `Logger` (INFO) in die Sandbox-`idea.log`. Kompiliert das nicht gegen Rider 2026.3 (Frage 4), Fehlermeldung festhalten und nur die öffentliche API (`ToolWindowManager.toolWindowIds`, `ToolWindow`-Getter) verwenden.
- [ ] **Step 2: Zweite Spike-Aktion „Spike: Apply“**, die für zwei fest verdrahtete IDs (z. B. `Explorer`, `Commit`, aus dem Dump übernehmen) per öffentlicher API `setAnchor` → `setSplitMode` → `setType` → `show/hide` setzt.
- [ ] **Step 3: In `runIde` prüfen:** Explorer auf left/top und Commit auf left/bottom legen, per Dump lesen, Layout manuell ändern, „Apply“ ausführen. Erwartet: Ausgangslage wird exakt erreicht. Notieren, in welcher Reihenfolge es funktioniert.
- [ ] **Step 4: Fensterposition prüfen:** Ein Toolfenster auf `FLOATING` und `WINDOWED` setzen, verschieben, Dump lesen (kommt `floatingBounds` an?), woanders hinverschieben, wiederherstellen. Funktioniert es nur über `getLayout().copy()` + Änderung + `setLayout`, das ebenfalls testen.
- [ ] **Step 5: Menü-ID prüfen:** Existiert die Action-Gruppe `WindowMenu` in Rider (Reiter `Window`)? Sonst die richtige ID per Tools > „Internal Actions“/Suche ermitteln.
- [ ] **Step 6: Ergebnis in `docs/wissen/intellij-plattform/spike-ergebnis.md`** schreiben: je Frage Antwort, Beleg (Logzeilen), gewählter Weg für `LayoutApply`, bekannte Einschränkungen. Machbarkeitsdoku und Design-Doc bei Abweichungen korrigieren, Index ergänzen.
- [ ] **Step 7: Rückmeldung an den Nutzer** bei Einschränkungen (z. B. Position nicht wiederherstellbar), bevor Task 5 beginnt. Spike-Aktionen und deren `plugin.xml`-Einträge bleiben bis Task 5 stehen.

---

### Task 3: Datenmodell und JSON

**Files:**
- Create: `src/main/kotlin/com/example/layoutmanager/model/Profile.kt`, `src/main/kotlin/com/example/layoutmanager/store/ProfileStore.kt` (hier zunächst nur `ProfileJson`)
- Test: `src/test/kotlin/com/example/layoutmanager/store/ProfileStoreTest.kt`

**Interfaces:**
- Produces:
  - `object ProfileJson { fun toJson(f: ProfileFile): String; fun fromJson(s: String): ProfileFile }`
  - `data class Bounds(val x: Int, val y: Int, val width: Int, val height: Int)`
  - `data class ToolWindowState(val id: String, val anchor: String, val split: Boolean, val type: String, val autoHide: Boolean, val visible: Boolean, val order: Int, val weight: Float, val bounds: Bounds?)` (`anchor` ∈ `left|right|bottom|top`, `type` ∈ `DOCKED|FLOATING|SLIDING|WINDOWED`, Strings, damit unbekannte Werte nicht beim Lesen scheitern)
  - `data class Profile(val name: String, val toolWindows: List<ToolWindowState>)`
  - `data class ProfileFile(val version: Int, val profiles: List<Profile>)`; Konstante `CURRENT_VERSION = 1`.

- [ ] **Step 1: Failing test `roundtripKeepsAllFields`:** Ein `ProfileFile(1, listOf(Profile("Debugging", listOf(ToolWindowState("Explorer","left",false,"DOCKED",false,true,0,0.25f,null), ToolWindowState("Commit","left",true,"FLOATING",false,false,1,0.3f,Bounds(10,20,300,400))))))` wird mit `ProfileJson.toJson` und `ProfileJson.fromJson` zurückgelesen und ist `assertEquals` zum Original.
- [ ] **Step 2: Test ausführen:** `./gradlew test --tests "*ProfileStoreTest.roundtripKeepsAllFields"`. Erwartet: FAIL (`ProfileJson` unbekannt).
- [ ] **Step 3: Datenklassen** wie oben in `Profile.kt` anlegen. In `ProfileStore.kt` (Task 4) liegt das `object ProfileJson { fun toJson(f: ProfileFile): String; fun fromJson(s: String): ProfileFile }`; hier vorerst in derselben Datei anlegen. Gson verwenden (`GsonBuilder().setPrettyPrinting()`). Ist `com.google.gson` nicht auflösbar, `implementation("com.google.code.gson:gson:2.11.0")` ergänzen und in `docs/wissen/build/` vermerken. `fromJson` wirft `IllegalArgumentException` bei leerem Text, ungültigem JSON oder `version > CURRENT_VERSION`.
- [ ] **Step 4: Tests ergänzen und ausführen:** `emptyTextIsRejected`, `invalidJsonIsRejected`, `newerVersionIsRejected` (`assertFailsWith<IllegalArgumentException>`).
- [ ] **Step 5: Alle Tests grün:** `./gradlew test`. Erwartet: PASS.
- [ ] **Step 6: Commit** (nur nach Rückfrage): `git add src && git commit -m "feat: Profilmodell und JSON"`

---

### Task 4: ProfileStore

**Files:**
- Create: `src/main/kotlin/com/example/layoutmanager/store/ProfileStore.kt` (um `ProfileStore` erweitern), `.../store/ProfileService.kt`
- Test: `src/test/kotlin/com/example/layoutmanager/store/ProfileStoreTest.kt`

**Interfaces:**
- Consumes: `ProfileFile`, `Profile`, `ProfileJson` (Task 3).
- Produces:
  - `class ProfileStore(private val file: java.nio.file.Path)`
  - `fun profiles(): List<Profile>`
  - `val loadError: String?` (`null`, wenn die Datei fehlt oder gültig ist)
  - `fun save(profile: Profile)` (neu oder ersetzen, Name vergleicht ohne Beachtung von Groß-/Kleinschreibung, trimmt den Namen)
  - `fun rename(oldName: String, newName: String)`
  - `fun delete(name: String)`
  - Fehler: `IllegalArgumentException` bei leerem Namen, doppeltem Zielnamen oder unbekanntem Namen; `IllegalStateException`, wenn `loadError != null` (Schreibzugriffe blockiert).
  - `@Service(Service.Level.APP) class ProfileService { val store: ProfileStore }` (Pfad `Path.of(PathManager.getConfigPath(), "layoutmanager", "profiles.json")`).

- [ ] **Step 1: Failing tests** (`@get:Rule TemporaryFolder`, Store auf `tmp/profiles.json`):
  - `missingFileGivesEmptyList` → `profiles().isEmpty()`, `loadError == null`.
  - `saveCreatesFileAndReadsBack` → nach `save(p)` liefert ein **neuer** `ProfileStore` auf derselben Datei `listOf(p)`.
  - `saveSameNameReplaces` (auch `"debug"` vs. `"Debug"`) → genau ein Profil, neuer Inhalt.
  - `blankNameIsRejected` → `save(Profile("  ", emptyList()))` wirft `IllegalArgumentException`.
  - `renameToExistingNameFails` → wirft, beide Profile unverändert.
  - `renameAndDeleteWork`.
  - `corruptFileIsNotOverwritten` → Datei mit `"{kaputt"`; `loadError != null`, `profiles()` leer, `save(...)` wirft `IllegalStateException`, Dateiinhalt unverändert.
  - `newerVersionFileIsNotOverwritten` → analog mit `{"version":2,"profiles":[]}`.
- [ ] **Step 2: Tests ausführen.** Erwartet: FAIL (`ProfileStore` unbekannt).
- [ ] **Step 3: `ProfileStore` implementieren.** Beim ersten Zugriff lesen (fehlende Datei = leer; Lesefehler oder `fromJson`-Exception → `loadError` setzen). Schreiben atomar: in `file.resolveSibling("profiles.json.tmp")` schreiben, dann `Files.move(tmp, file, REPLACE_EXISTING, ATOMIC_MOVE)`; Elternordner vorher anlegen.
- [ ] **Step 4: `ProfileService` anlegen** (nur Pfadverdrahtung, keine Logik); in `plugin.xml` `<applicationService serviceImplementation="com.example.layoutmanager.store.ProfileService"/>` unter `<extensions defaultExtensionNs="com.intellij">`.
- [ ] **Step 5: Tests ausführen.** Erwartet: PASS.
- [ ] **Step 6: Commit** (nur nach Rückfrage): `git add src && git commit -m "feat: ProfileStore"`

---

### Task 5: LayoutCapture und LayoutApply

Abhängig vom Ergebnis des Spikes (Task 2).

**Files:**
- Create: `src/main/kotlin/com/example/layoutmanager/layout/LayoutCapture.kt`, `.../layout/LayoutApply.kt`
- Delete: `SpikeAction.kt` und dessen `plugin.xml`-Einträge

**Interfaces:**
- Consumes: `Profile`, `ToolWindowState`, `Bounds` (Task 3); Spike-Ergebnis `docs/wissen/intellij-plattform/spike-ergebnis.md`.
- Produces:
  - `fun captureProfile(project: Project, name: String): Profile` (EDT; alle registrierten Toolfenster des Projekts, Zustand laut Feldern aus Task 3)
  - `fun applyProfile(project: Project, profile: Profile)` (EDT; pro `ToolWindowState` `ToolWindowManager.getToolWindow(id)`; `null` → überspringen; unbekannter `anchor`/`type`-String → diesen Eintrag überspringen; Reihenfolge der Setter und Behandlung von `bounds`/`weight`/`order` laut Spike; jede Exception pro Toolfenster wird geloggt und übersprungen)

- [ ] **Step 1: Pure Hilfsfunktionen mit Test.** In `LayoutApply.kt` `internal fun parseAnchor(s: String): ToolWindowAnchor?` und `internal fun parseType(s: String): ToolWindowType?` (`null` bei unbekanntem Wert). Test in `src/test/.../layout/ParseTest.kt`: `"left"`→`LEFT`, `"bogus"`→`null`, `"FLOATING"`→`FLOATING`, `"x"`→`null`.
- [ ] **Step 2: Tests ausführen:** erst FAIL, nach Implementierung PASS (`./gradlew test --tests "*ParseTest"`).
- [ ] **Step 3: `captureProfile` implementieren** wie in Interfaces beschrieben, Weg laut Spike.
- [ ] **Step 4: `applyProfile` implementieren** wie in Interfaces beschrieben, Weg laut Spike. Einschränkungen aus dem Spike als Kommentar `// ponytail:` mit Obergrenze festhalten.
- [ ] **Step 5: Spike-Code entfernen,** `./gradlew build` ausführen. Erwartet: `BUILD SUCCESSFUL`.
- [ ] **Step 6: Commit** (nur nach Rückfrage): `git add src && git commit -m "feat: LayoutCapture und LayoutApply"`

---

### Task 6: Menü `Window > Profile` mit Speichern und Anwenden

**Files:**
- Create: `.../actions/ProfilesActionGroup.kt`, `.../actions/SaveProfileAction.kt`
- Modify: `src/main/resources/META-INF/plugin.xml`

**Interfaces:**
- Consumes: `ProfileService.store` (Task 4), `captureProfile`, `applyProfile` (Task 5); Menü-ID aus dem Spike (vermutlich `WindowMenu`).
- Produces: Gruppe `LayoutManager.Profiles` (Popup „Profile“), `SaveProfileAction`, Platzhalter-ID `LayoutManager.ManageProfiles` für Task 7.

- [ ] **Step 1: `ProfilesActionGroup : ActionGroup`** (`getActionUpdateThread() = ActionUpdateThread.BGT`). `getChildren(e)` liefert je `store.profiles()` eine `AnAction` mit dem Profilnamen als Text (führt `applyProfile` im EDT aus), danach `Separator`, `SaveProfileAction`, `ManageProfilesAction`. Ist `store.loadError != null`, zusätzlich ein deaktivierter Eintrag „Profile nicht lesbar (siehe Log)“.
- [ ] **Step 2: `SaveProfileAction`:** `Messages.showInputDialog` für den Namen; Name trimmen; existiert der Name (ohne Beachtung der Schreibweise), `Messages.showYesNoDialog` „Überschreiben?“; dann `store.save(captureProfile(project, name))`. `IllegalArgumentException`/`IllegalStateException` als Fehlerdialog anzeigen.
- [ ] **Step 3: Registrierung in `plugin.xml`:** Gruppe mit `popup="true"`, `text="Profile"`, `class="…ProfilesActionGroup"`, `<add-to-group group-id="<Menü-ID aus Spike>" anchor="last"/>`.
- [ ] **Step 4: Manuell in `runIde`:** `Window > Profile > Speichern als…` mit Namen „A“, Layout ändern, Namen „B“ speichern, in der Liste zwischen A und B wechseln. Erwartet: jeweiliges Layout wird hergestellt. Leeren Namen versuchen: Fehlerdialog. Beispiel „Explorer left/top, Commit left/bottom, offen/zu“ prüfen.
- [ ] **Step 5: Commit** (nur nach Rückfrage): `git add src && git commit -m "feat: Menü Window > Profile"`

---

### Task 7: Verwalten-Dialog (Umbenennen, Löschen)

**Files:**
- Create: `.../actions/ManageProfilesAction.kt`, `.../actions/ManageProfilesDialog.kt`
- Modify: `src/main/resources/META-INF/plugin.xml` (nur falls Task 6 die Aktion noch nicht registriert hat)

**Interfaces:**
- Consumes: `ProfileStore.profiles/rename/delete` (Task 4).
- Produces: `ManageProfilesAction`, `ManageProfilesDialog(project: Project) : DialogWrapper`.

- [ ] **Step 1: `ManageProfilesDialog`:** `JBList` der Profilnamen, Schaltflächen „Umbenennen…“ (`Messages.showInputDialog`, vorbelegt mit altem Namen) und „Löschen“ (nach `Messages.showYesNoDialog`), Liste aktualisiert sich nach jeder Änderung. Fehler des Stores (`IllegalArgumentException`, `IllegalStateException`) als Fehlerdialog. Schaltflächen deaktiviert, wenn nichts ausgewählt ist.
- [ ] **Step 2: `ManageProfilesAction`** öffnet den Dialog, ist nur aktiv, wenn ein Projekt offen ist.
- [ ] **Step 3: Manuell in `runIde`:** Profil „A“ in „B“ umbenennen (wenn „B“ existiert: Fehlerdialog, beide unverändert; auch „b“), Profil löschen, Rider neu starten und prüfen, dass Änderungen erhalten bleiben (`profiles.json` im Config-Ordner ansehen).
- [ ] **Step 4: Commit** (nur nach Rückfrage): `git add src && git commit -m "feat: Profile verwalten"`

---

### Task 8: Abschlussprüfung und Doku

**Files:**
- Modify: `PLAN.md`, `README.md`, `docs/wissen/README.md`

- [ ] **Step 1: Gesamtlauf:** `./gradlew test buildPlugin`. Erwartet: `BUILD SUCCESSFUL`, `build/distributions/*.zip` vorhanden.
- [ ] **Step 2: Manuelle Review-Focus-Checks in `runIde`:** (1) Profil in anderem Projekt anwenden, in dem ein gespeichertes Toolfenster fehlt → Rest wird angewendet; (4) `profiles.json` mit `{kaputt` ersetzen → Menü zeigt „nicht lesbar“, Datei bleibt unverändert; (5) `"anchor":"bogus"` in einem Eintrag → dieser wird übersprungen.
- [ ] **Step 3: `./gradlew verifyPlugin`** (falls verfügbar) ausführen und Meldungen zu internen APIs (`DesktopLayout`/`ToolWindowManagerEx`) im Wissensordner `docs/wissen/intellij-plattform/` festhalten.
- [ ] **Step 4: `PLAN.md` auf den Ist-Stand bringen** (Status, offene Punkte, Plugin-ID/Vendor-Platzhalter), `README.md` kurz um Bedienung ergänzen.
- [ ] **Step 5: Commit** (nur nach Rückfrage): `git add . && git commit -m "docs: Abschluss"`
