package com.example.inmocontrol_v2.bluetooth

import android.util.Log

/**
 * HID Input Manager following Wear Mouse patterns for reliable input handling
 */
class HidInputManager(private val hidManager: BluetoothHidManager) {

    companion object {
        private const val TAG = "HidInputManager"

        // HID Report IDs (following Wear Mouse standard)
        private const val MOUSE_REPORT_ID: Byte = 0x01
        private const val KEYBOARD_REPORT_ID: Byte = 0x02
        private const val MEDIA_REPORT_ID: Byte = 0x03

        // Mouse button constants
        private const val MOUSE_BUTTON_LEFT: Byte = 0x01
        private const val MOUSE_BUTTON_RIGHT: Byte = 0x02
        private const val MOUSE_BUTTON_MIDDLE: Byte = 0x04

        // Keyboard modifier constants
        private const val KEY_MOD_CTRL: Byte = 0x01
        private const val KEY_MOD_SHIFT: Byte = 0x02
        private const val KEY_MOD_ALT: Byte = 0x04

        // Media key constants (Consumer Usage IDs)
        private const val MEDIA_PLAY_PAUSE: Short = 0x00CD.toShort()
        private const val MEDIA_VOLUME_UP: Short = 0x00E9.toShort()
        private const val MEDIA_VOLUME_DOWN: Short = 0x00EA.toShort()
        private const val MEDIA_NEXT_TRACK: Short = 0x00B5.toShort()
        private const val MEDIA_PREV_TRACK: Short = 0x00B6.toShort()
    }

    // Current button states (for proper press/release handling)
    private var currentMouseButtons: Byte = 0
    private var currentModifiers: Byte = 0

    /**
     * Send mouse movement (Wear Mouse compatible format)
     */
    fun sendMouseMove(deltaX: Int, deltaY: Int): Boolean {
        // Clamp delta values to signed byte range
        val dx = deltaX.coerceIn(-127, 127).toByte()
        val dy = deltaY.coerceIn(-127, 127).toByte()

        // Mouse report: [buttons, x, y, wheel]
        val report = byteArrayOf(currentMouseButtons, dx, dy, 0)

        return hidManager.sendHidReport(MOUSE_REPORT_ID, report)
    }

    /**
     * Send mouse click
     */
    fun sendMouseClick(button: MouseButton, pressed: Boolean): Boolean {
        val buttonMask = when (button) {
            MouseButton.LEFT -> MOUSE_BUTTON_LEFT
            MouseButton.RIGHT -> MOUSE_BUTTON_RIGHT
            MouseButton.MIDDLE -> MOUSE_BUTTON_MIDDLE
        }

        currentMouseButtons = if (pressed) {
            (currentMouseButtons.toInt() or buttonMask.toInt()).toByte()
        } else {
            (currentMouseButtons.toInt() and buttonMask.toInt().inv()).toByte()
        }

        // Mouse report: [buttons, x, y, wheel]
        val report = byteArrayOf(currentMouseButtons, 0, 0, 0)

        return hidManager.sendHidReport(MOUSE_REPORT_ID, report)
    }

    /**
     * Send mouse scroll
     */
    fun sendMouseScroll(wheelDelta: Int): Boolean {
        val wheel = wheelDelta.coerceIn(-127, 127).toByte()

        // Mouse report: [buttons, x, y, wheel]
        val report = byteArrayOf(currentMouseButtons, 0, 0, wheel)

        return hidManager.sendHidReport(MOUSE_REPORT_ID, report)
    }

    /**
     * Send keyboard key press/release
     */
    fun sendKeyboardKey(keyCode: Int, pressed: Boolean, modifier: Byte = 0): Boolean {
        if (pressed) {
            currentModifiers = (currentModifiers.toInt() or modifier.toInt()).toByte()
        } else {
            currentModifiers = (currentModifiers.toInt() and modifier.toInt().inv()).toByte()
        }

        // Keyboard report: [modifiers, reserved, key1, key2, key3, key4, key5, key6]
        val report = if (pressed) {
            byteArrayOf(currentModifiers, 0, keyCode.toByte(), 0, 0, 0, 0, 0)
        } else {
            byteArrayOf(currentModifiers, 0, 0, 0, 0, 0, 0, 0)
        }

        return hidManager.sendHidReport(KEYBOARD_REPORT_ID, report)
    }

    /**
     * Send media key (Wear Mouse compatible)
     */
    fun sendMediaKey(mediaKey: MediaKey): Boolean {
        val usage = when (mediaKey) {
            MediaKey.PLAY_PAUSE -> MEDIA_PLAY_PAUSE
            MediaKey.VOLUME_UP -> MEDIA_VOLUME_UP
            MediaKey.VOLUME_DOWN -> MEDIA_VOLUME_DOWN
            MediaKey.NEXT_TRACK -> MEDIA_NEXT_TRACK
            MediaKey.PREV_TRACK -> MEDIA_PREV_TRACK
        }

        // Media report: [usage_low, usage_high]
        val report = byteArrayOf(
            (usage.toInt() and 0xFF).toByte(),
            ((usage.toInt() shr 8) and 0xFF).toByte()
        )

        val success = hidManager.sendHidReport(MEDIA_REPORT_ID, report)

        // Send release after press (Wear Mouse pattern)
        if (success) {
            val releaseReport = byteArrayOf(0, 0)
            hidManager.sendHidReport(MEDIA_REPORT_ID, releaseReport)
        }

        return success
    }

    /**
     * Release all keys and buttons
     */
    fun releaseAll(): Boolean {
        currentMouseButtons = 0
        currentModifiers = 0

        // Release mouse buttons
        val mouseRelease = byteArrayOf(0, 0, 0, 0)
        val mouseSuccess = hidManager.sendHidReport(MOUSE_REPORT_ID, mouseRelease)

        // Release keyboard keys
        val keyRelease = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
        val keySuccess = hidManager.sendHidReport(KEYBOARD_REPORT_ID, keyRelease)

        return mouseSuccess && keySuccess
    }

    // Enums for type safety
    enum class MouseButton { LEFT, RIGHT, MIDDLE }
    enum class MediaKey { PLAY_PAUSE, VOLUME_UP, VOLUME_DOWN, NEXT_TRACK, PREV_TRACK }
}
