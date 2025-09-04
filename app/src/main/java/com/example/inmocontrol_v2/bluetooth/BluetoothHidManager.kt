package com.example.inmocontrol_v2.bluetooth

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.util.UUID

/**
 * Proper Bluetooth HID Manager implementation following Wear Mouse patterns
 * This fixes the timeout issues by using pure GATT client approach
 */
class BluetoothHidManager(
    private val context: Context,
    private val callback: BluetoothHidCallback
) {
    companion object {
        private const val TAG = "BluetoothHidManager"

        // HID over GATT UUIDs - Following Bluetooth SIG specification
        val HID_SERVICE_UUID = UUID.fromString("00001812-0000-1000-8000-00805f9b34fb")
        val HID_REPORT_UUID = UUID.fromString("00002a4d-0000-1000-8000-00805f9b34fb")
        val HID_REPORT_MAP_UUID = UUID.fromString("00002a4b-0000-1000-8000-00805f9b34fb")
        val HID_INFORMATION_UUID = UUID.fromString("00002a4a-0000-1000-8000-00805f9b34fb")
        val HID_CONTROL_POINT_UUID = UUID.fromString("00002a4c-0000-1000-8000-00805f9b34fb")
        val CLIENT_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        // Connection parameters optimized for Inmo Air 2
        private const val CONNECTION_TIMEOUT_MS = 30000L // 30 seconds
        private const val SERVICE_DISCOVERY_TIMEOUT_MS = 10000L
        private const val CHARACTERISTIC_WRITE_TIMEOUT_MS = 5000L

        // Retry parameters
        private const val MAX_CONNECTION_ATTEMPTS = 5
        private const val RETRY_DELAY_BASE_MS = 2000L
    }

    private var bluetoothGatt: BluetoothGatt? = null
    private var hidReportCharacteristic: BluetoothGattCharacteristic? = null
    private var isConnecting = false
    private var isConnected = false
    private var connectionAttempt = 0

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var connectionTimeoutJob: Job? = null
    private var serviceDiscoveryJob: Job? = null

    interface BluetoothHidCallback {
        fun onConnectionStateChanged(connected: Boolean, device: BluetoothDevice?)
        fun onServiceDiscovered(success: Boolean)
        fun onCharacteristicWrite(success: Boolean)
        fun onError(error: String)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            Log.d(TAG, "Connection state changed: status=$status, newState=$newState")

            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "Successfully connected to GATT server")
                    isConnected = true
                    isConnecting = false
                    connectionTimeoutJob?.cancel()

                    // Start service discovery with timeout
                    startServiceDiscovery(gatt)
                    callback.onConnectionStateChanged(true, gatt?.device)
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.i(TAG, "Disconnected from GATT server")
                    isConnected = false
                    isConnecting = false
                    connectionTimeoutJob?.cancel()
                    serviceDiscoveryJob?.cancel()

                    // Clean up
                    bluetoothGatt?.close()
                    bluetoothGatt = null
                    hidReportCharacteristic = null

                    callback.onConnectionStateChanged(false, gatt?.device)

                    // Auto-retry if unexpected disconnection
                    if (status != BluetoothGatt.GATT_SUCCESS && connectionAttempt < MAX_CONNECTION_ATTEMPTS) {
                        Log.w(TAG, "Unexpected disconnection, will retry...")
                        scheduleReconnection(gatt?.device)
                    }
                }

                BluetoothProfile.STATE_CONNECTING -> {
                    Log.d(TAG, "Connecting to GATT server...")
                    isConnecting = true
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            serviceDiscoveryJob?.cancel()

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Services discovered successfully")
                setupHidService(gatt)
                callback.onServiceDiscovered(true)
            } else {
                Log.e(TAG, "Service discovery failed with status: $status")
                callback.onServiceDiscovered(false)
                callback.onError("Service discovery failed (status: $status)")
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Characteristic write successful")
                callback.onCharacteristicWrite(true)
            } else {
                Log.w(TAG, "Characteristic write failed with status: $status")
                callback.onCharacteristicWrite(false)
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Descriptor write successful")
            } else {
                Log.w(TAG, "Descriptor write failed with status: $status")
            }
        }
    }

    /**
     * Connect to HID device using improved GATT client approach
     */
    fun connectToDevice(device: BluetoothDevice) {
        if (isConnecting || isConnected) {
            Log.w(TAG, "Already connecting or connected to ${device.address}")
            return
        }

        Log.i(TAG, "Connecting to HID device: ${device.address}")
        connectionAttempt++

        if (!hasBluetoothPermissions()) {
            callback.onError("Missing Bluetooth permissions")
            return
        }

        isConnecting = true
        startConnectionTimeout()

        try {
            // Improved connection approach: use autoConnect=false for faster initial connection
            // For Wear devices, direct connection is more reliable than autoConnect
            bluetoothGatt = device.connectGatt(
                context,
                false, // autoConnect=false for faster connection
                gattCallback,
                BluetoothDevice.TRANSPORT_LE
            )

            if (bluetoothGatt == null) {
                Log.e(TAG, "Failed to create GATT connection")
                isConnecting = false
                callback.onError("Failed to create GATT connection")
            } else {
                Log.d(TAG, "GATT connection initiated successfully")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception during connection: ${e.message}")
            isConnecting = false
            callback.onError("Permission denied: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Exception during connection: ${e.message}")
            isConnecting = false
            callback.onError("Connection exception: ${e.message}")
        }
    }

    private fun startConnectionTimeout() {
        connectionTimeoutJob?.cancel()
        connectionTimeoutJob = scope.launch {
            delay(CONNECTION_TIMEOUT_MS)

            if (isConnecting) {
                Log.w(TAG, "Connection timeout after ${CONNECTION_TIMEOUT_MS}ms")
                isConnecting = false

                bluetoothGatt?.let { gatt ->
                    if (hasBluetoothPermissions()) {
                        gatt.disconnect()
                        gatt.close()
                    }
                }
                bluetoothGatt = null

                callback.onError("Connection timeout - device may be out of range")
            }
        }
    }

    private fun startServiceDiscovery(gatt: BluetoothGatt?) {
        serviceDiscoveryJob?.cancel()
        serviceDiscoveryJob = scope.launch {
            delay(500) // Brief delay for stability

            if (hasBluetoothPermissions()) {
                val discoveryStarted = gatt?.discoverServices() ?: false

                if (!discoveryStarted) {
                    Log.e(TAG, "Failed to start service discovery")
                    callback.onError("Failed to start service discovery")
                    return@launch
                }

                // Timeout for service discovery
                delay(SERVICE_DISCOVERY_TIMEOUT_MS)
                if (hidReportCharacteristic == null) {
                    Log.w(TAG, "Service discovery timeout")
                    callback.onServiceDiscovered(false)
                    callback.onError("Service discovery timeout")
                }
            } else {
                callback.onError("Missing Bluetooth permissions for service discovery")
            }
        }
    }

    private fun setupHidService(gatt: BluetoothGatt?) {
        val hidService = gatt?.getService(HID_SERVICE_UUID)
        if (hidService == null) {
            Log.e(TAG, "HID service not found on device")
            callback.onError("Device does not support HID over GATT")
            return
        }

        Log.d(TAG, "HID service found, setting up characteristics...")

        // Find the HID report characteristic
        hidReportCharacteristic = hidService.getCharacteristic(HID_REPORT_UUID)
        if (hidReportCharacteristic == null) {
            Log.e(TAG, "HID report characteristic not found")
            callback.onError("HID report characteristic not available")
            return
        }

        // Enable notifications for the report characteristic
        enableNotifications(gatt, hidReportCharacteristic!!)
    }

    private fun enableNotifications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        if (!hasBluetoothPermissions()) return

        Log.d(TAG, "Enabling notifications for HID report characteristic")

        // Enable local notifications
        val success = gatt.setCharacteristicNotification(characteristic, true)
        if (!success) {
            Log.e(TAG, "Failed to enable local notifications")
            callback.onError("Failed to setup notifications")
            return
        }

        // Write to client configuration descriptor to enable remote notifications
        val descriptor = characteristic.getDescriptor(CLIENT_CONFIG_UUID)
        if (descriptor != null) {
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(descriptor)
        } else {
            Log.w(TAG, "Client configuration descriptor not found")
        }
    }

    private fun scheduleReconnection(device: BluetoothDevice?) {
        if (device == null || connectionAttempt >= MAX_CONNECTION_ATTEMPTS) return

        scope.launch {
            val delay = RETRY_DELAY_BASE_MS * connectionAttempt
            Log.d(TAG, "Scheduling reconnection attempt ${connectionAttempt + 1} in ${delay}ms")
            delay(delay)

            if (!isConnected) {
                connectToDevice(device)
            }
        }
    }

    /**
     * Send HID report to connected device (Wear Mouse pattern)
     */
    fun sendHidReport(reportId: Byte, data: ByteArray): Boolean {
        val characteristic = hidReportCharacteristic
        val gatt = bluetoothGatt

        if (!isConnected || gatt == null || characteristic == null) {
            Log.w(TAG, "Cannot send report: not connected or missing characteristic")
            return false
        }

        if (!hasBluetoothPermissions()) {
            Log.w(TAG, "Cannot send report: missing permissions")
            return false
        }

        try {
            // Prepare report with report ID
            val report = byteArrayOf(reportId) + data
            characteristic.value = report

            val success = gatt.writeCharacteristic(characteristic)
            if (!success) {
                Log.w(TAG, "Failed to write characteristic")
            }
            return success

        } catch (e: Exception) {
            Log.e(TAG, "Exception writing characteristic: ${e.message}")
            return false
        }
    }

    fun disconnect() {
        Log.i(TAG, "Disconnecting from device")

        isConnecting = false
        isConnected = false
        connectionTimeoutJob?.cancel()
        serviceDiscoveryJob?.cancel()

        bluetoothGatt?.let { gatt ->
            if (hasBluetoothPermissions()) {
                gatt.disconnect()
                gatt.close()
            }
        }

        bluetoothGatt = null
        hidReportCharacteristic = null
        connectionAttempt = 0
    }

    fun isDeviceConnected(): Boolean = isConnected && bluetoothGatt != null

    private fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun cleanup() {
        disconnect()
        scope.cancel()
    }
}
