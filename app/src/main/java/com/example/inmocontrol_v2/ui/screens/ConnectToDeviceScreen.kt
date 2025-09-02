package com.example.inmocontrol_v2.ui.screens

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.inmocontrol_v2.hid.HidClient
import com.example.inmocontrol_v2.hid.HidClient.InmoGesture
import com.example.inmocontrol_v2.hid.HidService
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.Text as WearText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.BluetoothSearching

sealed class ConnectionState {
    object Idle : ConnectionState()
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

sealed class DeviceProfile {
    object InmoAir2 : DeviceProfile()
    object GenericHid : DeviceProfile()
}

fun identifyDeviceProfile(deviceName: String): DeviceProfile {
    if (deviceName.contains("Inmo Air 2", ignoreCase = true)) return DeviceProfile.InmoAir2
    return DeviceProfile.GenericHid
}

@Composable
fun ConnectToDeviceScreenContent(
    state: ConnectionState,
    deviceList: List<Pair<String, Int>>,
    pairedDevices: List<String> = emptyList(),
    connectedDevice: String? = null,
    deviceProfileMap: Map<String, DeviceProfile> = emptyMap(),
    onScan: () -> Unit = {},
    onPair: (String) -> Unit = {},
    onConnect: (String) -> Unit = {},
    onOpenControls: () -> Unit = {}
) {
    Scaffold {
        TimeText()
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                WearText("Connect to Device", modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                Spacer(modifier = Modifier.height(8.dp))
            }
            item {
                when (state) {
                    is ConnectionState.Idle -> {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Button(
                                onClick = onScan,
                                modifier = Modifier
                                    .widthIn(max = 120.dp)
                                    .padding(vertical = 8.dp)
                            ) {
                                WearText("Scan for Devices")
                            }
                        }
                    }
                    is ConnectionState.Scanning -> {
                        WearText("Scanning for devices...", modifier = Modifier.padding(bottom = 4.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        if (deviceList.isEmpty()) {
                            WearText("No devices found yet", modifier = Modifier.padding(top = 4.dp))
                        } else {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                ScalingLazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    items(deviceList.size) { idx ->
                                        val (name, rssi) = deviceList[idx]
                                        val isPaired = pairedDevices.contains(name)
                                        val isConnected = connectedDevice == name
                                        Button(
                                            onClick = {
                                                if (!isPaired) onPair(name)
                                                onConnect(name)
                                            },
                                            modifier = Modifier.size(120.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (isConnected) {
                                                    Icon(Icons.Filled.BluetoothDisabled, contentDescription = "Connected", modifier = Modifier.size(24.dp))
                                                } else if (isPaired) {
                                                    Icon(Icons.Filled.Bluetooth, contentDescription = "Paired", modifier = Modifier.size(24.dp))
                                                } else {
                                                    Icon(Icons.Filled.BluetoothSearching, contentDescription = "Discovered", modifier = Modifier.size(24.dp))
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                WearText(name)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                WearText("RSSI: $rssi")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    is ConnectionState.Pairing -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        WearText("Pairing with device...", modifier = Modifier.padding(vertical = 8.dp))
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                    is ConnectionState.Connected -> {
                        WearText("Connected to ${state.deviceName}", modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        val profile = deviceProfileMap[state.deviceName] ?: DeviceProfile.GenericHid
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Button(
                                    onClick = onOpenControls,
                                    modifier = Modifier
                                        .widthIn(max = 120.dp)
                                        .padding(vertical = 8.dp)
                                ) {
                                    WearText("Open Controls")
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                if (profile is DeviceProfile.InmoAir2) {
                                    WearText("Inmo Air 2 Gestures:", modifier = Modifier.padding(bottom = 8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                        Button(onClick = { HidClient.sendGesture(InmoGesture.SINGLE_TAP) }) { WearText("Tap") }
                                        Button(onClick = { HidClient.sendGesture(InmoGesture.DOUBLE_TAP) }) { WearText("Double Tap") }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                        Button(onClick = { HidClient.sendGesture(InmoGesture.SWIPE_UP) }) { WearText("Swipe Up") }
                                        Button(onClick = { HidClient.sendGesture(InmoGesture.SWIPE_DOWN) }) { WearText("Swipe Down") }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                        Button(onClick = { HidClient.sendGesture(InmoGesture.SWIPE_FORWARD) }) { WearText("Swipe Right") }
                                        Button(onClick = { HidClient.sendGesture(InmoGesture.SWIPE_BACK) }) { WearText("Swipe Left") }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                        Button(onClick = { HidClient.sendGesture(InmoGesture.LONG_PRESS) }) { WearText("Long Press") }
                                        Button(onClick = { HidClient.sendGesture(InmoGesture.DOUBLE_LONG_PRESS) }) { WearText("Double Long Press") }
                                    }
                                } else {
                                    WearText("Generic HID Controls", modifier = Modifier.padding(bottom = 8.dp))
                                    // ...existing generic HID controls...
                                }
                            }
                        }
                    }
                    is ConnectionState.Error -> {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                WearText("Error: ${state.reason}", modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = onScan,
                                    modifier = Modifier
                                        .widthIn(max = 120.dp)
                                        .padding(vertical = 8.dp)
                                ) {
                                    WearText("Retry")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectToDeviceScreen(
    onOpenControls: () -> Unit = {}
) {
    val context = LocalContext.current
    val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter() // Deprecated, but required for compatibility
    var locationPermissionGranted by remember { mutableStateOf(false) }
    // Launcher for enabling Bluetooth
    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // You can handle the result if needed
    }
    // Launcher for location permission (Android 6-11)
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        locationPermissionGranted = isGranted
    }
    val scanner = bluetoothAdapter?.bluetoothLeScanner

    // Preview mode: use mock state and skip runtime logic
    var state by remember { mutableStateOf<ConnectionState>(ConnectionState.Idle) }
    var deviceList by remember { mutableStateOf(listOf<Pair<String, Int>>()) }
    if (false) {
        // Show a mock scanning state with sample devices
        state = ConnectionState.Scanning
        deviceList = listOf("Demo Device 1" to -40, "Demo Device 2" to -55)
    } else {
        // --- Runtime Logic ---
        var permissionsGranted by remember { mutableStateOf(false) }
        var deviceMap by remember { mutableStateOf(mapOf<String, BluetoothDevice>()) }
        var deviceProfileMap by remember { mutableStateOf(mapOf<String, DeviceProfile>()) }

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
        // Get permission status once in composable
        val hasBluetoothConnectPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true
        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result?.device?.let { device ->
                    val name = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (hasBluetoothConnectPermission) device.name ?: "Unknown" else "Unknown"
                    } else {
                        device.name ?: "Unknown"
                    }
                    val rssi = result.rssi
                    val profile = identifyDeviceProfile(name)
                    if (profile is DeviceProfile.InmoAir2 && deviceList.none { d -> d.first == name }) {
                        deviceList = deviceList + (name to rssi)
                        deviceMap = deviceMap + (name to device)
                        deviceProfileMap = deviceProfileMap + (name to profile)
                    }
                }
            }
            override fun onScanFailed(errorCode: Int) {
                state = ConnectionState.Error("Scan failed: $errorCode")
            }
        }
        fun startScanning() {
            if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                enableBluetoothLauncher.launch(enableBtIntent)
            }
            if (Build.VERSION.SDK_INT in 23..30) {
                if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                } else {
                    locationPermissionGranted = true
                }
            } else {
                locationPermissionGranted = true
            }
            if (!permissionsGranted) {
                state = ConnectionState.Error("Bluetooth permissions not granted")
                return
            }
            if (!locationPermissionGranted) {
                state = ConnectionState.Error("Location permission not granted")
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
                // You can add device-specific pairing logic here if needed
                // For now, use generic logic
                val paired = device.createBond()
                if (paired) {
                    state = ConnectionState.Pairing()
                } else {
                    state = ConnectionState.Error("Pairing initiation failed")
                }
            } catch (e: Exception) {
                state = ConnectionState.Error("Pairing error: ${e.message}")
            }
        }
        fun connectDevice(deviceName: String) {
            val device = deviceMap[deviceName]
            val profile = deviceProfileMap[deviceName] ?: DeviceProfile.GenericHid
            if (device == null) {
                state = ConnectionState.Error("Device not found for connection")
                return
            }
            try {
                HidClient.currentDeviceProfile = profile
                when (profile) {
                    is DeviceProfile.InmoAir2 -> {
                        // TODO: Add Inmo Air 2-specific connection logic here
                        HidClient.connect(device)
                    }
                    is DeviceProfile.GenericHid -> {
                        HidClient.connect(device)
                    }
                }
                state = ConnectionState.Connected(deviceName)
            } catch (e: Exception) {
                state = ConnectionState.Error("Connection error: ${e.message}")
            }
        }
        // --- End Bluetooth/HID Logic ---

        val serviceConnection = remember {
            HidServiceConnection(
                onServiceConnected = { },
                onServiceDisconnected = { }
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

        // --- Reconnect Status Broadcast Receiver ---
        DisposableEffect(Unit) {
            val reconnectReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action == HidService.ACTION_RECONNECT_STATUS) {
                        val status = intent.getStringExtra(HidService.EXTRA_RECONNECT_STATUS)
                        val attempt = intent.getIntExtra(HidService.EXTRA_RECONNECT_ATTEMPT, 0)
                        val deviceName = intent.getStringExtra(HidService.EXTRA_RECONNECT_DEVICE)
                        when (status) {
                            "attempt" -> state = ConnectionState.Error("Reconnecting to $deviceName (attempt $attempt)...")
                            "success" -> state = ConnectionState.Connected(deviceName ?: "Unknown")
                            "failure" -> state = ConnectionState.Error("Failed to reconnect to $deviceName after $attempt attempts")
                        }
                    }
                }
            }
            context.registerReceiver(reconnectReceiver, IntentFilter(HidService.ACTION_RECONNECT_STATUS))
            onDispose {
                context.unregisterReceiver(reconnectReceiver)
            }
        }

        ConnectToDeviceScreenContent(
            state = state,
            deviceList = deviceList,
            onScan = { requestBluetoothPermissions(); startScanning() },
            onPair = { pairDevice(it) },
            onConnect = { connectDevice(it) },
            onOpenControls = onOpenControls
        )
    }
}

@Preview(device = "id:wearos_small_round", showSystemUi = true)
@Composable
fun ConnectToDeviceScreenWearPreview() {
    ConnectToDeviceScreenContent(
        state = ConnectionState.Scanning,
        deviceList = listOf("Demo Device 1" to -40, "Demo Device 2" to -55),
        onScan = {},
        onPair = {},
        onConnect = {},
        onOpenControls = {}
    )
}
