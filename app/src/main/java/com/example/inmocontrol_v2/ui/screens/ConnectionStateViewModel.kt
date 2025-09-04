package com.example.inmocontrol_v2.ui.screens

import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    private var connectionTimeoutJob: Job? = null
    private var connectionValidationJob: Job? = null

    companion object {
        private const val CONNECTION_TIMEOUT_MS = 5000L // 5 seconds timeout
        private const val CONNECTION_CHECK_INTERVAL_MS = 500L // Check every 500ms
    }

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
    fun setConnecting() {
        Log.d("ConnectionStateViewModel", "Setting state to connecting")
        // Cancel any existing timeout job
        connectionTimeoutJob?.cancel()

        // Start connection timeout
        connectionTimeoutJob = viewModelScope.launch {
            delay(CONNECTION_TIMEOUT_MS)
            Log.w("ConnectionStateViewModel", "Connection timed out")
        }
    }

    fun setConnected() {
        Log.d("ConnectionStateViewModel", "Setting state to connected")
        connectionTimeoutJob?.cancel()
        connectionValidationJob?.cancel()
    }

    fun setError(message: String = "Connection failed") {
        Log.e("ConnectionStateViewModel", "Setting error state: $message")
        connectionTimeoutJob?.cancel()
        connectionValidationJob?.cancel()
    }

    fun setIdle() {
        Log.d("ConnectionStateViewModel", "Setting state to idle")
        connectionTimeoutJob?.cancel()
        connectionValidationJob?.cancel()
    }

    /**
     * Validates connection with timeout and periodic checks
     */
    fun validateConnection(
        connectionCheck: suspend () -> Boolean,
        onSuccess: () -> Unit,
        onTimeout: () -> Unit,
        onError: (String) -> Unit
    ) {
        connectionValidationJob?.cancel()
        connectionTimeoutJob?.cancel()

        connectionValidationJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            var isConnected = false

            while (System.currentTimeMillis() - startTime < CONNECTION_TIMEOUT_MS && !isConnected) {
                try {
                    if (connectionCheck()) {
                        isConnected = true
                        onSuccess()
                        return@launch
                    }
                    delay(CONNECTION_CHECK_INTERVAL_MS)
                } catch (e: Exception) {
                    onError("Connection validation failed: ${e.message}")
                    return@launch
                }
            }

            if (!isConnected) {
                onTimeout()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        connectionTimeoutJob?.cancel()
        connectionValidationJob?.cancel()
        Log.d("ConnectionStateViewModel", "ViewModel cleared, jobs cancelled")
    }
}
