# Spike 2: layout without internal API (public API only)

As of 2026-11-20 · Method: throwaway actions `Tools > Spike: Snapshot / Apply snapshot (public API)` in `runIde` (Rider 2026.3, build 263), branch `spike/public-api-only`; evidence from `idea.log` (lines `SNAP`, `AFTER`, `SIZE`, `FRAME`, `WINDOW`). Triggered by the Marketplace check reporting 5 internal API usages (`DesktopLayout`, see `../build/publishing-and-signing.md`).

## Result: everything needed works without `DesktopLayout`
| Property | Public API | Verified |
|---|---|---|
| anchor, split, type, autoHide, open/closed | `ToolWindow.setAnchor/setSplitMode/setType/setAutoHide`, `show/hide` (apply in this order) | yes |
| order | read with `ToolWindowManagerEx.getIdsOn(anchor)`; applying in saved order restores it | yes (user confirmed) |
| width/height of docked windows | read `component.width/height`; set with `ToolWindowEx.stretchWidth/stretchHeight(delta)` | yes, with convergence loop |
| position/size of floating/windowed windows | `SwingUtilities.getWindowAncestor(tw.component).bounds` (read and set) | yes |
| main frame bounds, maximized | `WindowManager.getFrame(project)`: `bounds`, `extendedState` | yes |
| other screen | `frame.graphicsConfiguration.bounds`, store positions as fractions of the main frame's screen | yes (1920x1200 <-> 3440x1440) |
| fullscreen | read `WindowManager.getIdeFrame(project).isInFullScreen`; set by running action `ToggleFullScreen` (`ActionUtil.invokeAction`) when it differs | **unverified**: no run logged `fullscreen=true` |

## Pitfalls found
- **Docked widths scatter** (target 340, got 161/24/289/391) when `stretchWidth(target - component.width)` is computed right after the change: the layout has not settled and windows in one column share the width. Fix: change one window per pass, re-measure in `invokeLater`, repeat until `|diff| <= 2`, max 5 passes. Converged in 1 to 2 passes.
- **Floating/windowed windows must be stored relative**, not as absolute coordinates, or they stay on the old screen after a screen change. Store as fractions of the main frame's screen (fractions outside 0..1 are fine); apply after the main frame has been placed.
- The fullscreen toggle is asynchronous: wait (spike used ~1.2 s timer) before setting bounds; tool window sizes only after the frame has settled.
- `ToolWindow.id` and `getIdsOn` elements are platform types (nullable in Kotlin): filter nulls.
- **`ToolWindowManagerEx.getToolWindows()` is `@ApiStatus.Internal`** (found by `verifyPlugin` after the implementation, not visible in the spike). Use `toolWindowIdSet` + `getToolWindow(id)`. `getIdsOn(anchor)` and `ToolWindowEx.stretchWidth/Height` are not flagged.
- `ActionUtil.invokeAction` (both the `Component` and the `DataContext` overload) is deprecated in 2026.3. `ActionManager.tryToExecute(action, null, component, place, true)` is not flagged.
- Check the verifier report files, not only the console summary: `build/reports/pluginVerifier/<ide>/plugins/<id>/<version>/` has `internal-api-usages.txt` and `deprecated-usages.txt` only when something was found.

## Production run in `runIde` (2026-11-20, after implementation)
- Verified (user, manual): save and apply of version 2 profiles, version 1 profiles (frame untouched, absolute bounds for floating windows), same screen, other screen, maximized, changed order, docked sizes, side change (Terminal bottom <-> right).
- **Sizing needed two more fixes** beyond the spike: (1) wait until every window has the saved anchor/split/type/visibility **and** the docked sizes are unchanged between two polls (a component keeps its old size for a while after `setAnchor`, measured sizes like 1496 px for a right-docked window were stale); (2) one window per pass can block the others if its size does not react to `stretchWidth/Height`: remember the last measured size per window and skip windows that did not change.
- Remaining oddity: a split window on the right (Terminal) sometimes does not react to a large `stretchWidth` delta (seen: delta -216 and -1008) and is skipped with a log warning `Size does not react, skipped`. Cause unknown, not reproducible on demand; visually acceptable for the user.
- **Fullscreen: still unverified.** No saved profile ever had `fullscreen: true`; neither `isInFullScreen` nor the `ToggleFullScreen` action were observed working. The code is defensive (3 s timeout, then continues).

## Consequence
The 5 internal API usages can be removed. Requires a new design (spec) and a new data model: main frame state (fullscreen, maximized, relative bounds) and relative window bounds. Existing profile files (version 1) need a migration or a format version bump.

## Review fixes (2026-11-20, whole-branch review)
- **Overlapping applies:** a second apply (two quick clicks) interleaved with the first and could leave the first profile's state. Each apply now has a generation per project; every asynchronous step stops when the project is closed or a newer apply started (`Run` in `LayoutApply.kt`). Platform-bound, verified by compile and manual run, no unit test.
- **Validation:** `ProfileJson.fromJson` rejects `rel` with missing/non-finite/non-positive values and a non-finite `weight` (Gson turns missing numbers into 0.0 and accepts NaN, which would apply a 0x0 rect or throw in `roundToInt`).
- **Size passes:** a window is skipped as "does not react" only after two unchanged measurements (hypothesis, unverified: the first re-measure after `stretchWidth/Height` can still be stale, which would explain the Terminal oddity above).
- Known, deferred (see ledger): settle wait times out (3 s) for windows that cannot reach the saved visibility; un-maximize followed by `bounds` may race on Windows; a floating window whose component is not yet reparented would resolve to the main frame as window ancestor; v1 `weight` may be relative to the tool window pane (a few px off).
