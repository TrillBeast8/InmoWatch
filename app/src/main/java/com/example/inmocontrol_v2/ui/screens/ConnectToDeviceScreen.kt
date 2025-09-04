package com.example.inmocontrol_v2.ui.screens

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.*
import androidx.wear.compose.material.Button as WearButton
import androidx.wear.compose.material.CircularProgressIndicator as WearCircularProgressIndicator
import androidx.wear.compose.material.Icon as WearIcon
import androidx.wear.compose.material.Scaffold as WearScaffold
import androidx.wear.compose.material.Text as WearText
import com.example.inmocontrol_v2.hid.HidClient
import com.example.inmocontrol_v2.hid.HidService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import android.content.pm.PackageManager
import kotlin.collections.LinkedHashMap

enum class DeviceType { BLE }

sealed class DeviceProfile {
    object InmoAir2 : DeviceProfile()
    object GenericHid : DeviceProfile()
}

fun identifyDeviceProfile(deviceName: String): DeviceProfile =
    if (deviceName.contains("Inmo", ignoreCase = true)) DeviceProfile.InmoAir2
    else DeviceProfile.GenericHid

fun hasGoodSignalStrength(rssi: Int): Boolean = rssi >= -70

// Optimized data class for device info
data class DeviceInfo(
    val name: String,
    val address: String,
    val rssi: Int,
    val type: DeviceType,
    val profile: DeviceProfile
)

@Composable
fun ConnectToDeviceScreenContent(
    state: ConnectionState,
    deviceList: List<DeviceInfo>,
    onScan: () -> Unit = {},
    onConnect: (String) -> Unit = {},
    onOpenControls: () -> Unit = {},
    onEnableBluetooth: () -> Unit = {}
) {
    WearScaffold {
        TimeText()
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                WearText("Connect Device", modifier = Modifier.padding(8.dp))
            }

            item {
                when (state) {
                    is ConnectionState.Idle -> {
                        WearButton(
                            onClick = onScan,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        ) {
                            WearText("Scan Devices")
                        }
                    }
                    is ConnectionState.Searching -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            WearText("Scanning...")
                            WearCircularProgressIndicator(modifier = Modifier.size(20.dp))
                        }
                    }
                    is ConnectionState.Connecting -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            WearText("Connecting...")
                            WearCircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                    is ConnectionState.Connected -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            WearText("Connected!")
                            LaunchedEffect(Unit) {
                                delay(500)
                                onOpenControls()
                            }
                        }
                    }
                    is ConnectionState.Error -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            WearText("Error: ${state.message}", modifier = Modifier.padding(8.dp))
                            if (state.message.contains("Bluetooth", ignoreCase = true)) {
                                WearButton(onClick = onEnableBluetooth) {
                                    WearText("Enable BT")
                                }
                            }
                            WearButton(onClick = onScan) {
                                WearText("Retry")
                            }
                        }
                    }
                }
            }

            if (state is ConnectionState.Searching && deviceList.isNotEmpty()) {
                items(minOf(deviceList.size, 5)) { idx ->
                    val device = deviceList[idx]
                    WearButton(
                        onClick = { onConnect(device.name) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            WearIcon(
                                if (device.profile is DeviceProfile.InmoAir2) Icons.Filled.Bluetooth
                                else Icons.Filled.BluetoothSearching,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            WearText(device.name, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectToDeviceScreen(onOpenControls: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val bluetoothManager = remember { context.getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager }
    val bluetoothAdapter = bluetoothManager.adapter

    var state by remember { mutableStateOf<ConnectionState>(ConnectionState.Idle) }
    var deviceMap by remember { mutableStateOf(LinkedHashMap<String, DeviceInfo>()) }
    var scanCallback by remember { mutableStateOf<ScanCallback?>(null) }
    var connectionReceiver by remember { mutableStateOf<BroadcastReceiver?>(null) }
    var scanJob by remember { mutableStateOf<Job?>(null) }

    val settingsStore = remember { com.example.inmocontrol_v2.data.SettingsStore.get(context) }

    // Improved cleanup function with better error handling
    val cleanup = remember {
        {
            try {
                // Cancel any ongoing scan job first
                scanJob?.cancel()
                scanJob = null

                // Stop BLE scanning
                scanCallback?.let { callback ->
                    try {
                        if (bluetoothAdapter?.isEnabled == true) {
                            bluetoothAdapter.bluetoothLeScanner?.stopScan(callback)
                            Log.d("ConnectScreen", "BLE scan stopped")
                        }
                    } catch (e: SecurityException) {
                        Log.w("ConnectScreen", "Permission denied stopping scan: ${e.message}")
                    } catch (e: Exception) {
                        Log.w("ConnectScreen", "Error stopping scan: ${e.message}")
                    }
                    scanCallback = null
                }

                // Unregister broadcast receiver
                connectionReceiver?.let { receiver ->
                    try {
                        context.unregisterReceiver(receiver)
                        Log.d("ConnectScreen", "Receiver unregistered")
                    } catch (e: Exception) {
                        Log.w("ConnectScreen", "Error unregistering receiver: ${e.message}")
                    }
                    connectionReceiver = null
                }
            } catch (e: Exception) {
                Log.e("ConnectScreen", "Cleanup error: ${e.message}")
            }
        }
    }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            Log.d("ConnectScreen", "Disposing ConnectToDeviceScreen")
            cleanup()
        }
    }

    // Improved permission checker
    val hasRequiredPermissions = remember {
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            } else {
                context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                context.checkSelfPermission(Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                context.checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
            }
        }
    }

    // Improved scan function with better settings and error handling
    val startScan: () -> Unit = remember {
        {
            // Prevent multiple scans
            if (state !is ConnectionState.Searching) {
                cleanup() // Clean up any previous state
                deviceMap.clear()
                state = ConnectionState.Searching

                Log.d("ConnectScreen", "Starting device scan")

                scanJob = scope.launch {
                    try {
                        // Check Bluetooth adapter
                        if (bluetoothAdapter?.isEnabled != true) {
                            state = ConnectionState.Error("Bluetooth not enabled")
                            return@launch
                        }

                        // Check permissions
                        if (!hasRequiredPermissions()) {
                            val missingPerms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                "BLUETOOTH_SCAN and BLUETOOTH_CONNECT"
                            } else {
                                "LOCATION and BLUETOOTH"
                            }
                            state = ConnectionState.Error("Missing permissions: $missingPerms")
                            return@launch
                        }

                        val scanner = bluetoothAdapter.bluetoothLeScanner
                        if (scanner == null) {
                            state = ConnectionState.Error("BLE scanner not available")
                            return@launch
                        }

                        // Improved scan settings for better performance and reliability
                        val scanSettings = ScanSettings.Builder()
                            .setScanMode(ScanSettings.SCAN_MODE_BALANCED) // Better than LOW_POWER
                            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE) // Find devices faster
                            .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
                            .setReportDelay(0) // Real-time results
                            .build()

                        // Create scan callback with improved filtering
                        scanCallback = object : ScanCallback() {
                            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                                result?.device?.let { device ->
                                    try {
                                        val name = device.name
                                        if (name.isNullOrBlank()) return

                                        // Improved signal strength check and device filtering
                                        if (result.rssi < -85) return // Too weak signal

                                        // Filter for relevant devices (you can customize this)
                                        val deviceNameLower = name.lowercase()
                                        val isRelevantDevice = deviceNameLower.contains("inmo") ||
                                                             deviceNameLower.contains("air") ||
                                                             deviceNameLower.contains("mouse") ||
                                                             deviceNameLower.contains("keyboard") ||
                                                             deviceNameLower.contains("hid")

                                        if (!isRelevantDevice) return

                                        val deviceInfo = DeviceInfo(
                                            name = name,
                                            address = device.address,
                                            rssi = result.rssi,
                                            type = DeviceType.BLE,
                                            profile = identifyDeviceProfile(name)
                                        )

                                        // Prevent memory issues with too many devices
                                        if (deviceMap.size >= 8) {
                                            deviceMap.remove(deviceMap.keys.first())
                                        }
                                        deviceMap[name] = deviceInfo

                                        Log.d("ConnectScreen", "Found device: $name (${result.rssi} dBm)")
                                    } catch (e: Exception) {
                                        Log.w("ConnectScreen", "Error processing scan result: ${e.message}")
                                    }
                                }
                            }

                            override fun onScanFailed(errorCode: Int) {
                                val errorMsg = when (errorCode) {
                                    ScanCallback.SCAN_FAILED_ALREADY_STARTED -> "Scan already started"
                                    ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "App registration failed"
                                    ScanCallback.SCAN_FAILED_INTERNAL_ERROR -> "Internal scanner error"
                                    ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED -> "Scan feature not supported"
                                    ScanCallback.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES -> "Out of hardware resources"
                                    ScanCallback.SCAN_FAILED_SCANNING_TOO_FREQUENTLY -> "Scanning too frequently"
                                    else -> "Unknown error: $errorCode"
                                }
                                Log.e("ConnectScreen", "Scan failed: $errorMsg")
                                state = ConnectionState.Error("Scan failed: $errorMsg")
                                cleanup()
                            }
                        }

                        // Start scanning
                        try {
                            scanner.startScan(null, scanSettings, scanCallback)
                            Log.d("ConnectScreen", "BLE scan started successfully")
                        } catch (e: SecurityException) {
                            state = ConnectionState.Error("Permission denied for scanning")
                            cleanup()
                            return@launch
                        } catch (e: Exception) {
                            state = ConnectionState.Error("Failed to start scan: ${e.message}")
                            cleanup()
                            return@launch
                        }

                        // Scan timeout with proper cleanup - reduced to 10 seconds
                        delay(10000)

                        // Stop scanning and check results
                        try {
                            scanner.stopScan(scanCallback)
                            scanCallback = null
                            Log.d("ConnectScreen", "Scan completed, found ${deviceMap.size} devices")
                        } catch (e: Exception) {
                            Log.w("ConnectScreen", "Error stopping scan: ${e.message}")
                        }

                        // Update state based on results
                        if (deviceMap.isEmpty()) {
                            state = ConnectionState.Error("No compatible devices found")
                        } else {
                            // Keep in searching state so devices are shown
                            Log.d("ConnectScreen", "Scan completed successfully with ${deviceMap.size} devices")
                        }

                    } catch (e: CancellationException) {
                        Log.d("ConnectScreen", "Scan cancelled")
                        cleanup()
                    } catch (e: Exception) {
                        Log.e("ConnectScreen", "Scan error: ${e.message}")
                        state = ConnectionState.Error("Scan error: ${e.message}")
                        cleanup()
                    }
                }
            } else {
                Log.w("ConnectScreen", "Scan already in progress")
            }
        }
    }

    val bluetoothEnableLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            scope.launch {
                delay(1000)
                startScan()
            }
        } else {
            state = ConnectionState.Error("Bluetooth required")
        }
    }

    // Improved connection function with better error handling
    val connectToDevice: (String) -> Unit = remember {
        { deviceName: String ->
            if (state !is ConnectionState.Connecting) {
                scope.launch {
                    try {
                        val deviceInfo = deviceMap[deviceName]
                        if (deviceInfo == null) {
                            state = ConnectionState.Error("Device not found")
                            return@launch
                        }

                        cleanup() // Clean up scanning
                        state = ConnectionState.Connecting
                        Log.d("ConnectScreen", "Connecting to device: $deviceName")

                        // Check permissions again
                        if (!hasRequiredPermissions()) {
                            state = ConnectionState.Error("Missing connection permissions")
                            return@launch
                        }

                        val device = bluetoothAdapter?.getRemoteDevice(deviceInfo.address)
                        if (device == null) {
                            state = ConnectionState.Error("Cannot get device")
                            return@launch
                        }

                        // Set device profile and save settings
                        HidClient.currentDeviceProfile = deviceInfo.profile
                        settingsStore.setLastConnectedDevice(deviceName, device.address)

                        // Setup connection state receiver
                        connectionReceiver = object : BroadcastReceiver() {
                            override fun onReceive(ctx: Context?, intent: Intent?) {
                                when (intent?.action) {
                                    HidService.ACTION_CONNECTION_STATE -> {
                                        val connected = intent.getBooleanExtra(HidService.EXTRA_CONNECTED, false)
                                        val deviceAddr = intent.getStringExtra(HidService.EXTRA_DEVICE)
                                        val error = intent.getStringExtra(HidService.EXTRA_ERROR)

                                        if (deviceAddr == device.address) {
                                            if (connected) {
                                                Log.d("ConnectScreen", "Device connected successfully")
                                                state = ConnectionState.Connected(deviceName)
                                                cleanup()
                                            } else if (error != null) {
                                                Log.e("ConnectScreen", "Connection failed: $error")
                                                state = ConnectionState.Error(error)
                                                cleanup()
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        context.registerReceiver(connectionReceiver, IntentFilter(HidService.ACTION_CONNECTION_STATE))

                        // Attempt connection
                        HidClient.connect(device)

                        // Connection timeout - reduced to 15 seconds
                        delay(15000)
                        if (state is ConnectionState.Connecting) {
                            Log.w("ConnectScreen", "Connection timeout")
                            state = ConnectionState.Error("Connection timeout - device may be out of range")
                            cleanup()
                        }

                    } catch (e: CancellationException) {
                        Log.d("ConnectScreen", "Connection cancelled")
                        cleanup()
                    } catch (e: Exception) {
                        Log.e("ConnectScreen", "Connection error: ${e.message}")
                        state = ConnectionState.Error("Connection failed: ${e.message}")
                        cleanup()
                    }
                }
            } else {
                Log.w("ConnectScreen", "Already connecting")
            }
        }
    }

    ConnectToDeviceScreenContent(
        state = state,
        deviceList = deviceMap.values.toList(),
        onScan = startScan,
        onConnect = connectToDevice,
        onOpenControls = onOpenControls,
        onEnableBluetooth = {
            bluetoothEnableLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }
    )
}
