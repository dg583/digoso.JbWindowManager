# Design: public API only, main frame state, relative positions

As of 2026-11-20 · Status: **draft, awaiting approval** · Amends `2026-11-19-profile-design.md` (replaces its capture/apply route, extends the profile content and the file format).
Basis: `docs/knowledge/intellij-platform/public-api-spike.md` (spike 2, verified in Rider 2026.3), Marketplace check: 5 internal API usages (`DesktopLayout`).

## Goal
1. Remove every internal API usage (`DesktopLayout`, `WindowInfoImpl`, `ToolWindowManagerEx.getLayout/setLayout`). Acceptance: the Marketplace/Plugin Verifier check reports **no** internal API usage.
2. A profile also stores the **main window** (fullscreen, or maximized, or position and size).
3. Positions are stored **relative to the screen**, so a profile applies sensibly on another screen or resolution.

## Scope
**New in the profile:** main frame state. **Changed:** window positions and docked sizes are relative. **Unchanged:** everything else in the previous design (usage, menu, storage location, error handling). Out of scope as before: editor splits, open files, export/import, shortcuts.

## Data model (file format version 2)
```json
{
  "version": 2,
  "profiles": [
    {
      "name": "Debugging",
      "frame": { "fullscreen": false, "maximized": false, "rel": { "x": -0.004, "y": 0.0, "w": 0.507, "h": 0.966 } },
      "toolWindows": [
        { "id": "Project", "anchor": "left", "split": false, "type": "DOCKED", "autoHide": false,
          "visible": true, "order": 4, "weight": 0.15, "bounds": null, "rel": null },
        { "id": "Database", "anchor": "right", "split": false, "type": "WINDOWED", "autoHide": false,
          "visible": true, "order": 3, "weight": 0.0, "bounds": null, "rel": { "x": 0.49, "y": 0.56, "w": 0.51, "h": 0.41 } }
      ]
    }
  ]
}
```
- `Rel` = `x, y, w, h` as fractions of the **screen bounds of the main frame** (`frame.graphicsConfiguration.bounds`) at capture time. Values outside 0..1 are allowed (windows partly off screen, other screen).
- `ToolWindowState.rel` replaces `bounds` for `FLOATING`/`WINDOWED`; `bounds` (absolute) is only read for version 1 files and no longer written.
- `ToolWindowState.weight` keeps its meaning: docked size as a fraction of the main frame (width for left/right, height for top/bottom). Captured as `component size / frame size`, applied as `weight * frame size`. Only for visible `DOCKED` windows, else `0`.
- `Profile.frame` is nullable: version 1 profiles have none, applying them leaves the main frame untouched.
- Reading: versions 1 and 2 are accepted. Writing always writes 2 (a version 1 file is upgraded on the next save). A newer version stays an error (no overwrite), as before.

## Capture (public API)
- Per tool window of `ToolWindowManagerEx.getInstanceEx(project).toolWindowIdSet` resolved with `getToolWindow(id)` (`ToolWindowManagerEx.getToolWindows()` is `@ApiStatus.Internal`, found by `verifyPlugin`; `getIdsOn` is fine): `anchor`, `isSplitMode`, `type`, `isAutoHide`, `isVisible`, size from `component`, window bounds from `SwingUtilities.getWindowAncestor(component)` for `FLOATING`/`WINDOWED`.
- `order`: index in `getIdsOn(anchor)`.
- Frame: `WindowManager.getFrame(project)`: `extendedState` (maximized), `bounds`, `graphicsConfiguration.bounds`; fullscreen via `getIdeFrame(project).isInFullScreen`.
- Tool windows not registered in the project do not appear (nothing to skip on capture).

## Apply (public API, EDT, asynchronous steps)
Sequence, each step starts when the previous one has settled:
1. **Frame** (only if the profile has one): if fullscreen differs, run action `ToggleFullScreen`. If the target is not fullscreen: set `extendedState`, and for a non-maximized frame set `bounds` from `rel` and the *current* screen of the frame.
2. **Tool windows:** per saved `(anchor, split, order)` ascending: `setAnchor` → `setSplitMode` → `setType` → `setAutoHide` → `show/hide`. Windows not in the profile or not registered in the project stay as they are; unknown `anchor`/`type` values are skipped with a log entry.
3. **Floating/windowed windows:** set the window bounds from `rel` and the current screen of the main frame.
4. **Docked sizes** in converging passes: per pass change **one** window (windows in a column share their width), re-measure after the layout has settled, repeat until `|target - actual| <= 2 px`, at most 5 passes (`stretchWidth/stretchHeight(delta)`).
"Settled" means: wait via `invokeLater`/polling until the expected state is observable, with a timeout (about 3 s) after which the sequence continues and logs a warning. No fixed sleeps in production code.
Applying stays best effort: an error in one tool window is logged and does not abort the profile.

## Components (changes)
| Component | Change |
|---|---|
| `model/Profile.kt` | add `FrameState`, `Rel`; `ToolWindowState.rel`; `Profile.frame` (nullable); `CURRENT_VERSION = 2` |
| `store/ProfileStore.kt` | `ProfileJson.fromJson` accepts versions 1 and 2, validates `rel`/`frame` |
| `layout/LayoutCapture.kt` | rewritten on the public API |
| `layout/LayoutApply.kt` | rewritten: sequence above |
| `layout/Geometry.kt` (new, pure) | `Rel.of(rect, screen)`, `Rel.toRect(screen)`, `nextStretch(target, actual): Int?` (null when within tolerance), sort of the apply order |
| `actions/` | unchanged |
The spike code (`actions/Spike.kt`, spike actions in `plugin.xml`) is removed before the first production commit.

## Tests
- **Unit (red first):** `Rel.of`/`toRect` round trip and scaling between two screens (1920x1200 to 3440x1440 from the spike log); `nextStretch` (within tolerance, grow, shrink); apply order sort; JSON round trip for version 2; reading a version 1 file (no `frame`, absolute `bounds`) and its upgrade on write; rejecting a newer version; missing `rel` values.
- **Manual in `runIde`:** the matrix from the spike: same screen, other screen (windows follow), maximized, fullscreen, changed order, changed widths, floating and windowed windows. Check `verifyPlugin`: 0 internal API usages.

## Risks and open points
- **Fullscreen unverified.** No spike run logged `fullscreen=true`. During implementation first verify that `isInFullScreen` reports correctly and that `ToggleFullScreen` switches. If it cannot be made to work reliably, drop fullscreen from the profile and document it in README and knowledge base. Decision to be taken then.
- Relative docked sizes (`weight` as a fraction of the frame) are **not yet verified** on a different screen (the spike only used pixels on the same screen); covered by the manual matrix.
- `ToolWindowManagerEx` and `ToolWindowEx.stretchWidth/stretchHeight` are public in 2026.3 but may change; the verifier run is the safeguard.
- Behavior on multi-screen setups where the main frame spans screens is not specified.

## Release
New Marketplace version **0.2.0** (new feature, format change). Update `CHANGELOG.md`, README (main window now stored, positions relative) and `docs/PLAN.md`. Remove the "internal API on purpose" note from the README and `CLAUDE.md` once the code no longer uses it.
