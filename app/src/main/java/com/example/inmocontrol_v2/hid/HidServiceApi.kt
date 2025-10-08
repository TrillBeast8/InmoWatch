package com.example.inmocontrol_v2.hid

import android.bluetooth.BluetoothDevice
import com.example.inmocontrol_v2.data.DeviceProfile

/**
 * HID Service API interface defining all methods for HID operations
 */
interface HidServiceApi {
    var currentDeviceProfile: DeviceProfile?

    // Service state methods
    fun isReady(): Boolean
    fun isDeviceConnected(): Boolean
    fun getConnectedDevice(): BluetoothDevice?

    // Connection methods
    fun connectToDevice(device: BluetoothDevice): Boolean
    fun disconnectFromDevice(): Boolean
    fun startAdvertising(): Boolean
    fun stopAdvertising(): Boolean

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
    fun sendRawInput(data: ByteArray): Boolean
}
