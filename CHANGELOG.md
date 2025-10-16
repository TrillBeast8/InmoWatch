# InmoWatch Changelog

## Version 1.0 - October 2025

### Major Features
- **Universal Quick Launcher** - ⋮ button on every screen for instant navigation between all modes (Mouse, Touchpad, Keyboard, Media, D-Pad, Settings)
- **Button-Based D-Pad** - Reliable 3x3 button grid with mode toggle (D-Pad ↔ Scroll) and ESC button
- **Bezel/Rotary Volume Control** - Rotate watch bezel to control volume on MediaScreen
- **Settings Connect Button** - Quick access to "Connect Device" screen when not connected

### Critical Bug Fixes
- **Android 14 Crash Fix** (October 16, 2025) - App no longer crashes on launch with `SecurityException: Starting FGS with type connectedDevice`. Fixed by requesting all three Bluetooth permissions (SCAN, CONNECT, ADVERTISE) before starting foreground service.

### Architecture
- **Platform**: Wear OS 3+ (Samsung Galaxy Watch4)
- **Target Device**: INMO Air2 Smart Glasses
- **Tech Stack**: Kotlin, Jetpack Compose for Wear OS, MVVM + Bluetooth HID Profile
- **Min SDK**: Android 31 (API Level 31)
- **Target SDK**: Android 34 (API Level 34)

---

## Changelog Guidelines

**When to add entries** (✅ YES):
- New user-visible feature (new screen, new button, new gesture)
- Critical bug fixes (crashes, broken functionality)
- Major architecture changes (switching frameworks, refactoring core services)
- Breaking changes (removing features, changing behavior)

**What NOT to document** (❌ NO):
- Code cleanup / refactoring (unless it changes behavior)
- Small UI tweaks (button size, color changes, spacing)
- Performance optimizations (unless user-noticeable)
- Individual line changes / typo fixes
- Internal implementation details

**Rule of thumb**: If a user or future developer wouldn't care, skip it. Keep this changelog scannable in 30 seconds.
