package com.example.inmocontrol_v2.bluetooth

/**
 * Base interface for input profiles that handle device-specific input mappings
 */
interface InputProfile {
    // Mouse input methods
    fun sendMouseMovement(deltaX: Float, deltaY: Float): Boolean
    fun sendLeftClick(): Boolean
    fun sendRightClick(): Boolean
    fun sendMiddleClick(): Boolean
    fun sendDoubleClick(): Boolean
    fun sendScroll(deltaX: Float, deltaY: Float): Boolean

    // Keyboard input methods
    fun sendKey(keyCode: Int, modifiers: Int = 0): Boolean
    fun sendText(text: String): Boolean

    // Media control methods
    fun sendPlayPause(): Boolean
    fun sendNextTrack(): Boolean
    fun sendPreviousTrack(): Boolean
    fun sendVolumeUp(): Boolean
    fun sendVolumeDown(): Boolean
    fun sendMute(): Boolean

    // D-pad/Gamepad methods
    fun sendDpadUp(): Boolean
    fun sendDpadDown(): Boolean
    fun sendDpadLeft(): Boolean
    fun sendDpadRight(): Boolean
    fun sendDpadCenter(): Boolean

    // Generic HID methods
    fun sendHidReport(reportId: Int, data: ByteArray): Boolean
}
