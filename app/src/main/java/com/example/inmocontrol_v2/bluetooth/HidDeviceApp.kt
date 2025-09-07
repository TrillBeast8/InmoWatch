package com.example.inmocontrol_v2.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothProfile
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.MainThread

/**
 * HID Device App implementation - optimized for seamless pairing and connection
 * Based on proven WearMouse pattern to eliminate connection delays and pairing issues
 */
class HidDeviceApp {

    companion object {
        private const val TAG = "HidDeviceApp"
    }

    /** Callback interface for device state changes */
    interface DeviceStateListener {
        /**
         * Called when device connection state changes
         * @param device Device that was connected or disconnected
         * @param state New connection state (BluetoothProfile.STATE_)
         */
        @MainThread
        fun onConnectionStateChanged(device: BluetoothDevice, state: Int)

        /**
         * Called when app registration status changes
         * @param registered True if app is registered, false otherwise
         */
        @MainThread
        fun onAppStatusChanged(registered: Boolean)
    }

    private val mainThreadHandler = Handler(Looper.getMainLooper())
    private var connectedDevice: BluetoothDevice? = null
    private var deviceStateListener: DeviceStateListener? = null
    private var inputHost: BluetoothHidDevice? = null
    private var isRegistered = false

    /** HID Device callback - handles all HID events seamlessly */
    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(device: BluetoothDevice?, registered: Boolean) {
            super.onAppStatusChanged(device, registered)
            Log.d(TAG, "App status changed: device=${device?.address} registered=$registered")
            isRegistered = registered
            onAppStatusChanged(registered)
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            super.onConnectionStateChanged(device, state)
            Log.d(TAG, "Connection state changed: device=${device?.address} state=$state")

            device?.let { bluetoothDevice ->
                when (state) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        connectedDevice = bluetoothDevice
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        if (connectedDevice?.address == bluetoothDevice.address) {
                            connectedDevice = null
                        }
                    }
                }
                // Removed recursive call - listener notification is handled by private method
                deviceStateListener?.let { listener ->
                    mainThreadHandler.post {
                        listener.onConnectionStateChanged(bluetoothDevice, state)
                    }
                }
            }
        }

        override fun onGetReport(device: BluetoothDevice?, type: Byte, id: Byte, bufferSize: Int) {
            super.onGetReport(device, type, id, bufferSize)
            Log.d(TAG, "Get report request: device=${device?.address} type=$type id=$id")

            inputHost?.let { host ->
                if (type != BluetoothHidDevice.REPORT_TYPE_INPUT) {
                    host.reportError(device, BluetoothHidDevice.ERROR_RSP_UNSUPPORTED_REQ)
                } else {
                    // Send appropriate empty report based on ID
                    val reportData = when (id.toInt()) {
                        HidConstants.ID_KEYBOARD.toInt() -> ByteArray(8) // Keyboard report
                        HidConstants.ID_MOUSE.toInt() -> ByteArray(4)    // Mouse report
                        HidConstants.ID_CONSUMER.toInt() -> ByteArray(2) // Consumer report
                        else -> {
                            host.reportError(device, BluetoothHidDevice.ERROR_RSP_INVALID_RPT_ID)
                            return@let
                        }
                    }
                    host.replyReport(device, type, id, reportData)
                }
            }
        }

        override fun onSetReport(device: BluetoothDevice?, type: Byte, id: Byte, data: ByteArray?) {
            super.onSetReport(device, type, id, data)
            Log.d(TAG, "Set report request: device=${device?.address} type=$type id=$id")
            inputHost?.reportError(device, BluetoothHidDevice.ERROR_RSP_SUCCESS)
        }

        override fun onVirtualCableUnplug(device: BluetoothDevice?) {
            super.onVirtualCableUnplug(device)
            Log.d(TAG, "Virtual cable unplugged: ${device?.address}")
            device?.let { bluetoothDevice ->
                if (connectedDevice?.address == bluetoothDevice.address) {
                    connectedDevice = null
                }
                // Removed recursive call - notify listener directly
                deviceStateListener?.let { listener ->
                    mainThreadHandler.post {
                        listener.onConnectionStateChanged(bluetoothDevice, BluetoothProfile.STATE_DISCONNECTED)
                    }
                }
            }
        }

        override fun onInterruptData(device: BluetoothDevice?, reportId: Byte, data: ByteArray?) {
            super.onInterruptData(device, reportId, data)
            Log.d(TAG, "Interrupt data received: device=${device?.address} reportId=$reportId")
        }
    }

    /**
     * Set device state listener
     */
    fun setDeviceStateListener(listener: DeviceStateListener?) {
        deviceStateListener = listener
    }

    /**
     * Register HID app with the Bluetooth stack - fast and reliable
     * @param inputHost BluetoothHidDevice service proxy
     * @return True if registration started successfully
     */
    fun registerApp(inputHost: BluetoothProfile): Boolean {
        return try {
            this.inputHost = inputHost as BluetoothHidDevice

            // Use the working SDP settings from HidConstants directly
            val success = this.inputHost?.registerApp(
                HidConstants.SDP_SETTINGS,
                null, // No inQoS settings needed
                HidConstants.QOS_OUT, // outQoS settings
                java.util.concurrent.Executors.newSingleThreadExecutor(), // Proper executor
                hidCallback
            ) ?: false

            Log.d(TAG, "HID app registration initiated: $success")
            if (!success) {
                Log.e(TAG, "HID app registration returned false - check permissions and Bluetooth state")
            }
            success
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception during HID app registration - missing permissions", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register HID app", e)
            false
        }
    }

    /**
     * Unregister HID app
     */
    fun unregisterApp() {
        try {
            inputHost?.unregisterApp()
            isRegistered = false
            connectedDevice = null
            Log.d(TAG, "HID app unregistered")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister HID app", e)
        }
    }

    /**
     * Request connection to device - seamless pairing and connection
     * @param device Target device
     * @return True if connection request was sent
     */
    fun requestConnect(device: BluetoothDevice): Boolean {
        return try {
            if (!isRegistered) {
                Log.w(TAG, "App not registered, cannot connect")
                return false
            }

            Log.d(TAG, "Requesting connection to device: ${device.address}")
            val success = inputHost?.connect(device) ?: false

            if (success) {
                Log.d(TAG, "Connection request sent successfully")
            } else {
                Log.w(TAG, "Failed to send connection request")
            }

            success
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied connecting to device: ${device.address}", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to device: ${device.address}", e)
            false
        }
    }

    /**
     * Request disconnection from device
     * @param device Target device
     * @return True if disconnection request was sent
     */
    fun requestDisconnect(device: BluetoothDevice): Boolean {
        return try {
            Log.d(TAG, "Requesting disconnection from device: ${device.address}")
            val success = inputHost?.disconnect(device) ?: false

            if (success) {
                Log.d(TAG, "Disconnection request sent successfully")
            } else {
                Log.w(TAG, "Failed to send disconnection request")
            }

            success
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied disconnecting from device: ${device.address}", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting from device: ${device.address}", e)
            false
        }
    }

    /**
     * Send HID report to connected device
     * @param device Target device
     * @param reportId Report ID
     * @param data Report data
     * @return True if report was sent successfully
     */
    fun sendReport(device: BluetoothDevice, reportId: Int, data: ByteArray): Boolean {
        return try {
            if (connectedDevice?.address != device.address) {
                Log.w(TAG, "Device not connected: ${device.address}")
                return false
            }

            val success = inputHost?.sendReport(device, reportId, data) ?: false

            if (!success) {
                Log.w(TAG, "Failed to send report $reportId to ${device.address}")
            }

            success
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied sending report to device: ${device.address}", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error sending report to device: ${device.address}", e)
            false
        }
    }

    /**
     * Get list of connected devices
     */
    fun getConnectedDevices(): List<BluetoothDevice> {
        return try {
            inputHost?.connectedDevices ?: emptyList()
        } catch (e: SecurityException) {
            Log.w(TAG, "Permission denied getting connected devices")
            emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "Error getting connected devices", e)
            emptyList()
        }
    }

    /**
     * Check if a specific device is connected
     */
    fun isDeviceConnected(device: BluetoothDevice): Boolean {
        return connectedDevice?.address == device.address
    }

    /**
     * Check if any device is connected
     */
    fun isAnyDeviceConnected(): Boolean = connectedDevice != null

    /**
     * Get current connected device
     */
    fun getConnectedDevice(): BluetoothDevice? = connectedDevice

    /**
     * Check if app is registered
     */
    fun isAppRegistered(): Boolean = isRegistered

    // Internal callback handlers
    private fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
        mainThreadHandler.post {
            deviceStateListener?.onConnectionStateChanged(device, state)
        }
    }

    private fun onAppStatusChanged(registered: Boolean) {
        mainThreadHandler.post {
            deviceStateListener?.onAppStatusChanged(registered)
        }
    }
}
