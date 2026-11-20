# digoso.JbWindowManager

A JetBrains IDE plugin that saves the arrangement of your tool windows as named **profiles**. Save, switch, rename and delete profiles from the `Window` menu. Rider's built-in `Window > Layouts` cannot manage profiles.

## Features
- Save the current tool window arrangement under a name and restore it with one click.
- Per tool window a profile stores: side (left/right/bottom/top), split position, open/closed, view mode (Dock Pinned/Unpinned, Undock, Float, Window), size/order, and the window position for floating/windowed tool windows.
- Profiles are global (shared by all projects) and stored as JSON.
- Tool windows that do not exist in the current project are skipped when a profile is applied.

Not included: editor splits, open files, main window size, export/import, shortcuts.

## Compatibility
Built and verified for **Rider 2026.3** (build 263, `sinceBuild = 263`). The plugin only depends on IntelliJ Platform APIs (`com.intellij.modules.platform`), so it should also work in other IntelliJ-based IDEs of the same version, but this has only been tested in Rider.

## Usage
Menu **`Window > Profiles`**:
- Click a profile in the list to apply it.
- **Save as…** stores the current arrangement under a name (an existing name is overwritten after confirmation).
- **Manage…** renames or deletes profiles.

Profiles are stored in `<IDE-Config>/digoso.JbWindowManager/profiles.json`.

## Install from source
```
./gradlew buildPlugin
```
Then in the IDE: `Settings > Plugins > ⚙ > Install Plugin from Disk…` and select the ZIP from `build/distributions`.

## Development
- `./gradlew test buildPlugin`: run tests and build the plugin ZIP (`build/distributions`)
- `./gradlew runIde`: sandbox Rider with the plugin
- `./gradlew verifyPlugin`: compatibility check against Rider 2026.3
- Local Rider instead of a download: set `riderPath=<path to the Rider installation>` in `~/.gradle/gradle.properties`
- Requirements: JDK 21
- More: `docs/PLAN.md`, `docs/knowledge/`, `CHANGELOG.md`

The plugin uses the internal API `DesktopLayout` on purpose, because it is the only way to restore the position of floating tool windows (see `docs/knowledge/intellij-platform/spike-result.md`). It may need adjustments for future IDE versions.

## License
[MIT](LICENSE), Copyright (c) 2026 Dirk Goldecker
