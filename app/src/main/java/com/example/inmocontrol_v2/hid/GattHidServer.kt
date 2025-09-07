package com.example.inmocontrol_v2.hid

import android.bluetooth.*
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.inmocontrol_v2.bluetooth.HidConstants
import java.util.*

class GattHidServer(private val context: Context) {
    companion object {
        private const val TAG = "GattHidServer"
        private val HID_SERVICE_UUID: UUID = UUID.fromString("00001812-0000-1000-8000-00805f9b34fb")
        private val REPORT_CHAR_UUID: UUID = UUID.fromString("00002a4d-0000-1000-8000-00805f9b34fb")
        private val REPORT_MAP_UUID: UUID = UUID.fromString("00002a4b-0000-1000-8000-00805f9b34fb")
    }

    private val bluetoothManager: BluetoothManager? = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private var gattServer: BluetoothGattServer? = null
    private var connectedDevice: BluetoothDevice? = null
    private var reportCharacteristic: BluetoothGattCharacteristic? = null

    fun start() {
        if (gattServer != null) return
        try {
            gattServer = bluetoothManager?.openGattServer(context, gattServerCallback)
            if (gattServer == null) {
                Log.w(TAG, "Failed to open GATT server")
                return
            }
            setupService()
            Log.d(TAG, "GATT HID server started")
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing BLUETOOTH_CONNECT permission to start GATT server", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start GATT server", e)
        }
    }

    fun stop() {
        try {
            // On Android S+ this requires BLUETOOTH_CONNECT permission. Check before calling to satisfy lint.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val hasPerm = ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) == android.content.pm.PackageManager.PERMISSION_GRANTED
                if (hasPerm) {
                    gattServer?.close()
                } else {
                    // Attempt close but guard against SecurityException
                    try { gattServer?.close() } catch (se: SecurityException) { Log.w(TAG, "Permission denied closing GATT server", se) }
                }
            } else {
                gattServer?.close()
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Permission denied closing GATT server", e)
        } catch (e: Exception) {
            Log.w(TAG, "Error closing GATT server", e)
        }
        gattServer = null
        connectedDevice = null
        Log.d(TAG, "GATT HID server stopped")
    }

    private fun setupService() {
        val service = BluetoothGattService(HID_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)

        // Report characteristic (notify)
        reportCharacteristic = BluetoothGattCharacteristic(
            REPORT_CHAR_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        service.addCharacteristic(reportCharacteristic)

        // Report Map characteristic
        val reportMapChar = BluetoothGattCharacteristic(
            REPORT_MAP_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        @Suppress("DEPRECATION")
        // Use canonical raw report descriptor bytes exposed by HidConstants
        reportMapChar.value = HidConstants.REPORT_DESCRIPTOR
        service.addCharacteristic(reportMapChar)

        try {
            gattServer?.addService(service)
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing permission to add GATT service", e)
        }
    }

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)
            // Avoid using 'let' expression here to ensure we use statement form (not expression)
            if (device != null) {
                val d = device
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    connectedDevice = d
                    Log.d(TAG, "GATT client connected: ${d.address}")
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    if (connectedDevice?.address == d.address) connectedDevice = null
                    Log.d(TAG, "GATT client disconnected: ${d.address}")
                }
            }
        }

        override fun onCharacteristicReadRequest(device: BluetoothDevice?, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic?) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic)
            try {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic?.value)
            } catch (e: SecurityException) {
                Log.w(TAG, "Permission denied responding to read request", e)
            }
        }
    }

    fun sendReport(device: BluetoothDevice?, reportId: Int, report: ByteArray): Boolean {
        val char = reportCharacteristic ?: return false
        val target = device ?: connectedDevice ?: return false
        try {
            // For GATT HID we usually pack report without report ID if using report map with report IDs.
            // Here we send the raw report bytes; hosts often expect the Report ID as first byte depending on descriptor.
            char.value = report
            val notified = gattServer?.notifyCharacteristicChanged(target, char, false) == true
            Log.d(TAG, "Sent GATT report to ${target.address}: ${report.joinToString(" ") { String.format("%02X", it) }} (success=$notified)")
            return notified
        } catch (e: SecurityException) {
            Log.w(TAG, "Permission denied sending GATT report", e)
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send GATT report", e)
            return false
        }
    }
}
