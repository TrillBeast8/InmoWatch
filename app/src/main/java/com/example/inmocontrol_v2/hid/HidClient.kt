package com.example.inmocontrol_v2.hid

import android.app.Application

object HidClient {
    private var service: HidService? = null

    fun setService(s: HidService?) { service = s }

    fun instance(): HidService? = service

    fun dpad(direction: Int) {
        service?.dpad(direction)
    }
    fun sendKey(keyCode: Int) {
        service?.sendKey(keyCode)
    }
    fun sendBack() {
        // Android KEYCODE_BACK = 4
        sendKey(4)
    }
    fun setVolume(volume: Int) {
        service?.setVolume(volume)
    }
    fun playPause() {
        service?.playPause()
    }
    fun previousTrack() {
        service?.previousTrack()
    }
    fun nextTrack() {
        service?.nextTrack()
    }
    fun switchOutput(output: Int) {
        service?.switchOutput(output)
    }
    fun moveMouse(x: Int, y: Int) {
        service?.moveMouse(x, y)
    }
    fun mouseClick() {
        service?.mouseClick()
    }
    fun mouseLeftClick() {
        service?.mouseLeftClick()
    }
    fun mouseRightClick() {
        service?.mouseRightClick()
    }
    fun mouseDoubleClick() {
        service?.mouseDoubleClick()
    }
    fun mouseDragMove(x: Int, y: Int) {
        service?.mouseDragMove(x, y)
    }
    fun mouseDragEnd() {
        service?.mouseDragEnd()
    }
    fun mouseScroll(x: Int, y: Int) {
        service?.mouseScroll(x, y)
    }
    fun connect(device: android.bluetooth.BluetoothDevice) {
        service?.connect(device)
    }
    fun sendReport(report: ByteArray) {
        service?.sendReport(report)
    }
    fun sendKeyboardReport(report: ByteArray) {
        service?.sendKeyboardReport(report)
    }

    // Gesture types for Inmo Air 2
    enum class InmoGesture {
        SINGLE_TAP, DOUBLE_TAP, LONG_PRESS, SWIPE_FORWARD, SWIPE_BACK, SWIPE_UP, SWIPE_DOWN,
        DOUBLE_LONG_PRESS, DOUBLE_SWIPE_FORWARD, DOUBLE_SWIPE_BACK, DOUBLE_SWIPE_UP, DOUBLE_SWIPE_DOWN
    }

    // Gesture to keycode mapping for Inmo Air 2
    val inmoAir2GestureKeycodes = mapOf(
        InmoGesture.SINGLE_TAP to 66, // KEYCODE_ENTER
        InmoGesture.DOUBLE_TAP to 4, // KEYCODE_BACK
        InmoGesture.LONG_PRESS to 289,
        InmoGesture.SWIPE_FORWARD to 22, // KEYCODE_DPAD_RIGHT
        InmoGesture.SWIPE_BACK to 21, // KEYCODE_DPAD_LEFT
        InmoGesture.SWIPE_UP to 19, // KEYCODE_DPAD_UP
        InmoGesture.SWIPE_DOWN to 20, // KEYCODE_DPAD_DOWN
        InmoGesture.DOUBLE_LONG_PRESS to 290
        // Double finger swipes: continuous reporting, use same keycodes as single swipes
    )

    // Device profile state (should be set by ConnectToDeviceScreen)
    var currentDeviceProfile: Any? = null

    fun sendGesture(gesture: InmoGesture) {
        when (currentDeviceProfile) {
            is com.example.inmocontrol_v2.ui.screens.DeviceProfile.InmoAir2 -> {
                val keyCode = inmoAir2GestureKeycodes[gesture]
                if (keyCode != null) {
                    sendKey(keyCode)
                }
            }
            else -> {
                // Fallback: use generic HID logic or ignore
            }
        }
    }
}
