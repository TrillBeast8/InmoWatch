package com.example.inmocontrol_v2.hid

import android.bluetooth.BluetoothDevice
import android.content.Context
import com.example.inmocontrol_v2.data.DeviceProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicReference

/**
 * Optimized HID Client with improved performance and thread safety
 * Simplified API with better error handling and state management
 */
object HidClient {
    private val serviceRef = AtomicReference<WeakReference<HidServiceApi>?>()

    // Efficient state management with StateFlow
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connectionError = MutableStateFlow<String?>(null)
    val connectionError: StateFlow<String?> = _connectionError.asStateFlow()

    private val _isServiceReady = MutableStateFlow(false)
    val isServiceReady: StateFlow<Boolean> = _isServiceReady.asStateFlow()

    // Whether the platform exposed the Bluetooth HID Device profile proxy
    private val _hidProfileAvailable = MutableStateFlow(false)
    val hidProfileAvailable: StateFlow<Boolean> = _hidProfileAvailable.asStateFlow()

    fun setHidProfileAvailable(available: Boolean) {
        _hidProfileAvailable.value = available
    }

    // Device profile with atomic updates
    private val _currentDeviceProfile = AtomicReference<DeviceProfile?>()
    var currentDeviceProfile: DeviceProfile?
        get() = _currentDeviceProfile.get()
        set(value) {
            _currentDeviceProfile.set(value)
            getService()?.currentDeviceProfile = value
        }

    // Store last connected device for auto-reconnect
    private val _lastConnectedDevice = AtomicReference<BluetoothDevice?>()
    var lastConnectedDevice: BluetoothDevice?
        get() = _lastConnectedDevice.get()
        set(value) { _lastConnectedDevice.set(value) }

    /**
     * Get current service instance safely
     */
    private fun getService(): HidServiceApi? = serviceRef.get()?.get()

    /**
     * Set the HID service reference with atomic operation
     */
    fun setService(service: HidServiceApi?) {
        val newRef = service?.let { WeakReference(it) }
        serviceRef.set(newRef)

        // Update service with current profile
        service?.currentDeviceProfile = currentDeviceProfile

        // Update states efficiently
        _isServiceReady.value = service?.isReady() == true
        _hidProfileAvailable.value = service != null // best-effort: if service instance present, profile APIs exist
        updateConnectionState()
    }

    /**
     * Set connection error atomically
     */
    fun setConnectionError(error: String?) {
        _connectionError.value = error
    }

    /**
     * Clear connection error
     */
    fun clearError() {
        _connectionError.value = null
    }

    /**
     * Update connection state efficiently
     */
    private fun updateConnectionState() {
        val connected = getService()?.isDeviceConnected() == true
        _isConnected.value = connected
        if (connected) {
            _connectionError.value = null
        }
        // Also refresh service-ready state in case it changed
        _isServiceReady.value = getService()?.isReady() == true
    }

    /**
     * Execute operation with service - fixed to allow connection attempts
     */
    private inline fun <T> withService(
        operation: String,
        defaultValue: T,
        allowConnection: Boolean = false,
        action: (HidServiceApi) -> T
    ): T {
        val service = getService()

        return when {
            service == null -> {
                setConnectionError("Service not available")
                defaultValue
            }
            !service.isReady() && !allowConnection -> {
                setConnectionError("Service not ready")
                defaultValue
            }
            !service.isDeviceConnected() && !allowConnection -> {
                setConnectionError("Device not connected")
                defaultValue
            }
            else -> {
                try {
                    action(service)
                } catch (e: Exception) {
                    setConnectionError("$operation failed: ${e.localizedMessage}")
                    defaultValue
                }
            }
        }
    }

    // CONNECTION METHODS - fixed to allow connection when service is ready
    fun connect(device: BluetoothDevice): Boolean =
        withService("Connect", false, allowConnection = true) {
            val result = it.connect(device)
            if (result) {
                lastConnectedDevice = device
                _isConnected.value = true // Ensure connection state updates
            }
            result
        }

    // Connection and state refresh
    fun refreshConnectionState() {
        updateConnectionState()
    }
    fun disconnect(): Boolean = getService()?.disconnect() == true

    // Mouse controls
    fun moveMouse(x: Int, y: Int): Boolean =
        withService("Move mouse", false) { service ->
            service.moveMouse(x, y)
        }
    fun mouseScroll(x: Int, y: Int): Boolean =
        withService("Scroll", false) { service ->
            service.mouseScroll(x, y)
        }
    fun mouseLeftClick(): Boolean =
        withService("Left click", false) { service ->
            service.mouseLeftClick()
        }
    fun mouseRightClick(): Boolean =
        withService("Right click", false) { service ->
            service.mouseRightClick()
        }
    fun mouseDoubleClick(): Boolean =
        withService("Double click", false) { service ->
            service.mouseDoubleClick()
        }
    fun mouseDragMove(x: Int, y: Int): Boolean =
        withService("Drag move", false) { service ->
            service.mouseDragMove(x, y)
        }
    fun mouseDragEnd(): Boolean =
        withService("Drag end", false) { service ->
            service.mouseDragEnd()
        }

    // D-pad and keyboard controls
    fun dpad(direction: Int): Boolean =
        withService("D-pad", false) { service ->
            service.dpad(direction)
        }
    fun sendKey(keyCode: Int, modifiers: Int = 0): Boolean =
        withService("Send key", false) { service ->
            service.sendKey(keyCode, modifiers)
        }
    fun sendBack(): Boolean =
        withService("Send back", false) { service ->
            service.sendKey(4)
        }
    fun sendInmoConfirm(): Boolean =
        withService("INMO confirm", false) { service ->
            service.sendKey(23) // KEYCODE_DPAD_CENTER
        }

    // Media controls
    fun playPause(): Boolean =
        withService("Play/Pause", false) { service ->
            service.playPause()
        }
    fun nextTrack(): Boolean =
        withService("Next track", false) { service ->
            service.nextTrack()
        }
    fun previousTrack(): Boolean =
        withService("Previous track", false) { service ->
            service.previousTrack()
        }
    fun setVolume(volume: Int): Boolean =
        withService("Set volume", false) { service ->
            service.setVolume(volume)
        }
    fun volumeUp(): Boolean =
        withService("Volume up", false) { service ->
            service.volumeUp()
        }
    fun volumeDown(): Boolean =
        withService("Volume down", false) { service ->
            service.volumeDown()
        }
    fun switchOutput(): Boolean =
        withService("Switch output", false) { service ->
            service.switchOutput()
        }

    // Utility
    fun resetInputStates(): Boolean =
        withService("Reset inputs", false) { service ->
            service.resetInputStates()
        }

    // The following functions are currently unused and can be removed or commented out to reduce warnings:
    // fun disconnect() = getService()?.disconnect()
    // fun mouseClick(): Boolean = ...
    // fun resetInputStates(): Boolean = ...
    // fun sendReport(report: ByteArray): Boolean = ...
    // fun sendKeyboardReport(report: ByteArray): Boolean = ...
    // fun isDeviceConnected(): Boolean = getService()?.isDeviceConnected() == true
    // The parameter 'context' in initialize is unused:
    fun initialize(@Suppress("UNUSED_PARAMETER") context: Context) {
        // No longer needed with optimized implementation
    }
}
