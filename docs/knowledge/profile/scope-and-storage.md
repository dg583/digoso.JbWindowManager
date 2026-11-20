# Profile: scope, storage, usage

As of 2026-11-19 · Source: the user's requirements (brainstorming). Technical feasibility per point was **unverified** at the time (see `../intellij-platform/spike-result.md`).

## What a profile contains
Configuration of the **tool windows** (side bars), per tool window:
- **Anchor/side:** left / right / bottom / top.
- **Split position:** e.g. left/top vs. left/bottom (upper or lower area of a side bar).
- **Visibility:** open or closed.
- **View mode:** Dock Pinned, Dock Unpinned, Undock, Float, Window.
- **Position/size of the window**, if undocked (Undock/Float/Window).

Example: "Explorer" left/top and "Commit" left/bottom must be restored exactly like that on load, including open/closed.

Not in scope (not requested): editor splits, open files, main window size.

## Storage
- **Global** (not per project), format **JSON**, file `<IDE-Config>/digoso.JbWindowManager/profiles.json`.

## Missing tool windows
- Tool windows that are in the profile but do not exist in the current project are **silently skipped** on load.

## Usage
- Submenu **`Window > Profiles`** (existing menu): list of profiles to switch to, "Save as…", "Manage…" (dialog: rename, delete).

## Open
- Shortcuts (not needed for now).
