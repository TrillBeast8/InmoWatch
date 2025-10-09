package com.example.inmocontrol_v2.hid

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.bluetooth.BluetoothDevice

/**
 * UI-facing singleton - Exposes StateFlow signals and HID actions to Compose screens.
 *
 * Critical Rules from Copilot Instructions:
 * - ALWAYS use HidClient methods in UI code, never direct service calls
 * - Keep all methods synchronous (Boolean return) for instant Compose reactivity
 * - Collect StateFlows with .collectAsState() in Composables
 */
object HidClient {

    private const val TAG = "HidClient"

    // Service binding
    private var hidService: HidService? = null
    private var isBound = false

    // State flows for reactive UI
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connectionError = MutableStateFlow<String?>(null)
    val connectionError: StateFlow<String?> = _connectionError.asStateFlow()

    private val _isServiceReady = MutableStateFlow(false)
    val isServiceReady: StateFlow<Boolean> = _isServiceReady.asStateFlow()

    private val _hidProfileAvailable = MutableStateFlow(false)
    val hidProfileAvailable: StateFlow<Boolean> = _hidProfileAvailable.asStateFlow()

    // Service connection callbacks
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as? HidService.LocalBinder
            hidService = localBinder?.getService()
            isBound = true
            _isServiceReady.value = hidService?.isReady() ?: false
            Log.d(TAG, "HidService bound successfully")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            hidService = null
            isBound = false
            _isServiceReady.value = false
            _isConnected.value = false
            Log.d(TAG, "HidService unbound")
        }
    }

    /**
     * Bind to HidService. Call from MainActivity.onCreate()
     */
    fun bindService(context: Context) {
        val intent = Intent(context, HidService::class.java)
        context.startForegroundService(intent)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        Log.d(TAG, "Binding to HidService")
    }

    /**
     * Unbind from HidService. Call from MainActivity.onDestroy()
     */
    fun unbindService(context: Context) {
        if (isBound) {
            context.unbindService(serviceConnection)
            isBound = false
            hidService = null
            Log.d(TAG, "Unbound from HidService")
        }
    }

    // Internal state setters (called by HidService)
    internal fun setConnectionState(connected: Boolean) {
        _isConnected.value = connected
    }

    internal fun setConnectionError(error: String?) {
        _connectionError.value = error
    }

    internal fun setServiceReady(ready: Boolean) {
        _isServiceReady.value = ready
    }

    internal fun setHidProfileAvailable(available: Boolean) {
        _hidProfileAvailable.value = available
    }

    fun clearError() {
        _connectionError.value = null
    }

    // Helper to get service safely
    private fun getService(): HidServiceApi? {
        return hidService?.takeIf { isBound && it.isReady() }
    }

    // Connection management
    fun connectToDevice(device: BluetoothDevice): Boolean {
        return getService()?.connectToDevice(device) ?: false
    }

    // ==================== MOUSE OPERATIONS ====================

    fun sendMouseMovement(deltaX: Float, deltaY: Float): Boolean {
        return getService()?.sendMouseMovement(deltaX, deltaY) ?: false
    }

    fun sendLeftClick(): Boolean {
        return getService()?.sendLeftClick() ?: false
    }

    fun sendRightClick(): Boolean {
        return getService()?.sendRightClick() ?: false
    }

    fun sendMiddleClick(): Boolean {
        return getService()?.sendMiddleClick() ?: false
    }

    fun sendDoubleClick(): Boolean {
        return getService()?.sendDoubleClick() ?: false
    }

    fun sendMouseScroll(deltaX: Float, deltaY: Float): Boolean {
        return getService()?.sendMouseScroll(deltaX, deltaY) ?: false
    }

    // ==================== KEYBOARD OPERATIONS ====================

    fun sendKey(keyCode: Int, modifiers: Int = 0): Boolean {
        return getService()?.sendKey(keyCode, modifiers) ?: false
    }

    fun sendText(text: String): Boolean {
        return getService()?.sendText(text) ?: false
    }

    // ==================== MEDIA CONTROLS ====================

    fun playPause(): Boolean {
        return getService()?.playPause() ?: false
    }

    fun nextTrack(): Boolean {
        return getService()?.nextTrack() ?: false
    }

    fun previousTrack(): Boolean {
        return getService()?.previousTrack() ?: false
    }

    fun volumeUp(): Boolean {
        return getService()?.volumeUp() ?: false
    }

    fun volumeDown(): Boolean {
        return getService()?.volumeDown() ?: false
    }

    fun mute(): Boolean {
        return getService()?.mute() ?: false
    }

    // ==================== D-PAD NAVIGATION ====================

    fun dpad(direction: Int): Boolean {
        return getService()?.dpad(direction) ?: false
    }

    fun sendDpadUp(): Boolean {
        return getService()?.sendDpadUp() ?: false
    }

    fun sendDpadDown(): Boolean {
        return getService()?.sendDpadDown() ?: false
    }

    fun sendDpadLeft(): Boolean {
        return getService()?.sendDpadLeft() ?: false
    }

    fun sendDpadRight(): Boolean {
        return getService()?.sendDpadRight() ?: false
    }

    fun sendDpadCenter(): Boolean {
        return getService()?.sendDpadCenter() ?: false
    }

    // ==================== RAW HID ====================

    fun sendRawInput(data: ByteArray): Boolean {
        return getService()?.sendRawInput(data) ?: false
    }
}
