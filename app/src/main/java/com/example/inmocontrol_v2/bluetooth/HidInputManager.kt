package com.example.inmocontrol_v2.bluetooth

import android.bluetooth.BluetoothDevice
import android.util.Log

/**
 * HID Input Manager based on proven WearMouse implementation
 * Fixed to use correct Report IDs and structures for reliable connectivity
 */
class HidInputManager(private val hidDeviceProfile: HidDeviceProfile) {

    companion object {
        private const val TAG = "HidInputManager"
    }

    // Helper to format bytes as hex for logging
    private fun toHex(bytes: ByteArray?): String {
        if (bytes == null) return "<null>"
        return bytes.joinToString(separator = " ") { String.format("%02X", it) }
    }

    /**
     * Send keyboard key press - uses Report ID 1 (matches wearmouse)
     */
    fun sendKeyPress(device: BluetoothDevice, keyCode: Int): Boolean {
        Log.d(TAG, "Sending key press: keyCode=$keyCode")

        // Create keyboard report: [modifier, reserved, key1, key2, key3, key4, key5, key6]
        val pressReport = byteArrayOf(0, 0, keyCode.toByte(), 0, 0, 0, 0, 0)
        Log.d(TAG, "Keyboard press report (id=${HidConstants.ID_KEYBOARD}): ${toHex(pressReport)}")
        val pressSuccess = hidDeviceProfile.sendReport(device, HidConstants.ID_KEYBOARD.toInt(), pressReport)

        // Small delay
        Thread.sleep(50)

        // Send key release (all zeros)
        val releaseReport = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
        Log.d(TAG, "Keyboard release report (id=${HidConstants.ID_KEYBOARD}): ${toHex(releaseReport)}")
        val releaseSuccess = hidDeviceProfile.sendReport(device, HidConstants.ID_KEYBOARD.toInt(), releaseReport)

        return pressSuccess && releaseSuccess
    }

    /**
     * Send keyboard key combination - uses Report ID 1
     */
    fun sendKeyCombo(device: BluetoothDevice, modifier: Int, keyCode: Int): Boolean {
        Log.d(TAG, "Sending key combo: modifier=$modifier, keyCode=$keyCode")

        // Create keyboard report with modifier
        val pressReport = byteArrayOf(modifier.toByte(), 0, keyCode.toByte(), 0, 0, 0, 0, 0)
        Log.d(TAG, "Keyboard combo report (id=${HidConstants.ID_KEYBOARD}): ${toHex(pressReport)}")
        val pressSuccess = hidDeviceProfile.sendReport(device, HidConstants.ID_KEYBOARD.toInt(), pressReport)

        // Small delay
        Thread.sleep(50)

        // Send key release
        val releaseReport = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
        Log.d(TAG, "Keyboard combo release (id=${HidConstants.ID_KEYBOARD}): ${toHex(releaseReport)}")
        val releaseSuccess = hidDeviceProfile.sendReport(device, HidConstants.ID_KEYBOARD.toInt(), releaseReport)

        return pressSuccess && releaseSuccess
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
     * Send mouse click - uses Report ID 2
     */
    fun sendMouseClick(device: BluetoothDevice, left: Boolean, right: Boolean, middle: Boolean): Boolean {
        Log.d(TAG, "Sending mouse click: left=$left, right=$right, middle=$middle")

        // Send button press
        val pressReport = createMouseReport(left, right, middle, 0, 0, 0)
        Log.d(TAG, "Mouse press report (id=${HidConstants.ID_MOUSE}): ${toHex(pressReport)}")
        val pressSuccess = hidDeviceProfile.sendReport(device, HidConstants.ID_MOUSE.toInt(), pressReport)

        // Small delay between press and release
        Thread.sleep(50)

        // Send button release
        val releaseReport = createMouseReport(false, false, false, 0, 0, 0)
        Log.d(TAG, "Mouse release report (id=${HidConstants.ID_MOUSE}): ${toHex(releaseReport)}")
        val releaseSuccess = hidDeviceProfile.sendReport(device, HidConstants.ID_MOUSE.toInt(), releaseReport)

        return pressSuccess && releaseSuccess
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

    fun sendMute(device: BluetoothDevice): Boolean {
        return sendConsumerControl(device, HidConstants.USAGE_MUTE)
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

        // Send key press - 2 bytes for 16-bit usage value
        val pressReport = byteArrayOf(
            (usage and 0xFF).toByte(),
            ((usage shr 8) and 0xFF).toByte()
        )
        Log.d(TAG, "Consumer press report (id=${HidConstants.ID_CONSUMER}): ${toHex(pressReport)}")
        val pressSuccess = hidDeviceProfile.sendReport(device, HidConstants.ID_CONSUMER.toInt(), pressReport)

        // Small delay
        Thread.sleep(50)

        // Send key release (all zeros)
        val releaseReport = byteArrayOf(0, 0)
        Log.d(TAG, "Consumer release report (id=${HidConstants.ID_CONSUMER}): ${toHex(releaseReport)}")
        val releaseSuccess = hidDeviceProfile.sendReport(device, HidConstants.ID_CONSUMER.toInt(), releaseReport)

        return pressSuccess && releaseSuccess
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
}

/**
 * Device-specific input profiles moved here to colocate with the low-level input manager.
 * This reduces file count and keeps related logic together.
 */

// InputProfile and implementations moved into the bluetooth package
// Normalize types to use the BluetoothDevice import rather than fully-qualified names
interface InputProfile {
    fun sendKey(device: BluetoothDevice, keyCode: Int, modifiers: Int = 0): Boolean
    fun moveMouse(device: BluetoothDevice, dx: Int, dy: Int): Boolean
    fun mouseScroll(device: BluetoothDevice, x: Int, y: Int): Boolean
    fun mouseLeftClick(device: BluetoothDevice): Boolean
    fun mouseRightClick(device: BluetoothDevice): Boolean
    fun mouseDoubleClick(device: BluetoothDevice): Boolean
    fun mouseDragMove(device: BluetoothDevice, dx: Int, dy: Int): Boolean
    fun mouseDragEnd(device: BluetoothDevice): Boolean
    fun dpad(device: BluetoothDevice, direction: Int): Boolean
    fun playPause(device: BluetoothDevice): Boolean
    fun nextTrack(device: BluetoothDevice): Boolean
    fun previousTrack(device: BluetoothDevice): Boolean
    fun setVolume(device: BluetoothDevice, volume: Int): Boolean
    fun volumeUp(device: BluetoothDevice): Boolean
    fun volumeDown(device: BluetoothDevice): Boolean
    fun switchOutput(device: BluetoothDevice): Boolean
    fun resetInputs(device: BluetoothDevice): Boolean
}

class GenericInputProfile(private val input: HidInputManager) : InputProfile {
    override fun sendKey(device: BluetoothDevice, keyCode: Int, modifiers: Int) =
        if (modifiers != 0) input.sendKeyCombo(device, modifiers, keyCode) else input.sendKeyPress(device, keyCode)

    override fun moveMouse(device: BluetoothDevice, dx: Int, dy: Int) = input.sendMouseMovement(device, dx, dy)
    override fun mouseScroll(device: BluetoothDevice, x: Int, y: Int) = input.sendMouseScroll(device, y)
    override fun mouseLeftClick(device: BluetoothDevice) = input.sendMouseClick(device, left = true, right = false, middle = false)
    override fun mouseRightClick(device: BluetoothDevice) = input.sendMouseClick(device, left = false, right = true, middle = false)
    override fun mouseDoubleClick(device: BluetoothDevice): Boolean {
        val ok = input.sendMouseClick(device, left = true, right = false, middle = false)
        Thread.sleep(50)
        return ok && input.sendMouseClick(device, left = true, right = false, middle = false)
    }
    override fun mouseDragMove(device: BluetoothDevice, dx: Int, dy: Int) = input.sendMouseDrag(device, dx, dy, leftButton = true)
    override fun mouseDragEnd(device: BluetoothDevice) = input.sendMouseDrag(device, 0, 0, leftButton = false)
    override fun dpad(device: BluetoothDevice, direction: Int) = when(direction) {
        0 -> input.sendKeyPress(device, 82) // UP (WearMouse: Key.UP = 82)
        1 -> input.sendKeyPress(device, 81) // DOWN (WearMouse: Key.DOWN = 81)
        2 -> input.sendKeyPress(device, 80) // LEFT (WearMouse: Key.LEFT = 80)
        3 -> input.sendKeyPress(device, 79) // RIGHT (WearMouse: Key.RIGHT = 79)
        4 -> input.sendKeyPress(device, 40) // CENTER/ENTER (WearMouse: Key.ENTER = 40)
        5 -> { // DOWN-LEFT diagonal
            input.sendKeyPress(device, 81) // DOWN
            Thread.sleep(10)
            input.sendKeyPress(device, 80) // LEFT
        }
        6 -> { // DOWN-RIGHT diagonal
            input.sendKeyPress(device, 81) // DOWN
            Thread.sleep(10)
            input.sendKeyPress(device, 79) // RIGHT
        }
        7 -> { // UP-LEFT diagonal
            input.sendKeyPress(device, 82) // UP
            Thread.sleep(10)
            input.sendKeyPress(device, 80) // LEFT
        }
        8 -> { // UP-RIGHT diagonal
            input.sendKeyPress(device, 82) // UP
            Thread.sleep(10)
            input.sendKeyPress(device, 79) // RIGHT
        }
        else -> false
    }
    override fun playPause(device: BluetoothDevice) = input.sendPlayPause(device)
    override fun nextTrack(device: BluetoothDevice) = input.sendNextTrack(device)
    override fun previousTrack(device: BluetoothDevice) = input.sendPreviousTrack(device)
    override fun setVolume(device: BluetoothDevice, volume: Int): Boolean = when {
        volume > 0 -> input.sendVolumeUp(device)
        volume < 0 -> input.sendVolumeDown(device)
        else -> false
    }
    override fun volumeUp(device: BluetoothDevice) = input.sendVolumeUp(device)
    override fun volumeDown(device: BluetoothDevice) = input.sendVolumeDown(device)
    override fun switchOutput(device: BluetoothDevice) = input.sendOutputSwitch(device)
    override fun resetInputs(device: BluetoothDevice): Boolean {
        input.clearAllInputs(device)
        return true
    }
}

class InmoAir2InputProfile(private val input: HidInputManager) : InputProfile {
    private val TAG = "InmoAir2Profile"

    override fun sendKey(device: BluetoothDevice, keyCode: Int, modifiers: Int): Boolean {
        Log.d(TAG, "sendKey: key=$keyCode modifiers=$modifiers")
        val primary = if (modifiers != 0) input.sendKeyCombo(device, modifiers, keyCode) else input.sendKeyPress(device, keyCode)
        if (primary) {
            try { input.sendKeyPress(device, keyCode) } catch (_: Throwable) {}
        }
        return primary
    }

    override fun moveMouse(device: BluetoothDevice, dx: Int, dy: Int): Boolean {
        val scale = 2.0f
        val sdx = (dx * scale).toInt().coerceIn(-127, 127)
        val sdy = (dy * scale).toInt().coerceIn(-127, 127)
        return input.sendMouseMovement(device, sdx, sdy)
    }

    override fun mouseScroll(device: BluetoothDevice, x: Int, y: Int): Boolean {
        val r1 = input.sendMouseScroll(device, y)
        val r2 = if (y > 0) input.sendVolumeUp(device) else if (y < 0) input.sendVolumeDown(device) else true
        return r1 || r2
    }

    override fun mouseLeftClick(device: BluetoothDevice) = input.sendMouseClick(device, left = true, right = false, middle = false)
    override fun mouseRightClick(device: BluetoothDevice) = input.sendMouseClick(device, left = false, right = true, middle = false)
    override fun mouseDoubleClick(device: BluetoothDevice): Boolean {
        val ok = input.sendMouseClick(device, left = true, right = false, middle = false)
        Thread.sleep(40)
        return ok && input.sendMouseClick(device, left = true, right = false, middle = false)
    }

    override fun mouseDragMove(device: BluetoothDevice, dx: Int, dy: Int) = input.sendMouseDrag(device, dx, dy, leftButton = true)
    override fun mouseDragEnd(device: BluetoothDevice) = input.sendMouseDrag(device, 0, 0, leftButton = false)

    override fun dpad(device: BluetoothDevice, direction: Int): Boolean {
        val keySent = when(direction) {
            0 -> input.sendKeyPress(device, 82)
            1 -> input.sendKeyPress(device, 81)
            2 -> input.sendKeyPress(device, 80)
            3 -> input.sendKeyPress(device, 79)
            4 -> input.sendKeyPress(device, 40)
            5 -> { // DOWN-LEFT diagonal
                input.sendKeyPress(device, 81) // DOWN
                Thread.sleep(10)
                input.sendKeyPress(device, 80) // LEFT
            }
            6 -> { // DOWN-RIGHT diagonal
                input.sendKeyPress(device, 81) // DOWN
                Thread.sleep(10)
                input.sendKeyPress(device, 79) // RIGHT
            }
            7 -> { // UP-LEFT diagonal
                input.sendKeyPress(device, 82) // UP
                Thread.sleep(10)
                input.sendKeyPress(device, 80) // LEFT
            }
            8 -> { // UP-RIGHT diagonal
                input.sendKeyPress(device, 82) // UP
                Thread.sleep(10)
                input.sendKeyPress(device, 79) // RIGHT
            }
            else -> false
        }
        if (!keySent) {
            try { input.sendMouseMovement(device, 0, 0) } catch (_: Throwable) {}
        }
        return keySent
    }

    override fun playPause(device: BluetoothDevice) = input.sendPlayPause(device)
    override fun nextTrack(device: BluetoothDevice) = input.sendNextTrack(device)
    override fun previousTrack(device: BluetoothDevice) = input.sendPreviousTrack(device)

    override fun setVolume(device: BluetoothDevice, volume: Int): Boolean = when {
        volume > 0 -> input.sendVolumeUp(device)
        volume < 0 -> input.sendVolumeDown(device)
        else -> false
    }

    override fun volumeUp(device: BluetoothDevice) = input.sendVolumeUp(device)
    override fun volumeDown(device: BluetoothDevice) = input.sendVolumeDown(device)
    override fun switchOutput(device: BluetoothDevice) = input.sendOutputSwitch(device)

    override fun resetInputs(device: BluetoothDevice): Boolean {
        input.clearAllInputs(device)
        return true
    }
}
