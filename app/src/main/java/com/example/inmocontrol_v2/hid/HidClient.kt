package com.example.inmocontrol_v2.hid

import android.bluetooth.BluetoothDevice
import com.example.inmocontrol_v2.ui.screens.DeviceProfile
import java.lang.ref.WeakReference

object HidClient {
    private var serviceRef: WeakReference<HidService>? = null

    // Device profile state - Fixed type to be DeviceProfile? instead of Any?
    var currentDeviceProfile: DeviceProfile? = null
        set(value) {
            field = value
            serviceRef?.get()?.currentDeviceProfile = value
        }

    fun setService(s: HidService?) {
        serviceRef = s?.let { WeakReference(it) }
        s?.currentDeviceProfile = currentDeviceProfile
    }

    private inline fun withService(action: (HidService) -> Unit) {
        serviceRef?.get()?.let(action)
    }

    fun isConnected(): Boolean = serviceRef?.get()?.isDeviceConnected() == true

    // Input functions - optimized with inline
    fun dpad(direction: Int) = withService { it.dpad(direction) }
    fun sendKey(keyCode: Int) = withService { it.sendKey(keyCode) }
    fun sendBack() = sendKey(4) // KEYCODE_BACK

    // Media functions
    fun setVolume(volume: Int) = withService { it.setVolume(volume) }
    fun playPause() = withService { it.playPause() }
    fun previousTrack() = withService { it.previousTrack() }
    fun nextTrack() = withService { it.nextTrack() }
    fun switchOutput(output: Int) = withService { it.switchOutput(output) }

    // Mouse functions
    fun moveMouse(x: Int, y: Int) = withService { it.moveMouse(x, y) }
    fun mouseClick() = withService { it.mouseClick() }
    fun mouseLeftClick() = withService { it.mouseLeftClick() }
    fun mouseRightClick() = withService { it.mouseRightClick() }
    fun mouseDoubleClick() = withService { it.mouseDoubleClick() }
    fun mouseDragMove(x: Int, y: Int) = withService { it.mouseDragMove(x, y) }
    fun mouseDragEnd() = withService { it.mouseDragEnd() }
    fun mouseScroll(x: Int, y: Int) = withService { it.mouseScroll(x, y) }

    // Connection functions
    fun connect(device: BluetoothDevice) = withService { it.connect(device) }
    fun sendReport(report: ByteArray) = withService { it.sendReport(report) }
    fun sendKeyboardReport(report: ByteArray) = withService { it.sendKeyboardReport(report) }

    // Optimized gesture mapping for Inmo Air 2
    enum class InmoGesture {
        SINGLE_TAP, DOUBLE_TAP, LONG_PRESS,
        SWIPE_FORWARD, SWIPE_BACK, SWIPE_UP, SWIPE_DOWN,
        DOUBLE_LONG_PRESS
    }

    private val gestureKeycodes = intArrayOf(
        66, 4, 289, 22, 21, 19, 20, 290
    )

    fun getKeycodeForGesture(gesture: InmoGesture): Int =
        gestureKeycodes[gesture.ordinal]
}
