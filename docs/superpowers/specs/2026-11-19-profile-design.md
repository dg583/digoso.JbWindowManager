# Design: tool window profiles for Rider

As of 2026-11-19 · Status: **approved** (originally written as a draft awaiting approval)
Basis: `docs/PLAN.md`, `docs/knowledge/profile/scope-and-storage.md`, `docs/knowledge/intellij-platform/toolwindow-api-feasibility.md`

## Goal
The user saves the current arrangement of the tool windows as a named **profile**, manages several profiles and switches between them with a click. `Window > Layouts` cannot do that (no profile management).

## Scope
**In the profile, per tool window:** side (left/right/bottom/top), split (upper/lower), open/closed, view mode (`type` + `autoHide`), position/size of undocked windows (`floatingBounds`), size/order of docked windows (`weight`, `order`), as far as the spike confirms they can be restored.
**Out of scope:** editor splits, open files, main window size, export/import, shortcuts, sharing within a team.

## Usage
Submenu **`Window > Profiles`**:
- List of saved profiles: a click applies the profile.
- **Save as…**: enter a name, save the current state. If the name already exists, it is overwritten after confirmation.
- **Manage…**: dialog with a list, actions **Rename** and **Delete**.

## Storage
One file `<IDE-Config>/digoso.JbWindowManager/profiles.json` (global, via `PathManager.getConfigPath()`). Content:

```json
{
  "version": 1,
  "profiles": [
    {
      "name": "Debugging",
      "toolWindows": [
        { "id": "Explorer", "anchor": "left", "split": false, "type": "DOCKED",
          "autoHide": false, "visible": true, "order": 0, "weight": 0.25,
          "bounds": null }
      ]
    }
  ]
}
```
`bounds` (`x, y, width, height`) only for `FLOATING`/`WINDOWED`. The format is deliberately decoupled from the internal `WindowInfoImpl`.

## Components
| Component | Responsibility |
|---|---|
| `Profile`, `ToolWindowState` | data classes for the JSON model |
| `ProfileStore` | application service: load, save, add/replace, rename, delete (reads/writes the JSON file) |
| `LayoutCapture` | reads the current state of all tool windows of the project (route according to the spike: `ToolWindowManagerEx.getLayout().getInfos()`) |
| `LayoutApply` | applies a profile: for each existing tool window set side → split → view mode → visibility. Unknown ids are **skipped**. |
| Actions | `ProfilesActionGroup` (dynamic children under `Window > Profiles`), `SaveProfileAction`, `ManageProfilesAction` (+ dialog) |

JSON via Gson (shipped with the platform, availability on the Rider classpath **unverified** at the time; otherwise pick an alternative). All tool window access on the EDT.

## Error handling
- Invalid or unreadable `profiles.json`: do not overwrite, log the error, show the user a notification and continue with an empty list.
- Writes are atomic (temporary file, then rename) so no half-written state remains.
- Applying is best effort: a tool window that cannot be set does not abort the profile (log the error, continue).

## Tests
- Automatic: tests for JSON round trip and `ProfileStore` (add, replace, rename, delete, corrupt file).
- Manual in `runIde`: save and apply with the example "Explorer left/top, Commit left/bottom", open/closed, Float/Window with position.

## Spike (before implementation)
Clarifies what applying can really achieve (see the feasibility doc):
1. Does the order `setAnchor` → `setSplitMode` → `setType` → `show/hide` give the desired result?
2. Can `floatingBounds` be restored (publicly or only via `DesktopLayout` + `setLayout`)?
3. Does Rider behave like IntelliJ?
4. Are `ToolWindowManagerEx`/`DesktopLayout` usable against Rider 2026.3?
The result decides whether `LayoutApply` uses the public API only or additionally `setLayout`. Limited restoration (e.g. without window position) is then documented deliberately.

## Prerequisites
JDK 21 and the Gradle wrapper were still missing (see `docs/PLAN.md`). Plugin id, package name and vendor were still placeholders. Both have since been resolved.
