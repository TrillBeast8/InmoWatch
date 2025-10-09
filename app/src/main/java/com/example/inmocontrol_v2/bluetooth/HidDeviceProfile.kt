package com.example.inmocontrol_v2.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log

/**
 * Wrapper for BluetoothHidDevice profile operations.
 * Manages profile connection lifecycle and provides thread-safe report transmission.
 */
class HidDeviceProfile {

    companion object {
        private const val TAG = "HidDeviceProfile"
    }

    @Volatile
    private var proxy: BluetoothHidDevice? = null

    interface ServiceStateListener {
        fun onServiceStateChanged(proxy: BluetoothProfile?)
    }

    interface DeviceStateListener {
        fun onConnectionStateChanged(device: BluetoothDevice, state: Int)
        fun onAppStatusChanged(device: BluetoothDevice, registered: Boolean)
    }

    private var serviceListener: ServiceStateListener? = null
    private var deviceListener: DeviceStateListener? = null

    /**
     * Register HID device profile service listener.
     * Must be called on main thread.
     */
    fun registerServiceListener(context: Context, listener: ServiceStateListener) {
        this.serviceListener = listener

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter
        adapter.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                if (profile == BluetoothProfile.HID_DEVICE) {
                    this@HidDeviceProfile.proxy = proxy as? BluetoothHidDevice
                    serviceListener?.onServiceStateChanged(proxy)
                    Log.d(TAG, "HID Device profile connected")
                }
            }

            override fun onServiceDisconnected(profile: Int) {
                if (profile == BluetoothProfile.HID_DEVICE) {
                    this@HidDeviceProfile.proxy = null
                    serviceListener?.onServiceStateChanged(null)
                    Log.d(TAG, "HID Device profile disconnected")
                }
            }
        }, BluetoothProfile.HID_DEVICE)
    }

    fun registerDeviceListener(listener: DeviceStateListener) {
        this.deviceListener = listener
    }

    /**
     * Attempt to initiate a connection to the given host device.
     */
    fun connect(device: BluetoothDevice): Boolean {
        val hidDevice = proxy ?: run {
            Log.w(TAG, "Cannot connect: HID proxy not available")
            return false
        }
        return try {
            hidDevice.connect(device)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception connecting", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Exception connecting", e)
            false
        }
    }

    /**
     * Disconnect from the given host device.
     */
    fun disconnect(device: BluetoothDevice): Boolean {
        val hidDevice = proxy ?: run {
            Log.w(TAG, "Cannot disconnect: HID proxy not available")
            return false
        }
        return try {
            hidDevice.disconnect(device)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception disconnecting", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Exception disconnecting", e)
            false
        }
    }

    /**
     * Send HID report to connected device.
     * Thread-safe via @Volatile proxy access.
     *
     * @param device Target device
     * @param reportId Report ID (1=Keyboard, 2=Mouse, 3=Consumer)
     * @param data Report payload
     * @return true if sent successfully
     */
    fun sendReport(device: BluetoothDevice, reportId: Int, data: ByteArray): Boolean {
        val hidDevice = proxy ?: run {
            Log.w(TAG, "Cannot send report: HID proxy not available")
            return false
        }

        return try {
            hidDevice.sendReport(device, reportId, data)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception sending report", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Exception sending report", e)
            false
        }
    }

    /**
     * Register HID application with SDP/QoS descriptors.
     */
    fun registerApp(hidDeviceApp: HidDeviceApp): Boolean {
        val hidDevice = proxy ?: return false

        return try {
            hidDevice.registerApp(
                hidDeviceApp.sdpRecord,
                null,
                hidDeviceApp.qosOut,
                { it.run() },
                hidDeviceApp.callback
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception registering app", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register HID app", e)
            false
        }
    }

    /**
     * Unregister HID application.
     */
    fun unregisterApp(): Boolean {
        val hidDevice = proxy ?: return false

        return try {
            hidDevice.unregisterApp()
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception unregistering app", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister HID app", e)
            false
        }
    }

    /**
     * Get connected devices.
     * Utility function for future features.
     */
    @Suppress("unused")
    fun getConnectedDevices(): List<BluetoothDevice> {
        val hidDevice = proxy ?: return emptyList()

        return try {
            hidDevice.connectedDevices ?: emptyList()
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception getting devices", e)
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Exception getting devices", e)
            emptyList()
        }
    }

    /**
     * Cleanup and release profile proxy.
     */
    fun cleanup(adapter: BluetoothAdapter) {
        try {
            proxy?.let { adapter.closeProfileProxy(BluetoothProfile.HID_DEVICE, it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error closing profile proxy", e)
        }
        proxy = null
    }
}
