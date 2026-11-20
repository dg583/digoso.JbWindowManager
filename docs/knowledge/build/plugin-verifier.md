# Plugin Verifier (`verifyPlugin`)

As of 2026-11-19 · Evidence: run against the local Rider 2026.3 (build 263.7307.258), Plugin Verifier 1.411.

## Result
- **No compatibility problems.**
- **5 internal API usages:** `DesktopLayout` (class, `getInfos()`, `copy()`), used in `LayoutCapture` and `LayoutApply`. Accepted on purpose, see `docs/knowledge/intellij-platform/spike-result.md` (only `setLayout` restores `floatingBounds`).
- Dynamic plugin eligibility: the plugin can presumably be enabled/disabled without restarting the IDE.

## Configuration in `build.gradle.kts`
- `failureLevel` = `COMPATIBILITY_PROBLEMS` and `INVALID_PLUGIN`: internal API does not fail the check, real breakage does.
- The Verifier rejected the placeholder id `com.example.*` (`ForbiddenPluginIdPrefix`); with `com.digoso.jbwindowmanager` the message is gone and the earlier mute was removed.
- Verification target: the same platform as the build (`riderPath` or download).

## Quirks
- The plugin description must start with Latin characters and be at least 40 characters long (otherwise "Invalid plugin descriptor 'description'"), hence English.
- The run downloads about 370 MB of dependencies (plugins bundled with Rider modules) into the cache and takes about 1.5 minutes.
- Running in parallel with a running `runIde` sandbox, a Gradle run aborted once ("daemon disappeared"); the same command succeeded afterwards.
