package com.example.inmocontrol_v2.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.MainThread

/**
 * HID Device Profile wrapper - optimized for fast initialization and reliable connections
 * Based on proven WearMouse pattern to eliminate scanning delays and pairing issues
 */
class HidDeviceProfile {

    companion object {
        private const val TAG = "HidDeviceProfile"
        // Standard HID UUIDs for proper device detection
        private val HOGP_UUID = ParcelUuid.fromString("00001812-0000-1000-8000-00805f9b34fb")
        private val HID_UUID = ParcelUuid.fromString("00001124-0000-1000-8000-00805f9b34fb")
    }

    /** Callback for profile proxy connection state changes */
    interface ServiceStateListener {
        /**
         * Called when profile proxy state changes
         * @param proxy Profile proxy object or null if disconnected
         */
        @MainThread
        fun onServiceStateChanged(proxy: BluetoothProfile?)
    }

    /** Callback for device connection state changes */
    interface DeviceStateListener {
        /**
         * Called when device connection state changes
         * @param device The remote device
         * @param state New connection state
         */
        @MainThread
        fun onConnectionStateChanged(device: BluetoothDevice, state: Int)

        /**
         * Called when app registration state changes
         * @param device The remote device
         * @param registered Whether the app is registered
         */
        @MainThread
        fun onAppStatusChanged(device: BluetoothDevice, registered: Boolean)
    }

    private val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        ?: throw IllegalStateException("Bluetooth not supported")

    private var serviceStateListener: ServiceStateListener? = null
    private var deviceStateListener: DeviceStateListener? = null
    private var service: BluetoothHidDevice? = null

    /**
     * Check if a device supports HID Host profile (fast detection)
     * @param device Device to check
     * @return true if HID Host is supported, false if device is HID Device
     */
    fun isProfileSupported(device: BluetoothDevice): Boolean {
        try {
            // If device reports itself as HID Device, it's not a HID Host
            val uuidArray = device.uuids
            if (uuidArray != null) {
                for (uuid in uuidArray) {
                    if (HID_UUID == uuid || HOGP_UUID == uuid) {
                        return false
                    }
                }
            }
            return true
        } catch (e: SecurityException) {
            Log.w(TAG, "Permission denied checking profile for ${device.address}")
            return true // Default to supported if we can't check
        }
    }

    /**
     * Fast profile proxy registration - no delays or retries
     * @param context Application context
     * @param listener Callback for proxy state changes
     */
    @MainThread
    fun registerServiceListener(context: Context, listener: ServiceStateListener) {
        val appContext = context.applicationContext
        serviceStateListener = listener

        // Get profile proxy immediately - WearMouse pattern
        bluetoothAdapter.getProfileProxy(
            appContext,
            ServiceListener(),
            BluetoothProfile.HID_DEVICE
        )
    }

    /**
     * Register device state listener for connection events
     * @param listener Callback for device state changes
     */
    @MainThread
    fun registerDeviceListener(listener: DeviceStateListener) {
        deviceStateListener = listener
    }

    /** Close profile service connection */
    @MainThread
    fun unregisterServiceListener() {
        service?.let { hidService ->
            try {
                bluetoothAdapter.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidService)
            } catch (t: Throwable) {
                Log.w(TAG, "Error cleaning up proxy", t)
            }
        }
        service = null
        serviceStateListener = null
    }

    /** Get current HID service proxy */
    fun getService(): BluetoothHidDevice? = service

    /** Check if service is ready */
    fun isServiceReady(): Boolean = service != null

    /**
     * Get all connected devices
     */
    fun getConnectedDevices(): List<BluetoothDevice> {
        return try {
            service?.connectedDevices ?: emptyList()
        } catch (e: SecurityException) {
            Log.w(TAG, "Permission denied getting connected devices")
            emptyList()
        }
    }

    /**
     * Get connection state for specific device
     */
    fun getConnectionState(device: BluetoothDevice): Int {
        return try {
            service?.getConnectionState(device) ?: BluetoothProfile.STATE_DISCONNECTED
        } catch (e: SecurityException) {
            Log.w(TAG, "Permission denied getting connection state for ${device.address}")
            BluetoothProfile.STATE_DISCONNECTED
        }
    }

    /**
     * Internal service listener - handles profile proxy connection
     */
    private inner class ServiceListener : BluetoothProfile.ServiceListener {
        @MainThread
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            Log.d(TAG, "HID Device profile connected")
            service = proxy as? BluetoothHidDevice
            serviceStateListener?.onServiceStateChanged(proxy)
        }

        @MainThread
        override fun onServiceDisconnected(profile: Int) {
            Log.d(TAG, "HID Device profile disconnected")
            service = null
            serviceStateListener?.onServiceStateChanged(null)
        }
    }

    /**
     * Internal device state callback - handles connection events
     */
    inner class DeviceCallback : BluetoothHidDevice.Callback() {
        @MainThread
        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            Log.d(TAG, "Device connection state changed: ${device?.address} -> $state")
            device?.let {
                deviceStateListener?.onConnectionStateChanged(it, state)
            }
        }

        @MainThread
        override fun onAppStatusChanged(device: BluetoothDevice?, registered: Boolean) {
            Log.d(TAG, "App status changed: ${device?.address} -> $registered")
            device?.let {
                deviceStateListener?.onAppStatusChanged(it, registered)
            }
        }
    }

    /** Get device callback for app registration */
    fun getDeviceCallback(): BluetoothHidDevice.Callback = DeviceCallback()

    /**
     * Send HID report to device - the missing critical method
     * @param device Target device
     * @param reportId Report ID (1=keyboard, 2=mouse, 3=consumer)
     * @param data Report data
     * @return true if sent successfully
     */
    fun sendReport(device: BluetoothDevice, reportId: Int, data: ByteArray): Boolean {
        return try {
            val success = service?.sendReport(device, reportId, data) ?: false
            if (!success) {
                Log.w(TAG, "Failed to send HID report $reportId to ${device.address}")
            } else {
                Log.d(TAG, "Successfully sent HID report $reportId to ${device.address}")
            }
            success
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied sending report to ${device.address}", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error sending HID report to ${device.address}", e)
            false
        }
    }

    /**
     * Connect to device
     */
    fun connect(device: BluetoothDevice): Boolean {
        return try {
            val success = service?.connect(device) ?: false
            Log.d(TAG, "Connect request to ${device.address}: $success")
            success
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied connecting to ${device.address}", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to ${device.address}", e)
            false
        }
    }

    /**
     * Disconnect from device
     */
    fun disconnect(device: BluetoothDevice): Boolean {
        return try {
            val success = service?.disconnect(device) ?: false
            Log.d(TAG, "Disconnect request from ${device.address}: $success")
            success
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied disconnecting from ${device.address}", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting from ${device.address}", e)
            false
        }
    }
}
