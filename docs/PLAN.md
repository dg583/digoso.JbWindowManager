# digoso.JbWindowManager – Plan & context

As of 2026-11-20 · Status: version 0.2.0 implemented (38 unit tests, manual verification in Rider 2026.3, fullscreen untested), first version 0.1.0 already uploaded to the Marketplace. Package and plugin id `com.digoso.jbwindowmanager`, vendor `digoso`.

Further findings and research results, sorted by topic, live under `docs/knowledge/` (see `.claude/CLAUDE.md`). Design: `docs/superpowers/specs/2026-11-19-profile-design.md`, implementation plan: `docs/superpowers/plans/2026-11-19-toolwindow-profile.md`.

## Goal
JetBrains plugin for **Rider**: save the arrangement of the tool windows as a named **profile**, manage several profiles (create, switch, rename, delete). Rider's `Window > Layouts` is not enough because profiles cannot be managed there.

## Decisions
- **Kotlin + Gradle** (IntelliJ Platform Gradle Plugin). A pure .NET plugin is not possible: layouts live in the IDE's JVM frontend (e.g. `ToolWindowManager`). The .NET/ReSharper backend has no access to it.
- No .NET project as long as it has no purpose.
- Target IDE: Rider 2026.3 (`sinceBuild = 263`), JDK 21. Versions confirmed: `docs/knowledge/build/gradle-setup.md`.
- Development IDE: IntelliJ IDEA. Rider for testing via `runIde` (sandbox).
- **Profile = tool window state:** per tool window side (left/right/bottom/top), split position (e.g. left/top vs. left/bottom), open/closed, view mode (`type` + `autoHide`), size/order, and the position for float/window. No editor splits, no open files.
- **Storage:** global, JSON, `<IDE-Config>/digoso.JbWindowManager/profiles.json`.
- **Usage:** submenu `Window > Profiles` (switch profiles, "Save as…", "Manage…" with rename/delete).
- Missing tool windows are skipped when applying.
- **Public API only (since 0.2.0).** Spike 1 used the internal `DesktopLayout`; the Marketplace check flagged 5 usages. Spike 2 showed position, order and sizes work via the public API plus the Swing window (`docs/knowledge/intellij-platform/public-api-spike.md`, design `docs/superpowers/specs/2026-11-20-public-api-design.md`). Profiles also store the main window and relative positions (file format version 2). Fullscreen is untested.
- **License:** MIT.

## Structure
```
build.gradle.kts / settings.gradle.kts / gradle.properties
.github/workflows/build.yml   CI: test, buildPlugin, verifyPlugin
CHANGELOG.md
src/main/kotlin/com/digoso/jbwindowmanager/
  model/Profile.kt            data model (Profile, ToolWindowState, Bounds)
  store/ProfileStore.kt       read/write JSON, management (pure logic)
  store/ProfileService.kt     app service with the global path
  layout/LayoutCapture.kt     read the current state
  layout/LayoutApply.kt       apply a profile
  actions/                    menu, save, manage dialog
src/main/resources/META-INF/plugin.xml
src/test/kotlin/...           tests for store/JSON and parsing
docs/                         this plan, knowledge base (docs/knowledge), design specs and plans (docs/superpowers)
```
Build and test: `gradlew test buildPlugin` · sandbox: `gradlew runIde` · check: `gradlew verifyPlugin`.
Local Rider instead of a download: `riderPath` in `~/.gradle/gradle.properties`.

## Open
- **Package and plugin id:** `com.digoso.jbwindowmanager`, vendor `digoso` (set 2026-11-19; the Verifier had rejected the `com.example` prefix). Product, project and config folder name: `digoso.JbWindowManager`.
- Out of scope, possible later: export/import, shortcuts, sharing within a team, editor splits.

## Working style
- Project artifacts (code, comments, docs, commit messages) in English; conversation with the user in German.
- Ponytail mode active: smallest working solution, no speculative abstractions.
