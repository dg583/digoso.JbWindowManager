# Tool window API: what can be read and set?

As of 2026-11-19 · Method: source code of `JetBrains/intellij-community`, branch `252` (2025.2) and a spot check on `263` (= Rider 2026.3, the plugin's target: `ToolWindow.setAnchor/setSplitMode/setType`, `ToolWindowManagerEx.getLayout/setLayout`, `DesktopLayout.getInfos` still present unchanged), read via `gh api`. **Not tested in a running IDE** at the time of writing (the later spike confirmed the main points: see `spike-result.md`). Rider frontend behavior was unverified here.

## Conclusion
Everything a profile needs is **readable**. **Setting** side, split, view mode and visibility works through the public API. For **position/size and order** there are no public setters, only the internal route via `DesktopLayout`.

## Public API (stable)
`com.intellij.openapi.wm.ToolWindow` (`platform/ide-core/.../wm/ToolWindow.java`) and `ToolWindowManager` (`platform/platform-api/.../wm/ToolWindowManager.kt`):

| Profile field | Read | Set |
|---|---|---|
| Side (left/right/bottom/top) | `getAnchor()` | `setAnchor(anchor, runnable)` |
| Split (upper/lower area) | `isSplitMode()` | `setSplitMode(split, runnable)` |
| View mode | `getType()`: `DOCKED`, `FLOATING`, `SLIDING`, `WINDOWED` | `setType(type, runnable)` |
| Auto-hide (unpinned) | `isAutoHide()` | `setAutoHide(bool)` |
| open/closed | `isVisible()` | `show(runnable)` / `hide(runnable)` |
| Stripe button visible | `isShowStripeButton()` | `setShowStripeButton(bool)` |
| All ids / access | `ToolWindowManager.toolWindowIds`, `getToolWindow(id)` (returns `null` for missing ones, so easy to skip) | – |

Note: the view mode in the UI is a combination of `type` and `isAutoHide` (Dock Pinned = `DOCKED`, Dock Unpinned = `DOCKED` + autoHide, Undock = `SLIDING`, Float = `FLOATING`, Window = `WINDOWED`). **Mapping unverified.**

## Gaps in the public API
- **Window position/size (floatingBounds):** only indirectly via `setDefaultState(anchor, type, floatingBounds)` (default only, does not reliably affect already registered windows – unverified). No getter.
- **Size of docked windows (`weight`, `sideWeight`):** only `stretchWidth/stretchHeight` (`ToolWindowEx`, `platform-impl`).
- **Order in the stripe (`order`):** no public access.

## Internal route (covers everything)
- `ToolWindowManagerEx.getInstanceEx(project)` (`platform/platform-impl/.../wm/ex/ToolWindowManagerEx.kt`) offers `getLayout(): DesktopLayout` and `setLayout(DesktopLayout)`. Both on the **EDT** only.
- `DesktopLayout.getInfos(): Map<String, WindowInfoImpl>`, `copy()`.
- `WindowInfoImpl` / interface `WindowInfo` (`platform-api`) contain: `id`, `anchor`, `isSplit`, `order`, `weight`, `sideWeight`, `isVisible`, `type`, `internalType`, `isAutoHide`, `floatingBounds`, `isMaximized`, `isShowStripeButton`, `toolWindowPaneId`.
- Risk: `ToolWindowManagerImpl` is `@ApiStatus.Internal`, `DesktopLayout` lives in `platform-impl`. May change between versions (the Plugin Verifier reports it).

## Similar in the platform: named layouts
`ToolWindowDefaultLayoutManager` (`platform/platform-impl/.../toolWindow/`, file exists on 252) stores named layouts (active layout, save, restore, rename, delete) and is presumably the basis of `Window > Layouts`. Methods read on `master`; **not verified on 252**. Not needed for our goal (own JSON profiles, own management), but useful as a reference.

## Recommendation for the design
1. **Read:** `getLayout().getInfos()` (all fields incl. bounds/weight/order), only for ids that exist.
2. **Own JSON format** with the fields above (do not serialize `WindowInfoImpl` directly, to stay decoupled from internals).
3. **Apply:** first via the public API (anchor, split, type, autoHide, visible). If position/size/order cannot be restored that way, additionally via a `DesktopLayout` copy + `setLayout`. Decision by **spike in `runIde`**.

## To be verified (spike)
- Do `setAnchor` + `setSplitMode` + `setType` in the right order produce exactly "Explorer left/top, Commit left/bottom"?
- Can `floatingBounds` really be restored for `FLOATING`/`WINDOWED`?
- Does Rider (frontend, possibly split mode with backend) behave like IntelliJ?
- Do `ToolWindowManagerEx`/`DesktopLayout` compile and verify against Rider 2026.3?
