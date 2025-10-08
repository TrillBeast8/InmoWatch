package com.example.inmocontrol_v2.hid

import android.bluetooth.BluetoothDevice
import com.example.inmocontrol_v2.data.DeviceProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicReference

/**
 * HID Client singleton that provides access to HID service functionality
 * Updated for 2025 compatibility with improved error handling
 */
object HidClient {
    // Service reference with improved memory management
    private val serviceRef = AtomicReference<WeakReference<HidServiceApi>?>()

    // State flows for UI with enhanced reactivity
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connectionError = MutableStateFlow<String?>(null)
    val connectionError: StateFlow<String?> = _connectionError.asStateFlow()

    private val _isServiceReady = MutableStateFlow(false)
    val isServiceReady: StateFlow<Boolean> = _isServiceReady.asStateFlow()

    private val _hidProfileAvailable = MutableStateFlow(false)
    val hidProfileAvailable: StateFlow<Boolean> = _hidProfileAvailable.asStateFlow()

    // Current device profile with null safety
    var currentDeviceProfile: DeviceProfile?
        get() = getService()?.currentDeviceProfile
        set(value) {
            getService()?.currentDeviceProfile = value
        }

    // Service management with improved error handling
    fun setService(service: HidServiceApi?) {
        val newRef = service?.let { WeakReference(it) }
        serviceRef.set(newRef)

        // Update state flows atomically
        val isReady = service?.isReady() == true
        val isConnected = service?.isDeviceConnected() == true

        _isServiceReady.value = isReady
        _hidProfileAvailable.value = service != null
        _isConnected.value = isConnected

        if (service == null) {
            _connectionError.value = "Service not available"
        } else {
            _connectionError.value = null
        }
    }

    fun setConnectionError(error: String?) {
        _connectionError.value = error
    }

    fun clearError() {
        _connectionError.value = null
    }

    fun setConnectionState(connected: Boolean) {
        _isConnected.value = connected
    }

    fun setHidProfileAvailable(available: Boolean) {
        _hidProfileAvailable.value = available
    }

    // Private helper to get service with null safety
    private fun getService(): HidServiceApi? = serviceRef.get()?.get()

    // Connection methods with error handling
    fun connectToDevice(device: BluetoothDevice): Boolean {
        return try {
            getService()?.connectToDevice(device) ?: false
        } catch (e: Exception) {
            setConnectionError("Connection failed: ${e.message}")
            false
        }
    }

    fun disconnectFromDevice(): Boolean {
        return try {
            getService()?.disconnectFromDevice() ?: false
        } catch (e: Exception) {
            setConnectionError("Disconnection failed: ${e.message}")
            false
        }
    }

    fun startAdvertising(): Boolean {
        return try {
            getService()?.startAdvertising() ?: false
        } catch (e: Exception) {
            setConnectionError("Failed to start advertising: ${e.message}")
            false
        }
    }

    fun stopAdvertising(): Boolean {
        return try {
            getService()?.stopAdvertising() ?: false
        } catch (e: Exception) {
            setConnectionError("Failed to stop advertising: ${e.message}")
            false
        }
    }

    // Input methods with improved error handling
    fun sendMouseMovement(deltaX: Float, deltaY: Float): Boolean {
        return try {
            getService()?.sendMouseMovement(deltaX, deltaY) ?: false
        } catch (e: Exception) {
            false
        }
    }

    // Media control methods
    fun sendPlayPause(): Boolean = getService()?.sendPlayPause() ?: false
    fun sendNextTrack(): Boolean = getService()?.sendNextTrack() ?: false
    fun sendPreviousTrack(): Boolean = getService()?.sendPreviousTrack() ?: false
    fun sendVolumeUp(): Boolean = getService()?.sendVolumeUp() ?: false
    fun sendVolumeDown(): Boolean = getService()?.sendVolumeDown() ?: false
    fun sendMute(): Boolean = getService()?.sendMute() ?: false

    // D-pad methods
    fun sendDpadUp(): Boolean = getService()?.sendDpadUp() ?: false
    fun sendDpadDown(): Boolean = getService()?.sendDpadDown() ?: false
    fun sendDpadLeft(): Boolean = getService()?.sendDpadLeft() ?: false
    fun sendDpadRight(): Boolean = getService()?.sendDpadRight() ?: false
    fun sendDpadCenter(): Boolean = getService()?.sendDpadCenter() ?: false

    // Raw input for compatibility with all HID protocols
    fun sendRawInput(data: ByteArray): Boolean = getService()?.sendRawInput(data) ?: false

    // Additional helper methods used by UI screens
    fun sendInmoConfirm(): Boolean = sendDpadCenter()

    fun dpad(direction: Int): Boolean {
        return when (direction) {
            0 -> sendDpadUp()
            1 -> sendDpadDown()
            2 -> sendDpadLeft()
            3 -> sendDpadRight()
            4 -> sendDpadCenter()
            5 -> sendDpadDown() && sendDpadLeft() // DOWN-LEFT
            6 -> sendDpadDown() && sendDpadRight() // DOWN-RIGHT
            7 -> sendDpadUp() && sendDpadLeft() // UP-LEFT
            8 -> sendDpadUp() && sendDpadRight() // UP-RIGHT
            else -> false
        }
    }

    // Mouse action methods that are missing
    fun mouseLeftClick(): Boolean = sendLeftClick()
    fun mouseRightClick(): Boolean = sendRightClick()
    fun mouseScroll(deltaX: Int, deltaY: Int): Boolean = sendScroll(deltaX.toFloat(), deltaY.toFloat())

    // Additional convenience methods
    fun sendLeftClick(): Boolean = getService()?.sendLeftClick() ?: false
    fun sendRightClick(): Boolean = getService()?.sendRightClick() ?: false
    fun sendMiddleClick(): Boolean = getService()?.sendMiddleClick() ?: false
    fun sendDoubleClick(): Boolean = getService()?.sendDoubleClick() ?: false

    // Media control convenience methods
    fun playPause(): Boolean = sendPlayPause()
    fun nextTrack(): Boolean = sendNextTrack()
    fun previousTrack(): Boolean = sendPreviousTrack()
    fun volumeUp(): Boolean = sendVolumeUp()
    fun volumeDown(): Boolean = sendVolumeDown()
    fun mute(): Boolean = sendMute()

    // Text and key input methods
    fun sendScroll(deltaX: Float, deltaY: Float): Boolean {
        return try {
            getService()?.sendScroll(deltaX, deltaY) ?: false
        } catch (e: Exception) {
            false
        }
    }

    fun sendKey(keyCode: Int, modifiers: Int = 0): Boolean {
        return try {
            getService()?.sendKey(keyCode, modifiers) ?: false
        } catch (e: Exception) {
            false
        }
    }

    fun sendText(text: String): Boolean {
        return try {
            getService()?.sendText(text) ?: false
        } catch (e: Exception) {
            false
        }
    }
}
