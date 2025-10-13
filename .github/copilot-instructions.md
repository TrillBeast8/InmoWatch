# GitHub Copilot Workspace Instructions - InmoWatch
## Professional Wear OS HID Input Controller for INMO Air2 & Galaxy Watch4

> **Document Type**: AI Assistant Instructions  
> **Target Models**: GitHub Copilot, Claude, Sonnet, GPT-4, Gemini, and all LLMs  
> **Last Updated**: October 2025  
> **Project Language**: Kotlin (Jetpack Compose for Wear OS)  
> **Architecture**: MVVM + Bluetooth HID Profile

---

## ⚠️ CRITICAL: Documentation Guidelines for AI Assistants

**READ THIS FIRST** - Rules for GitHub Copilot, Claude, Sonnet, GPT, and all AI models:

### 🚫 **NEVER Create Redundant Documentation Files**

**THIS FILE (`copilot-instructions.md`) IS THE SINGLE SOURCE OF TRUTH.**

❌ **DO NOT CREATE**:
- Separate summary files (UI_IMPROVEMENTS_SUMMARY.md, CHANGES.md, etc.)
- Duplicate design guides (DPAD_DESIGN.md, SCREEN_GUIDE.md, etc.)
- Redundant quick reference documents
- Architecture overviews in separate files
- "What changed" documentation files

✅ **INSTEAD**:
- **UPDATE THIS FILE ONLY** - All documentation goes here
- Use the **Quick Reference Guide** section below for navigation
- Add new sections to existing screen documentation
- Update inline examples and code patterns
- Keep everything in one searchable location

### 📝 **When User Requests Documentation**

**If user asks**: *"Can you document the new feature?"*
- ✅ **DO**: Add/update the relevant section in THIS file
- ❌ **DON'T**: Create a new markdown file

**If user asks**: *"Can you create a guide for X?"*
- ✅ **DO**: Add a new subsection under the appropriate heading in THIS file
- ❌ **DON'T**: Create X_GUIDE.md

**If user asks**: *"Can you summarize what changed?"*
- ✅ **DO**: Provide a verbal summary in chat OR update the relevant sections here
- ❌ **DON'T**: Create SUMMARY.md or CHANGES.md

### 🎯 **File Creation Rules**

**ONLY create NEW files for**:
1. **Source code** (`.kt`, `.java`, `.xml`, etc.)
2. **Build/Config files** (`build.gradle.kts`, `AndroidManifest.xml`, etc.)
3. **Resources** (images, strings, layouts)
4. **Project metadata** (`README.md` - if it doesn't exist)

**NEVER create files for**:
- Documentation that belongs in THIS file
- Temporary summaries or change logs
- Design reference guides
- Architecture diagrams (use Mermaid blocks in THIS file instead)

### 🔍 **Before Creating Any .md File**

**ASK YOURSELF**:
1. Does `copilot-instructions.md` already cover this topic? → **Update that section**
2. Is this a one-time explanation? → **Provide in chat, don't create file**
3. Is this permanent project documentation? → **Add to THIS file**
4. Is this a README for the repo root? → **Only create if missing**

### ✅ **Correct Workflow Example**

User: *"Document the new circular D-pad design"*

```
❌ WRONG:
- Create DPAD_CIRCULAR_DESIGN.md
- Create DPAD_REFERENCE.md
- Create UI_DESIGN_GUIDE.md

✅ RIGHT:
- Update the "DpadScreen" section in copilot-instructions.md
- Add code examples inline
- Update "Animation Standards" section if needed
```

### 📚 **This File Structure**

All documentation sections are already in THIS file:
- **Quick Reference** - Jump to any topic
- **Project Overview** - Architecture, technologies, platforms
- **Core Components** - HidService, HidClient, HidInputManager
- **UI Screens** - Mouse, Touchpad, D-Pad, Keyboard, Media, Settings
- **Design Principles** - Seamless, fluid, elegant UX standards
- **Animation Standards** - Spring physics, transitions
- **Development Guidelines** - Code style, testing, debugging
- **Navigation System** - Routes and patterns
- **Performance** - Timing, optimization, battery

**If a section is missing** → Add it HERE, don't create a new file.

---

## 📋 AI Parsing Conventions

**This section defines how AI models should interpret this document.**

### Document Structure
- **Headers**: `##` = Major section, `###` = Subsection, `####` = Component/File
- **Code blocks**: All code examples are production-ready and should be used as-is
- **Checkmarks**: ✅ = MUST follow this rule, ❌ = NEVER do this
- **File paths**: Always in backticks (`` `path/to/file.kt` ``)
- **Component names**: **Bold** with file path in parentheses

### Critical Rules Formatting
```
✅ **Rule to follow** - Explanation of what to do
❌ **Anti-pattern** - Explanation of what NOT to do
```

### Code Example Format
````kotlin
// Context comment explaining purpose
fun exampleFunction() {
    // Implementation with inline comments
}
````

### Navigation Links
- Internal links use `#section-name-lowercase-with-dashes`
- Jump to specific files: `[FileName](#filename-pathfilenamekt)`
- Quick reference at top provides main navigation

### Required Sections
1. **Project Overview** - Technologies, platforms, architecture
2. **Core Components** - Service layer, business logic
3. **UI Screens** - All Composable screens with patterns
4. **Design Principles** - UX standards and animations
5. **Development Guidelines** - Code style, testing, debugging

### When to Update This File
- ✅ New feature added → Update relevant screen section
- ✅ Architecture change → Update Core Components section
- ✅ UI pattern change → Update Design Principles section
- ✅ Bug fix with pattern → Add to Development Guidelines
- ❌ One-time change log → Provide verbally, don't add here

---

## 🚨 CRITICAL: Build Error Detection (FOR ALL AI ASSISTANTS)

**⚠️ MANDATORY FOR ALL LLMs/AI/COPILOTS - READ THIS BEFORE MAKING ANY CODE CHANGES**

### ❌ **PROBLEM: Static Analysis Tools Miss Real Compilation Errors**

The `get_errors` tool and IDE static analysis **ONLY** show warnings and IDE-level errors. They **DO NOT** show actual Gradle/Kotlin compilation errors that occur during `gradlew build`.

**This means**:
- ❌ You can check a file with `get_errors` and see "No errors"
- ❌ But the file might still have **syntax errors** that break the build
- ❌ The user will see 10+ compilation failures when building
- ❌ You won't know about them until the user pastes the build output

### ✅ **SOLUTION: Always Ask User to Share Build Output**

**BEFORE claiming "no errors found"**, you MUST:

1. **Ask the user to run a build** and share the output:
   ```
   "Please run 'gradlew.bat assembleDebug' (or 'Build > Rebuild Project' in Android Studio) 
   and paste the full build output here so I can see actual compilation errors."
   ```

2. **Look for these patterns in build output**:
   - `> Task :app:compileDebugKotlin FAILED`
   - `e: file:///path/to/file.kt:123:5 Expecting an element`
   - `e: file:///path/to/file.kt:456:25 Unresolved reference`
   - `Compilation error. See log for more details`
   - `BUILD FAILED`

3. **Never say "no errors found" based only on `get_errors` results**
   - ✅ DO: "I see no IDE warnings. Please run a build to check for compilation errors."
   - ❌ DON'T: "Your project has no errors!" (when you only checked static analysis)

### 📋 **Correct Build Error Workflow**

```
USER: "Fix my build errors"

❌ WRONG APPROACH:
1. Run get_errors on all files
2. See "No errors found"
3. Tell user "Everything looks good!"
4. User runs build → 14 compilation errors
5. User has to paste build output manually

✅ RIGHT APPROACH:
1. Ask user: "Please share your full build output (gradlew assembleDebug or Build > Rebuild)"
2. User pastes: "> Task :app:compileDebugKotlin FAILED" with error list
3. You see real errors: "MouseScreen.kt:123:5 Expecting an element"
4. Fix actual compilation errors
5. Verify with user's next build
```

### 🔍 **How to Identify Real Build Errors in Output**

Look for these exact patterns (case-sensitive):

```
> Task :app:compileDebugKotlin FAILED
e: file:///C:/path/file.kt:LINE:COL Error message here
e: file:///C:/path/file.kt:LINE:COL Unresolved reference: Something

FAILURE: Build failed with an exception.
* What went wrong:
Execution failed for task ':app:compileDebugKotlin'.
```

**Each `e:` line is ONE compilation error** - count them all.

### 🎯 **Summary for All AI Models**

- `get_errors` = IDE warnings only (incomplete)
- **Real errors** = In Gradle build output (complete)
- Always request build output when user mentions "errors"
- Never claim "no errors" without seeing actual build results

---

## 🛡️ CRITICAL: Safe Code Editing Rules (FOR ALL AI ASSISTANTS)

**⚠️ MANDATORY - NEVER WIPE OUT EXISTING CODE**

### ❌ **PROBLEM: Using `create_file` on Existing Files Destroys Code**

When you use `create_file` on a file that already exists, it **completely replaces** the file content. This is catastrophic if the file has hundreds of lines and you only meant to fix a small section.

**Real example that happened**:
- User had `MouseScreen.kt` with 250 lines of working code
- AI used `create_file` to "fix" a syntax error on line 123
- **Result**: Entire file replaced with just the corrected 240 lines
- **Lost**: Any code after line 240 was permanently deleted
- User response: *"that was horrible doesn't code like that again"*

### ✅ **SOLUTION: Always Use `insert_edit_into_file` for Existing Files**

**The `insert_edit_into_file` tool is smart** - it understands minimal edits and preserves surrounding code.

**Rules**:
1. ✅ **ALWAYS** use `insert_edit_into_file` when editing existing files
2. ❌ **NEVER** use `create_file` on files that already exist
3. ✅ **Use comments** like `// ...existing code...` to represent unchanged sections
4. ✅ **Show only the changed lines** plus minimal context

### 📋 **Correct vs Wrong Editing Patterns**

**❌ WRONG - Using `create_file` (Destroys entire file)**:
```kotlin
// User's file has 500 lines, you need to fix line 123

create_file("MouseScreen.kt", """
package com.example...
// Paste all 500 lines with tiny fix on line 123
""")

RESULT: If you miss any lines, they're GONE FOREVER
```

**✅ RIGHT - Using `insert_edit_into_file` (Safe, surgical edit)**:
```kotlin
// User's file has 500 lines, you need to fix line 123

insert_edit_into_file("MouseScreen.kt", """
    Scaffold(
        timeText = { TimeText() },
        modifier = Modifier.detectTwoFingerSwipe(...)
    ) {
        // Remove the duplicate line below ↓ (was causing error)
        Box(modifier = Modifier.fillMaxSize()) {
            // ...existing code...
        }
    }
""")

RESULT: Only lines 120-130 are changed, rest is preserved
```

### 🎯 **How to Use `insert_edit_into_file` Correctly**

**Pattern 1 - Small Fix (Duplicate Line Removal)**:
```kotlin
// CONTEXT: File has duplicate closing brace on line 123

insert_edit_into_file(
    filePath = "path/to/MouseScreen.kt",
    explanation = "Remove duplicate closing brace on line 123",
    code = """
    Scaffold(...) {
        Box(...) {
            // ...existing code...
        }
    }
    """
)
```

**Pattern 2 - Import Addition**:
```kotlin
// CONTEXT: Missing import causing "Unresolved reference: Text"

insert_edit_into_file(
    filePath = "path/to/TouchpadScreen.kt", 
    explanation = "Add missing Text import from Wear Compose Material",
    code = """
    package com.example.inmocontrol_v2.ui.screens
    
    import androidx.compose.animation.*
    import androidx.wear.compose.material.Text
    // ...existing imports...
    """
)
```

**Pattern 3 - Function Change**:
```kotlin
// CONTEXT: Fix function parameter type on line 456

insert_edit_into_file(
    filePath = "path/to/HidClient.kt",
    explanation = "Change parameter type from Int to Float",
    code = """
    fun sendMouseMovement(deltaX: Float, deltaY: Float): Boolean {
        // ...existing code...
    }
    """
)
```

### 🚫 **When `create_file` IS Allowed**

**ONLY use `create_file` when**:
1. ✅ Creating a **brand new file** that doesn't exist yet
2. ✅ Creating **utility scripts** (like cleanup.bat)
3. ✅ Creating **new test files**

**Example - This is SAFE**:
```kotlin
create_file("NewFeatureScreen.kt", """
// This file doesn't exist yet, safe to create
package com.example...
""")
```

### 📝 **Minimal Context Pattern**

When using `insert_edit_into_file`, show **just enough context** for the tool to locate the change:

```kotlin
// ✅ GOOD - Minimal but clear
fun handleClick() {
    // ...existing code...
    if (isConnected) {  // ← Fixed: was missing this check
        HidClient.sendLeftClick()
    }
    // ...existing code...
}

// ❌ TOO MUCH - Don't repeat entire function
fun handleClick() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // ...200 lines of unchanged code...
    if (isConnected) {  // ← The actual fix
        HidClient.sendLeftClick()
    }
    // ...100 more lines of unchanged code...
}
```

### 🎯 **Summary for All AI Models**

- **Existing file?** → Use `insert_edit_into_file` (preserves code)
- **New file?** → Use `create_file` (safe to create)
- **Show minimal context** → Use `// ...existing code...` comments
- **Never rewrite entire files** → User will lose work and be angry

---

## 📖 Quick Reference Guide

**For screen-specific UI patterns**, jump to:
- [MouseScreen](#mousescreen-uiscreensmousescreenkt) - Seamless sensor-based cursor control
- [TouchpadScreen](#touchpadscreen-uiscreenstouchpadscreenkt) - Elegant drag-based input
- [DpadScreen](#dpadscreen-uiscreensdpadscreenkt) - Circular touch navigation
- [MediaScreen](#mediascreen-uiscreensMediascreenkt) - Gesture-based media controls
- [KeyboardScreen](#keyboardscreen-uiscreenskeyboardscreenkt) - System keyboard HID input

**For architecture/service patterns**, see:
- [HidService](#hidservice-hidhidservicekt) - Bluetooth HID orchestrator
- [HidClient](#hidclient-hidhidclientkt) - UI-facing singleton API
- [HidInputManager](#hidinputmanager-bluetoothhidinputmanagerkt) - HID report transmission

**For UI/UX standards**, see:
- [UI Design Principles](#-summary) - Seamless, fluid, elegant design philosophy
- [Animation Standards](#-summary) - Spring physics, AnimatedVisibility patterns

---

## 🎯 Project Overview

**InmoWatch** is a highly optimized Wear OS application that transforms Galaxy Watch4 into a universal Bluetooth HID (Human Interface Device) controller. The app provides mouse, touchpad, keyboard, media controls, and D-pad input modes for controlling INMO Air2 smart glasses and other Bluetooth-enabled devices.

### ✨ New Features (October 2025)

**1. Universal Quick Launcher** - Beautiful circular menu system
- **⋮ button** on every screen (replaces old X/Back buttons)
- Instant navigation to any mode without returning to main menu
- Circular arrangement of 6 modes (Mouse, Touchpad, Keyboard, Media, D-Pad, Settings)
- Elegant fade/scale animations with spring physics
- Current mode highlighted with reduced opacity
- File: `ui/components/QuickLauncher.kt`

**2. Bezel/Rotary Input for MediaScreen**
- **Rotate watch bezel** to control volume on target device
- Clockwise rotation = Volume Up
- Counter-clockwise rotation = Volume Down
- Visual feedback with "Vol +" / "Vol -" overlay
- Uses Wear OS `onRotaryScrollEvent` modifier
- **Universal control** - works with any HID device

**3. Button-Based D-Pad** (October 13, 2025)
- ❌ **REMOVED**: Gesture-based circular D-pad (unreliable)
- ✅ **NEW**: Physical button layout for reliable input
- 3x3 button grid: `↖ ↑ ↗` / `← ● →` / `↙ ↓ ↘`
- Mode toggle: D-Pad (8-way directional) ↔ Scroll (4-way + click)
- Diagonal buttons hidden in scroll mode
- Smooth spring animations for feedback
- Quick Launcher integration (⋮ button)
- File: `ui/screens/DpadScreen.kt` (completely rewritten)

**4. Removed Two-Finger Swipe Navigation** (October 13, 2025)
- ❌ **REMOVED**: Two-finger horizontal swipe gesture (caused conflicts)
- Reason: Interfered with single-finger gestures and touch detection
- Replaced by: Quick Launcher menu on all screens
- Deleted file: `ui/gestures/TwoFingerSwipe.kt`

### Target Platforms
- **Primary**: Samsung Galaxy Watch4 (Wear OS 3+)
- **Secondary**: INMO Air2 Smart Glasses
- **Compatibility**: Android 31+ (API Level 31), Wear OS 3.0+

### Key Technologies
- **Language**: Kotlin 1.9+
- **UI Framework**: Jetpack Compose for Wear OS
- **Bluetooth Stack**: BluetoothHidDevice API (Android HID over Bluetooth)
- **Architecture**: MVVM with StateFlow, Coroutines, DataStore
- **Sensors**: Rotation Vector Sensor for motion-based cursor control

---

## 🏗️ Architecture & Core Components

### 1. Application Entry Point
**File**: `MainActivity.kt`

**Responsibilities**:
- Boots the Wear OS app lifecycle
- Requests runtime Bluetooth permissions (BLUETOOTH_SCAN, BLUETOOTH_CONNECT, BLUETOOTH_ADVERTISE)
- Starts and binds `HidService` as a foreground service
- Routes navigation through Jetpack Compose NavHost
- Handles double-back press exit logic via `SettingsStore.remoteBackDoubleClick`

**Critical Rules**:
- ✅ **NEVER** call BluetoothHidDevice APIs directly from UI code
- ✅ All Bluetooth interactions **MUST** route through `HidService` and `HidClient`
- ✅ Preserve foreground service lifecycle to avoid Samsung doze killing connections
- ✅ Use `startForegroundService()` on API 26+ for background Bluetooth operations

---

### 2. Bluetooth HID Service Layer

#### **HidService** (`hid/HidService.kt`)
**Core HID orchestrator** - Manages Bluetooth HID profile registration, device connections, and input dispatching.

**Components**:
- `HidDeviceProfile`: Registers Bluetooth HID profile proxy with the Android stack
- `HidDeviceApp`: Defines SDP/QoS descriptors and app registration callbacks
- `HidInputManager`: Assembles and sends HID reports for all input modes

**Critical Rules**:
- ✅ Register/unregister HID profile **on main thread only**
- ✅ Release proxy resources in `unregisterServiceListener()` to prevent memory leaks
- ✅ Maintain foreground notification while paired to dodge Samsung doze
- ✅ When host transitions to `BluetoothProfile.STATE_DISCONNECTING`, retry `registerApp()` with exponential backoff (100ms → 500ms → 1s)

#### **HidClient** (`hid/HidClient.kt`)
**UI-facing singleton** - Exposes StateFlow signals and HID actions to Compose screens.

**State Flows**:
- `isConnected: StateFlow<Boolean>` - Real-time connection status
- `connectionError: StateFlow<String?>` - Error messages for UI display
- `isServiceReady: StateFlow<Boolean>` - HID service availability
- `hidProfileAvailable: StateFlow<Boolean>` - Profile registration status

**Input Methods** (all return `Boolean` success):
```kotlin
// Mouse
fun sendMouseMovement(deltaX: Float, deltaY: Float): Boolean
fun sendLeftClick(): Boolean
fun sendRightClick(): Boolean
fun sendMiddleClick(): Boolean
fun sendDoubleClick(): Boolean
fun sendScroll(deltaX: Float, deltaY: Float): Boolean

// Media Controls
fun playPause(): Boolean
fun nextTrack(): Boolean
fun previousTrack(): Boolean
fun volumeUp(): Boolean
fun volumeDown(): Boolean
fun mute(): Boolean

// D-Pad/Navigation
fun dpad(direction: Int): Boolean // 0=UP, 1=DOWN, 2=LEFT, 3=RIGHT, 4=CENTER, 5-8=diagonals
fun sendDpadUp/Down/Left/Right/Center(): Boolean

// Keyboard
fun sendKey(keyCode: Int, modifiers: Int = 0): Boolean
fun sendText(text: String): Boolean

// Raw HID
fun sendRawInput(data: ByteArray): Boolean
```

**Critical Rules**:
- ✅ **ALWAYS** use `HidClient` methods in UI code, never direct service calls
- ✅ Keep all methods synchronous (`Boolean` return) for instant Compose reactivity
- ✅ Collect StateFlows with `.collectAsState()` in Composables for reactive UI updates

#### **HidInputManager** (`bluetooth/HidInputManager.kt`)
**HID report assembly engine** - Converts high-level input actions into raw HID reports.

**Report Structure** (matches WearMouse descriptor):
```kotlin
// Report ID 1: Keyboard (8 bytes)
// [modifier, reserved, key1, key2, key3, key4, key5, key6]

// Report ID 2: Mouse (4 bytes)
// [buttons, deltaX, deltaY, wheel]

// Report ID 3: Consumer (2 bytes)
// [usage_LSB, usage_MSB]
```

**Timing Constants** (optimized for responsiveness):
```kotlin
PRESS_RELEASE_DELAY_MS = 15L    // Delay between press and release reports
DEBOUNCE_DELAY_MS = 30L         // Minimum interval for media controls only
KEY_REPEAT_DELAY_MS = 3L        // Delay between characters in text
```

**Critical Rules**:
- ✅ Use `sendReport(device, reportId, data)` for all HID transmissions
- ✅ **ALWAYS** schedule zeroed release report via coroutine scope to prevent sticky inputs
- ✅ Reuse helpers (`sendMouseMovement`, `sendMouseScroll`, `sendConsumerControl`) wherever possible
- ✅ For rapid key bursts, enqueue into Channel and flush at ~3ms intervals to balance responsiveness vs power
- ✅ Keyboard layout: Modifier byte (0x01=CTRL, 0x02=SHIFT, 0x04=ALT, 0x08=WIN), then 6 key slots
- ✅ Mouse deltas **MUST** be clamped to `[-127, 127]` range
- ✅ Stop coroutine scope in `cleanup()` to prevent leaks
- ✅ **Mouse clicks do NOT use debouncing** - only media controls use 30ms debounce
- ✅ Press-release delay is 15ms (optimized from 20ms for faster response)
- ✅ Key repeat delay is 3ms (optimized from 5ms for faster typing)

---

### 3. Input Modes & UI Screens

All Composables in `ui/screens/` consume state via StateFlows and callbacks. **Keep business logic in ViewModel
