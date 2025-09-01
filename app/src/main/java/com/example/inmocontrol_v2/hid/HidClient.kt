package com.example.inmocontrol_v2.hid

import android.app.Application

object HidClient {
    private var service: HidService? = null

    fun init(app: Application) {
        // Directly instantiate HidService for now
        service = HidService()
    }

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
}
