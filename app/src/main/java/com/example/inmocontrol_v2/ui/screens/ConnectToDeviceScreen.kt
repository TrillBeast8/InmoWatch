package com.example.inmocontrol_v2.ui.screens

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.os.ParcelUuid
import androidx.annotation.RequiresPermission
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.wear.compose.material.*
import androidx.compose.ui.platform.LocalContext
import com.example.inmocontrol_v2.hid.HidClient
import com.example.inmocontrol_v2.hid.HidService
import java.util.UUID
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.Text as WearText

sealed class ConnectionState {
    object Idle : ConnectionState()
    object Advertising : ConnectionState()
    object Scanning : ConnectionState()
    data class Pairing(val code: String? = null) : ConnectionState()
    data class Connected(val deviceName: String, val battery: Int? = null, val latency: Int? = null) : ConnectionState()
    data class Error(val reason: String) : ConnectionState()
}

private class HidServiceConnection(
    private val onServiceConnected: (HidService?) -> Unit,
    private val onServiceDisconnected: () -> Unit
) : ServiceConnection {
    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
        onServiceConnected((binder as? HidService.LocalBinder)?.getService())
    }
    override fun onServiceDisconnected(name: ComponentName?) {
        onServiceDisconnected()
    }
}

@Composable
fun ConnectToDeviceScreen(
    onOpenControls: () -> Unit = {}
) {
    val context = LocalContext.current
    val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter() // Deprecated, but required for compatibility
    val scanner = bluetoothAdapter?.bluetoothLeScanner

    var state by remember { mutableStateOf<ConnectionState>(ConnectionState.Idle) }
    var permissionsGranted by remember { mutableStateOf(false) }
    var deviceList by remember { mutableStateOf(listOf<Pair<String, Int>>()) } // name, RSSI
    var deviceMap by remember { mutableStateOf(mapOf<String, BluetoothDevice>()) }
    var pairedDevice by remember { mutableStateOf<String?>(null) }
    var hidService by remember { mutableStateOf<HidService?>(null) }

    // --- Permission Launcher ---
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        permissionsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms[Manifest.permission.BLUETOOTH_ADVERTISE] == true &&
            perms[Manifest.permission.BLUETOOTH_SCAN] == true &&
            perms[Manifest.permission.BLUETOOTH_CONNECT] == true
        } else true // Permissions not required below API 31
    }
    fun requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            )
        } else {
            permissionsGranted = true
        }
    }
    // --- End Permission Launcher ---

    // --- Bluetooth/HID Logic ---
    val scanCallback = remember {
        object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                try {
                    result?.device?.let {
                        val name = it.name ?: "Unknown"
                        val rssi = result.rssi
                        if (deviceList.none { d -> d.first == name }) {
                            deviceList = deviceList + (name to rssi)
                            deviceMap = deviceMap + (name to it)
                        }
                    }
                } catch (e: SecurityException) {
                    state = ConnectionState.Error("Scan permission error: ${e.message}")
                }
            }
            override fun onScanFailed(errorCode: Int) {
                state = ConnectionState.Error("Scan failed: $errorCode")
            }
        }
    }
    fun startScanning() {
        if (!permissionsGranted) {
            state = ConnectionState.Error("Bluetooth permissions not granted")
            return
        }
        if (scanner == null) {
            state = ConnectionState.Error("No scanner available")
            return
        }
        deviceList = emptyList()
        try {
            scanner.startScan(scanCallback)
            state = ConnectionState.Scanning
        } catch (e: SecurityException) {
            state = ConnectionState.Error("Bluetooth permission error: ${e.message}")
        }
    }
    fun pairDevice(deviceName: String) {
        val device = deviceMap[deviceName]
        if (device == null) {
            state = ConnectionState.Error("Device not found for pairing")
            return
        }
        try {
            val paired = device.createBond()
            if (paired) {
                state = ConnectionState.Pairing()
                pairedDevice = deviceName
            } else {
                state = ConnectionState.Error("Pairing initiation failed")
            }
        } catch (e: SecurityException) {
            state = ConnectionState.Error("Pairing permission error: ${e.message}")
        }
    }
    fun connectToDevice(deviceName: String) {
        val device = deviceMap[deviceName]
        if (device == null) {
            state = ConnectionState.Error("Device not found for connection")
            return
        }
        try {
            HidClient.connect(device)
            state = ConnectionState.Connected(deviceName, battery = 90, latency = 10)
        } catch (e: Exception) {
            state = ConnectionState.Error("Connection error: ${e.message}")
        }
    }
    // --- End Bluetooth/HID Logic ---

    val serviceConnection = remember {
        HidServiceConnection(
            onServiceConnected = { hidService = it },
            onServiceDisconnected = { hidService = null }
        )
    }
    LaunchedEffect(Unit) {
        val intent = Intent(context, HidService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }
    DisposableEffect(Unit) {
        onDispose {
            context.unbindService(serviceConnection)
        }
    }

    Scaffold {
        TimeText()
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                WearText("Connect to Device", modifier = Modifier.padding(top = 12.dp))
            }
            item {
                when (state) {
                    is ConnectionState.Idle -> {
                        Button(
                            onClick = { requestBluetoothPermissions(); startScanning() },
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                        ) {
                            WearText("Scan for Devices")
                        }
                    }
                    is ConnectionState.Scanning -> {
                        WearText("Scanning for devices...")
                        if (deviceList.isEmpty()) {
                            WearText("No devices found yet", modifier = Modifier.padding(top = 8.dp))
                        } else {
                            deviceList.forEach { (name, rssi) ->
                                Button(
                                    onClick = { pairDevice(name); connectToDevice(name) },
                                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                                ) {
                                    WearText("$name (RSSI: $rssi)")
                                }
                            }
                        }
                    }
                    is ConnectionState.Pairing -> {
                        WearText("Pairing with device...")
                    }
                    is ConnectionState.Connected -> {
                        val connected = state as ConnectionState.Connected
                        WearText("Connected to ${connected.deviceName}", modifier = Modifier.padding(top = 8.dp))
                        Button(
                            onClick = onOpenControls,
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                        ) {
                            WearText("Open Controls")
                        }
                    }
                    is ConnectionState.Error -> {
                        val error = state as ConnectionState.Error
                        WearText("Error: ${error.reason}", modifier = Modifier.padding(top = 8.dp))
                        Button(
                            onClick = { state = ConnectionState.Idle },
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                        ) {
                            WearText("Retry")
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}
