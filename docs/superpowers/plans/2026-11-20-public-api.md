# Public API only, main frame, relative positions: Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove all internal API usage, store the main window state in profiles, and store window positions relative to the screen (profile file version 2).

**Architecture:** Pure geometry/format logic is unit-tested (`model`, `layout/Geometry.kt`, `ProfileJson`). `LayoutCapture`/`LayoutApply` are rewritten on the public API and verified manually in `runIde` (platform code, no unit test possible). Apply is a sequence of asynchronous steps, each waiting for the previous one to settle.

**Tech Stack:** Kotlin 2.4.0, JDK 21, Gradle 9.8.0, IntelliJ Platform Gradle Plugin 2.20.1, Rider 2026.3 (build 263), Gson, JUnit 4.

**Spec:** `docs/superpowers/specs/2026-11-20-public-api-design.md` (evidence: `docs/knowledge/intellij-platform/public-api-spike.md`, spike code `actions/Spike.kt`).

## Global Constraints
- Public API only. `DesktopLayout`, `WindowInfoImpl`, `ToolWindowManagerEx.getLayout/setLayout/getLayoutToRestoreLater` must not appear. Acceptance: `./gradlew verifyPlugin` reports no internal API usage.
- Project artifacts (code, comments, docs, commit messages) in English. Test first: a test that failed first, then the code (`CLAUDE.md`).
- File format version 2; versions 1 and 2 are read, 2 is written, a newer version stays an error.
- JDK 21 as `JAVA_HOME` (Git Bash: `export JAVA_HOME=/c/Users/dgoldecker/.jdks/jdk-21.0.13+9`). Do not run Gradle while a `runIde` sandbox is open.
- Unit test count in `CLAUDE.md`/`docs/PLAN.md` ("22") must be updated at the end.
- Commits go on branch `feature/public-api-only` (renamed from `spike/public-api-only`), no push without an explicit request.

## Review Focus
- A version 1 file (absolute `bounds`, no `frame`) must still load and apply: floating windows fall back to the absolute `bounds`, the main frame stays untouched.
- A screen or frame with width or height 0 must not cause a division by zero or a nonsensical rect (`Rel.of` returns null).
- A profile that names tool windows not registered in the project, or unknown `anchor`/`type` strings, must skip them and still apply the rest.
- A profile saved fullscreen applied while not fullscreen (and the reverse), and a maximized profile applied to a normal frame, must reach the saved state.
- `weight = 0` (hidden or undocked window) must not stretch anything; unreachable target sizes must stop after 5 passes.

---

### Task 0: Branch and spike record

**Files:** none changed; commits the spike and the knowledge docs.

- [ ] **Step 1:** `git branch -m feature/public-api-only`; `git add -A`; commit `spike: public-API-only layout spike (throwaway) and findings`. This keeps `Spike.kt` in history as the reference for tasks 3 and 4.
- [ ] **Step 2:** commit the two spec/plan docs: `docs: design and plan for public-API-only layout`.

### Task 1: Data model and JSON format version 2

**Files:**
- Modify: `src/main/kotlin/com/digoso/jbwindowmanager/model/Profile.kt`, `src/main/kotlin/com/digoso/jbwindowmanager/store/ProfileStore.kt` (`ProfileJson.fromJson`)
- Test: `src/test/kotlin/com/digoso/jbwindowmanager/store/ProfileStoreTest.kt`

**Interfaces:**
- Produces: `data class Rel(val x: Double, val y: Double, val w: Double, val h: Double)`, `data class FrameState(val fullscreen: Boolean, val maximized: Boolean, val rel: Rel?)`, `ToolWindowState(..., val bounds: Bounds?, val rel: Rel? = null)`, `Profile(val name: String, val toolWindows: List<ToolWindowState>, val frame: FrameState? = null)`, `CURRENT_VERSION = 2`. New fields have defaults so existing positional constructor calls in tests still compile.

- [ ] **Step 1: Write the failing tests** in `ProfileStoreTest`:
  - `roundtripKeepsFrameAndRel`: a version 2 `ProfileFile` with `Profile.frame = FrameState(false, false, Rel(0.1, 0.2, 0.5, 0.6))` and a window with `rel = Rel(0.49, 0.56, 0.51, 0.41)` survives `fromJson(toJson(f))` unchanged.
  - `version1FileIsReadWithoutFrame`: a hand-written version 1 JSON (absolute `bounds`, no `frame`, no `rel`) loads; `profile.frame == null`, `toolWindows[0].rel == null`, `bounds == Bounds(10, 20, 300, 400)`.
  - `saveUpgradesVersion1ToCurrent`: a version 1 file on disk, `store.save(profile)`, the file now has `"version": 2`.
  - `newerVersionIsStillRejected`: `"version": 3` throws `IllegalArgumentException`.
  - `frameWithoutFlagsIsAccepted`: `"frame": {"rel": null}` (missing booleans) loads without exception (Gson defaults them to `false`).
- [ ] **Step 2:** `./gradlew test --tests "*ProfileStoreTest*"`. Expected: compile errors / FAIL (types missing).
- [ ] **Step 3:** Add the types and `CURRENT_VERSION = 2` in `Profile.kt`; adjust the existing `sample` in the test to `ProfileFile(CURRENT_VERSION, …)` if it asserts the version. No change to `fromJson` beyond what the tests need (the version check already accepts `<= CURRENT_VERSION`).
- [ ] **Step 4:** `./gradlew test`. Expected: all tests PASS (22 existing + new).
- [ ] **Step 5:** commit `feat: profile format version 2 (frame state, relative bounds)`.

### Task 2: Geometry helpers (pure)

**Files:**
- Create: `src/main/kotlin/com/digoso/jbwindowmanager/layout/Geometry.kt`
- Test: `src/test/kotlin/com/digoso/jbwindowmanager/layout/GeometryTest.kt`

**Interfaces:**
- Produces (all `internal`): `fun relOf(r: java.awt.Rectangle, screen: java.awt.Rectangle): Rel?` (null when `screen.width <= 0 || screen.height <= 0`), `fun Rel.toRect(screen: java.awt.Rectangle): java.awt.Rectangle`, `fun nextStretch(target: Int, actual: Int, tolerance: Int = 2): Int?` (returns `target - actual`, null when `abs(target - actual) <= tolerance`), `fun applyOrder(states: List<ToolWindowState>): List<ToolWindowState>` (sorted by `anchor`, `split`, `order`, `id`).

- [ ] **Step 1: Write the failing tests** in `GeometryTest`:
  - `relRoundTripOnSameScreen`: `relOf(r, s)!!.toRect(s) == r` for `r = Rectangle(943, 669, 983, 489)`, `s = Rectangle(0, 0, 1920, 1200)`.
  - `scalesToAnotherScreen`: the same `r` captured on 1920x1200 and applied to `Rectangle(-848, -1440, 3440, 1440)` has width `(983 / 1920.0 * 3440).toInt()` and x `-848 + (943 / 1920.0 * 3440).toInt()`.
  - `negativeAndOutsideFractionsSurvive`: `r = Rectangle(-7, 0, 974, 1159)` round-trips on `Rectangle(0, 0, 1920, 1200)` (within 1 px).
  - `zeroSizedScreenGivesNull`: `relOf(r, Rectangle(0, 0, 0, 1200)) == null`.
  - `nextStretchWithinTolerance`: `nextStretch(340, 341) == null`, `nextStretch(340, 342) == null`.
  - `nextStretchGrowAndShrink`: `nextStretch(340, 161) == 179`, `nextStretch(340, 391) == -51`.
  - `applyOrderSortsPerAnchorSplitOrder`: windows `[B(left,split,order 1), A(left,false,order 4), C(left,false,order 2), D(bottom,false,0)]` come back as `[D, C, A, B]` (anchor string `bottom` < `left`, then `split=false` before `true`, then `order`).
- [ ] **Step 2:** `./gradlew test --tests "*GeometryTest*"`. Expected: FAIL (functions not defined).
- [ ] **Step 3:** Implement the four functions in `Geometry.kt`, using `Math.floor`-free plain `toInt()` truncation as in the spike.
- [ ] **Step 4:** `./gradlew test`. Expected: PASS.
- [ ] **Step 5:** commit `feat: geometry helpers for relative bounds and size convergence`.

### Task 3: Capture on the public API

**Files:**
- Modify (rewrite): `src/main/kotlin/com/digoso/jbwindowmanager/layout/LayoutCapture.kt`
- Keep: `anchorName`/`parseAnchor`/`parseType` (currently in `LayoutApply.kt`; move to `Geometry.kt` or leave, so `ParseTest` still compiles).

**Interfaces:**
- Produces: `fun captureProfile(project: Project, name: String): Profile` (same signature, EDT only). Fills `Profile.frame` (fullscreen via `WindowManager.getIdeFrame(project).isInFullScreen`, `maximized = extendedState and MAXIMIZED_BOTH != 0`, `rel = relOf(frame.bounds, frame.graphicsConfiguration.bounds)`) and per window: `anchor`, `split`, `type`, `autoHide`, `visible`, `order` (index in `ToolWindowManagerEx.getIdsOn(anchor)`, `-1` if absent), `weight` (docked and visible only: `component.width / frame.width` for left/right, `component.height / frame.height` for top/bottom, else `0f`), `bounds = null`, `rel` (`relOf` of the window ancestor bounds for `FLOATING`/`WINDOWED`, else null). Windows with a null `id` are dropped. Output sorted as before (`anchor`, `split`, `order`, `id`).

- [ ] **Step 1:** Rewrite `captureProfile` on the signature above; no import from `com.intellij.openapi.wm.impl` or `DesktopLayout`.
- [ ] **Step 2:** `./gradlew compileKotlin`. Expected: no errors.
- [ ] **Step 3 (manual, `runIde`):** start the sandbox, arrange windows (one floating, one windowed, a column with Project + Commit, a changed order), `Window > Profiles > Save as…`, close the sandbox, open `<sandbox config>/digoso.JbWindowManager/profiles.json`. Expected: `"version": 2`, a `frame` object, `rel` on the floating/windowed windows, `weight` between 0 and 1 on docked ones, `bounds: null`.
- [ ] **Step 4:** commit `feat: capture layout and main frame via public API`.

### Task 4: Apply on the public API

**Files:**
- Modify (rewrite): `src/main/kotlin/com/digoso/jbwindowmanager/layout/LayoutApply.kt`

**Interfaces:**
- Consumes: `Rel.toRect`, `nextStretch`, `applyOrder`, `parseAnchor`, `parseType` (Tasks 1 and 2).
- Produces: `fun applyProfile(project: Project, profile: Profile)` (same signature, EDT only, returns immediately, steps run asynchronously).

- [ ] **Step 1:** Implement the sequence from the spec, each step started from the previous one:
  1. Frame (skipped when `profile.frame == null`): fullscreen differs → `ActionUtil.invokeAction(ActionManager.getInstance().getAction("ToggleFullScreen"), frame.rootPane, "ProfileApply", null, null)`; if the target is not fullscreen: set `extendedState` (maximized or normal) and, for a normal frame, `bounds = rel.toRect(frame.graphicsConfiguration.bounds)`. Wait until `isInFullScreen` equals the target and the frame bounds stopped changing between two `invokeLater` ticks, with a 3 s timeout (log a warning on timeout, then continue).
  2. Tool windows in `applyOrder(profile.toolWindows)` order: skip windows not registered in the project and unknown `anchor`/`type` (log); per window `setAnchor` → `setSplitMode` → `setType` → `setAutoHide` → `show/hide`, each wrapped in try/catch with a log entry (best effort).
  3. Floating/windowed windows: bounds from `rel.toRect(mainScreen)`; when `rel == null` and the legacy `bounds != null` (version 1), use the absolute `bounds`.
  4. Docked sizes: passes as in the spec (one window per pass, re-measure in `invokeLater`, at most 5 passes) using `nextStretch(target, actual)`; `weight <= 0` windows are skipped.
  Use `ApplicationManager.getApplication().invokeLater`/`javax.swing.Timer`-free polling; no fixed sleeps.
- [ ] **Step 2:** `./gradlew compileKotlin && ./gradlew test`. Expected: PASS, no `DesktopLayout` import left (`grep -r "DesktopLayout\|WindowInfoImpl\|getLayout\|setLayout" src/main` finds nothing apart from `Spike.kt`).
- [ ] **Step 3 (manual matrix in `runIde`, Review Focus items included):** same screen; other screen (frame and windows follow, docked widths about right); maximized; changed order; changed widths; floating and windowed windows; applying a profile with a missing tool window; **fullscreen both directions**; a version 1 `profiles.json` copied into the sandbox config (floating windows use absolute bounds, frame untouched). Record the outcome in `docs/knowledge/intellij-platform/public-api-spike.md`.
- [ ] **Step 4 (decision):** if fullscreen does not switch reliably, remove `fullscreen` handling from capture/apply and the model default stays `false`; document it in the knowledge doc and README and tell the user. Otherwise mark fullscreen as verified in the knowledge doc.
- [ ] **Step 5:** commit `feat: apply profiles via public API incl. main frame and relative positions`.

### Task 5: Remove the spike, verify, document, version

**Files:**
- Delete: `src/main/kotlin/com/digoso/jbwindowmanager/actions/Spike.kt`; modify `src/main/resources/META-INF/plugin.xml` (remove the two spike actions)
- Modify: `build.gradle.kts` (version `0.2.0`), `CHANGELOG.md`, `README.md`, `.claude/CLAUDE.md`, `docs/PLAN.md`, `docs/knowledge/intellij-platform/spike-result.md`, `docs/knowledge/build/publishing-and-signing.md`, `docs/knowledge/README.md` if entries changed

- [ ] **Step 1:** delete `Spike.kt` and the `<!-- SPIKE -->` actions in `plugin.xml`.
- [ ] **Step 2:** `./gradlew test buildPlugin verifyPlugin`. Expected: BUILD SUCCESSFUL; the verifier output shows **no** internal API usages (it printed 5 before). Paste the relevant verifier lines into the knowledge doc.
- [ ] **Step 3:** documentation: `CHANGELOG.md` entry `0.2.0` (main window saved, positions relative to the screen, no internal API); README "Features"/"What a profile stores" and remove the "internal API on purpose" paragraph; `CLAUDE.md` (architecture lines for capture/apply, the "Apply goes through internal API" key point replaced, the test count); `docs/PLAN.md` status; `plugin.xml` `<change-notes>` and the description if they mention what is stored.
- [ ] **Step 4:** `./gradlew buildPlugin` once more, check `build/distributions/digoso.JbWindowManager-0.2.0.zip` exists.
- [ ] **Step 5:** commit `chore: remove spike, release 0.2.0 (public API only)`.
