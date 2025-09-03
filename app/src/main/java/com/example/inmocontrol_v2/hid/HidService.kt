package com.example.inmocontrol_v2.hid

import android.Manifest
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothProfile
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.util.UUID

class HidService : Service() {
    // Supported input modes
    enum class InputMode {
        MOUSE, TOUCHPAD, KEYBOARD, MEDIA, DPAD
    }

    // Current mode
    private var currentMode: InputMode = InputMode.MOUSE

    private val binder = LocalBinder()
    private var gattServer: BluetoothGattServer? = null
    private var connectedDevice: BluetoothDevice? = null
    private lateinit var reportCharacteristic: BluetoothGattCharacteristic

    // Enhanced connection stability features
    private var lastConnectedDevice: BluetoothDevice? = null
    private var reconnectJob: Job? = null
    private var connectionMonitorJob: Job? = null
    private var heartbeatJob: Job? = null
    private val maxReconnectAttempts = 5 // Increased from 3
    private val reconnectDelayMillis = 1500L // Reduced from 2000L for faster reconnect
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var reconnectGatt: BluetoothGatt? = null
    private var reconnectGattCallback: BluetoothGattCallback? = null

    // Connection stability monitoring (like Wear Mouse)
    private var lastActivityTime = System.currentTimeMillis()
    private var isStableConnection = false
    private val connectionTimeoutMs = 15000L // 15 seconds without activity = unstable
    private val heartbeatIntervalMs = 5000L // Send heartbeat every 5 seconds
    private val stabilityCheckIntervalMs = 3000L // Check connection every 3 seconds

    // Connection quality metrics
    private var packetsSent = 0
    private var packetsAcknowledged = 0
    private var connectionStartTime = 0L

    companion object {
        // HID Service UUID (Bluetooth SIG)
        val HID_SERVICE_UUID: UUID = UUID.fromString("00001812-0000-1000-8000-00805f9b34fb")
        // HID Report Characteristic UUID (Bluetooth SIG)
        val REPORT_CHAR_UUID: UUID = UUID.fromString("00002a4d-0000-1000-8000-00805f9b34fb")
        // Broadcast actions for reconnection status
        const val ACTION_RECONNECT_STATUS = "com.example.inmocontrol_v2.hid.RECONNECT_STATUS"
        const val EXTRA_RECONNECT_STATUS = "status"
        const val EXTRA_RECONNECT_ATTEMPT = "attempt"
        const val EXTRA_RECONNECT_DEVICE = "device"
        // Broadcast action for mode changes
        const val ACTION_MODE_CHANGED = "com.example.inmocontrol_v2.ACTION_MODE_CHANGED"
        const val EXTRA_MODE = "com.example.inmocontrol_v2.EXTRA_MODE"
    }

    inner class LocalBinder : Binder() {
        fun getService(): HidService = this@HidService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            gattServer = manager.openGattServer(this, gattServerCallback)
            setupGattServer()
        } else {
            Log.e("HidService", "BLUETOOTH_CONNECT permission not granted")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            gattServer?.close()
        }
        // Cancel any ongoing jobs
        reconnectJob?.cancel()
        connectionMonitorJob?.cancel()
        heartbeatJob?.cancel()
    }

    private fun setupGattServer() {
        val hidService = BluetoothGattService(HID_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        // HID Report Characteristic
        reportCharacteristic = BluetoothGattCharacteristic(
            REPORT_CHAR_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        hidService.addCharacteristic(reportCharacteristic)

        // HID Report Map Characteristic (required for HID over GATT)
        val reportMapChar = BluetoothGattCharacteristic(
            UUID.fromString("00002a4b-0000-1000-8000-00805f9b34fb"), // HID Report Map UUID
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        // Example report map for mouse and keyboard (update as needed for your use case)
        val reportMap = byteArrayOf(
            // Mouse report descriptor
            0x05.toByte(), 0x01.toByte(), // Usage Page (Generic Desktop)
            0x09.toByte(), 0x02.toByte(), // Usage (Mouse)
            0xA1.toByte(), 0x01.toByte(), // Collection (Application)
            0x09.toByte(), 0x01.toByte(), //   Usage (Pointer)
            0xA1.toByte(), 0x00.toByte(), //   Collection (Physical)
            0x05.toByte(), 0x09.toByte(), //     Usage Page (Buttons)
            0x19.toByte(), 0x01.toByte(), //     Usage Minimum (1)
            0x29.toByte(), 0x03.toByte(), //     Usage Maximum (3)
            0x15.toByte(), 0x00.toByte(), //     Logical Minimum (0)
            0x25.toByte(), 0x01.toByte(), //     Logical Maximum (1)
            0x95.toByte(), 0x03.toByte(), //     Report Count (3)
            0x75.toByte(), 0x01.toByte(), //     Report Size (1)
            0x81.toByte(), 0x02.toByte(), //     Input (Data, Variable, Absolute)
            0x95.toByte(), 0x01.toByte(), //     Report Count (1)
            0x75.toByte(), 0x05.toByte(), //     Report Size (5)
            0x81.toByte(), 0x03.toByte(), //     Input (Constant, Variable, Absolute)
            0x05.toByte(), 0x01.toByte(), //     Usage Page (Generic Desktop)
            0x09.toByte(), 0x30.toByte(), //     Usage (X)
            0x09.toByte(), 0x31.toByte(), //     Usage (Y)
            0x15.toByte(), 0x81.toByte(), //     Logical Minimum (-127)
            0x25.toByte(), 0x7F.toByte(), //     Logical Maximum (127)
            0x75.toByte(), 0x08.toByte(), //     Report Size (8)
            0x95.toByte(), 0x02.toByte(), //     Report Count (2)
            0x81.toByte(), 0x06.toByte(), //     Input (Data, Variable, Relative)
            0xC0.toByte(),       //   End Collection
            0xC0.toByte()        // End Collection
            // Add keyboard descriptor if needed
        )
        @Suppress("DEPRECATION")
        reportMapChar.setValue(reportMap)
        hidService.addCharacteristic(reportMapChar)

        // Protocol Mode Characteristic
        val protocolModeChar = BluetoothGattCharacteristic(
            UUID.fromString("00002a4e-0000-1000-8000-00805f9b34fb"), // Protocol Mode UUID
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
            BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        @Suppress("DEPRECATION")
        protocolModeChar.setValue(byteArrayOf(0x01)) // Report Protocol Mode
        hidService.addCharacteristic(protocolModeChar)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            gattServer?.addService(hidService)
        } else {
            Log.e("HidService", "BLUETOOTH_CONNECT permission not granted for addService")
        }
    }

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectedDevice = device
                    lastConnectedDevice = device
                    connectionStartTime = System.currentTimeMillis()
                    lastActivityTime = System.currentTimeMillis()
                    packetsSent = 0
                    packetsAcknowledged = 0
                    isStableConnection = false

                    Log.d("HidService", "Device connected: $device")

                    // Cancel any ongoing reconnection attempts
                    reconnectJob?.cancel()
                    connectionMonitorJob?.cancel()
                    heartbeatJob?.cancel()

                    // Start enhanced connection monitoring
                    startConnectionMonitoring()
                    startAdaptiveHeartbeat()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d("HidService", "Device disconnected (status: $status)")
                    val wasConnected = connectedDevice != null
                    connectedDevice = null

                    // Stop monitoring jobs
                    connectionMonitorJob?.cancel()
                    heartbeatJob?.cancel()

                    // Enhanced reconnection logic for all devices, not just Inmo Air2
                    if (wasConnected && lastConnectedDevice != null) {
                        startSmartReconnection()
                    }
                }
            }
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            // Update activity timestamp
            lastActivityTime = System.currentTimeMillis()
            packetsAcknowledged++

            if (characteristic?.uuid == REPORT_CHAR_UUID && value != null) {
                // Automatic mode detection
                when {
                    value.size == 4 -> {
                        // Mouse report: [buttons, x, y, wheel]
                        setInputMode(InputMode.MOUSE)
                    }
                    value.size == 8 && (value[2].toInt() in 0x04..0x73) -> {
                        // Keyboard report: [modifier, reserved, key1, ...]
                        setInputMode(InputMode.KEYBOARD)
                    }
                    value.size == 2 && (value[0].toInt() in listOf(0xE9, 0xEA, 0xCD, 0xB5, 0xB6)) -> {
                        // Media report: [usage, 0x00]
                        setInputMode(InputMode.MEDIA)
                    }
                    value.size == 8 && (value[2].toInt() in listOf(0x52, 0x51, 0x50, 0x4F)) -> {
                        // DPad report: [modifier, reserved, arrow key, ...]
                        setInputMode(InputMode.DPAD)
                    }
                    // Add touchpad detection logic if needed
                }
            }

            if (responseNeeded) {
                if (ContextCompat.checkSelfPermission(this@HidService, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
                }
            }
        }

        override fun onCharacteristicReadRequest(
            device: BluetoothDevice?,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic?
        ) {
            // Update activity timestamp
            lastActivityTime = System.currentTimeMillis()

            if (ContextCompat.checkSelfPermission(this@HidService, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                @Suppress("DEPRECATION")
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic?.getValue())
            }
        }
    }
    private fun sendReconnectStatusBroadcast(status: String, attempt: Int, deviceName: String?) {
        val intent = Intent(ACTION_RECONNECT_STATUS)
        intent.putExtra(EXTRA_RECONNECT_STATUS, status)
        intent.putExtra(EXTRA_RECONNECT_ATTEMPT, attempt)
        intent.putExtra(EXTRA_RECONNECT_DEVICE, deviceName)
        sendBroadcast(intent)
    }

    // Use setValue() instead of deprecated .value for BluetoothGattCharacteristic
    private fun setCharacteristicValue(characteristic: BluetoothGattCharacteristic, value: ByteArray) {
        @Suppress("DEPRECATION")
        characteristic.setValue(value)
    }

    // Mouse report: [buttons, x, y, wheel]
    fun sendMouseReport(buttons: Byte, x: Byte, y: Byte, wheel: Byte = 0) {
        if (currentMode != InputMode.MOUSE && currentMode != InputMode.TOUCHPAD) {
            Log.d("HidService", "Ignoring mouse report: current mode is $currentMode")
            return
        }
        val report = byteArrayOf(buttons, x, y, wheel)
        connectedDevice?.let {
            setCharacteristicValue(reportCharacteristic, report)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                @Suppress("DEPRECATION")
                gattServer?.notifyCharacteristicChanged(it, reportCharacteristic, false)
            } else {
                Log.e("HidService", "BLUETOOTH_CONNECT permission not granted for notifyCharacteristicChanged")
            }
        }
    }

    // API for HidClient
    fun moveMouse(dx: Int, dy: Int) {
        if (currentMode == InputMode.MOUSE || currentMode == InputMode.TOUCHPAD) {
            sendMouseReport(0, dx.toByte(), dy.toByte())
        } else {
            Log.d("HidService", "Ignoring moveMouse: current mode is $currentMode")
        }
    }

    fun mouseClick() {
        if (currentMode == InputMode.MOUSE || currentMode == InputMode.TOUCHPAD) {
            sendMouseReport(1, 0, 0)
            sendMouseReport(0, 0, 0)
        } else {
            Log.d("HidService", "Ignoring mouseClick: current mode is $currentMode")
        }
    }

    fun mouseLeftClick() {
        mouseClick()
    }
    fun mouseRightClick() {
        if (currentMode == InputMode.MOUSE || currentMode == InputMode.TOUCHPAD) {
            sendMouseReport(2, 0, 0)
            sendMouseReport(0, 0, 0)
        } else {
            Log.d("HidService", "Ignoring mouseRightClick: current mode is $currentMode")
        }
    }
    fun mouseDoubleClick() {
        mouseClick(); mouseClick()
    }
    fun mouseDragMove(dx: Int, dy: Int) {
        if (currentMode == InputMode.MOUSE || currentMode == InputMode.TOUCHPAD) {
            sendMouseReport(1, dx.toByte(), dy.toByte())
        } else {
            Log.d("HidService", "Ignoring mouseDragMove: current mode is $currentMode")
        }
    }
    fun mouseDragEnd() {
        if (currentMode == InputMode.MOUSE || currentMode == InputMode.TOUCHPAD) {
            sendMouseReport(0, 0, 0)
        } else {
            Log.d("HidService", "Ignoring mouseDragEnd: current mode is $currentMode")
        }
    }
    fun mouseScroll(dx: Int, dy: Int) {
        if (currentMode == InputMode.MOUSE || currentMode == InputMode.TOUCHPAD) {
            sendMouseReport(0, dx.toByte(), dy.toByte(), dy.toByte())
        } else {
            Log.d("HidService", "Ignoring mouseScroll: current mode is $currentMode")
        }
    }

    fun sendReport(report: ByteArray) {
        connectedDevice?.let {
            setCharacteristicValue(reportCharacteristic, report)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                @Suppress("DEPRECATION")
                gattServer?.notifyCharacteristicChanged(it, reportCharacteristic, false)
            } else {
                Log.e("HidService", "BLUETOOTH_CONNECT permission not granted for notifyCharacteristicChanged")
            }
        }
    }

    fun sendKeyboardReport(report: ByteArray) {
        if (currentMode == InputMode.KEYBOARD || currentMode == InputMode.DPAD) {
            sendReport(report)
        } else {
            Log.d("HidService", "Ignoring keyboard report: current mode is $currentMode")
        }
    }

    // D-pad navigation (arrow keys)
    fun dpad(direction: Int) {
        if (currentMode == InputMode.DPAD) {
            // direction: 0=up, 1=down, 2=left, 3=right
            val keyCode = when (direction) {
                0 -> 0x52 // Up Arrow
                1 -> 0x51 // Down Arrow
                2 -> 0x50 // Left Arrow
                3 -> 0x4F // Right Arrow
                else -> 0x00
            }
            val report = byteArrayOf(0x00, 0x00, keyCode.toByte(), 0x00, 0x00, 0x00, 0x00, 0x00)
            sendKeyboardReport(report)
            // Release key
            sendKeyboardReport(byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00))
        } else {
            Log.d("HidService", "Ignoring dpad: current mode is $currentMode")
        }
    }

    // Volume control
    fun setVolume(volume: Int) {
        if (currentMode == InputMode.MEDIA) {
            if (connectedDevice == null || gattServer == null) {
                Log.e("HidService", "No connected device or GATT server for setVolume")
                return
            }
            // volume: 1=up, -1=down
            val usage = when (volume) {
                1 -> 0xE9 // Volume Up
                -1 -> 0xEA // Volume Down
                else -> 0x00
            }
            if (usage != 0x00) {
                val report = byteArrayOf(usage.toByte(), 0x00)
                sendReport(report)
                // Release key
                sendReport(byteArrayOf(0x00, 0x00))
            }
        } else {
            Log.d("HidService", "Ignoring setVolume: current mode is $currentMode")
        }
    }

    // Media controls
    fun playPause() {
        if (currentMode == InputMode.MEDIA) {
            if (connectedDevice == null || gattServer == null) {
                Log.e("HidService", "No connected device or GATT server for playPause")
                return
            }
            val report = byteArrayOf(0xCD.toByte(), 0x00) // Play/Pause
            sendReport(report)
            sendReport(byteArrayOf(0x00, 0x00))
        } else {
            Log.d("HidService", "Ignoring playPause: current mode is $currentMode")
        }
    }
    fun previousTrack() {
        if (currentMode == InputMode.MEDIA) {
            if (connectedDevice == null || gattServer == null) {
                Log.e("HidService", "No connected device or GATT server for previousTrack")
                return
            }
            val report = byteArrayOf(0xB6.toByte(), 0x00) // Previous Track
            sendReport(report)
            sendReport(byteArrayOf(0x00, 0x00))
        } else {
            Log.d("HidService", "Ignoring previousTrack: current mode is $currentMode")
        }
    }
    fun nextTrack() {
        if (currentMode == InputMode.MEDIA) {
            if (connectedDevice == null || gattServer == null) {
                Log.e("HidService", "No connected device or GATT server for nextTrack")
                return
            }
            val report = byteArrayOf(0xB5.toByte(), 0x00) // Next Track
            sendReport(report)
            sendReport(byteArrayOf(0x00, 0x00))
        } else {
            Log.d("HidService", "Ignoring nextTrack: current mode is $currentMode")
        }
    }
    fun switchOutput(output: Int) {
        if (currentMode == InputMode.MEDIA) {
            if (connectedDevice == null || gattServer == null) {
                Log.e("HidService", "No connected device or GATT server for switchOutput")
                return
            }
            // Implement output switching logic here if supported
            Log.d("HidService", "Switching output to $output (stub)")
        } else {
            Log.d("HidService", "Ignoring switchOutput: current mode is $currentMode")
        }
    }

    // Send a single key press (keyboard HID report)
    fun sendKey(keyCode: Int) {
        if (currentMode == InputMode.KEYBOARD) {
            // HID keyboard report: [modifier, reserved, key1, key2, key3, key4, key5, key6]
            // Only key1 is used here
            val report = byteArrayOf(0, 0, keyCode.toByte(), 0, 0, 0, 0, 0)
            sendKeyboardReport(report)
            // Release key
            val releaseReport = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
            sendKeyboardReport(releaseReport)
        } else {
            Log.d("HidService", "Ignoring sendKey: current mode is $currentMode")
        }
    }

    // Method to change mode
    fun setInputMode(mode: InputMode) {
        if (currentMode != mode) {
            currentMode = mode
            val intent = Intent(ACTION_MODE_CHANGED)
            intent.putExtra(EXTRA_MODE, mode.name)
            sendBroadcast(intent)
            Log.d("HidService", "Input mode changed to $mode")
        }
    }

    // Public method to connect to a Bluetooth device
    fun connect(device: BluetoothDevice) {
        connectedDevice = device
        Log.d("HidService", "Connected to device: $device")
        // Add any additional connection logic here if needed
    }

    private fun startConnectionMonitoring() {
        connectionMonitorJob = serviceScope.launch {
            while (isActive) {
                delay(stabilityCheckIntervalMs)

                if (connectedDevice == null) {
                    // If disconnected, attempt to reconnect
                    startSmartReconnection() // Or appropriate status
                    continue // Skip the rest of the loop until reconnected
                }

                // Check connection stability only if connected
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastActivityTime > connectionTimeoutMs) {
                    // Connection is unstable, attempt to reconnect
                    Log.w("HidService", "Connection unstable, attempting to reconnect")
                    if (ContextCompat.checkSelfPermission(this@HidService, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        sendReconnectStatusBroadcast("unstable", 0, connectedDevice?.name)
                    }
                    startSmartReconnection() // Indicate failure
                } else {
                    // Connection is stable
                    isStableConnection = true
                }
            }
        }
    }

    private fun startAdaptiveHeartbeat() {
        heartbeatJob = serviceScope.launch {
            while (connectedDevice != null) {
                delay(heartbeatIntervalMs)
                // Send a heartbeat signal (can be a dummy report or actual data)
                val heartbeatReport = byteArrayOf(0x00, 0x00, 0x00, 0x00)
                sendReport(heartbeatReport)
                Log.d("HidService", "Adaptive heartbeat sent")
            }
        }
    }

    private fun startSmartReconnection() {
        if (reconnectJob?.isActive == true) {
            Log.d("HidService", "Reconnection already in progress")
            return
        }
        reconnectJob = serviceScope.launch {
            var attempt = 0
            var success = false
            while (attempt < maxReconnectAttempts && !success && isActive) {
                Log.d("HidService", "Smart reconnect attempt "+(attempt+1))
                // Broadcast attempt
                sendReconnectStatusBroadcast("attempt", attempt+1, lastConnectedDevice?.name)
                try {
                    if (ContextCompat.checkSelfPermission(this@HidService, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        reconnectGatt?.close()
                        reconnectGattCallback = object : BluetoothGattCallback() {
                            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                                if (newState == BluetoothProfile.STATE_CONNECTED) {
                                    connectedDevice = gatt.device
                                    success = true
                                    Log.d("HidService", "Reconnected successfully (smart)")
                                    if (ContextCompat.checkSelfPermission(this@HidService, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                                        sendReconnectStatusBroadcast("success", attempt+1, gatt.device.name)
                                    } else {
                                        Log.e("HidService", "BLUETOOTH_CONNECT permission not granted for sendReconnectStatusBroadcast")
                                    }
                                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                                    Log.d("HidService", "Reconnect client disconnected (smart)")
                                }
                            }
                        }
                        reconnectGatt = lastConnectedDevice?.connectGatt(this@HidService, false, reconnectGattCallback!!)
                        delay(reconnectDelayMillis)
                    }
                } catch (e: Exception) {
                    Log.e("HidService", "Reconnect error (smart): ${e.message}")
                }
                attempt++
            }
            if (!success) {
                Log.e("HidService", "Failed to reconnect after $maxReconnectAttempts attempts (smart)")
                sendReconnectStatusBroadcast("failure", attempt, lastConnectedDevice?.name)
            }
        }
    }
}
