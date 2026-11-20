# Spike result: reading and applying tool windows in Rider 2026.3

> **Superseded in part (2026-11-20):** the conclusion that only `setLayout` restores window positions is wrong. Position, order and size work through the public API plus the Swing window, see [Spike 2](public-api-spike.md). The text below stays as the record of spike 1.

As of 2026-11-19 · Method: throwaway actions `Tools > Spike: …` in the `runIde` sandbox (Rider 2026.3, build 263); the user checked manually, evidence from `idea.log` (lines `SNAP`, `APPLY`, `AFTER-PUBLIC`, `AFTER-SETLAYOUT`). One project, 16 entries in the layout.

## Answers to the open questions
1. **Setter order (public API):** `setAnchor` → `setSplitMode` → `setType` → `setAutoHide` → `show/hide` correctly restores side, split (e.g. Project left/top, Commit left/bottom) and open/closed. 39 calls without error, 0 exceptions.
2. **Window position (`floatingBounds`):** the public API does **not** restore it. After `AFTER-PUBLIC` a moved floating window (Database) stayed at the new position (x=1529/1300 instead of 1014). **`ToolWindowManagerEx.setLayout(DesktopLayout)` restores it** (`AFTER-SETLAYOUT`: x=1014, y=1794 as recorded).
3. **Rider:** behaves like IntelliJ, nothing Rider-specific observed.
4. **Internal API usable:** `ToolWindowManagerEx.getInstanceEx(project).getLayout()/setLayout()`, `DesktopLayout.copy()/getInfos()` and `WindowInfoImpl` compile against Rider 2026.3 and run on the EDT without errors.

## Further observations
- `getLayout().getInfos()` also contains tool windows that are **not registered** in the opened project (Gradle, Maven, Problems). `ToolWindowManager.getToolWindow(id)` returns `null` for them, so the skip path holds.
- `floatingBounds` is only set for `FLOATING`/`WINDOWED`, otherwise `null`.
- `order` can be `-1` (Database.Output). `weight` is a fraction (0 to 1) of the width/height.
- IDs are not display names (e.g. "Explorer" = `Project`).

## Decision for `LayoutApply`
Apply via **`setLayout`**: take a copy of the current layout (`getLayout().copy()`), for each profile entry whose id exists in the copy set the `WindowInfoImpl` fields (`anchor`, `isSplit`, `type`, `isAutoHide`, `isVisible`, `order`, `weight`, `floatingBounds`), then `setLayout(copy)`. Profile entries without a counterpart are skipped, windows the profile does not know stay unchanged.
Unverified: whether `setLayout` with **changed** infos (instead of an unchanged copy, as in the spike) applies everything cleanly. This was checked manually in task 6 with the example "Project left/top, Commit left/bottom"; had it failed, the fallback would have been the public API plus `setLayout` only for bounds.
Risk: `DesktopLayout`/`WindowInfoImpl` are internal classes and may change between versions (`verifyPlugin`, task 8).
