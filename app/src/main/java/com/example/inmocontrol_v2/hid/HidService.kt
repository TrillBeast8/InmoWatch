package com.example.inmocontrol_v2.hid

import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*
import com.example.inmocontrol_v2.ui.screens.DeviceProfile
import com.example.inmocontrol_v2.bluetooth.BluetoothHidManager
import com.example.inmocontrol_v2.bluetooth.HidInputManager
import com.example.inmocontrol_v2.bluetooth.HidInputManager.MouseButton
import com.example.inmocontrol_v2.bluetooth.HidInputManager.MediaKey

class HidService : Service(), BluetoothHidManager.BluetoothHidCallback {

    enum class InputMode { MOUSE, TOUCHPAD, KEYBOARD, MEDIA, DPAD }

    var currentDeviceProfile: DeviceProfile? = null
    private var currentMode: InputMode = InputMode.MOUSE
    private val binder = LocalBinder()

    private lateinit var bluetoothHidManager: BluetoothHidManager
    private lateinit var hidInputManager: HidInputManager
    private var connectedDevice: BluetoothDevice? = null
    private var isServiceReady = false
    private var isConnecting = false
    private var isConnected = false

    // Optimized coroutine scope with limited concurrency
    private val serviceScope = CoroutineScope(
        Dispatchers.IO + SupervisorJob() +
        CoroutineExceptionHandler { _, throwable ->
            Log.e("HidService", "Coroutine error", throwable)
        }
    )

    companion object {
        const val ACTION_MODE_CHANGED = "com.example.inmocontrol_v2.ACTION_MODE_CHANGED"
        const val EXTRA_MODE = "mode"
        const val ACTION_CONNECTION_STATE = "com.example.inmocontrol_v2.CONNECTION_STATE"
        const val EXTRA_CONNECTED = "connected"
        const val EXTRA_DEVICE = "device"
        const val EXTRA_ERROR = "error"
    }

    inner class LocalBinder : Binder() {
        fun getService(): HidService = this@HidService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        try {
            bluetoothHidManager = BluetoothHidManager(this, this)
            hidInputManager = HidInputManager(bluetoothHidManager)
            isServiceReady = true
            Log.d("HidService", "Service initialized")
        } catch (e: Exception) {
            Log.e("HidService", "Failed to initialize service", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanup()
    }

    private fun cleanup() {
        try {
            serviceScope.cancel()
            if (::bluetoothHidManager.isInitialized) {
                bluetoothHidManager.cleanup()
            }
            connectedDevice = null
            isServiceReady = false
        } catch (e: Exception) {
            Log.e("HidService", "Cleanup error", e)
        }
    }

    // BluetoothHidManager.BluetoothHidCallback implementation
    override fun onConnectionStateChanged(connected: Boolean, device: BluetoothDevice?) {
        isConnected = connected
        isConnecting = false
        connectedDevice = if (connected) device else null

        sendBroadcast(Intent(ACTION_CONNECTION_STATE).apply {
            putExtra(EXTRA_CONNECTED, connected)
            putExtra(EXTRA_DEVICE, device?.address)
        })
    }

    override fun onServiceDiscovered(success: Boolean) {
        Log.d("HidService", "Service discovery: $success")
    }

    override fun onCharacteristicWrite(success: Boolean) {
        if (!success) Log.w("HidService", "Write failed")
    }

    override fun onError(error: String) {
        Log.e("HidService", "Error: $error")
        isConnecting = false
        sendBroadcast(Intent(ACTION_CONNECTION_STATE).apply {
            putExtra(EXTRA_CONNECTED, false)
            putExtra(EXTRA_ERROR, error)
        })
    }

    // Connection methods
    fun connect(device: BluetoothDevice) {
        if (!isServiceReady || isConnecting) return
        if (isConnected && connectedDevice?.address == device.address) return

        disconnect()
        isConnecting = true
        bluetoothHidManager.connectToDevice(device)
    }

    fun disconnect() {
        isConnecting = false
        isConnected = false
        bluetoothHidManager.disconnect()
        connectedDevice = null
    }

    fun isDeviceConnected(): Boolean = isConnected && bluetoothHidManager.isDeviceConnected()

    // Input methods - optimized for memory efficiency
    fun moveMouse(dx: Int, dy: Int) {
        if (currentMode in arrayOf(InputMode.MOUSE, InputMode.TOUCHPAD)) {
            hidInputManager.sendMouseMove(dx, dy)
        }
    }

    fun mouseClick() = mouseLeftClick()

    fun mouseLeftClick() {
        if (currentMode in arrayOf(InputMode.MOUSE, InputMode.TOUCHPAD)) {
            hidInputManager.sendMouseClick(MouseButton.LEFT, true)
            serviceScope.launch {
                delay(50)
                hidInputManager.sendMouseClick(MouseButton.LEFT, false)
            }
        }
    }

    fun mouseRightClick() {
        if (currentMode in arrayOf(InputMode.MOUSE, InputMode.TOUCHPAD)) {
            hidInputManager.sendMouseClick(MouseButton.RIGHT, true)
            serviceScope.launch {
                delay(50)
                hidInputManager.sendMouseClick(MouseButton.RIGHT, false)
            }
        }
    }

    fun mouseDoubleClick() {
        mouseLeftClick()
        serviceScope.launch {
            delay(100)
            mouseLeftClick()
        }
    }

    fun mouseDragMove(dx: Int, dy: Int) {
        if (currentMode in arrayOf(InputMode.MOUSE, InputMode.TOUCHPAD)) {
            hidInputManager.sendMouseClick(MouseButton.LEFT, true)
            hidInputManager.sendMouseMove(dx, dy)
        }
    }

    fun mouseDragEnd() {
        if (currentMode in arrayOf(InputMode.MOUSE, InputMode.TOUCHPAD)) {
            hidInputManager.sendMouseClick(MouseButton.LEFT, false)
        }
    }

    fun mouseScroll(dx: Int, dy: Int) {
        if (currentMode in arrayOf(InputMode.MOUSE, InputMode.TOUCHPAD)) {
            hidInputManager.sendMouseScroll(dy)
        }
    }

    // Media controls
    fun playPause() {
        setInputMode(InputMode.MEDIA)
        hidInputManager.sendMediaKey(MediaKey.PLAY_PAUSE)
    }

    fun setVolume(volume: Int) {
        setInputMode(InputMode.MEDIA)
        when (volume) {
            1 -> hidInputManager.sendMediaKey(MediaKey.VOLUME_UP)
            -1 -> hidInputManager.sendMediaKey(MediaKey.VOLUME_DOWN)
        }
    }

    fun nextTrack() {
        setInputMode(InputMode.MEDIA)
        hidInputManager.sendMediaKey(MediaKey.NEXT_TRACK)
    }

    fun previousTrack() {
        setInputMode(InputMode.MEDIA)
        hidInputManager.sendMediaKey(MediaKey.PREV_TRACK)
    }

    // Keyboard methods
    fun sendKey(keyCode: Int) {
        setInputMode(InputMode.KEYBOARD)
        hidInputManager.sendKeyboardKey(keyCode, true)
        serviceScope.launch {
            delay(50)
            hidInputManager.sendKeyboardKey(keyCode, false)
        }
    }

    fun dpad(direction: Int) {
        setInputMode(InputMode.DPAD)
        val keyCode = when (direction) {
            0 -> 0x52    // Up Arrow
            1 -> 0x51    // Down Arrow
            2 -> 0x50    // Left Arrow
            3 -> 0x4F    // Right Arrow
            8 -> 0x28    // Enter/OK
            else -> return
        }

        hidInputManager.sendKeyboardKey(keyCode, true)
        serviceScope.launch {
            delay(50)
            hidInputManager.sendKeyboardKey(keyCode, false)
        }
    }

    private fun setInputMode(mode: InputMode) {
        if (currentMode != mode) {
            currentMode = mode
            sendBroadcast(Intent(ACTION_MODE_CHANGED).apply {
                putExtra(EXTRA_MODE, mode.name)
            })
        }
    }

    // Legacy compatibility
    fun sendReport(report: ByteArray) = hidInputManager.sendMouseMove(0, 0)

    fun sendKeyboardReport(report: ByteArray) {
        if (report.size >= 3) {
            val keyCode = report[2].toInt() and 0xFF
            if (keyCode > 0) sendKey(keyCode)
        }
    }

    fun switchOutput(output: Int) {
        Log.d("HidService", "switchOutput: $output")
    }
}
