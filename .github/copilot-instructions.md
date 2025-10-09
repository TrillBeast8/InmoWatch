# GitHub Copilot Workspace Instructions - InmoWatch
## Professional Wear OS HID Input Controller for INMO Air2 & Galaxy Watch4

---

## 🎯 Project Overview

**InmoWatch** is a highly optimized Wear OS application that transforms Galaxy Watch4 into a universal Bluetooth HID (Human Interface Device) controller. The app provides mouse, touchpad, keyboard, media controls, and D-pad input modes for controlling INMO Air2 smart glasses and other Bluetooth-enabled devices.

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

**Critical Rules**:
- ✅ Use `sendReport(device, reportId, data)` for all HID transmissions
- ✅ **ALWAYS** schedule zeroed release report via coroutine scope to prevent sticky inputs
- ✅ Reuse helpers (`sendMouseMovement`, `sendMouseScroll`, `sendConsumerControl`) wherever possible
- ✅ For rapid key bursts, enqueue into Channel and flush at ~8ms intervals to balance responsiveness vs power
- ✅ Keyboard layout: Modifier byte (0x01=CTRL, 0x02=SHIFT, 0x04=ALT, 0x08=WIN), then 6 key slots
- ✅ Mouse deltas **MUST** be clamped to `[-127, 127]` range
- ✅ Stop coroutine scope in `cleanup()` to prevent leaks

---

### 3. Input Modes & UI Screens

All Composables in `ui/screens/` consume state via StateFlows and callbacks. **Keep business logic in ViewModels or HidClient**.

#### **MainMenuScreen** (`ui/screens/MainMenuScreen.kt`)
**Main navigation hub**

**Features**:
- Connection status display from `HidClient.isConnected`
- Error messages from `HidClient.connectionError`
- Mode selection chips (Mouse, Touchpad, Keyboard, Media, D-Pad)
- Settings and Connect buttons
- Quick reconnect to last device via `BluetoothSettingsStore`

**Critical Rules**:
- ✅ Clear errors on screen entry with `HidClient.clearError()`
- ✅ Show connection error prominently if `connectionError != null`
- ✅ Guard mode navigation with `if (isConnected)` checks

#### **MouseScreen** (`ui/screens/MouseScreen.kt`)
**Sensor-based cursor control**

**Features**:
- `WearMouseSensorFusion` for rotation-vector-to-delta conversion
- Circular touch area for left/right/middle/double click
- Tap = left click, Long press = right click, Double tap = double click
- Scroll popup button for accessing scroll/D-pad overlay
- Visual feedback circle during actions

**Critical Rules**:
- ✅ Start sensor fusion in `LaunchedEffect(isConnected)`, stop in `DisposableEffect`
- ✅ Only emit movements exceeding ±0.5px to reduce CPU usage (done in `WearMouseSensorFusion.stabilizeValue()`)
- ✅ Update orientation behavior via `setHandMode()`, `setStabilize()`, `setLefty()`
- ✅ Keep touch targets ≥48dp for Wear OS accessibility

#### **TouchpadScreen** (`ui/screens/TouchpadScreen.kt`)
**Drag-based cursor control**

**Features**:
- Drag gestures with sensitivity scaling
- Tap = left click, Long press = right click, Double tap = double click
- Scroll popup access via top button
- Scaled movement (deltaX * 0.5f) for touchpad feel

**Critical Rules**:
- ✅ Track last position to calculate deltas, reset on drag end
- ✅ Apply sensitivity from `SettingsStore.sensitivity`
- ✅ Preserve scroll popup navigation

#### **KeyboardScreen** (`ui/screens/KeyboardScreen.kt`)
**Text input using Android system keyboard with HID transmission**

**Architecture**:
- **Input Method**: Android system keyboard (IME) via `TextField` or `BasicTextField` - **NEVER** create custom on-screen keyboard UI
- **Realtime Mode**: Transmits each character immediately as HID reports when typed
- **Deferred Mode**: Accumulates text in buffer, sends entire string on "Send" button press
- **HID Conversion**: Converts typed characters to HID scan codes with proper modifiers programmatically

**Features**:
- Opens Android system keyboard automatically on screen entry
- Realtime/deferred mode toggle (`SettingsStore.realtimeKeyboard`)
- Visual text buffer display (shows what user is typing and what will be sent)
- Backspace handling via system keyboard delete key
- Clear and Send action buttons
- Mode toggle chip (RT ↔ D)
- Connection status indicator
- Scroll popup button for navigation shortcuts

**Implementation Pattern**:
```kotlin
@Composable
fun KeyboardScreen(
    onBack: () -> Unit = {},
    onScrollPopup: () -> Unit = {}
) {
    val context = LocalContext.current
    val settingsStore = remember { SettingsStore.get(context) }
    val scope = rememberCoroutineScope()
    
    val isConnected by HidClient.isConnected.collectAsState()
    val realtimeMode by settingsStore.realtimeKeyboard.collectAsState(initial = false)
    
    var textBuffer by remember { mutableStateOf("") }
    var lastSentText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    
    // Auto-focus text field to show system keyboard
    LaunchedEffect(Unit) {
        delay(200) // Wait for composition
        focusRequester.requestFocus()
    }
    
    // Realtime transmission: Send only new characters
    LaunchedEffect(textBuffer, realtimeMode, isConnected) {
        if (realtimeMode && isConnected && textBuffer != lastSentText) {
            if (textBuffer.length < lastSentText.length) {
                // Handle backspace/deletion
                repeat(lastSentText.length - textBuffer.length) {
                    HidClient.sendKey(0x2A) // Backspace scan code
                    delay(5)
                }
            } else {
                // Send new characters
                val newChars = textBuffer.substring(lastSentText.length)
                newChars.forEach { char ->
                    val (scanCode, modifiers) = charToScanCode(char)
                    HidClient.sendKey(scanCode, modifiers)
                    delay(5) // Prevent report flooding
                }
            }
            lastSentText = textBuffer
        }
    }
    
    // UI with TextField that triggers system keyboard
    ScalingLazyColumn {
        item {
            Button(
                onClick = onScrollPopup,
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent)
            ) {
                Text(
                    text = if (realtimeMode) "Keyboard-RT" else "Keyboard-D",
                    color = MaterialTheme.colors.primary
                )
            }
        }
        
        item {
            // System keyboard text input
            BasicTextField(
                value = textBuffer,
                onValueChange = { textBuffer = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                textStyle = LocalTextStyle.current.copy(
                    color = MaterialTheme.colors.onBackground
                )
            )
        }
        
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { if (isConnected) HidClient.sendKey(0x2A) },
                    enabled = isConnected
                ) { Text("⌫") }
                
                Button(onClick = { textBuffer = ""; lastSentText = "" }) {
                    Text("Clear")
                }
                
                Button(onClick = onBack) { Text("Back") }
            }
        }
        
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!realtimeMode) {
                    Button(
                        onClick = {
                            if (isConnected) {
                                HidClient.sendText(textBuffer)
                                textBuffer = ""
                                lastSentText = ""
                            }
                        },
                        enabled = isConnected && textBuffer.isNotEmpty()
                    ) { Text("Send") }
                }
                
                ToggleChip(
                    checked = realtimeMode,
                    onCheckedChange = { scope.launch { settingsStore.setRealtimeKeyboard(it) } },
                    label = { Text(if (realtimeMode) "RT" else "D") }
                )
            }
        }
    }
}
```

**Character to HID Scan Code Conversion**:
Map typed characters to HID scan codes programmatically (extend as needed):

```kotlin
// In HidInputManager or shared utility
private val charToKeyMap = mapOf(
    // Lowercase letters (no modifier)
    'a' to (0x04 to 0), 'b' to (0x05 to 0), 'c' to (0x06 to 0), 'd' to (0x07 to 0),
    'e' to (0x08 to 0), 'f' to (0x09 to 0), 'g' to (0x0A to 0), 'h' to (0x0B to 0),
    'i' to (0x0C to 0), 'j' to (0x0D to 0), 'k' to (0x0E to 0), 'l' to (0x0F to 0),
    'm' to (0x10 to 0), 'n' to (0x11 to 0), 'o' to (0x12 to 0), 'p' to (0x13 to 0),
    'q' to (0x14 to 0), 'r' to (0x15 to 0), 's' to (0x16 to 0), 't' to (0x17 to 0),
    'u' to (0x18 to 0), 'v' to (0x19 to 0), 'w' to (0x1A to 0), 'x' to (0x1B to 0),
    'y' to (0x1C to 0), 'z' to (0x1D to 0),
    
    // Uppercase letters (Shift modifier 0x02)
    'A' to (0x04 to 0x02), 'B' to (0x05 to 0x02), 'C' to (0x06 to 0x02), 'D' to (0x07 to 0x02),
    'E' to (0x08 to 0x02), 'F' to (0x09 to 0x02), 'G' to (0x0A to 0x02), 'H' to (0x0B to 0x02),
    'I' to (0x0C to 0x02), 'J' to (0x0D to 0x02), 'K' to (0x0E to 0x02), 'L' to (0x0F to 0x02),
    'M' to (0x10 to 0x02), 'N' to (0x11 to 0x02), 'O' to (0x12 to 0x02), 'P' to (0x13 to 0x02),
    'Q' to (0x14 to 0x02), 'R' to (0x15 to 0x02), 'S' to (0x16 to 0x02), 'T' to (0x17 to 0x02),
    'U' to (0x18 to 0x02), 'V' to (0x19 to 0x02), 'W' to (0x1A to 0x02), 'X' to (0x1B to 0x02),
    'Y' to (0x1C to 0x02), 'Z' to (0x1D to 0x02),
    
    // Numbers
    '1' to (0x1E to 0), '2' to (0x1F to 0), '3' to (0x20 to 0), '4' to (0x21 to 0),
    '5' to (0x22 to 0), '6' to (0x23 to 0), '7' to (0x24 to 0), '8' to (0x25 to 0),
    '9' to (0x26 to 0), '0' to (0x27 to 0),
    
    // Special characters
    ' ' to (0x2C to 0),      // Space
    '\n' to (0x28 to 0),     // Enter
    '\t' to (0x2B to 0),     // Tab
    '.' to (0x37 to 0),      // Period
    ',' to (0x36 to 0),      // Comma
    '!' to (0x1E to 0x02),   // Shift + 1
    '@' to (0x1F to 0x02),   // Shift + 2
    '#' to (0x20 to 0x02),   // Shift + 3
    '$' to (0x21 to 0x02),   // Shift + 4
    '%' to (0x22 to 0x02),   // Shift + 5
    '?' to (0x38 to 0x02),   // Shift + /
    ';' to (0x33 to 0),      // Semicolon
    ':' to (0x33 to 0x02)    // Shift + semicolon
    // Extend as needed for more characters
)

private fun charToScanCode(char: Char): Pair<Int, Int> {
    return charToKeyMap[char] ?: (0x00 to 0) // Unknown char = no-op
}
```

**Critical Rules**:
- ✅ **ALWAYS** use `TextField` or `BasicTextField` to trigger Android system keyboard (IME) - **NEVER** create custom keyboard UI
- ✅ Use `FocusRequester` to auto-focus input field on screen entry
- ✅ In **realtime mode**: Detect delta between `textBuffer` and `lastSentText`, send only new characters or backspaces
- ✅ In **deferred mode**: Accumulate text, send on "Send" button press via `HidClient.sendText()`
- ✅ Track `lastSentText` to detect new characters (realtime delta detection)
- ✅ Add 5ms delay between characters to prevent HID report flooding
- ✅ Clear text buffer and `lastSentText` after successful deferred send
- ✅ Handle backspace via system keyboard (automatically removes from `textBuffer` via `onValueChange`)
- ✅ Extend `charToKeyMap` when new characters needed (emojis → ignore or map to text alternatives)
- ✅ Preserve scroll popup button for navigation shortcuts
- ✅ Use `HidClient.sendText(string)` for deferred mode (batch transmission)
- ✅ Use `HidClient.sendKey(scanCode, modifiers)` for realtime mode (per-character)

**System Keyboard Configuration** (Optional enhancements):
```kotlin
TextField(
    value = textBuffer,
    onValueChange = { textBuffer = it },
    keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Text,  // Or .Email, .Number, .Uri
        imeAction = ImeAction.Send         // "Send" button on keyboard
    ),
    keyboardActions = KeyboardActions(
        onSend = {
            if (isConnected && !realtimeMode) {
                HidClient.sendText(textBuffer)
                textBuffer = ""
                lastSentText = ""
            }
        }
    )
)
```

**Performance Notes**:
- System keyboard runs in separate process (no custom UI overhead)
- Character conversion happens only on typing (minimal CPU)
- Realtime mode uses coroutine with 5ms throttle (prevents report spam)
- Deferred mode batches into single `sendText()` call (efficient for long strings)
- No custom keyboard layout maintenance required

**Testing Checklist**:
- [ ] System keyboard appears automatically on screen entry
- [ ] Typing in realtime mode sends characters immediately to HID device
- [ ] Typing in deferred mode accumulates, sends on button press
- [ ] Backspace removes characters from buffer (both modes)
- [ ] Special characters (!, @, #, $) map correctly with Shift modifier
- [ ] IME "Send" action triggers deferred send (if configured)
- [ ] Switching modes mid-typing preserves text buffer
- [ ] Clear button resets both `textBuffer` and `lastSentText`

#### **MediaScreen** (`ui/screens/MediaScreen.kt`)
**Gesture-based media control with scrubbable progress ring and album artwork**

**UI Architecture**:
- **Circular album artwork card** (centered, primary visual element)
- **Track title + artist overlay** (non-clickable text, top-left aligned over artwork)
- **Elapsed/total time overlay** (non-clickable text, bottom-center over artwork)
- **Scrubbable progress ring** (circular progress indicator around artwork edge)
- **Scroll popup access button** (gear/overflow icon for additional controls)
- **Connection status indicator** (top of screen when not connected)

**Gesture Controls**:
- **Single tap on artwork** → Play/Pause toggle via `HidClient.playPause()`
- **Swipe left on artwork** → Previous track via `HidClient.previousTrack()`
- **Swipe right on artwork** → Next track via `HidClient.nextTrack()`
- **Drag on progress ring** → Seek to position (via HID consumer reports if supported, or visual-only fallback)
- **Volume controls** → Access via scroll popup overlay

**Implementation Pattern**:
```kotlin
@Composable
fun MediaScreen(
    onBack: () -> Unit = {},
    onScrollPopup: () -> Unit = {}
) {
    LaunchedEffect(Unit) {
        HidClient.currentDeviceProfile = DeviceProfile.Media
    }
    
    val isConnected by HidClient.isConnected.collectAsState()
    val connectionError by HidClient.connectionError.collectAsState()
    
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0f) } // 0.0 to 1.0
    var totalDuration by remember { mutableStateOf(180) } // seconds, mock value
    var showActionFeedback by remember { mutableStateOf(false) }
    var lastAction by remember { mutableStateOf("") }
    
    // Mock metadata (replace with actual metadata from MediaSession/Bluetooth)
    var trackTitle by remember { mutableStateOf("Track Title") }
    var artistName by remember { mutableStateOf("Artist Name") }
    var albumArtUrl by remember { mutableStateOf<String?>(null) }
    
    // Auto-clear action feedback
    LaunchedEffect(showActionFeedback) {
        if (showActionFeedback) {
            delay(200)
            showActionFeedback = false
        }
    }
    
    Scaffold(timeText = { TimeText() }) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Connection status
                if (!isConnected) {
                    Text(
                        text = connectionError ?: "Not connected",
                        color = MaterialTheme.colors.error,
                        style = MaterialTheme.typography.caption1
                    )
                }
                
                // Album artwork with progress ring
                Box(
                    modifier = Modifier.size(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Progress ring (outer)
                    CircularProgressIndicator(
                        progress = currentPosition,
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 4.dp,
                        color = MaterialTheme.colors.primary
                    )
                    
                    // Album artwork (center)
                    Card(
                        modifier = Modifier
                            .size(140.dp)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = {
                                        if (isConnected) {
                                            isPlaying = !isPlaying
                                            HidClient.playPause()
                                            lastAction = if (isPlaying) "Playing" else "Paused"
                                            showActionFeedback = true
                                        }
                                    }
                                )
                            }
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures { _, dragAmount ->
                                    if (isConnected) {
                                        if (dragAmount < -50) { // Swipe left = previous
                                            HidClient.previousTrack()
                                            lastAction = "Previous"
                                            showActionFeedback = true
                                        } else if (dragAmount > 50) { // Swipe right = next
                                            HidClient.nextTrack()
                                            lastAction = "Next"
                                            showActionFeedback = true
                                        }
                                    }
                                }
                            },
                        onClick = {}
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            // Album art (use Coil for actual loading)
                            if (albumArtUrl != null) {
                                // AsyncImage(model = albumArtUrl, ...)
                                Icon(
                                    imageVector = Icons.Default.Album,
                                    contentDescription = "Album Art",
                                    modifier = Modifier.size(80.dp)
                                )
                            } else {
                                // Fallback icon
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = "No Artwork",
                                    modifier = Modifier.size(60.dp),
                                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                                )
                            }
                            
                            // Track title + artist (top-left overlay)
                            Column(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = trackTitle,
                                    style = MaterialTheme.typography.caption2,
                                    color = Color.White,
                                    maxLines = 1,
                                    fontSize = 9.sp
                                )
                                Text(
                                    text = artistName,
                                    style = MaterialTheme.typography.caption2,
                                    color = Color.White.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    fontSize = 8.sp
                                )
                            }
                            
                            // Elapsed/total time (bottom-center overlay)
                            Text(
                                text = "${formatTime(currentPosition * totalDuration)} / ${formatTime(totalDuration.toFloat())}",
                                style = MaterialTheme.typography.caption2,
                                color = Color.White,
                                fontSize = 9.sp,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(8.dp)
                            )
                            
                            // Play/Pause icon overlay (center, semi-transparent)
                            if (showActionFeedback) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
                
                // Action feedback text
                if (showActionFeedback) {
                    Text(
                        text = "✓ $lastAction",
                        color = MaterialTheme.colors.secondary,
                        style = MaterialTheme.typography.caption1
                    )
                }
                
                // Bottom controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Button(onClick = onScrollPopup) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    Button(onClick = onBack) {
                        Text("Back")
                    }
                }
            }
        }
    }
}

private fun formatTime(seconds: Float): String {
    val mins = (seconds.toInt() / 60)
    val secs = (seconds.toInt() % 60)
    return String.format("%d:%02d", mins, secs)
}
```

**Metadata Integration** (Future implementation):
```kotlin
// Option 1: MediaSessionCompat listener (if host supports)
val mediaController = MediaControllerCompat.getMediaController(activity)
mediaController?.metadata?.let { metadata ->
    trackTitle = metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE) ?: "Unknown"
    artistName = metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST) ?: "Unknown"
    albumArtUrl = metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART)?.let { 
        // Cache bitmap or convert to URL
    }
    totalDuration = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION).toInt() / 1000
}

// Option 2: Bluetooth AVRCP metadata (if available via HID connection)
// Read metadata characteristics from connected device GATT profile
```

**Album Artwork Loading** (Use Coil for caching):
```kotlin
// Add Coil dependency: implementation("io.coil-kt:coil-compose:2.5.0")

AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data(albumArtUrl)
        .crossfade(true)
        .placeholder(R.drawable.ic_music_placeholder)
        .error(R.drawable.ic_music_placeholder)
        .memoryCacheKey("album_art_$trackTitle")
        .diskCacheKey("album_art_$trackTitle")
        .build(),
    contentDescription = "Album Art",
    modifier = Modifier.fillMaxSize(),
    contentScale = ContentScale.Crop
)
```

**Swipe Gesture Debouncing**:
```kotlin
// Debounce rapid swipes to prevent track skipping spam
var lastSwipeTime by remember { mutableStateOf(0L) }
val SWIPE_DEBOUNCE_MS = 500L

detectHorizontalDragGestures { change, dragAmount ->
    val currentTime = System.currentTimeMillis()
    if (isConnected && (currentTime - lastSwipeTime) > SWIPE_DEBOUNCE_MS) {
        when {
            dragAmount < -50 -> {
                HidClient.previousTrack()
                lastSwipeTime = currentTime
            }
            dragAmount > 50 -> {
                HidClient.nextTrack()
                lastSwipeTime = currentTime
            }
        }
    }
}
```

**Progress Ring Seek** (Optional - if HID supports seek):
```kotlin
// Detect drag on progress ring circumference
.pointerInput(Unit) {
    detectDragGestures { change, _ ->
        val centerOffset = Offset(size.width / 2f, size.height / 2f)
        val touchOffset = change.position - centerOffset
        val angle = atan2(touchOffset.y, touchOffset.x)
        val normalizedAngle = (angle + PI).toFloat() / (2 * PI).toFloat()
        
        currentPosition = normalizedAngle.coerceIn(0f, 1f)
        // Send seek command via HID if supported
        // HidClient.sendSeek((currentPosition * totalDuration).toInt())
    }
}
```

**Critical Rules**:
- ✅ Use `HidClient.playPause()`, `nextTrack()`, `previousTrack()` for primary controls
- ✅ Volume controls accessed via **scroll popup overlay** (not on main screen)
- ✅ Single tap on artwork = play/pause toggle
- ✅ Swipe left = previous track, swipe right = next track
- ✅ Debounce swipes with 500ms minimum interval to prevent spam
- ✅ Use **Coil** for album artwork loading with memory/disk caching
- ✅ Fallback to music note icon if no artwork available
- ✅ Track title + artist overlay: **top-left** aligned, non-clickable, semi-transparent background
- ✅ Elapsed/total time overlay: **bottom-center** aligned, non-clickable
- ✅ Progress ring updates in real-time (mock with timer or actual metadata)
- ✅ Show play/pause icon feedback for 200ms on tap
- ✅ Preserve scroll popup access via gear/overflow button
- ✅ Handle missing metadata gracefully (show "Unknown Track" / "Unknown Artist")
- ✅ Pre-cache album art on track change to minimize loading delays
- ✅ Use `CircularProgressIndicator` for progress ring (or custom Canvas for advanced seek)
- ✅ Connection status shown at top when disconnected

**Performance Notes**:
- Album artwork cached by Coil (memory + disk) - no repeated network fetches
- Swipe gestures debounced at 500ms to prevent excessive HID commands
- Progress ring uses efficient `CircularProgressIndicator` (no custom drawing overhead)
- Metadata updates happen via coroutine flow (non-blocking UI)

**Testing Checklist**:
- [ ] Tapping artwork toggles play/pause with visual feedback
- [ ] Swipe left sends previous track command
- [ ] Swipe right sends next track command
- [ ] Progress ring shows current playback position
- [ ] Track title + artist display correctly in top-left
- [ ] Elapsed/total time display correctly in bottom-center
- [ ] Fallback icon shown when no artwork available
- [ ] Scroll popup provides volume controls
- [ ] Connection error shown when HID disconnected
- [ ] Swipes debounced (no double-triggering within 500ms)

#### **DpadScreen** (`ui/screens/DpadScreen.kt`)
**8-way directional navigation with scroll mode toggle**

**Features**:
- 8 directional buttons (UP, DOWN, LEFT, RIGHT, UP-LEFT, UP-RIGHT, DOWN-LEFT, DOWN-RIGHT)
- Center button: Single click = confirm/select, Double click = right-click
- Mode toggle: D-Pad ↔ Scroll
- Scroll mode: 4-way only (UP/DOWN/LEFT/RIGHT scroll wheel simulation)
- Diagonal buttons disabled in scroll mode

**Direction Mapping**:
```kotlin
0 = UP, 1 = DOWN, 2 = LEFT, 3 = RIGHT, 4 = CENTER
5 = DOWN-LEFT, 6 = DOWN-RIGHT, 7 = UP-LEFT, 8 = UP-RIGHT
```

**Critical Rules**:
- ✅ Call `HidClient.dpad(direction)` for D-pad mode
- ✅ Call `HidClient.mouseScroll(deltaX, deltaY)` for scroll mode
- ✅ Disable diagonal buttons in scroll mode (visual alpha 0.3f)
- ✅ Auto-clear pressed button feedback after 150ms
- ✅ Preserve scroll popup access

#### **ScrollPopupScreen** (`ui/screens/ScrollPopupScreen.kt`)
**Shared overlay for scroll/D-pad hybrid actions**

**Features**:
- Dual mode: Scroll (4-way) and D-Pad (8-way)
- ESC key mapped to UP-LEFT button (orange, HID scan code 0x29)
- Center button: Single click = left-click (scroll mode) or D-pad center (D-pad mode), Double click = right-click (both modes)
- Sensitivity control slider (1-100%)
- Independent sensitivity for scroll vs D-pad modes
- Click-to-return title button

**Critical Rules**:
- ✅ Receive `parentScreen` parameter ("mouse", "touchpad", "keyboard", "media", "dpad")
- ✅ ESC button (UP-LEFT) uses `HidClient.sendKey(0x29)` - universal across devices
- ✅ Maintain separate `scrollSensitivity` and `dpadSensitivity` state
- ✅ 4-way buttons (UP/DOWN/LEFT/RIGHT) work in both modes
- ✅ Diagonal buttons (UP-LEFT except ESC, UP-RIGHT, DOWN-LEFT, DOWN-RIGHT) only active in D-pad mode
- ✅ Center button double-click detection: Wait 300ms, cancel pending click on double

#### **ConnectToDeviceScreen** (`ui/screens/ConnectToDeviceScreen.kt`)
**Bluetooth device scanning and pairing**

**Features**:
- Bluetooth enable/disable status check
- Permission request launcher (BLUETOOTH_SCAN, BLUETOOTH_CONNECT on API 31+)
- Active device scanning with 10-second timeout
- Discovered devices list (live updates during scan)
- Paired devices list
- Last connected device quick reconnect

**Critical Rules**:
- ✅ Use `ConnectViewModel` for all Bluetooth operations (scan, connect, state management)
- ✅ Register/unregister BroadcastReceiver in `LaunchedEffect` / `DisposableEffect`
- ✅ Guard all Bluetooth API calls with permission checks (`ContextCompat.checkSelfPermission`)
- ✅ Filter discovered devices to only show those with valid names
- ✅ Save last connected device via `BluetoothSettingsStore.saveLastDevice()`
- ✅ Cancel discovery before starting new scan to prevent duplicates

#### **SettingsScreen** (`ui/screens/SettingsScreen.kt`)
**App configuration**

**Settings**:
- **Pointer Sensitivity** (0.1-2.0): Scales cursor movement in mouse/touchpad modes
- **Scroll Sensitivity** (0.1-2.0): Scales scroll wheel movement
- **Remote Back Double Click** (boolean): Requires double-tap to exit app
- **Realtime Keyboard** (boolean): Types as you type vs deferred send
- **Mouse Calibration** button: Navigates to calibration screen

**Critical Rules**:
- ✅ All settings persisted via `SettingsStore` (DataStore)
- ✅ Use `collectAsState()` for reactive UI updates
- ✅ Update settings with `scope.launch { settingsStore.setSetting(value) }`
- ✅ Provide +/- buttons for numeric settings (increment/decrement by 0.1)
- ✅ Use ToggleChip with Switch for boolean settings

#### **MouseCalibrationScreen** (`ui/screens/MouseCalibrationScreen.kt`)
**Sensor baseline calibration**

**Features**:
- 3-second sensor data collection
- Progress ring visualization (0-100%)
- Hold watch in neutral position instruction
- Auto-saves calibration status to `SettingsStore.mouseCalibrationComplete`
- Auto-returns to settings after 2 seconds

**Critical Rules**:
- ✅ Run calibration in coroutine scope (don't block UI)
- ✅ Configure `WearMouseSensorFusion` with `HandMode.CENTER`, `setStabilize(true)`, `setLefty(false)`
- ✅ Collect 100 samples over ~3 seconds
- ✅ Update progress UI during collection
- ✅ Stop sensor fusion in `DisposableEffect` cleanup
- ✅ Handle timeout (10 seconds) if calibration hangs

---

### 5. Sensor Fusion System

#### **WearMouseSensorFusion** (`sensors/WearMouseSensorFusion.kt`)
**Rotation vector to cursor delta converter**

**Features**:
- Uses `TYPE_GAME_ROTATION_VECTOR` (preferred) or `TYPE_ROTATION_VECTOR`
- Converts orientation deltas to cursor movement with configurable speed
- Stabilization filter to eliminate jitter
- Hand mode support (LEFT, CENTER, RIGHT for different watch positions)
- Lefty mode for left-handed users

**Mathematics**:
```kotlin
CURSOR_SPEED = 1024.0 / (π/4)  // ~1304 px per radian
STABILIZE_BIAS = 16.0          // Minimum movement threshold

// Stabilization formula (reduce noise)
stabilizedValue = if (abs(value) < STABILIZE_BIAS) {
    value * (abs(value) / STABILIZE_BIAS)  // Quadratic scaling
} else {
    value  // Pass through large movements
}
```

**Critical Rules**:
- ✅ Start sensor listener with `registerListener(sensor, DATA_RATE_US = 11250)` for ~89 Hz updates
- ✅ Handle wraparound for yaw (±π) and pitch (±π/2)
- ✅ Only emit movements exceeding ±0.5px to reduce unnecessary HID reports
- ✅ Apply hand mode corrections: `LEFT = (-pitch, yaw)`, `RIGHT = (pitch, -yaw)`, `CENTER = (yaw, pitch)`
- ✅ Unregister sensor listener on stop to save battery
- ✅ Pre-allocate rotation matrix and orientation arrays to avoid GC pressure

---

### 6. Data Persistence Layer

#### **SettingsStore** (`data/SettingsStore.kt`)
**DataStore-backed app preferences**

**Preferences**:
```kotlin
sensitivity: Flow<Float>              // Default: 1.0
scrollSensitivity: Flow<Float>        // Default: 1.0
realtimeKeyboard: Flow<Boolean>       // Default: false
remoteBackDoubleClick: Flow<Boolean>  // Default: false
mouseCalibrationComplete: Flow<Boolean> // Default: false
```

**Critical Rules**:
- ✅ Use singleton pattern: `SettingsStore.get(context)`
- ✅ All flows use `distinctUntilChanged()` to prevent redundant emissions
- ✅ Update methods are `suspend`: `setSensitivity(value)`, `setRealtimeKeyboard(enabled)`, etc.
- ✅ Collect flows in Composables with `.collectAsState(initial = defaultValue)`

#### **BluetoothSettingsStore** (`data/BluetoothSettingsStore.kt`)
**Last connected device storage**

**Features**:
- Stores last device address and name
- Provides Flow for reactive reconnect UI
- Handles SecurityException gracefully (permission-denied cases)

**Critical Rules**:
- ✅ Save device after successful connection: `BluetoothSettingsStore.saveLastDevice(context, device)`
- ✅ Retrieve via Flow: `BluetoothSettingsStore.lastDeviceFlow(context).collectAsState()`
- ✅ Clear on manual disconnect: `BluetoothSettingsStore.clearLastDevice(context)`
- ✅ Handle null device name with fallback "Unknown Device"

---

### 7. Navigation System

#### **NavRoutes** (`nav/NavRoutes.kt`)
**Route constants for Compose Navigation**

```kotlin
object Routes {
    const val MainMenu = "main"
    const val Keyboard = "keyboard"
    const val Touchpad = "touchpad"
    const val Mouse = "mouse"
    const val DPad = "dpad"
    const val Media = "media"
    const val Settings = "settings"
    const val MouseCalibration = "mouse_calibration"
    const val ConnectDevice = "connect_device"
}
```

**Scroll Popup Route** (parameterized):
```kotlin
"scrollPopup/{parent}"  // parent = "mouse" | "touchpad" | "keyboard" | "media" | "dpad"
```

**Critical Rules**:
- ✅ When adding new modes: Add route constant → Update NavHost in `MainActivity` → Add chip in `MainMenuScreen`
- ✅ Use `navController.navigate(route)` for navigation
- ✅ Use `navController.popBackStack()` or `onBack()` callback for returning
- ✅ Preserve BackHandler in `MainActivity` for double-back exit logic

---

### 8. Device Compatibility & Optimization

#### **Galaxy Watch4 Optimizations**
- **Foreground Service**: Maintain notification while pairing to dodge Samsung doze
- **Touch Targets**: ≥48dp minimum for fat-finger accessibility
- **Battery**: Stop sensor fusion when disconnected, use WorkManager without charging/idle constraints
- **Reconnection**: Exponential backoff (100ms → 500ms → 1s) on `BluetoothProfile.STATE_DISCONNECTING`

#### **Universal HID Compatibility**
The app acts as a standard, universal HID device. It does not require device-specific profiles or optimizations.

**HID Descriptor** (`HidConstants.REPORT_DESCRIPTOR`):
- Report ID 1: Keyboard (8 bytes) - Boot protocol compatible
- Report ID 2: Mouse (4 bytes) - Boot protocol compatible
- Report ID 3: Consumer (2 bytes) - Media controls

**Critical Rules**:
- ✅ **NEVER** modify `HidConstants.REPORT_DESCRIPTOR` unless HID spec changes
- ✅ Use WearMouse descriptor layout (proven compatibility)
- ✅ Keyboard layout: `[modifier, reserved, key1, key2, key3, key4, key5, key6]`
- ✅ Mouse layout: `[buttons, deltaX, deltaY, wheel]`
- ✅ Consumer layout: `[usage_LSB, usage_MSB]`

---

## 🔧 Development Guidelines

### 💡 Ultimate Cross-Check for Precision & Smart Code
Before and after any code is changed, added, altered, or removed, a rigorous check must be performed to ensure correctness. This process, the "Ultimate Cross-Check," guarantees precision and adherence to the best coding approaches.

**Pre-Change Analysis**:
1.  **Identify Scope**: Clearly define the files and components affected by the requested change.
2.  **Check for Existing Errors**: Run `get_errors` on the identified files to establish a baseline. Do not proceed if existing errors will interfere with the change.
3.  **Review Architecture**: Ensure the proposed change aligns with the project's core architecture (MVVM, HidService/Client pattern).

**Post-Change Verification**:
1.  **Apply Changes**: Use the `insert_edit_into_file` tool to apply the code modifications.
2.  **Verify Correctness**: Immediately run `get_errors` on all modified files.
3.  **Analyze & Fix**: If any errors are reported, analyze them.
    - If the errors are a direct result of the change, fix them immediately.
    - If the errors are unrelated, inform the user but proceed if the core task is complete.
4.  **Final Review**: Mentally confirm that the changes are efficient, safe, and follow the project's best practices as outlined in these instructions.

This cross-check ensures that every modification enhances the codebase's quality and stability.

### Code Style & Best Practices

#### **Kotlin Style**
```kotlin
// ✅ Good: Explicit modifiers, immutable data
@Composable
fun MouseScreen(
    onBack: () -> Unit = {},
    onScrollPopup: () -> Unit = {}
) {
    val isConnected by HidClient.isConnected.collectAsState()
    var lastAction by remember { mutableStateOf("Ready") }
    
    DisposableEffect(isConnected) {
        // Cleanup code
        onDispose { /* ... */ }
    }
}

// ❌ Bad: Missing type safety, nullable without guards
fun sendInput(device: BluetoothDevice?) {
    device.let { // Unsafe - device can be null
        sendReport(it, 1, data)
    }
}
```

#### **Compose Best Practices**
- ✅ Use `remember` for state that survives recomposition
- ✅ Use `collectAsState()` for StateFlow consumption
- ✅ Use `LaunchedEffect(Unit)` for one-time initialization
- ✅ Use `DisposableEffect` for cleanup (sensors, receivers)
- ✅ Keep Composables pure - no side effects outside LaunchedEffect
- ✅ Hoist state to parent when shared across siblings

#### **Bluetooth Safety**
```kotlin
// ✅ Good: Permission-guarded Bluetooth calls
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) 
        == PackageManager.PERMISSION_GRANTED) {
        HidClient.connectToDevice(device)
    }
} else {
    HidClient.connectToDevice(device)
}

// ✅ Good: SecurityException handling
try {
    val deviceName = device.name ?: "Unknown"
} catch (e: SecurityException) {
    val deviceName = "Permission Denied"
}
```

#### **Performance Optimizations**
- ✅ Minimize allocations in HID paths: Reuse byte arrays
- ✅ Avoid blocking waits in UI threads: Use coroutines
- ✅ Enqueue rapid key bursts into Channel, flush at ~8ms intervals to balance responsiveness vs power
- ✅ Stop sensor fusion when disconnected to save battery
- ✅ Use `distinctUntilChanged()` on StateFlows to reduce recompositions
- ✅ Cache computed values with `remember(key)` in Composables
- ✅ Pre-allocate arrays in hot paths (e.g., `WearMouseSensorFusion` rotation matrix)

#### **Logging**
```kotlin
// ✅ Good: Use Logger utility (auto-disabled in release)
Logger.d("HidService", "Device connected: ${device.address}")
Logger.e("HidService", "Connection failed", exception)

// ✅ Good: Guard expensive logging in release
if (BuildConfig.DEBUG) {
    Log.d(TAG, "Complex log: ${expensiveComputation()}")
}

// ❌ Bad: Direct Log calls that run in release
Log.d(TAG, "Debug message: ${device.name}")
```

---

## 🧪 Testing & Quality Gates

### Unit Tests
**Focus Areas**:
- `HidInputManager`: Key mapping, modifier combos, report assembly
- `SettingsStore`: Flow emissions, default values, persistence
- `WearMouseSensorFusion`: Delta calculations, wraparound handling, stabilization math

**Run Tests**:
```bash
./gradlew test
```

### Integration Tests
**Focus Areas**:
- Mock `BluetoothDevice` and `BluetoothHidDevice` to validate report payloads
- Test reconnection logic with simulated disconnects
- Validate StateFlow state transitions in `HidClient`

### Lint Checks
```bash
./gradlew lint
```

**Critical Rules**:
- ✅ Fix all errors before shipping
- ✅ Address warnings related to Bluetooth permissions
- ✅ Suppress only when genuinely unavoidable (with justification comment)

---

## 🚀 Extensibility & Adding New Features

### Adding a New Input Mode

**Example: Adding a "Volume Dial" mode**

1. **Extend HidServiceApi & HidService**:
```kotlin
// hid/HidServiceApi.kt
fun sendVolumeDialRotate(delta: Int): Boolean

// hid/HidService.kt
override fun sendVolumeDialRotate(delta: Int): Boolean =
    hidInputManager.sendVolumeDialRotate(delta)
```

2. **Implement in HidInputManager**:
```kotlin
// bluetooth/HidInputManager.kt
fun sendVolumeDialRotate(delta: Int): Boolean {
    return if (delta > 0) {
        repeat(delta) { sendVolumeUp() }
    } else {
        repeat(-delta) { sendVolumeDown() }
    }
    true
}
```

3. **Surface in HidClient**:
```kotlin
// hid/HidClient.kt
fun volumeDialRotate(delta: Int): Boolean = 
    getService()?.sendVolumeDialRotate(delta) ?: false
```

4. **Create UI Screen**:
```kotlin
// ui/screens/VolumeDialScreen.kt
@Composable
fun VolumeDialScreen(onBack: () -> Unit = {}) {
    val isConnected by HidClient.isConnected.collectAsState()
    var currentVolume by remember { mutableStateOf(50) }
    
    // Rotary input handling, visual dial, etc.
}
```

5. **Update Navigation**:
```kotlin
// nav/NavRoutes.kt
const val VolumeDial = "volume_dial"

// MainActivity.kt NavHost
composable("volume_dial") {
    VolumeDialScreen(onBack = { navController.popBackStack() })
}

// MainMenuScreen.kt
if (isConnected) {
    item { ControlButton(text = "Volume Dial", icon = Icons.Default.VolumeUp, onClick = onNavigateToVolumeDial) }
}
```

---

## 📝 KDoc Documentation Standards

**Public APIs**:
```kotlin
/**
 * Sends a mouse movement delta to the connected HID device.
 *
 * Deltas are clamped to the range [-127, 127] as per HID mouse specification.
 * Movement is only sent if a device is connected and the HID profile is ready.
 *
 * @param deltaX Horizontal movement delta in pixels. ∈ [-127, 127]
 * @param deltaY Vertical movement delta in pixels. ∈ [-127, 127]
 * @return `true` if the report was sent successfully, `false` otherwise
 *
 * @see sendLeftClick
 * @see sendScroll
 */
fun sendMouseMovement(deltaX: Float, deltaY: Float): Boolean
```

**LaTeX Math** (for sensor fusion):
```kotlin
/**
 * Stabilization formula:
 *
 * $$ f(x) = \begin{cases}
 *   x \cdot \frac{|x|}{B} & \text{if } |x| < B \\
 *   x & \text{otherwise}
 * \end{cases} $$
 *
 * where $B = 16.0$ is the stabilization bias threshold.
 */
private fun stabilizeValue(value: Float): Float
```

---

## 🎯 Copilot Usage Tips

### **When Writing Code**
- Specify screen/layer: *"Update MediaScreen to add swipe gesture handling for track navigation"*
- Mention hardware: *"Optimize for Galaxy Watch4 - reduce battery usage in sensor fusion"*
- Request tests: *"Generate unit tests for HidInputManager key mapping logic"*

### **When Adding Features**
- Describe full flow: *"Add screenshot mode: Capture screen on triple-tap in mouse mode, save to storage, show toast confirmation"*
- Reference existing patterns: *"Add new GestureScreen like MouseScreen, but use swipe gestures instead of sensor fusion"*

### **When Refactoring**
- State goal: *"Refactor HidClient to use Dagger/Hilt dependency injection instead of singleton"*
- Preserve behavior: *"Extract scroll popup logic into shared ViewModel while keeping existing UI behavior"*

---

## 📊 Settings Summary

| Setting | Type | Default | Range | Purpose |
|---------|------|---------|-------|---------|
| `sensitivity` | Float | 1.0 | 0.1-2.0 | Cursor movement speed multiplier |
| `scrollSensitivity` | Float | 1.0 | 0.1-2.0 | Scroll wheel speed multiplier |
| `realtimeKeyboard` | Boolean | false | - | Realtime typing vs deferred send |
| `remoteBackDoubleClick` | Boolean | false | - | Double-tap to exit app |
| `mouseCalibrationComplete` | Boolean | false | - | Sensor calibration status |

**Access Pattern**:
```kotlin
val settingsStore = remember { SettingsStore.get(context) }
val sensitivity by settingsStore.sensitivity.collectAsState(initial = 1.0f)

scope.launch {
    settingsStore.setSensitivity(newValue)
}
```

---

## 🔒 Security & Permissions

### **Required Permissions** (API 31+)
```xml
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />
```

### **Runtime Permission Checks**
```kotlin
// In MainActivity
private val permissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
) { permissions ->
    if (permissions.values.all { it }) {
        startAndBindHidService()
    }
}

// Request on onCreate
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    permissionLauncher.launch(arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_ADVERTISE
    ))
}
```

---

## 🎨 UI Component Library

### **Wear OS Components**
```kotlin
import androidx.wear.compose.material.*

// Core components
Scaffold(timeText = { TimeText() }) { /* content */ }
ScalingLazyColumn { items { /* ... */ } }
Chip(label = { Text("Label") }, onClick = { /* ... */ })
ToggleChip(checked = state, onCheckedChange = { /* ... */ })
Button(onClick = { /* ... */ }) { Text("Button") }
Card(onClick = { /* ... */ }) { /* content */ }
```

### **Standard Patterns**
```kotlin
// Connection status display
val isConnected by HidClient.isConnected.collectAsState()
val connectionError by HidClient.connectionError.collectAsState()

if (!isConnected) {
    Text(
        text = connectionError ?: "Not connected",
        color = MaterialTheme.colors.error,
        style = MaterialTheme.typography.caption1
    )
}

// Action feedback
LaunchedEffect(showFeedback) {
    if (showFeedback) {
        delay(1500)
        showFeedback = false
    }
}

if (showFeedback) {
    Text(
        text = "✓ $lastAction",
        color = MaterialTheme.colors.secondary
    )
}
```

---

## 🏁 Final Checklist for Code Changes

Before committing changes, ensure:

- [ ] **Bluetooth calls** are guarded by permission checks
- [ ] **StateFlows** are collected with `.collectAsState()`
- [ ] **Sensor fusion** is stopped in `DisposableEffect` cleanup
- [ ] **HID reports** use correct report IDs and byte layouts
- [ ] **Touch targets** are ≥48dp for Wear OS
- [ ] **Logging** uses `Logger` utility or `BuildConfig.DEBUG` guards
- [ ] **Tests** pass: `./gradlew test`
- [ ] **Lint** is clean: `./gradlew lint`
- [ ] **Navigation** preserves back stack and scroll popup access
- [ ] **Error handling** shows user-friendly messages in UI

---

## 📚 Additional Resources

### **HID Specifications**
- [USB HID Usage Tables](https://usb.org/document-library/hid-usage-tables-13)
- [Bluetooth HID Profile Specification](https://www.bluetooth.com/specifications/specs/human-interface-device-profile-1-1-1/)

### **Wear OS Development**
- [Wear OS Compose Guidelines](https://developer.android.com/training/wearables/compose)
- [Wear OS Design Principles](https://developer.android.com/design/ui/wear)

### **Bluetooth Android**
- [BluetoothHidDevice API](https://developer.android.com/reference/android/bluetooth/BluetoothHidDevice)
- [Bluetooth Permissions (API 31+)](https://developer.android.com/guide/topics/connectivity/bluetooth/permissions)

---

## 🎯 Summary

This InmoWatch project is a **production-grade Wear OS HID controller** designed for:
- **Performance**: Minimal allocations, coroutine-based async, sensor fusion optimizations
- **Reliability**: Foreground service lifecycle, auto-reconnect, error recovery
- **Extensibility**: Clean architecture, pluggable input modes
- **User Experience**: Reactive Compose UI, visual feedback, settings persistence

**When generating code, prioritize**:
- ✅ **Safety**: Permission checks, null safety, error handling
- ✅ **Efficiency**: Battery-aware, memory-conscious, responsive
- ✅ **Maintainability**: KDoc, clear naming, separation of concerns
- ✅ **Compatibility**: Galaxy Watch4, and any standard HID-compliant host.

**Always route HID operations through HidService → HidClient → UI** to maintain the clean architecture that makes this app robust and maintainable.

---

## ⚡ Global Gestures

### Two-Finger Swipe Navigation
A global two-finger horizontal swipe gesture is implemented for "power users" to quickly navigate between the primary input modes. This allows for seamless switching without returning to the main menu.

**Navigation Cycle**:
- **Swipe Right**: `Mouse` → `Touchpad` → `Keyboard` → `Media` → `Dpad` → `Settings` → `Mouse`
- **Swipe Left**: `Mouse` → `Settings` → `Dpad` → `Media` → `Keyboard` → `Touchpad` → `Mouse`

**Rules**:
- The gesture is active on all primary mode screens: `MouseScreen`, `TouchpadScreen`, `KeyboardScreen`, `MediaScreen`, `DpadScreen`, and `SettingsScreen`.
- The gesture is **disabled** on `MainMenuScreen`, `ConnectToDeviceScreen`, and any popup screens.
- The gesture requires two or more pointers (fingers) to activate, preventing conflicts with single-finger gestures like swiping on the `MediaScreen` artwork.
