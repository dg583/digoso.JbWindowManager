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

## Consequence
The 5 internal API usages can be removed. Requires a new design (spec) and a new data model: main frame state (fullscreen, maximized, relative bounds) and relative window bounds. Existing profile files (version 1) need a migration or a format version bump.
