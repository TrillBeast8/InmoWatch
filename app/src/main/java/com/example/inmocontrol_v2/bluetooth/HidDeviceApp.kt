package com.example.inmocontrol_v2.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.util.Log

/**
 * HID Device Application descriptor and callbacks.
 * Defines the SDP record and QoS settings for HID profile registration.
 */
class HidDeviceApp {

    companion object {
        private const val TAG = "HidDeviceApp"

        // SDP record constants
        private const val DEVICE_NAME = "InmoWatch HID Controller"
        private const val DEVICE_DESCRIPTION = "Wear OS Universal HID Input"
        private const val PROVIDER_NAME = "InmoWatch"

        // HID device subclass (combo keyboard/mouse)
        private const val SUBCLASS = 0xC0.toByte()
    }

    interface DeviceStateListener {
        fun onConnectionStateChanged(device: BluetoothDevice, state: Int)
        fun onAppStatusChanged(registered: Boolean)
    }

    private var listener: DeviceStateListener? = null

    /**
     * SDP (Service Discovery Protocol) record for HID profile.
     * Advertises device capabilities to host during pairing.
     */
    val sdpRecord = BluetoothHidDeviceAppSdpSettings(
        DEVICE_NAME,
        DEVICE_DESCRIPTION,
        PROVIDER_NAME,
        SUBCLASS,
        HidConstants.REPORT_DESCRIPTOR
    )

    /**
     * QoS (Quality of Service) settings for Bluetooth transmission.
     * Uses default values for reliable HID communication.
     */
    val qosOut = BluetoothHidDeviceAppQosSettings(
        BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
        800,  // Token rate
        9000, // Token bucket size
        800,  // Peak bandwidth
        BluetoothHidDeviceAppQosSettings.MAX, // Latency
        BluetoothHidDeviceAppQosSettings.MAX  // Delay variation
    )

    /**
     * Bluetooth HID device callback handler.
     * Receives connection state changes and app registration events.
     */
    val callback = object : BluetoothHidDevice.Callback() {
        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            device?.let {
                Log.d(TAG, "Connection state changed: device=${it.address}, state=$state")
                listener?.onConnectionStateChanged(it, state)
            }
        }

        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            Log.d(TAG, "App status changed: registered=$registered")
            listener?.onAppStatusChanged(registered)
        }

        override fun onGetReport(
            device: BluetoothDevice?,
            type: Byte,
            id: Byte,
            bufferSize: Int
        ) {
            // Host is requesting a report (rare for output devices)
            Log.d(TAG, "onGetReport: type=$type, id=$id, bufferSize=$bufferSize")
        }

        override fun onSetReport(device: BluetoothDevice?, type: Byte, id: Byte, data: ByteArray?) {
            // Host is sending a report (e.g., keyboard LED state)
            Log.d(TAG, "onSetReport: type=$type, id=$id, data=${data?.size ?: 0} bytes")
        }

        override fun onSetProtocol(device: BluetoothDevice?, protocol: Byte) {
            // Host is setting protocol (boot vs report protocol)
            Log.d(TAG, "onSetProtocol: protocol=$protocol")
        }

        override fun onInterruptData(device: BluetoothDevice?, reportId: Byte, data: ByteArray?) {
            // Host sent interrupt data (rare)
            Log.d(TAG, "onInterruptData: reportId=$reportId")
        }

        override fun onVirtualCableUnplug(device: BluetoothDevice?) {
            // Host requested disconnect
            Log.d(TAG, "onVirtualCableUnplug")
        }
    }

    fun setDeviceStateListener(listener: DeviceStateListener) {
        this.listener = listener
    }
}
