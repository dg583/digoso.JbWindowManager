# CLAUDE.md

`digoso.JbWindowManager`: a JetBrains plugin (Kotlin + Gradle) that saves the tool window arrangement as named **profiles** and switches between them via `Window > Profiles`. Target: **Rider 2026.3** (build 263). Goals and decisions: `docs/PLAN.md`.

## Working style
- Language: project artifacts (code, comments, docs, commit messages) are English. Conversation with the user is German.
- Smallest working solution, no speculative abstractions.
- No code before the design in `docs/superpowers/specs/` is approved.
- Test first (red, green, refactor). A fix without a test that failed first is not verified.
- Commit only when asked. Never push or force-push without an explicit request.

## Commands
```
./gradlew test                                  # unit tests (33)
./gradlew buildPlugin                           # ZIP in build/distributions
./gradlew verifyPlugin                          # Plugin Verifier against Rider 2026.3 (~1.5 min, ~370 MB cache)
./gradlew runIde                                # sandbox Rider with the plugin
```
- Needs **JDK 21** as `JAVA_HOME` (`jvmToolchain(21)`). Gradle 9.8.0 via the wrapper.
- `riderPath=<Rider install dir>` in `~/.gradle/gradle.properties` (per machine, not in the repo) uses a local Rider; without it the build downloads Rider 2026.3.
- Do not run Gradle in parallel with a running `runIde` sandbox (the daemon died once). On low memory, `./gradlew --stop`.
- Windows/Git Bash: `python3` hangs (Store stub), use the Edit tool instead of scripts.

## Architecture
Package `com.digoso.jbwindowmanager`, plugin id `com.digoso.jbwindowmanager`, vendor `digoso`.
```
model/Profile.kt          data model: Profile, ToolWindowState, Bounds, ProfileFile
store/ProfileStore.kt     JSON read/write, profile management (pure logic, unit-tested)
store/ProfileService.kt   application service, global path <IDE-Config>/digoso.JbWindowManager/profiles.json
layout/LayoutCapture.kt   read current state (captureProfile)
layout/LayoutApply.kt     apply a profile (applyProfile)
actions/                  Window > Profiles menu, Save as…, Manage… dialog
src/main/resources/META-INF/plugin.xml
```
Key points:
- **Public API only, no internal API** (the Marketplace check flagged `DesktopLayout`). Capture/apply use `ToolWindow`, `ToolWindowManagerEx.getIdsOn/toolWindows`, `ToolWindowEx.stretchWidth/Height`, the Swing window of floating windows and `WindowManager.getFrame`. Positions are stored relative to the main frame's screen (`layout/Geometry.kt`). Apply is an asynchronous sequence (frame, tool windows, window bounds, converging size passes). Fullscreen is untested. Details and pitfalls: `docs/knowledge/intellij-platform/public-api-spike.md`. Adjust there first when a new IDE version breaks.
- Profile file format **version 2** (`frame`, `rel`); version 1 is still read, a save writes version 2.
- Tool windows missing in the current project are skipped on apply.
- `ProfileStore`: immutable snapshots, atomic write (tmp file + `ATOMIC_MOVE`), names unique ignoring case. An unreadable file sets `loadError` and blocks all writes (the file stays untouched). IO errors surface as `IllegalStateException` for the UI to show as a dialog.
- `ProfileJson.fromJson` validates explicitly because Gson sets missing fields to `null` despite Kotlin types.
- `anchor` and `type` are stored as strings so unknown values do not fail on read.
- The plugin only needs `com.intellij.modules.platform`, so other IntelliJ-based IDEs of the same version may work, but only Rider is verified.

## Build and verifier gotchas
- IntelliJ Platform Gradle Plugin **2.20.1** and Kotlin **2.4.0** are required for Rider 2026.3 (older versions fail on descriptors and Kotlin metadata). `useInstaller = false` for Rider.
- `verifyPlugin` fails only on `COMPATIBILITY_PROBLEMS` and `INVALID_PLUGIN`. Internal API usage is not an error there but the Marketplace check reports it: keep it at 0.
- The plugin description must start with Latin characters and be at least 40 characters long. Plugin ids with the prefix `com.example` are rejected.
- Details: `docs/knowledge/build/`.

## Knowledge base (mandatory)
Record new findings in `docs/knowledge/` **immediately**, organized by topic. Do not let them get lost in chat answers, `docs/PLAN.md` or commit messages.

```
docs/knowledge/
  <topic>/            # one folder per topic, kebab-case
    <subtopic>.md     # one file per self-contained finding
  README.md           # index: one entry per file (link + one line)
```

Topic folders (check first whether an existing one fits):
- `intellij-platform/` – APIs and behavior of the IntelliJ Platform (ToolWindowManager, layout persistence, services, actions)
- `rider/` – Rider specifics (frontend/backend split, `Window > Layouts`)
- `profile/` – design and decisions for profile management (data model, storage, format, usage)
- `build/` – Gradle, IntelliJ Platform Gradle Plugin, versions, `runIde`, signing/publishing
- `testing/` – test strategy, sandbox findings

Rules:
1. Before writing, check `docs/knowledge/README.md` and the matching folder. Update an existing file instead of creating a duplicate.
2. Per file: heading, finding in a few sentences, **source/evidence** (doc link, class, test), date (`YYYY-MM-DD`). Mark anything uncertain as "unverified".
3. New or changed file: add an entry to `docs/knowledge/README.md`.
4. Correct or delete findings that have become wrong.
5. `docs/PLAN.md` stays the short overview (goal, decisions, next steps) and points to `docs/knowledge/`.
6. Record only what is not already evident from the code or the git history.

## Docs map
- `README.md`: user-facing overview. `CHANGELOG.md`: Keep a Changelog, update on user-visible changes.
- `docs/PLAN.md`: status and decisions. `docs/superpowers/`: design specs and implementation plans.
- CI: `.github/workflows/build.yml` (`test buildPlugin verifyPlugin`).
