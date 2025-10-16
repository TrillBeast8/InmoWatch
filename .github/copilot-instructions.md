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

### 📝 **When to Update CHANGELOG.md**

**ONLY add to CHANGELOG.md if**:
- ✅ User can see/feel the change (new feature, fixed crash, removed button)
- ✅ Major architecture shift (new library, rewritten service)
- ✅ Breaking change (removed feature, changed workflow)
- ✅ Critical bug fix (app crash, broken core functionality)

**DON'T add to CHANGELOG.md if**:
- ❌ Code refactoring with no behavior change
- ❌ Small UI polish (font size, spacing, color tweak)
- ❌ Internal code cleanup or optimization
- ❌ Single-line bug fixes in existing features
- ❌ Comment updates or documentation tweaks

**Rule of thumb**: If you're unsure, DON'T add it. Changelogs should be scannable in 30 seconds.

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

**Exception**: `CHANGELOG.md` in repo root is the ONLY separate documentation file allowed. Use it for user-facing feature history only.

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

## � CRITICAL: Read Instructions First Protocol (FOR ALL AI ASSISTANTS)

**⚠️ MANDATORY - ALWAYS READ THIS FILE BEFORE GENERATING SOLUTIONS**

### ✅ **Required Workflow for Every User Request**

**BEFORE** proposing or implementing ANY solution, you MUST:

1. ✅ **READ** this entire `copilot-instructions.md` file
   - Check for existing patterns that solve the problem
   - Review relevant screen/component sections
   - Understand architecture constraints and rules
   - Identify any anti-patterns to avoid

2. ✅ **SEARCH** the workspace for similar implementations
   - Use `semantic_search` or `grep_search` to find existing code
   - Check how similar features are already implemented
   - Reuse established patterns instead of inventing new ones

3. ✅ **VERIFY** your solution follows documented standards
   - Animation patterns (spring physics, timing)
   - State management (StateFlow, collectAsState)
   - HID communication (always via HidClient)
   - UI principles (seamless, fluid, elegant)

4. ✅ **UPDATE** this file AFTER implementing changes
   - Add new patterns to relevant sections
   - Document new components or screens
   - Update code examples if behavior changed
   - Add to "What Changed" section if major feature

### ❌ **Anti-Patterns to AVOID**

```
❌ WRONG:
User: "Add a new button to MouseScreen"
AI: *Immediately creates code* "Here's the button implementation!"

✅ RIGHT:
User: "Add a new button to MouseScreen"
AI: *Reads copilot-instructions.md*
    *Searches for "MouseScreen" implementation*
    *Reviews button patterns in other screens*
    *Checks animation standards*
    → Then proposes solution following established patterns
    → After implementation, updates MouseScreen section in this file
```

### 🎯 **Why This Matters**

- **Consistency**: All screens follow the same animation, state, and interaction patterns
- **Quality**: Reuse battle-tested code instead of reinventing bugs
- **Documentation**: This file stays current and useful
- **Efficiency**: Faster development by following proven patterns

**If you skip reading instructions**:
- ❌ You'll create duplicate implementations
- ❌ You'll violate established patterns
- ❌ You'll introduce bugs that were already solved
- ❌ You'll waste user's time with inconsistent code

---

## 👤 CRITICAL: Agent Mode Approval Protocol (FOR ALL AI ASSISTANTS)

**⚠️ MANDATORY - ALWAYS GET USER APPROVAL BEFORE IMPLEMENTING IN AGENT MODE**

### ✅ **Required Workflow When Operating in Agent/Autonomous Mode**

When the user has enabled **agent mode**, **autonomous mode**, or **multi-step mode**, you MUST:

1. ✅ **STOP** before making any code changes
2. ✅ **PROPOSE** your solution with clear explanation:
   ```
   📋 PROPOSED SOLUTION:
   
   I will make the following changes:
   
   1. Update `MouseScreen.kt` (lines 120-135)
      - Add new "Reset" button to toolbar
      - Wire up to HidClient.resetCursor()
   
   2. Add new function to `HidClient.kt`
      - resetCursor(): Boolean
      - Sends [0,0] mouse movement
   
   3. Update `copilot-instructions.md`
      - Document new reset pattern
      - Add to MouseScreen section
   
   ⏸️ **WAITING FOR YOUR APPROVAL**
   Reply with:
   - "approve" or "yes" to proceed
   - "no" or "stop" to cancel
   - Suggestions for changes to the approach
   ```

3. ✅ **WAIT** for explicit user confirmation
   - User says: "approve", "yes", "go ahead", "do it" → Proceed
   - User says: "no", "stop", "wait", "cancel" → Halt immediately
   - User provides feedback → Revise proposal and ask again

4. ✅ **IMPLEMENT** only after approval received

5. ✅ **REPORT** what was done after completion

### ❌ **What NOT to Do in Agent Mode**

```
❌ WRONG (Auto-implementing without approval):
User: "Fix the mouse cursor drift issue"
AI: *Immediately edits MouseScreen.kt*
    *Immediately edits HidClient.kt*
    *Done!*

✅ RIGHT (Propose first, implement after approval):
User: "Fix the mouse cursor drift issue"
AI: *Reads instructions*
    *Analyzes code*
    *Proposes solution with file-by-file breakdown*
    
    📋 PROPOSED SOLUTION:
    1. Add cursor reset to MouseScreen.kt
    2. Implement HidClient.resetCursor()
    3. Update docs
    
    ⏸️ WAITING FOR YOUR APPROVAL
    
User: "approve"
AI: *Now implements changes*
    ✅ Changes complete!
```

### 🎯 **Why This Protocol Exists**

**Without approval protocol**:
- ❌ AI might misunderstand requirements and waste time
- ❌ User can't review approach before code is changed
- ❌ Mistakes are harder to undo after implementation
- ❌ User loses control over their codebase

**With approval protocol**:
- ✅ User reviews approach before any changes
- ✅ Catches misunderstandings early
- ✅ User can suggest improvements to the plan
- ✅ User maintains full control

### 📋 **Approval Request Template**

**Use this format for every proposal**:

```markdown
📋 PROPOSED SOLUTION:

**Problem**: [Brief description of what needs to be fixed/added]

**Approach**: [High-level strategy]

**Changes**:
1. **File**: `path/to/file.kt` (lines X-Y)
   - [What will change]
   - [Why it's needed]

2. **File**: `path/to/other.kt` (new file)
   - [What will be created]
   - [Purpose]

3. **Documentation**: Update `copilot-instructions.md`
   - [What sections will be updated]

**Testing**: [How to verify the changes work]

⏸️ **WAITING FOR YOUR APPROVAL**
Reply "approve" to proceed, "no" to cancel, or provide feedback.
```

### 🚨 **EXCEPTION: Emergency Bug Fixes**

**ONLY skip approval if**:
- ✅ User explicitly says "just fix it" or "do it now"
- ✅ User uses urgent language ("emergency", "critical bug", "broke production")
- ✅ Change is trivial (fixing typo, missing import, syntax error)

**Otherwise, ALWAYS get approval first.**

---

## �🚨 CRITICAL: Build Error Detection (FOR ALL AI ASSISTANTS)

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

### 🔄 Feature History

**For detailed changelog and feature history**, see [`CHANGELOG.md`](../CHANGELOG.md) in the repo root.

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
- **Remote Back Button Control**: Press duration detection for sending HID commands to target device
  - Single press (< 500ms) → Send ESC key via `HidClient.sendEscapeKey()`
  - 3-second hold (≥ 3000ms) → Send Windows/Home key via `HidClient.sendWindowsKey()`
  - Shows visual feedback text ("ESC" or "Home") in center of screen for 1 second
  - Only active when `SettingsStore.remoteBackDoubleClick` enabled AND device connected

**Critical Rules**:
- ✅ **NEVER** call BluetoothHidDevice APIs directly from UI code
- ✅ All Bluetooth interactions **MUST** route through `HidService` and `HidClient`
- ✅ Preserve foreground service lifecycle to avoid Samsung doze killing connections
- ✅ Use `startForegroundService()` on API 26+ for background Bluetooth operations
- ✅ **Android 14+ Requirement**: ALL three Bluetooth permissions (SCAN, CONNECT, ADVERTISE) must be granted at runtime BEFORE starting HidService with foregroundServiceType="connectedDevice"
- ✅ Manifest must declare `FOREGROUND_SERVICE_CONNECTED_DEVICE` permission for Android 14+
- ✅ **Remote Back Button**: Track `backPressStartTime` on first press, calculate duration on second press/release
- ✅ **Visual Feedback**: Use `feedbackText` state with `LaunchedEffect` to auto-clear after 1 second

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

// Remote Back Button Controls (for target device control)
fun sendEscapeKey(): Boolean  // ESC key (0x29 scan code)
fun sendWindowsKey(): Boolean // Windows/Home key (0x08 modifier)

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
