package com.example.inmocontrol_v2.hid

import android.bluetooth.BluetoothDevice

/**
 * Interface defining HID service operations exposed to UI layer via HidClient.
 * All methods return Boolean for instant Compose reactivity.
 */
interface HidServiceApi {

    // Service state
    fun isReady(): Boolean
    fun isDeviceConnected(): Boolean

    // Connection management
    fun connectToDevice(device: BluetoothDevice): Boolean

    // Mouse operations
    fun sendMouseMovement(deltaX: Float, deltaY: Float): Boolean
    fun sendLeftClick(): Boolean
    fun sendRightClick(): Boolean
    fun sendMiddleClick(): Boolean
    fun sendDoubleClick(): Boolean
    fun sendMouseScroll(deltaX: Float, deltaY: Float): Boolean

    // Keyboard operations
    fun sendKey(keyCode: Int, modifiers: Int = 0): Boolean
    fun sendText(text: String): Boolean

    // Media controls
    fun playPause(): Boolean
    fun nextTrack(): Boolean
    fun previousTrack(): Boolean
    fun volumeUp(): Boolean
    fun volumeDown(): Boolean
    fun mute(): Boolean

    // D-Pad navigation
    fun dpad(direction: Int): Boolean
    fun sendDpadUp(): Boolean
    fun sendDpadDown(): Boolean
    fun sendDpadLeft(): Boolean
    fun sendDpadRight(): Boolean
    fun sendDpadCenter(): Boolean

    // ESC key for back functionality
    fun sendEscape(): Boolean

    // Raw HID
    fun sendRawInput(data: ByteArray): Boolean
}
