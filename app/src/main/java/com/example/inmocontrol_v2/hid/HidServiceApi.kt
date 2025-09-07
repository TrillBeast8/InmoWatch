package com.example.inmocontrol_v2.hid

import android.bluetooth.BluetoothDevice
import com.example.inmocontrol_v2.data.DeviceProfile

interface HidServiceApi {
    var currentDeviceProfile: DeviceProfile?

    fun connect(device: BluetoothDevice): Boolean
    fun disconnect(): Boolean
    fun isDeviceConnected(): Boolean
    fun isReady(): Boolean
    fun getConnectedDevice(): BluetoothDevice?
    fun getConnectedDevices(): List<BluetoothDevice>

    // Mouse
    fun moveMouse(x: Int, y: Int): Boolean
    fun mouseScroll(x: Int, y: Int): Boolean
    fun mouseLeftClick(): Boolean
    fun mouseRightClick(): Boolean
    fun mouseDoubleClick(): Boolean
    fun mouseDragMove(x: Int, y: Int): Boolean
    fun mouseDragEnd(): Boolean

    // D-pad / keyboard
    fun dpad(direction: Int): Boolean
    fun sendKey(keyCode: Int, modifiers: Int = 0): Boolean

    // Media
    fun playPause(): Boolean
    fun nextTrack(): Boolean
    fun previousTrack(): Boolean
    fun setVolume(volume: Int): Boolean
    fun volumeUp(): Boolean
    fun volumeDown(): Boolean
    fun switchOutput(): Boolean

    // Utility
    fun resetInputStates(): Boolean
}
