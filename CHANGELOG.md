# Changelog

Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), versioning follows [SemVer](https://semver.org/).

## [Unreleased]

## [0.2.0] - 2026-11-20

### Added
- Profiles store the main window: fullscreen, maximized, or position and size.

### Changed
- Positions of floating/windowed tool windows and the main window are stored relative to the screen, so profiles adapt to another screen or resolution.
- Profile file format version 2. Version 1 files (0.1.0) are still read; floating windows from them use their absolute position and the main window is left untouched.
- The plugin no longer uses any internal IntelliJ Platform API (previously `DesktopLayout`).

### Known limitations
- Fullscreen handling is untested in the sandbox.

## [0.1.0]

### Added
- Save the tool window arrangement as named profiles, switch, rename and delete them (menu `Window > Profiles`).
