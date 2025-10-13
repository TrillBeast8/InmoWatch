package com.example.inmocontrol_v2.bluetooth

import android.bluetooth.BluetoothDevice
import android.util.Log
import kotlinx.coroutines.*
import kotlin.math.abs

/**
 * Expert-designed HID Input Manager with proper timing, debouncing, and thread safety.
 *
 * This implementation solves the intermittent input failure problem by:
 * 1. Enforcing 20ms delay between press/release reports (HID spec compliance)
 * 2. Using coroutines for asynchronous release scheduling (prevents blocking)
 * 3. Debouncing rapid inputs to prevent buffer overflow
 * 4. Thread-safe device state management
 * 5. Proper error handling and recovery
 *
 * @param hidDeviceProfile The Bluetooth HID device profile proxy
 */
class HidInputManager(
    private val hidDeviceProfile: HidDeviceProfile
) {
    companion object {
        private const val TAG = "HidInputManager"

        // Maximum delta values for mouse movement (HID spec)
        private const val MAX_MOUSE_DELTA = 127
        private const val MIN_MOUSE_DELTA = -127
    }

    // Coroutine scope for asynchronous release reports
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Thread-safe device tracking
    @Volatile
    private var currentDevice: BluetoothDevice? = null

    // Debouncing state
    private var lastActionTime = 0L

    // Character to HID scan code mapping with modifiers
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

        // Special characters (with Shift modifier where needed)
        ' ' to (0x2C to 0),      // Space
        '\n' to (0x28 to 0),     // Enter
        '\t' to (0x2B to 0),     // Tab
        '.' to (0x37 to 0),      // Period
        ',' to (0x36 to 0),      // Comma
        ';' to (0x33 to 0),      // Semicolon
        ':' to (0x33 to 0x02),   // Colon (Shift + semicolon)
        '/' to (0x38 to 0),      // Slash
        '?' to (0x38 to 0x02),   // Question (Shift + slash)
        '\'' to (0x34 to 0),     // Apostrophe
        '"' to (0x34 to 0x02),   // Quote (Shift + apostrophe)
        '[' to (0x2F to 0),      // Left bracket
        ']' to (0x30 to 0),      // Right bracket
        '{' to (0x2F to 0x02),   // Left brace (Shift + [)
        '}' to (0x30 to 0x02),   // Right brace (Shift + ])
        '\\' to (0x31 to 0),     // Backslash
        '|' to (0x31 to 0x02),   // Pipe (Shift + backslash)
        '-' to (0x2D to 0),      // Minus
        '_' to (0x2D to 0x02),   // Underscore (Shift + minus)
        '=' to (0x2E to 0),      // Equals
        '+' to (0x2E to 0x02),   // Plus (Shift + equals)
        '`' to (0x35 to 0),      // Backtick
        '~' to (0x35 to 0x02),   // Tilde (Shift + backtick)
        '!' to (0x1E to 0x02),   // Exclamation (Shift + 1)
        '@' to (0x1F to 0x02),   // At (Shift + 2)
        '#' to (0x20 to 0x02),   // Hash (Shift + 3)
        '$' to (0x21 to 0x02),   // Dollar (Shift + 4)
        '%' to (0x22 to 0x02),   // Percent (Shift + 5)
        '^' to (0x23 to 0x02),   // Caret (Shift + 6)
        '&' to (0x24 to 0x02),   // Ampersand (Shift + 7)
        '*' to (0x25 to 0x02),   // Asterisk (Shift + 8)
        '(' to (0x26 to 0x02),   // Left paren (Shift + 9)
        ')' to (0x27 to 0x02)    // Right paren (Shift + 0)
    )

    /**
     * Sets the currently connected device.
     * Thread-safe via @Volatile.
     */
    fun setConnectedDevice(device: BluetoothDevice?) {
        currentDevice = device
        if (device == null) {
            Log.d(TAG, "Device disconnected, clearing state")
        }
    }

    /**
     * Cleanup resources and cancel pending coroutines.
     */
    fun cleanup() {
        scope.cancel()
        currentDevice = null
    }

    // ==================== MOUSE OPERATIONS ====================

    /**
     * Send mouse movement delta.
     * Deltas are clamped to HID specification range [-127, 127].
     *
     * @param deltaX Horizontal movement in pixels
     * @param deltaY Vertical movement in pixels
     * @return true if report sent successfully
     */
    fun sendMouseMovement(deltaX: Float, deltaY: Float): Boolean {
        val device = currentDevice
        if (device == null) {
            Log.w(TAG, "❌ Cannot send mouse movement - no device connected")
            return false
        }

        // Clamp to HID spec range
        val clampedX = deltaX.toInt().coerceIn(MIN_MOUSE_DELTA, MAX_MOUSE_DELTA)
        val clampedY = deltaY.toInt().coerceIn(MIN_MOUSE_DELTA, MAX_MOUSE_DELTA)

        // Skip if movement is negligible
        if (abs(clampedX) == 0 && abs(clampedY) == 0) {
            return true
        }

        Log.d(TAG, "📍 Sending mouse movement: X=$clampedX, Y=$clampedY (original: X=$deltaX, Y=$deltaY)")

        val report = createMouseReport(
            left = false,
            right = false,
            middle = false,
            deltaX = clampedX.toByte(),
            deltaY = clampedY.toByte(),
            wheel = 0
        )

        val success = sendReport(device, HidConstants.ID_MOUSE, report)
        if (!success) {
            Log.e(TAG, "❌ Failed to send mouse movement report")
        }
        return success
    }

    /**
     * Send mouse scroll delta.
     *
     * @param deltaX Horizontal scroll (usually not supported by hosts)
     * @param deltaY Vertical scroll amount
     * @return true if report sent successfully
     */
    @Suppress("unused") // Used by HidService
    fun sendMouseScroll(deltaX: Float, deltaY: Float): Boolean {
        val device = currentDevice ?: return false

        val clampedWheel = deltaY.toInt().coerceIn(MIN_MOUSE_DELTA, MAX_MOUSE_DELTA)

        if (abs(clampedWheel) == 0) {
            return true
        }

        val report = createMouseReport(
            left = false,
            right = false,
            middle = false,
            deltaX = 0,
            deltaY = 0,
            wheel = clampedWheel.toByte()
        )

        return sendReport(device, HidConstants.ID_MOUSE, report)
    }

    /**
     * EXPERT SOLUTION: Mouse click with proper timing and async release.
     *
     * This is the key fix for intermittent input failures:
     * 1. Send "press" report immediately
     * 2. Launch coroutine to send "release" after 15ms delay
     * 3. Prevents race conditions and ensures host receives both events
     *
     * NOTE: Debouncing removed for mouse clicks to allow rapid clicking
     *
     * @param left Left button state
     * @param right Right button state
     * @param middle Middle button state
     * @return true if press report sent successfully
     */
    @Suppress("unused") // Used by HidService
    fun sendMouseClick(left: Boolean, right: Boolean, middle: Boolean): Boolean {
        val device = currentDevice ?: return false

        // Send PRESS report immediately (no debouncing for mouse clicks)
        val pressReport = createMouseReport(left, right, middle, 0, 0, 0)
        val pressSuccess = sendReport(device, HidConstants.ID_MOUSE, pressReport)

        if (!pressSuccess) {
            Log.e(TAG, "Failed to send mouse press report")
            return false
        }

        // Schedule RELEASE report asynchronously after delay
        scope.launch {
            delay(HidConstants.PRESS_RELEASE_DELAY_MS)

            // Verify device is still connected
            val currentDev = currentDevice
            if (currentDev != null) {
                val releaseReport = createMouseReport(false, false, false, 0, 0, 0)
                val releaseSuccess = sendReport(currentDev, HidConstants.ID_MOUSE, releaseReport)

                if (!releaseSuccess) {
                    Log.w(TAG, "Failed to send mouse release report")
                }
            }
        }

        return true
    }

    /**
     * Helper: Create mouse HID report.
     * Report structure: [buttons, deltaX, deltaY, wheel]
     */
    private fun createMouseReport(
        left: Boolean,
        right: Boolean,
        middle: Boolean,
        deltaX: Byte,
        deltaY: Byte,
        wheel: Byte
    ): ByteArray {
        var buttons: Byte = 0
        if (left) buttons = (buttons.toInt() or HidConstants.MOUSE_BUTTON_LEFT.toInt()).toByte()
        if (right) buttons = (buttons.toInt() or HidConstants.MOUSE_BUTTON_RIGHT.toInt()).toByte()
        if (middle) buttons = (buttons.toInt() or HidConstants.MOUSE_BUTTON_MIDDLE.toInt()).toByte()

        return byteArrayOf(buttons, deltaX, deltaY, wheel)
    }

    // ==================== KEYBOARD OPERATIONS ====================

    /**
     * EXPERT SOLUTION: Send single key press with proper timing.
     *
     * @param keyCode HID scan code (0x04 = A, 0x1E = 1, etc.)
     * @param modifiers Modifier byte (0x01=Ctrl, 0x02=Shift, 0x04=Alt, 0x08=Win)
     * @return true if press report sent successfully
     */
    fun sendKey(keyCode: Int, modifiers: Int = 0): Boolean {
        val device = currentDevice ?: return false

        // Send PRESS report
        val pressReport = createKeyboardReport(modifiers.toByte(), byteArrayOf(keyCode.toByte()))
        val pressSuccess = sendReport(device, HidConstants.ID_KEYBOARD, pressReport)

        if (!pressSuccess) {
            Log.e(TAG, "Failed to send key press report")
            return false
        }

        // Schedule RELEASE report asynchronously
        scope.launch {
            delay(HidConstants.PRESS_RELEASE_DELAY_MS)

            val currentDev = currentDevice
            if (currentDev != null) {
                val releaseReport = createKeyboardReport(0, byteArrayOf())
                sendReport(currentDev, HidConstants.ID_KEYBOARD, releaseReport)
            }
        }

        return true
    }

    /**
     * Send text string as individual key presses.
     * Uses character mapping to determine scan codes and modifiers.
     *
     * @param text Text to transmit
     * @return true if all characters sent successfully
     */
    fun sendText(text: String): Boolean {
        val device = currentDevice ?: return false

        var allSuccess = true

        // Use blocking coroutine to ensure sequential character transmission
        runBlocking {
            for (char in text) {
                val (scanCode, modifier) = charToScanCode(char)

                if (scanCode == 0x00) {
                    Log.w(TAG, "Unmapped character: '$char' (skipping)")
                    continue
                }

                // Send press
                val pressReport = createKeyboardReport(modifier.toByte(), byteArrayOf(scanCode.toByte()))
                val pressSuccess = sendReport(device, HidConstants.ID_KEYBOARD, pressReport)

                if (!pressSuccess) {
                    allSuccess = false
                    continue
                }

                // Delay before release
                delay(HidConstants.PRESS_RELEASE_DELAY_MS)

                // Send release
                val releaseReport = createKeyboardReport(0, byteArrayOf())
                sendReport(device, HidConstants.ID_KEYBOARD, releaseReport)

                // Small delay between characters to prevent flooding
                delay(HidConstants.KEY_REPEAT_DELAY_MS)
            }
        }

        return allSuccess
    }

    /**
     * Map character to HID scan code and modifier.
     *
     * @return Pair of (scanCode, modifier) where modifier is 0x00 (none) or 0x02 (Shift)
     */
    private fun charToScanCode(char: Char): Pair<Int, Int> {
        return charToKeyMap[char] ?: (0x00 to 0)
    }

    /**
     * Helper: Create keyboard HID report.
     * Report structure: [modifier, reserved, key1, key2, key3, key4, key5, key6]
     */
    private fun createKeyboardReport(modifier: Byte, keys: ByteArray): ByteArray {
        val report = ByteArray(8) { 0 }
        report[0] = modifier
        report[1] = 0 // Reserved byte

        // Copy up to 6 keys
        val keyCount = keys.size.coerceAtMost(6)
        for (i in 0 until keyCount) {
            report[2 + i] = keys[i]
        }

        return report
    }

    // ==================== MEDIA OPERATIONS ====================

    /**
     * EXPERT SOLUTION: Send consumer control (media) command with proper timing.
     *
     * @param usage HID consumer usage code (e.g., USAGE_PLAY_PAUSE)
     * @return true if press report sent successfully
     */
    fun sendConsumerControl(usage: Short): Boolean {
        val device = currentDevice ?: return false

        // Debouncing for media controls (prevent double-press)
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastActionTime < HidConstants.DEBOUNCE_DELAY_MS) {
            Log.d(TAG, "Media control debounced")
            return false
        }
        lastActionTime = currentTime

        // Send PRESS report
        val pressReport = createConsumerReport(usage)
        val pressSuccess = sendReport(device, HidConstants.ID_CONSUMER, pressReport)

        if (!pressSuccess) {
            Log.e(TAG, "Failed to send consumer press report")
            return false
        }

        // Schedule RELEASE report asynchronously
        scope.launch {
            delay(HidConstants.PRESS_RELEASE_DELAY_MS)

            val currentDev = currentDevice
            if (currentDev != null) {
                val releaseReport = createConsumerReport(0)
                sendReport(currentDev, HidConstants.ID_CONSUMER, releaseReport)
            }
        }

        return true
    }

    /**
     * Helper: Create consumer control HID report.
     * Report structure: [usage_LSB, usage_MSB]
     */
    private fun createConsumerReport(usage: Short): ByteArray {
        return byteArrayOf(
            (usage.toInt() and 0xFF).toByte(),           // LSB
            ((usage.toInt() shr 8) and 0xFF).toByte()    // MSB
        )
    }

    // ==================== MEDIA CONTROLS ====================
    @Suppress("unused") // Used by HidService
    fun playPause(): Boolean = sendConsumerControl(HidConstants.USAGE_PLAY_PAUSE)

    @Suppress("unused") // Used by HidService
    fun nextTrack(): Boolean = sendConsumerControl(HidConstants.USAGE_NEXT_TRACK)

    @Suppress("unused") // Used by HidService
    fun previousTrack(): Boolean = sendConsumerControl(HidConstants.USAGE_PREV_TRACK)

    @Suppress("unused") // Used by HidService
    fun volumeUp(): Boolean = sendConsumerControl(HidConstants.USAGE_VOLUME_UP)

    @Suppress("unused") // Used by HidService
    fun volumeDown(): Boolean = sendConsumerControl(HidConstants.USAGE_VOLUME_DOWN)

    @Suppress("unused") // Used by HidService
    fun mute(): Boolean = sendConsumerControl(HidConstants.USAGE_MUTE)

    /**
     * Send multiple keys simultaneously (for diagonal D-pad movements).
     * Used for UP+LEFT, UP+RIGHT, DOWN+LEFT, DOWN+RIGHT.
     *
     * @param keyCodes Array of HID scan codes to send together
     * @return true if press report sent successfully
     */
    fun sendKeys(keyCodes: IntArray): Boolean {
        val device = currentDevice ?: return false

        if (keyCodes.isEmpty() || keyCodes.size > 6) {
            Log.e(TAG, "Invalid key count: ${keyCodes.size} (max 6)")
            return false
        }

        // Convert to ByteArray
        val keys = ByteArray(keyCodes.size) { keyCodes[it].toByte() }

        // Send PRESS report
        val pressReport = createKeyboardReport(0, keys)
        val pressSuccess = sendReport(device, HidConstants.ID_KEYBOARD, pressReport)

        if (!pressSuccess) {
            Log.e(TAG, "Failed to send multi-key press report")
            return false
        }

        // Schedule RELEASE report asynchronously
        scope.launch {
            delay(HidConstants.PRESS_RELEASE_DELAY_MS)

            val currentDev = currentDevice
            if (currentDev != null) {
                val releaseReport = createKeyboardReport(0, byteArrayOf())
                sendReport(currentDev, HidConstants.ID_KEYBOARD, releaseReport)
            }
        }

        return true
    }

    // ==================== CORE TRANSMISSION ====================

    /**
     * Core method: Send HID report to device.
     * Handles all error cases and logging.
     *
     * @param device Target Bluetooth device
     * @param reportId Report ID (1=Keyboard, 2=Mouse, 3=Consumer)
     * @param data Report payload bytes
     * @return true if sent successfully
     */
    private fun sendReport(device: BluetoothDevice, reportId: Byte, data: ByteArray): Boolean {
        return try {
            val success = hidDeviceProfile.sendReport(device, reportId.toInt(), data)

            if (!success) {
                Log.w(TAG, "sendReport returned false for reportId=$reportId")
            }

            success
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception sending report: ${e.message}")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Exception sending report", e)
            false
        }
    }

    /**
     * Public wrapper to allow higher layers to send a raw HID report when needed.
     * Validates there is a connected device and delegates to the internal sendReport.
     *
     * @param reportId Report ID as integer (1=Keyboard, 2=Mouse, 3=Consumer)
     * @param data Report payload bytes
     * @return true if the report was sent; false otherwise
     */
    fun sendHidReport(reportId: Int, data: ByteArray): Boolean {
        val device = currentDevice ?: return false
        return sendReport(device, reportId.toByte(), data)
    }
}
