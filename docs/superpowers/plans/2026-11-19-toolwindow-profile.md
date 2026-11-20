# Tool Window Profiles Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rider plugin that saves the tool window arrangement as named profiles, manages them (rename, delete) and applies them via `Window > Profiles`.

**Architecture:** Own JSON model (`Profile`/`ToolWindowState`) in a global file, managed by `ProfileStore` (pure logic, no IDE dependency, hence unit-testable). `LayoutCapture` reads the current state, `LayoutApply` applies it (route according to the spike, task 2). Menu actions hang off `Window` as a dynamic group.

**Tech Stack:** Kotlin 2.4.0, Gradle with IntelliJ Platform Gradle Plugin 2.20.1, Rider 2026.3 (`sinceBuild = 263`), JDK 21, Gson (JSON), JUnit 4.

**Spec:** `docs/superpowers/specs/2026-11-19-profile-design.md` (background: `docs/knowledge/intellij-platform/toolwindow-api-feasibility.md`)

## Global Constraints
- Package `com.digoso.jbwindowmanager` (placeholder stays), plugin id `com.digoso.jbwindowmanager`.
- File: `<PathManager.getConfigPath()>/digoso.JbWindowManager/profiles.json`, global, format `{"version":1,"profiles":[…]}`.
- Menu: submenu `Window > Profiles` with profile list, "Save as…", "Manage…" (rename, delete).
- **Skip** missing tool windows when applying; applying is best effort (one error does not abort).
- All tool window access on the EDT.
- Out of scope: editor splits, open files, main window size, export/import, shortcuts.
- Ponytail: smallest solution, no speculative abstractions. Commits only after asking the user.
- Do not use versions from memory without checking: plugin 2.10.2, Kotlin 2.1.20, Rider 2026.3 are **untested** and get confirmed or corrected in task 1.

## Review Focus
1. Profile contains a tool window that does not exist in the project → it is skipped, the rest is applied (task 5).
2. "Save as…" with an empty or whitespace-only name → rejected (task 4).
3. Rename to an already existing name (also with different letter case) → error, nothing is lost (task 4).
4. `profiles.json` empty, corrupt or half-written → not overwritten, empty list, error message (task 4).
5. File with `version` > 1 or an unknown `anchor`/`type` value → file stays untouched, or the affected entry is skipped when applying (tasks 3 and 5).

---

## File structure
| File | Responsibility |
|---|---|
| `build.gradle.kts` (modify) | test setup, Gson dependency if needed |
| `src/main/kotlin/com/digoso/jbwindowmanager/model/Profile.kt` | `Profile`, `ToolWindowState`, `Bounds` |
| `.../store/ProfileStore.kt` | read/write JSON, management (pure logic) |
| `.../store/ProfileService.kt` | app service providing the `ProfileStore` with the config path |
| `.../layout/LayoutCapture.kt`, `LayoutApply.kt` | read the current state, apply a profile |
| `.../actions/ProfilesActionGroup.kt`, `SaveProfileAction.kt`, `ManageProfilesAction.kt`, `ManageProfilesDialog.kt` | menu and dialogs |
| `src/main/resources/META-INF/plugin.xml` (modify) | action registration |
| `src/test/kotlin/com/digoso/jbwindowmanager/store/ProfileStoreTest.kt` | tests for store and JSON |

---

### Task 1: Build setup and first run

**Files:**
- Modify: `build.gradle.kts`
- Create: Gradle wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/*`), `src/test/kotlin/com/digoso/jbwindowmanager/SmokeTest.kt`

**Interfaces:**
- Produces: working `gradlew test`, `gradlew buildPlugin`, `gradlew runIde`.

- [ ] **Step 1: Check prerequisites.** `java -version` must report 21, `gradle -v` must report Gradle. If either is missing, ask the user to install JDK 21 and Gradle (e.g. via IntelliJ import or `winget`). Do not continue without both.
- [ ] **Step 2: Generate the wrapper:** `gradle wrapper`. Expected: `gradlew.bat` exists.
- [ ] **Step 3: Add test dependencies to `build.gradle.kts`:** `testImplementation("junit:junit:4.13.2")` and, in `dependencies { intellijPlatform { … } }`, `testFramework(TestFrameworkType.Platform)` (import `org.jetbrains.intellij.platform.gradle.TestFrameworkType`).
- [ ] **Step 4: Smoke test `SmokeTest.kt`:** `@Test fun buildRuns() { assertTrue(true) }` (JUnit 4).
- [ ] **Step 5: Run:** `./gradlew test buildPlugin`. Expected: `BUILD SUCCESSFUL`. If the untested versions fail (plugin 2.10.2, Kotlin 2.1.20, Rider 2026.3), read the error message, correct to the versions actually available and adjust `docs/PLAN.md`.
- [ ] **Step 6: Start `./gradlew runIde` once.** Expected: the Rider sandbox opens, the plugin shows up under Plugins. Close the sandbox.
- [ ] **Step 7: Record the result (confirmed versions, quirks) in `docs/knowledge/build/gradle-setup.md` and update the index.**
- [ ] **Step 8: Commit** (only after asking): `git add . && git commit -m "build: Gradle wrapper and test setup"`

---

### Task 2: Spike – check applying and reading in Rider

Answers the four open questions from the design before the final components are built. Throwaway code, replaced in task 5.

**Files:**
- Create (temporary): `src/main/kotlin/com/digoso/jbwindowmanager/actions/SpikeAction.kt`
- Modify (temporary): `src/main/resources/META-INF/plugin.xml`
- Create: `docs/knowledge/intellij-platform/spike-result.md`

**Interfaces:**
- Produces: documented answers to questions 1–4 with evidence; decision which apply route task 5 uses.

- [ ] **Step 1: Create `SpikeAction : AnAction`**, register it in `plugin.xml` under `Tools` (text "Spike: Dump layout"). `actionPerformed` calls, in `invokeLater`/EDT, `ToolWindowManagerEx.getInstanceEx(project).getLayout().getInfos()` and writes per entry `id, anchor, isSplit, type, isAutoHide, isVisible, order, weight, sideWeight, floatingBounds` via `Logger` (INFO) to the sandbox `idea.log`. If this does not compile against Rider 2026.3 (question 4), record the error message and use only the public API (`ToolWindowManager.toolWindowIds`, `ToolWindow` getters).
- [ ] **Step 2: Second spike action "Spike: Apply"** that sets two hard-wired ids (e.g. `Explorer`, `Commit`, taken from the dump) via the public API `setAnchor` → `setSplitMode` → `setType` → `show/hide`.
- [ ] **Step 3: Check in `runIde`:** put Explorer left/top and Commit left/bottom, read via dump, change the layout manually, run "Apply". Expected: the starting state is reached exactly. Note in which order it works.
- [ ] **Step 4: Check window position:** set a tool window to `FLOATING` and `WINDOWED`, move it, read the dump (does `floatingBounds` arrive?), move it elsewhere, restore. If it only works via `getLayout().copy()` + change + `setLayout`, test that as well.
- [ ] **Step 5: Check the menu id:** does the action group `WindowMenu` exist in Rider (the `Window` tab)? Otherwise determine the right id via Tools > "Internal Actions"/search.
- [ ] **Step 6: Write the result to `docs/knowledge/intellij-platform/spike-result.md`:** per question answer, evidence (log lines), chosen route for `LayoutApply`, known limitations. Correct the feasibility doc and design doc if they deviate, update the index.
- [ ] **Step 7: Report to the user** if there are limitations (e.g. position not restorable) before task 5 starts. Spike actions and their `plugin.xml` entries stay until task 5.

---

### Task 3: Data model and JSON

**Files:**
- Create: `src/main/kotlin/com/digoso/jbwindowmanager/model/Profile.kt`, `src/main/kotlin/com/digoso/jbwindowmanager/store/ProfileStore.kt` (for now only `ProfileJson` here)
- Test: `src/test/kotlin/com/digoso/jbwindowmanager/store/ProfileStoreTest.kt`

**Interfaces:**
- Produces:
  - `object ProfileJson { fun toJson(f: ProfileFile): String; fun fromJson(s: String): ProfileFile }`
  - `data class Bounds(val x: Int, val y: Int, val width: Int, val height: Int)`
  - `data class ToolWindowState(val id: String, val anchor: String, val split: Boolean, val type: String, val autoHide: Boolean, val visible: Boolean, val order: Int, val weight: Float, val bounds: Bounds?)` (`anchor` ∈ `left|right|bottom|top`, `type` ∈ `DOCKED|FLOATING|SLIDING|WINDOWED`, strings so that unknown values do not fail on read)
  - `data class Profile(val name: String, val toolWindows: List<ToolWindowState>)`
  - `data class ProfileFile(val version: Int, val profiles: List<Profile>)`; constant `CURRENT_VERSION = 1`.

- [ ] **Step 1: Failing test `roundtripKeepsAllFields`:** a `ProfileFile(1, listOf(Profile("Debugging", listOf(ToolWindowState("Explorer","left",false,"DOCKED",false,true,0,0.25f,null), ToolWindowState("Commit","left",true,"FLOATING",false,false,1,0.3f,Bounds(10,20,300,400))))))` is read back with `ProfileJson.toJson` and `ProfileJson.fromJson` and is `assertEquals` to the original.
- [ ] **Step 2: Run the test:** `./gradlew test --tests "*ProfileStoreTest.roundtripKeepsAllFields"`. Expected: FAIL (`ProfileJson` unknown).
- [ ] **Step 3: Create the data classes** as above in `Profile.kt`. `ProfileStore.kt` (task 4) holds `object ProfileJson { fun toJson(f: ProfileFile): String; fun fromJson(s: String): ProfileFile }`; for now create it in the same file. Use Gson (`GsonBuilder().setPrettyPrinting()`). If `com.google.gson` cannot be resolved, add `implementation("com.google.code.gson:gson:2.11.0")` and note it in `docs/knowledge/build/`. `fromJson` throws `IllegalArgumentException` for empty text, invalid JSON or `version > CURRENT_VERSION`.
- [ ] **Step 4: Add and run tests:** `emptyTextIsRejected`, `invalidJsonIsRejected`, `newerVersionIsRejected` (`assertFailsWith<IllegalArgumentException>`).
- [ ] **Step 5: All tests green:** `./gradlew test`. Expected: PASS.
- [ ] **Step 6: Commit** (only after asking): `git add src && git commit -m "feat: profile model and JSON"`

---

### Task 4: ProfileStore

**Files:**
- Create: `src/main/kotlin/com/digoso/jbwindowmanager/store/ProfileStore.kt` (extend with `ProfileStore`), `.../store/ProfileService.kt`
- Test: `src/test/kotlin/com/digoso/jbwindowmanager/store/ProfileStoreTest.kt`

**Interfaces:**
- Consumes: `ProfileFile`, `Profile`, `ProfileJson` (task 3).
- Produces:
  - `class ProfileStore(private val file: java.nio.file.Path)`
  - `fun profiles(): List<Profile>`
  - `val loadError: String?` (`null` if the file is missing or valid)
  - `fun save(profile: Profile)` (new or replace, name compared case-insensitively, trims the name)
  - `fun rename(oldName: String, newName: String)`
  - `fun delete(name: String)`
  - Errors: `IllegalArgumentException` for an empty name, duplicate target name or unknown name; `IllegalStateException` if `loadError != null` (writes blocked).
  - `@Service(Service.Level.APP) class ProfileService { val store: ProfileStore }` (path `Path.of(PathManager.getConfigPath(), "digoso.JbWindowManager", "profiles.json")`).

- [ ] **Step 1: Failing tests** (`@get:Rule TemporaryFolder`, store on `tmp/profiles.json`):
  - `missingFileGivesEmptyList` → `profiles().isEmpty()`, `loadError == null`.
  - `saveCreatesFileAndReadsBack` → after `save(p)` a **new** `ProfileStore` on the same file returns `listOf(p)`.
  - `saveSameNameReplaces` (also `"debug"` vs. `"Debug"`) → exactly one profile, new content.
  - `blankNameIsRejected` → `save(Profile("  ", emptyList()))` throws `IllegalArgumentException`.
  - `renameToExistingNameFails` → throws, both profiles unchanged.
  - `renameAndDeleteWork`.
  - `corruptFileIsNotOverwritten` → file with `"{broken"`; `loadError != null`, `profiles()` empty, `save(...)` throws `IllegalStateException`, file content unchanged.
  - `newerVersionFileIsNotOverwritten` → analogous with `{"version":2,"profiles":[]}`.
- [ ] **Step 2: Run the tests.** Expected: FAIL (`ProfileStore` unknown).
- [ ] **Step 3: Implement `ProfileStore`.** Read on first access (missing file = empty; read error or `fromJson` exception → set `loadError`). Write atomically: write to `file.resolveSibling("profiles.json.tmp")`, then `Files.move(tmp, file, REPLACE_EXISTING, ATOMIC_MOVE)`; create the parent folder beforehand.
- [ ] **Step 4: Create `ProfileService`** (path wiring only, no logic); in `plugin.xml` add `<applicationService serviceImplementation="com.digoso.jbwindowmanager.store.ProfileService"/>` under `<extensions defaultExtensionNs="com.intellij">`.
- [ ] **Step 5: Run the tests.** Expected: PASS.
- [ ] **Step 6: Commit** (only after asking): `git add src && git commit -m "feat: ProfileStore"`

---

### Task 5: LayoutCapture and LayoutApply

Depends on the result of the spike (task 2).

**Files:**
- Create: `src/main/kotlin/com/digoso/jbwindowmanager/layout/LayoutCapture.kt`, `.../layout/LayoutApply.kt`
- Delete: `SpikeAction.kt` and its `plugin.xml` entries

**Interfaces:**
- Consumes: `Profile`, `ToolWindowState`, `Bounds` (task 3); spike result `docs/knowledge/intellij-platform/spike-result.md`.
- Produces:
  - `fun captureProfile(project: Project, name: String): Profile` (EDT; all registered tool windows of the project, state according to the fields from task 3)
  - `fun applyProfile(project: Project, profile: Profile)` (EDT; per `ToolWindowState` `ToolWindowManager.getToolWindow(id)`; `null` → skip; unknown `anchor`/`type` string → skip that entry; order of setters and handling of `bounds`/`weight`/`order` according to the spike; any exception per tool window is logged and skipped)

- [ ] **Step 1: Pure helper functions with test.** In `LayoutApply.kt` `internal fun parseAnchor(s: String): ToolWindowAnchor?` and `internal fun parseType(s: String): ToolWindowType?` (`null` for an unknown value). Test in `src/test/.../layout/ParseTest.kt`: `"left"`→`LEFT`, `"bogus"`→`null`, `"FLOATING"`→`FLOATING`, `"x"`→`null`.
- [ ] **Step 2: Run the tests:** first FAIL, after implementation PASS (`./gradlew test --tests "*ParseTest"`).
- [ ] **Step 3: Implement `captureProfile`** as described in the interfaces, route according to the spike.
- [ ] **Step 4: Implement `applyProfile`** as described in the interfaces, route according to the spike. Record limitations from the spike as a comment `// ponytail:` with the ceiling.
- [ ] **Step 5: Remove the spike code,** run `./gradlew build`. Expected: `BUILD SUCCESSFUL`.
- [ ] **Step 6: Commit** (only after asking): `git add src && git commit -m "feat: LayoutCapture and LayoutApply"`

---

### Task 6: Menu `Window > Profiles` with save and apply

**Files:**
- Create: `.../actions/ProfilesActionGroup.kt`, `.../actions/SaveProfileAction.kt`
- Modify: `src/main/resources/META-INF/plugin.xml`

**Interfaces:**
- Consumes: `ProfileService.store` (task 4), `captureProfile`, `applyProfile` (task 5); menu id from the spike (presumably `WindowMenu`).
- Produces: group `digoso.JbWindowManager.Profiles` (popup "Profiles"), `SaveProfileAction`, placeholder id `digoso.JbWindowManager.ManageProfiles` for task 7.

- [ ] **Step 1: `ProfilesActionGroup : ActionGroup`** (`getActionUpdateThread() = ActionUpdateThread.BGT`). `getChildren(e)` returns one `AnAction` per `store.profiles()` with the profile name as text (runs `applyProfile` on the EDT), then a `Separator`, `SaveProfileAction`, `ManageProfilesAction`. If `store.loadError != null`, additionally a disabled entry "Profiles not readable (see log)".
- [ ] **Step 2: `SaveProfileAction`:** `Messages.showInputDialog` for the name; trim the name; if the name exists (case-insensitive), `Messages.showYesNoDialog` "Overwrite?"; then `store.save(captureProfile(project, name))`. Show `IllegalArgumentException`/`IllegalStateException` as an error dialog.
- [ ] **Step 3: Registration in `plugin.xml`:** group with `popup="true"`, `text="Profiles"`, `class="…ProfilesActionGroup"`, `<add-to-group group-id="<menu id from the spike>" anchor="last"/>`.
- [ ] **Step 4: Manually in `runIde`:** `Window > Profiles > Save as…` with the name "A", change the layout, save the name "B", switch between A and B in the list. Expected: the respective layout is restored. Try an empty name: error dialog. Check the example "Explorer left/top, Commit left/bottom, open/closed".
- [ ] **Step 5: Commit** (only after asking): `git add src && git commit -m "feat: menu Window > Profiles"`

---

### Task 7: Manage dialog (rename, delete)

**Files:**
- Create: `.../actions/ManageProfilesAction.kt`, `.../actions/ManageProfilesDialog.kt`
- Modify: `src/main/resources/META-INF/plugin.xml` (only if task 6 has not yet registered the action)

**Interfaces:**
- Consumes: `ProfileStore.profiles/rename/delete` (task 4).
- Produces: `ManageProfilesAction`, `ManageProfilesDialog(project: Project) : DialogWrapper`.

- [ ] **Step 1: `ManageProfilesDialog`:** `JBList` of the profile names, buttons "Rename…" (`Messages.showInputDialog`, prefilled with the old name) and "Delete" (after `Messages.showYesNoDialog`), the list refreshes after every change. Show store errors (`IllegalArgumentException`, `IllegalStateException`) as an error dialog. Buttons disabled when nothing is selected.
- [ ] **Step 2: `ManageProfilesAction`** opens the dialog, only enabled when a project is open.
- [ ] **Step 3: Manually in `runIde`:** rename profile "A" to "B" (if "B" exists: error dialog, both unchanged; also "b"), delete a profile, restart Rider and check that the changes persist (look at `profiles.json` in the config folder).
- [ ] **Step 4: Commit** (only after asking): `git add src && git commit -m "feat: manage profiles"`

---

### Task 8: Final check and docs

**Files:**
- Modify: `docs/PLAN.md`, `README.md`, `docs/knowledge/README.md`

- [ ] **Step 1: Full run:** `./gradlew test buildPlugin`. Expected: `BUILD SUCCESSFUL`, `build/distributions/*.zip` present.
- [ ] **Step 2: Manual review-focus checks in `runIde`:** (1) apply a profile in another project in which a saved tool window is missing → the rest is applied; (4) replace `profiles.json` with `{broken` → the menu shows "not readable", the file stays unchanged; (5) `"anchor":"bogus"` in one entry → it is skipped.
- [ ] **Step 3: Run `./gradlew verifyPlugin`** (if available) and record messages about internal APIs (`DesktopLayout`/`ToolWindowManagerEx`) in the knowledge folder `docs/knowledge/intellij-platform/`.
- [ ] **Step 4: Bring `docs/PLAN.md` up to date** (status, open points, plugin id/vendor placeholder), briefly extend `README.md` with usage.
- [ ] **Step 5: Commit** (only after asking): `git add . && git commit -m "docs: wrap-up"`
