package com.example.inmocontrol_v2.ui.screens

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Add the missing ConnectionState sealed class
sealed class ConnectionState {
    object Idle : ConnectionState()
    object Searching : ConnectionState()
    object Connecting : ConnectionState()
    data class Connected(val deviceName: String) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

class ConnectionStateViewModel : ViewModel() {
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // Add isConnected property to check if currently connected
    val isConnected: Boolean
        get() = _connectionState.value is ConnectionState.Connected

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = _discoveredDevices.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDevice>> = _pairedDevices.asStateFlow()

    fun updateConnectionState(state: ConnectionState) {
        _connectionState.value = state
    }

    fun updateDiscoveredDevices(devices: List<BluetoothDevice>) {
        _discoveredDevices.value = devices
    }

    fun updatePairedDevices(devices: List<BluetoothDevice>) {
        _pairedDevices.value = devices
    }

    fun addDiscoveredDevice(device: BluetoothDevice) {
        val currentDevices = _discoveredDevices.value.toMutableList()
        if (!currentDevices.any { it.address == device.address }) {
            currentDevices.add(device)
            _discoveredDevices.value = currentDevices
        }
    }

    fun clearDiscoveredDevices() {
        _discoveredDevices.value = emptyList()
    }

    // Add missing connection state methods
    fun setConnected() {
        _connectionState.value = ConnectionState.Connected("Connected Device")
    }

    fun setDisconnected() {
        _connectionState.value = ConnectionState.Idle
    }
}
