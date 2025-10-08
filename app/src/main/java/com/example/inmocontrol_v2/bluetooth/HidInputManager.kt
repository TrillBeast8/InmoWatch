package com.example.inmocontrol_v2.bluetooth

import android.bluetooth.BluetoothDevice
import android.util.Log
import kotlinx.coroutines.*

/**
 * Universal HID Input Manager - Enhanced for maximum compatibility
 * Optimized version with async operations and improved performance
 */
class HidInputManager(private val hidDeviceProfile: HidDeviceProfile) {

    companion object {
        private const val TAG = "HidInputManager"

        // Universal key codes that work across platforms
        private const val KEY_ESC = 0x29
        private const val KEY_ENTER = 0x28
        private const val KEY_SPACE = 0x2C
        private const val KEY_TAB = 0x2B
        private const val KEY_BACKSPACE = 0x2A
        private const val KEY_DELETE = 0x4C
        private const val KEY_HOME = 0x4A
        private const val KEY_END = 0x4D

        // Arrow keys
        private const val KEY_RIGHT = 0x4F
        private const val KEY_LEFT = 0x50
        private const val KEY_DOWN = 0x51
        private const val KEY_UP = 0x52

        // Function keys
        private const val KEY_F11 = 0x44

        // Modifier keys
        private const val MOD_CTRL = 0x01
        private const val MOD_SHIFT = 0x02
        private const val MOD_ALT = 0x04
        private const val MOD_WIN = 0x08

        // Platform detection cache
        private var detectedPlatform: Platform? = null
    }

    enum class Platform {
        WINDOWS, MACOS, LINUX, ANDROID, IOS, UNKNOWN
    }

    // Universal gesture tracking
    private var lastActionTime = System.currentTimeMillis()

    // Auto-detect target platform based on device behavior and optimize accordingly
    private fun detectPlatform(device: BluetoothDevice): Platform {
        detectedPlatform?.let { return it }

        try {
            // Use device name and behavior patterns to detect platform
            val deviceName = device.name?.lowercase() ?: ""

            detectedPlatform = when {
                deviceName.contains("windows") || deviceName.contains("pc") -> Platform.WINDOWS
                deviceName.contains("mac") || deviceName.contains("macbook") -> Platform.MACOS
                deviceName.contains("linux") || deviceName.contains("ubuntu") -> Platform.LINUX
                deviceName.contains("android") || deviceName.contains("phone") -> Platform.ANDROID
                deviceName.contains("iphone") || deviceName.contains("ipad") -> Platform.IOS
                else -> Platform.UNKNOWN
            }

            Log.d(TAG, "Detected platform: $detectedPlatform for device: ${device.name}")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for device name", e)
            detectedPlatform = Platform.UNKNOWN
        }
        return detectedPlatform ?: Platform.UNKNOWN
    }

    // Helper to format bytes as hex for logging
    private fun toHex(bytes: ByteArray?): String {
        if (bytes == null) return "<null>"
        return bytes.joinToString(separator = " ") { String.format("%02X", it) }
    }

    /**
     * Send keyboard key press - OPTIMIZED with async operations
     */
    fun sendKeyPress(device: BluetoothDevice, keyCode: Int): Boolean {
        Log.d(TAG, "Sending key press: keyCode=$keyCode")

        return try {
            // Create keyboard report: [modifier, reserved, key1, key2, key3, key4, key5, key6]
            val pressReport = byteArrayOf(0, 0, keyCode.toByte(), 0, 0, 0, 0, 0)
            Log.d(TAG, "Keyboard press report (id=${HidConstants.ID_KEYBOARD}): ${toHex(pressReport)}")
            val pressSuccess = hidDeviceProfile.sendReport(device, HidConstants.ID_KEYBOARD.toInt(), pressReport)

            // Use async delay instead of blocking sleep
            scope.launch {
                delay(50)
                // Send key release (all zeros)
                val releaseReport = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
                Log.d(TAG, "Keyboard release report (id=${HidConstants.ID_KEYBOARD}): ${toHex(releaseReport)}")
                hidDeviceProfile.sendReport(device, HidConstants.ID_KEYBOARD.toInt(), releaseReport)
            }

            pressSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Error sending key press", e)
            false
        }
    }

    /**
     * Send keyboard key combination - OPTIMIZED with async operations
     */
    fun sendKeyCombo(device: BluetoothDevice, modifier: Int, keyCode: Int): Boolean {
        Log.d(TAG, "Sending key combo: modifier=$modifier, keyCode=$keyCode")

        return try {
            // Create keyboard report with modifier
            val pressReport = byteArrayOf(modifier.toByte(), 0, keyCode.toByte(), 0, 0, 0, 0, 0)
            Log.d(TAG, "Keyboard combo report (id=${HidConstants.ID_KEYBOARD}): ${toHex(pressReport)}")
            val pressSuccess = hidDeviceProfile.sendReport(device, HidConstants.ID_KEYBOARD.toInt(), pressReport)

            // Use async delay instead of blocking sleep
            scope.launch {
                delay(50)
                // Send key release
                val releaseReport = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
                Log.d(TAG, "Keyboard combo release (id=${HidConstants.ID_KEYBOARD}): ${toHex(releaseReport)}")
                hidDeviceProfile.sendReport(device, HidConstants.ID_KEYBOARD.toInt(), releaseReport)
            }

            pressSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Error sending key combo", e)
            false
        }
    }

    /**
     * Send mouse movement - uses Report ID 2 (matches wearmouse)
     */
    fun sendMouseMovement(device: BluetoothDevice, deltaX: Int, deltaY: Int): Boolean {
        val report = createMouseReport(false, false, false, deltaX, deltaY, 0)
        Log.d(TAG, "Mouse movement report (id=${HidConstants.ID_MOUSE}): ${toHex(report)}")
        return hidDeviceProfile.sendReport(device, HidConstants.ID_MOUSE.toInt(), report)
    }

    /**
     * Send mouse click - OPTIMIZED with async operations
     */
    fun sendMouseClick(device: BluetoothDevice, left: Boolean, right: Boolean, middle: Boolean): Boolean {
        Log.d(TAG, "Sending mouse click: left=$left, right=$right, middle=$middle")

        return try {
            // Send button press
            val pressReport = createMouseReport(left, right, middle, 0, 0, 0)
            Log.d(TAG, "Mouse press report (id=${HidConstants.ID_MOUSE}): ${toHex(pressReport)}")
            val pressSuccess = hidDeviceProfile.sendReport(device, HidConstants.ID_MOUSE.toInt(), pressReport)

            // Use async delay instead of blocking sleep
            scope.launch {
                delay(50)
                // Send button release
                val releaseReport = createMouseReport(false, false, false, 0, 0, 0)
                Log.d(TAG, "Mouse release report (id=${HidConstants.ID_MOUSE}): ${toHex(releaseReport)}")
                hidDeviceProfile.sendReport(device, HidConstants.ID_MOUSE.toInt(), releaseReport)
            }

            pressSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Error sending mouse click", e)
            false
        }
    }

    /**
     * Send mouse scroll - uses Report ID 2 (matches wearmouse structure)
     */
    fun sendMouseScroll(device: BluetoothDevice, scrollY: Int): Boolean {
        Log.d(TAG, "Sending mouse scroll: scrollY=$scrollY")

        // Create scroll report - wearmouse puts wheel as 3rd byte in mouse report
        val report = createMouseReport(false, false, false, 0, 0, scrollY)
        Log.d(TAG, "Mouse scroll report (id=${HidConstants.ID_MOUSE}): ${toHex(report)}")
        return hidDeviceProfile.sendReport(device, HidConstants.ID_MOUSE.toInt(), report)
    }

    /**
     * Send mouse drag - uses Report ID 2
     */
    fun sendMouseDrag(device: BluetoothDevice, deltaX: Int, deltaY: Int, leftButton: Boolean): Boolean {
        val report = createMouseReport(leftButton, false, false, deltaX, deltaY, 0)
        return hidDeviceProfile.sendReport(device, HidConstants.ID_MOUSE.toInt(), report)
    }

    /**
     * Create mouse report - matches wearmouse structure: [buttons, deltaX, deltaY, wheel]
     */
    private fun createMouseReport(left: Boolean, right: Boolean, middle: Boolean, deltaX: Int, deltaY: Int, wheel: Int): ByteArray {
        val buttons = (if (left) HidConstants.MOUSE_BUTTON_LEFT else 0) or
                     (if (right) HidConstants.MOUSE_BUTTON_RIGHT else 0) or
                     (if (middle) HidConstants.MOUSE_BUTTON_MIDDLE else 0)

        return byteArrayOf(
            buttons.toByte(),
            deltaX.coerceIn(-127, 127).toByte(),
            deltaY.coerceIn(-127, 127).toByte(),
            wheel.coerceIn(-127, 127).toByte()
        )
    }

    /**
     * Media Controls - uses Report ID 3 (separate from mouse to prevent interference)
     */
    fun sendPlayPause(device: BluetoothDevice): Boolean {
        return sendConsumerControl(device, HidConstants.USAGE_PLAY_PAUSE)
    }

    fun sendNextTrack(device: BluetoothDevice): Boolean {
        return sendConsumerControl(device, HidConstants.USAGE_NEXT_TRACK)
    }

    fun sendPreviousTrack(device: BluetoothDevice): Boolean {
        return sendConsumerControl(device, HidConstants.USAGE_PREV_TRACK)
    }

    fun sendVolumeUp(device: BluetoothDevice): Boolean {
        return sendConsumerControl(device, HidConstants.USAGE_VOLUME_UP)
    }

    fun sendVolumeDown(device: BluetoothDevice): Boolean {
        return sendConsumerControl(device, HidConstants.USAGE_VOLUME_DOWN)
    }

    /**
     * Send device/audio output switch command
     */
    fun sendOutputSwitch(device: BluetoothDevice): Boolean {
        Log.d(TAG, "Sending output switch command")
        // Use Windows key + K for audio device switching on Windows
        return sendKeyCombo(device, 0x08, 0x0E) // WIN + K
    }

    /**
     * Send consumer control command (media keys) - uses Report ID 3
     */
    private fun sendConsumerControl(device: BluetoothDevice, usage: Int): Boolean {
        Log.d(TAG, "Sending consumer control: usage=0x${usage.toString(16)}")

        return try {
            // Send key press - 2 bytes for 16-bit usage value
            val pressReport = byteArrayOf(
                (usage and 0xFF).toByte(),
                ((usage shr 8) and 0xFF).toByte()
            )
            Log.d(TAG, "Consumer press report (id=${HidConstants.ID_CONSUMER}): ${toHex(pressReport)}")
            val pressSuccess = hidDeviceProfile.sendReport(device, HidConstants.ID_CONSUMER.toInt(), pressReport)

            // Use async delay instead of blocking sleep
            scope.launch {
                delay(50)
                // Send key release (all zeros)
                val releaseReport = byteArrayOf(0, 0)
                Log.d(TAG, "Consumer release report (id=${HidConstants.ID_CONSUMER}): ${toHex(releaseReport)}")
                hidDeviceProfile.sendReport(device, HidConstants.ID_CONSUMER.toInt(), releaseReport)
            }

            pressSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Error sending consumer control", e)
            false
        }
    }

    /**
     * Clear all inputs - useful for cleanup
     */
    fun clearAllInputs(device: BluetoothDevice) {
        try {
            // Clear keyboard
            val keyboardReport = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
            Log.d(TAG, "Clearing keyboard (id=${HidConstants.ID_KEYBOARD}): ${toHex(keyboardReport)}")
            hidDeviceProfile.sendReport(device, HidConstants.ID_KEYBOARD.toInt(), keyboardReport)

            // Clear mouse
            val mouseReport = byteArrayOf(0, 0, 0, 0)
            Log.d(TAG, "Clearing mouse (id=${HidConstants.ID_MOUSE}): ${toHex(mouseReport)}")
            hidDeviceProfile.sendReport(device, HidConstants.ID_MOUSE.toInt(), mouseReport)

            // Clear consumer
            val consumerReport = byteArrayOf(0, 0)
            Log.d(TAG, "Clearing consumer (id=${HidConstants.ID_CONSUMER}): ${toHex(consumerReport)}")
            hidDeviceProfile.sendReport(device, HidConstants.ID_CONSUMER.toInt(), consumerReport)
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing inputs", e)
        }
    }

    /**
     * Universal platform-specific key mapping
     */
    fun sendUniversalKey(device: BluetoothDevice, action: String): Boolean {
        val platform = detectPlatform(device)

        return when (action.lowercase()) {
            "back", "escape" -> sendKeyPress(device, KEY_ESC)
            "enter", "confirm" -> sendKeyPress(device, KEY_ENTER)
            "space" -> sendKeyPress(device, KEY_SPACE)
            "tab" -> sendKeyPress(device, KEY_TAB)
            "backspace" -> sendKeyPress(device, KEY_BACKSPACE)
            "delete" -> sendKeyPress(device, KEY_DELETE)
            "home" -> when (platform) {
                Platform.MACOS -> sendKeyCombo(device, MOD_CTRL, KEY_LEFT) // Cmd+Left
                else -> sendKeyPress(device, KEY_HOME)
            }
            "end" -> when (platform) {
                Platform.MACOS -> sendKeyCombo(device, MOD_CTRL, KEY_RIGHT) // Cmd+Right
                else -> sendKeyPress(device, KEY_END)
            }
            "copy" -> when (platform) {
                Platform.MACOS -> sendKeyCombo(device, MOD_WIN, 0x06) // Cmd+C
                else -> sendKeyCombo(device, MOD_CTRL, 0x06) // Ctrl+C
            }
            "paste" -> when (platform) {
                Platform.MACOS -> sendKeyCombo(device, MOD_WIN, 0x19) // Cmd+V
                else -> sendKeyCombo(device, MOD_CTRL, 0x19) // Ctrl+V
            }
            "cut" -> when (platform) {
                Platform.MACOS -> sendKeyCombo(device, MOD_WIN, 0x18) // Cmd+X
                else -> sendKeyCombo(device, MOD_CTRL, 0x18) // Ctrl+X
            }
            "undo" -> when (platform) {
                Platform.MACOS -> sendKeyCombo(device, MOD_WIN, 0x1A) // Cmd+Z
                else -> sendKeyCombo(device, MOD_CTRL, 0x1A) // Ctrl+Z
            }
            "redo" -> when (platform) {
                Platform.MACOS -> sendKeyCombo(device, MOD_WIN or MOD_SHIFT, 0x1A) // Cmd+Shift+Z
                else -> sendKeyCombo(device, MOD_CTRL, 0x1C) // Ctrl+Y
            }
            "selectall" -> when (platform) {
                Platform.MACOS -> sendKeyCombo(device, MOD_WIN, 0x04) // Cmd+A
                else -> sendKeyCombo(device, MOD_CTRL, 0x04) // Ctrl+A
            }
            "fullscreen" -> sendKeyPress(device, KEY_F11)
            else -> false
        }
    }

    // D-pad methods for gamepad functionality
    fun sendDpadUp(): Boolean = sendKey(KEY_UP, 0)
    fun sendDpadDown(): Boolean = sendKey(KEY_DOWN, 0)
    fun sendDpadLeft(): Boolean = sendKey(KEY_LEFT, 0)
    fun sendDpadRight(): Boolean = sendKey(KEY_RIGHT, 0)
    fun sendDpadCenter(): Boolean = sendKey(KEY_ENTER, 0)

    // Media control methods
    fun sendPlayPause(): Boolean = connectedDevice?.let { sendPlayPause(it) } ?: false
    fun sendNextTrack(): Boolean = connectedDevice?.let { sendNextTrack(it) } ?: false
    fun sendPreviousTrack(): Boolean = connectedDevice?.let { sendPreviousTrack(it) } ?: false
    fun sendVolumeUp(): Boolean = connectedDevice?.let { sendVolumeUp(it) } ?: false
    fun sendVolumeDown(): Boolean = connectedDevice?.let { sendVolumeDown(it) } ?: false
    fun sendMute(): Boolean = connectedDevice?.let { sendConsumerControl(it, 0xE2) } ?: false

    // Mouse methods without device parameter
    fun sendMouseMovement(deltaX: Float, deltaY: Float): Boolean =
        connectedDevice?.let { sendMouseMovement(it, deltaX.toInt(), deltaY.toInt()) } ?: false

    fun sendLeftClick(): Boolean =
        connectedDevice?.let { sendMouseClick(it, true, false, false) } ?: false

    fun sendRightClick(): Boolean =
        connectedDevice?.let { sendMouseClick(it, false, true, false) } ?: false

    fun sendMiddleClick(): Boolean =
        connectedDevice?.let { sendMouseClick(it, false, false, true) } ?: false

    fun sendDoubleClick(): Boolean =
        connectedDevice?.let {
            sendMouseClick(it, true, false, false) &&
            sendMouseClick(it, true, false, false)
        } ?: false

    fun sendScroll(deltaX: Float, deltaY: Float): Boolean =
        connectedDevice?.let { sendMouseScroll(it, deltaY.toInt()) } ?: false

    // Key methods without device parameter
    fun sendKey(keyCode: Int, modifiers: Int = 0): Boolean =
        connectedDevice?.let { device ->
            if (modifiers != 0) {
                sendKeyCombo(device, modifiers, keyCode)
            } else {
                sendKeyPress(device, keyCode)
            }
        } ?: false

    fun sendText(text: String): Boolean {
        return connectedDevice?.let { device ->
            text.all { char ->
                val keyCode = charToKeyCode(char)
                if (keyCode > 0) {
                    sendKeyPress(device, keyCode)
                } else false
            }
        } ?: false
    }

    fun sendHidReport(reportId: Int, data: ByteArray): Boolean =
        connectedDevice?.let { hidDeviceProfile.sendReport(it, reportId, data) } ?: false

    private var connectedDevice: BluetoothDevice? = null

    fun setConnectedDevice(device: BluetoothDevice?) {
        connectedDevice = device
    }

    private fun charToKeyCode(char: Char): Int {
        return when (char) {
            'a', 'A' -> 0x04
            'b', 'B' -> 0x05
            'c', 'C' -> 0x06
            'd', 'D' -> 0x07
            'e', 'E' -> 0x08
            'f', 'F' -> 0x09
            'g', 'G' -> 0x0A
            'h', 'H' -> 0x0B
            'i', 'I' -> 0x0C
            'j', 'J' -> 0x0D
            'k', 'K' -> 0x0E
            'l', 'L' -> 0x0F
            'm', 'M' -> 0x10
            'n', 'N' -> 0x11
            'o', 'O' -> 0x12
            'p', 'P' -> 0x13
            'q', 'Q' -> 0x14
            'r', 'R' -> 0x15
            's', 'S' -> 0x16
            't', 'T' -> 0x17
            'u', 'U' -> 0x18
            'v', 'V' -> 0x19
            'w', 'W' -> 0x1A
            'x', 'X' -> 0x1B
            'y', 'Y' -> 0x1C
            'z', 'Z' -> 0x1D
            '1' -> 0x1E
            '2' -> 0x1F
            '3' -> 0x20
            '4' -> 0x21
            '5' -> 0x22
            '6' -> 0x23
            '7' -> 0x24
            '8' -> 0x25
            '9' -> 0x26
            '0' -> 0x27
            ' ' -> KEY_SPACE
            '\n' -> KEY_ENTER
            '\t' -> KEY_TAB
            else -> 0
        }
    }

    // Add coroutine scope for async operations
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Add cleanup method
    fun cleanup() {
        scope.cancel()
    }
}
