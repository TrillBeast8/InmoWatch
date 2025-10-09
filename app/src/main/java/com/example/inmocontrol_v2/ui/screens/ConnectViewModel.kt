package com.example.inmocontrol_v2.ui.screens

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConnectUiState(
    val bluetoothEnabled: Boolean = false,
    val isScanning: Boolean = false,
    val pairedDevices: List<BluetoothDevice> = emptyList(),
    val discoveredDevices: List<BluetoothDevice> = emptyList()
)

/**
 * ViewModel for the ConnectToDeviceScreen, handling all Bluetooth logic (2025)
 */
class ConnectViewModel(application: Application) : AndroidViewModel(application) {
    private val bluetoothManager = application.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    private val _uiState = MutableStateFlow(ConnectUiState())
    val uiState = _uiState.asStateFlow()

    private val discoveredDevicesSet = mutableSetOf<BluetoothDevice>()

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    _uiState.update { it.copy(bluetoothEnabled = state == BluetoothAdapter.STATE_ON) }
                    if (state == BluetoothAdapter.STATE_ON) {
                        loadPairedDevices()
                    }
                }
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    device?.let { foundDevice ->
                        try {
                            // Check if device has a name and add it to discovered devices
                            var deviceName: String? = null
                            try {
                                deviceName = foundDevice.name
                            } catch (_: SecurityException) {
                                // Permission denied, deviceName stays null
                            }

                            // Only add devices that have valid names - avoid nested ifs
                            when {
                                deviceName.isNullOrEmpty() -> {
                                    // Skip devices without names
                                }
                                else -> {
                                    val wasAdded = discoveredDevicesSet.add(foundDevice)
                                    when (wasAdded) {
                                        true -> {
                                            _uiState.update { state ->
                                                state.copy(discoveredDevices = discoveredDevicesSet.toList())
                                            }
                                        }
                                        false -> {
                                            // Device was already in the set, do nothing
                                        }
                                    }
                                }
                            }
                        } catch (e: SecurityException) {
                            Log.e("ConnectViewModel", "Permission denied for device discovery.", e)
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _uiState.update { it.copy(isScanning = false) }
                }
            }
        }
    }

    init {
        _uiState.update { it.copy(bluetoothEnabled = bluetoothAdapter?.isEnabled == true) }
        if (_uiState.value.bluetoothEnabled) {
            loadPairedDevices()
        }
    }

    fun registerReceiver() {
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        getApplication<Application>().registerReceiver(bluetoothStateReceiver, filter)
    }

    fun unregisterReceiver() {
        getApplication<Application>().unregisterReceiver(bluetoothStateReceiver)
    }

    fun startScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(
                getApplication(),
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        viewModelScope.launch {
            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter.cancelDiscovery()
            }
            discoveredDevicesSet.clear()
            _uiState.update { it.copy(discoveredDevices = emptyList(), isScanning = true) }
            bluetoothAdapter?.startDiscovery()
            delay(10000) // Scan for 10 seconds
            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter.cancelDiscovery()
            }
            _uiState.update { it.copy(isScanning = false) }
        }
    }

    fun connect(device: BluetoothDevice) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(
                getApplication(),
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        // Fully qualify HidClient to avoid any potential resolution ambiguity
        com.example.inmocontrol_v2.hid.HidClient.connectToDevice(device)
    }

    private fun loadPairedDevices() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(
                getApplication(),
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        try {
            val paired = bluetoothAdapter?.bondedDevices ?: emptySet()
            _uiState.update { it.copy(pairedDevices = paired.toList()) }
        } catch (e: SecurityException) {
            Log.e("ConnectViewModel", "Permission denied for loading paired devices.", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            unregisterReceiver()
        } catch (_: Exception) {
            // Receiver might not be registered
        }
    }
}
