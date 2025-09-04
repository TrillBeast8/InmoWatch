package com.example.inmocontrol_v2.ui.screens

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.*
import androidx.wear.compose.material.Button as WearButton
import androidx.wear.compose.material.CircularProgressIndicator as WearCircularProgressIndicator
import androidx.wear.compose.material.Icon as WearIcon
import androidx.wear.compose.material.Scaffold as WearScaffold
import androidx.wear.compose.material.Text as WearText
import com.example.inmocontrol_v2.hid.HidClient
import com.example.inmocontrol_v2.hid.HidService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.content.pm.PackageManager
import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder

enum class DeviceType { BLE, CLASSIC }

sealed class DeviceProfile {
    object InmoAir2 : DeviceProfile()
    object GenericHid : DeviceProfile()
}

fun identifyDeviceProfile(deviceName: String): DeviceProfile {
    if (deviceName.contains("Inmo Air 2", ignoreCase = true)) return DeviceProfile.InmoAir2
    return DeviceProfile.GenericHid
}

// Smart filter function to identify devices with good signal strength
fun hasGoodSignalStrength(rssi: Int): Boolean {
    // RSSI values are negative, closer to 0 means stronger signal
    // Typical ranges: -30 to -50 = excellent, -50 to -70 = good, -70 to -90 = fair, below -90 = poor
    return rssi >= -70 // Improved threshold for better device detection
}

// Keep track of seen devices to prevent duplicates
data class DeviceInfo(
    val name: String,
    val address: String,
    val rssi: Int,
    val type: DeviceType,
    val lastSeen: Long = System.currentTimeMillis()
)

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
    onOpenControls: () -> Unit = {},
    onEnableBluetooth: () -> Unit = {}
) {
    WearScaffold {
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
                            WearButton(
                                onClick = onScan,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                WearText("Scan for Devices", modifier = Modifier.padding(8.dp))
                            }
                        }
                    }
                    is ConnectionState.Searching -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            WearText("Scanning for devices...", modifier = Modifier.padding(bottom = 4.dp))
                            WearCircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    is ConnectionState.Connecting -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            WearText("Connecting to device...", modifier = Modifier.padding(vertical = 8.dp))
                            WearCircularProgressIndicator(modifier = Modifier.size(32.dp))
                        }
                    }
                    is ConnectionState.Connected -> {
                        val connectedState = state
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            WearText("Connected to ${connectedState.deviceName}", modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                            WearText("Opening controls...", modifier = Modifier.padding(vertical = 8.dp))
                            WearCircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }

                        // Auto-navigate immediately when connected
                        LaunchedEffect(connectedState.deviceName) {
                            delay(1000) // Brief delay to show connection success
                            onOpenControls()
                        }
                    }
                    is ConnectionState.Error -> {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                WearText("Error: ${state.message}", modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                                Spacer(modifier = Modifier.height(16.dp))

                                // Show "Enable Bluetooth" button for Bluetooth-related errors
                                if (state.message.contains("Bluetooth", ignoreCase = true)) {
                                    WearButton(
                                        onClick = onEnableBluetooth,
                                        modifier = Modifier
                                            .widthIn(max = 150.dp)
                                            .padding(vertical = 4.dp)
                                    ) {
                                        WearText("Enable Bluetooth")
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                WearButton(
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
            item {
                when (state) {
                    is ConnectionState.Searching -> {
                        // Show device list
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
                                        WearButton(
                                            onClick = {
                                                if (!isPaired) onPair(name)
                                                onConnect(name)
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (isConnected) {
                                                    WearIcon(Icons.Filled.Bluetooth, contentDescription = "Connected", modifier = Modifier.size(24.dp))
                                                } else if (isPaired) {
                                                    WearIcon(Icons.Filled.Bluetooth, contentDescription = "Paired", modifier = Modifier.size(24.dp))
                                                } else {
                                                    WearIcon(Icons.Filled.BluetoothSearching, contentDescription = "Discovered", modifier = Modifier.size(24.dp))
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                WearText(name, modifier = Modifier.weight(1f))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                // WearText("RSSI: $rssi", modifier = Modifier)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else -> { /* Do nothing */ }
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
    val coroutineScope = rememberCoroutineScope()
    val connectionStateViewModel: ConnectionStateViewModel = viewModel()
    var scanCallback: ScanCallback? by remember { mutableStateOf(null) }
    var scanRequested by remember { mutableStateOf(false) }
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
    val bluetoothAdapter = bluetoothManager.adapter
    var state by remember { mutableStateOf<ConnectionState>(ConnectionState.Idle) }
    var deviceList by remember { mutableStateOf(listOf<Triple<String, Int, DeviceType>>()) }
    var deviceMap by remember { mutableStateOf(mapOf<String, BluetoothDevice>()) }
    var deviceProfileMap by remember { mutableStateOf(mapOf<String, DeviceProfile>()) }
    var scanDebugInfo by remember { mutableStateOf("Ready to scan") }

    // Settings store for device memory
    val settingsStore = remember { com.example.inmocontrol_v2.data.SettingsStore.get(context) }
    val lastDeviceName by settingsStore.lastConnectedDeviceName.collectAsState(initial = null)
    val lastDeviceAddress by settingsStore.lastConnectedDeviceAddress.collectAsState(initial = null)
    val autoReconnectEnabled by settingsStore.autoReconnectEnabled.collectAsState(initial = true)

    // Bluetooth enable launcher - NEW FEATURE
    val bluetoothEnableLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            // Bluetooth was enabled successfully, start scanning automatically
            coroutineScope.launch {
                delay(1000) // Brief delay to ensure Bluetooth is fully enabled
                scanRequested = true
            }
        } else {
            // User declined to enable Bluetooth
            state = ConnectionState.Error("Bluetooth is required for device connection")
        }
    }

    // NEW: Enhanced connection function with validation
    val connectToDeviceWithValidation: (BluetoothDevice, String, HidService?, ConnectionStateViewModel, com.example.inmocontrol_v2.data.SettingsStore, () -> Unit, (String) -> Unit, () -> Unit) -> Unit = { device, deviceName, hidService, connectionStateViewModel, settingsStore, onSuccess, onError, onTimeout ->
        coroutineScope.launch {
            try {
                // Establish actual GATT connection
                hidService?.connect(device)

                // Set device profile for HidClient
                HidClient.currentDeviceProfile = deviceProfileMap[deviceName]

                // Save device for auto-reconnect
                settingsStore.setLastConnectedDevice(deviceName, device.address)

                // FIXED: Use connection validation with timeout instead of fixed delay
                connectionStateViewModel.validateConnection(
                    connectionCheck = {
                        // Check if HidService reports actual connection
                        hidService?.isDeviceConnected() == true
                    },
                    onSuccess = {
                        Log.d("ConnectToDeviceScreen", "Connection validated successfully")
                        connectionStateViewModel.setConnected()
                        onSuccess()
                    },
                    onTimeout = {
                        Log.w("ConnectToDeviceScreen", "Connection validation timed out")
                        connectionStateViewModel.setError("Connection timeout")
                        onTimeout()
                    },
                    onError = { errorMessage ->
                        Log.e("ConnectToDeviceScreen", "Connection validation failed: $errorMessage")
                        connectionStateViewModel.setError(errorMessage)
                        onError(errorMessage)
                    }
                )

            } catch (e: Exception) {
                Log.e("ConnectToDeviceScreen", "Connection failed with exception: ${e.message}")
                connectionStateViewModel.setError("Connection failed: ${e.message}")
                onError("Connection failed: ${e.message}")
            }
        }
    }

    // Enhanced function to start scanning for devices
    val startScanning: () -> Unit = remember {
        {
            coroutineScope.launch {
                scanDebugInfo = "Starting scan..."
                deviceList = emptyList()
                deviceMap = emptyMap()
                deviceProfileMap = emptyMap()
                state = ConnectionState.Searching

                // FIXED: Check if Bluetooth is enabled and request enable if not
                if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                    scanDebugInfo = "Bluetooth not enabled"

                    // NEW: Request Bluetooth enable instead of showing error
                    if (bluetoothAdapter != null) {
                        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                        bluetoothEnableLauncher.launch(enableBtIntent)
                        return@launch
                    } else {
                        state = ConnectionState.Error("Bluetooth adapter not available")
                        return@launch
                    }
                }

                val hasPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                    context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                } else {
                    context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                }

                if (!hasPermissions) {
                    scanDebugInfo = "Missing permissions"
                    state = ConnectionState.Error("Bluetooth permissions not granted")
                    return@launch
                }

                // Start BLE scan
                val scanner = bluetoothAdapter.bluetoothLeScanner
                if (scanner != null) {
                    try {
                        scanCallback = object : ScanCallback() {
                            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                                result?.device?.let { device ->
                                    val rawName = try {
                                        device.name
                                    } catch (_: SecurityException) { null }
                                    val name = rawName?.takeIf { it.isNotBlank() } ?: "Unknown (${device.address ?: "NoAddr"})"
                                    val rssi = result.rssi
                                    val profile = identifyDeviceProfile(name)

                                    // Apply smart filter and prevent duplicates using device address
                                    if (hasGoodSignalStrength(rssi)) {
                                        coroutineScope.launch(Dispatchers.Main) {
                                            val deviceAddress = device.address
                                            val existingDeviceIndex = deviceList.indexOfFirst { (_, _, _) ->
                                                val existingDevice = deviceMap.values.find { it.address == deviceAddress }
                                                existingDevice != null
                                            }

                                            if (existingDeviceIndex == -1) {
                                                deviceList = deviceList + Triple(name, rssi, DeviceType.BLE)
                                                deviceMap = deviceMap + (name to device)
                                                deviceProfileMap = deviceProfileMap + (name to profile)
                                                scanDebugInfo = "BLE found: $name (RSSI: $rssi)"
                                            }
                                        }
                                    }
                                }
                            }

                            override fun onScanFailed(errorCode: Int) {
                                coroutineScope.launch(Dispatchers.Main) {
                                    scanDebugInfo = "BLE Scan failed: $errorCode"
                                    if (errorCode != ScanCallback.SCAN_FAILED_ALREADY_STARTED) {
                                        state = ConnectionState.Error("BLE Scan failed: $errorCode")
                                    }
                                }
                            }
                        }

                        val settings = android.bluetooth.le.ScanSettings.Builder()
                            .setScanMode(android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY)
                            .build()

                        scanner.startScan(null, settings, scanCallback!!)
                        scanDebugInfo = "BLE scan started"

                        coroutineScope.launch {
                            delay(30000)
                            if (scanCallback != null) {
                                try {
                                    scanner.stopScan(scanCallback!!)
                                    scanDebugInfo = "BLE scan stopped after timeout"
                                } catch (e: Exception) {
                                    scanDebugInfo = "Error stopping BLE scan: ${e.message}"
                                }
                                scanCallback = null

                                if (bluetoothAdapter.isDiscovering != true) {
                                    state = ConnectionState.Idle
                                }
                            }
                        }
                    } catch (e: Exception) {
                        scanDebugInfo = "Error starting BLE scan: ${e.message}"
                        state = ConnectionState.Error("Failed to start BLE scan: ${e.message}")
                    }
                } else {
                    scanDebugInfo = "BLE scanner not available"
                }

                // Start classic Bluetooth discovery
                try {
                    if (bluetoothAdapter.isDiscovering) {
                        bluetoothAdapter.cancelDiscovery()
                    }
                    val started = bluetoothAdapter.startDiscovery()
                    scanDebugInfo = "Classic discovery " + (if (started) "started" else "failed")
                    if (!started) {
                        if (scanner == null) {
                            state = ConnectionState.Error("Failed to start Bluetooth discovery")
                        }
                    }
                } catch (e: Exception) {
                    scanDebugInfo = "Error with classic discovery: ${e.message}"
                    if (scanner == null) {
                        state = ConnectionState.Error("Error with Bluetooth discovery: ${e.message}")
                    }
                }
            }
        }
    }

    // HidService connection
    var hidService by remember { mutableStateOf<HidService?>(null) }
    val serviceConnection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(className: ComponentName, service: IBinder) {
                val binder = service as HidService.LocalBinder
                hidService = binder.getService()
                HidClient.setService(hidService)

                // Try auto-reconnect if enabled and we have a saved device
                val deviceName = lastDeviceName
                val deviceAddress = lastDeviceAddress
                if (autoReconnectEnabled && deviceName != null && deviceAddress != null) {
                    coroutineScope.launch {
                        delay(1000) // Brief delay to ensure service is ready
                        try {
                            val pairedDevices = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                                    bluetoothAdapter?.bondedDevices
                                } else {
                                    emptySet()
                                }
                            } else {
                                bluetoothAdapter?.bondedDevices
                            }
                            val savedDevice = pairedDevices?.find { it.address == deviceAddress }

                            if (savedDevice != null) {
                                state = ConnectionState.Connecting
                                connectionStateViewModel.setConnecting()

                                // Use enhanced connection with validation
                                connectToDeviceWithValidation(
                                    savedDevice,
                                    deviceName,
                                    hidService,
                                    connectionStateViewModel,
                                    settingsStore,
                                    {
                                        state = ConnectionState.Connected(deviceName)
                                        coroutineScope.launch {
                                            delay(1000)
                                            onOpenControls()
                                        }
                                    },
                                    { errorMessage: String ->
                                        state = ConnectionState.Error("Auto-reconnect failed: $errorMessage")
                                    },
                                    {
                                        state = ConnectionState.Error("Auto-reconnect timed out")
                                    }
                                )
                            }
                        } catch (e: Exception) {
                            // Auto-reconnect failed, user will need to manually connect
                            state = ConnectionState.Idle
                        }
                    }
                }
            }

            override fun onServiceDisconnected(arg0: ComponentName) {
                hidService = null
                HidClient.setService(null)
            }
        }
    }

    // Bind to HidService
    DisposableEffect(Unit) {
        val intent = Intent(context, HidService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        onDispose {
            context.unbindService(serviceConnection)
        }
    }

    // Classic Bluetooth BroadcastReceiver
    val classicReceiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= 33) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                        val rssi: Int = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()
                        val rawName = try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                if (context != null && context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                                    device?.name
                                } else {
                                    null
                                }
                            } else {
                                device?.name
                            }
                        } catch (_: SecurityException) { null }
                        val name = rawName?.takeIf { it.isNotBlank() } ?: "Unknown (${device?.address ?: "NoAddr"})"

                        // Apply smart filter and prevent duplicates using device address
                        if (device != null && hasGoodSignalStrength(rssi)) {
                            val deviceAddress = device.address
                            val existingDeviceIndex = deviceList.indexOfFirst { (_, _, _) ->
                                val existingDevice = deviceMap.values.find { it.address == deviceAddress }
                                existingDevice != null
                            }

                            if (existingDeviceIndex == -1) {
                                deviceList = deviceList + Triple(name, rssi, DeviceType.CLASSIC)
                                deviceMap = deviceMap + (name to device)
                                deviceProfileMap = deviceProfileMap + (name to identifyDeviceProfile(name))
                                scanDebugInfo = "Classic found: $name (RSSI: $rssi)"
                            }
                        }
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        scanDebugInfo = "Classic discovery finished"
                        if (scanCallback == null && state is ConnectionState.Searching) {
                            state = ConnectionState.Idle
                        }
                    }
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        val bluetoothState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                        when (bluetoothState) {
                            BluetoothAdapter.STATE_ON -> {
                                scanDebugInfo = "Bluetooth turned ON"
                                if (scanRequested) {
                                    startScanning()
                                    scanRequested = false
                                }
                            }
                            BluetoothAdapter.STATE_OFF -> {
                                scanDebugInfo = "Bluetooth turned OFF"
                            }
                        }
                    }
                }
            }
        }
    }

    // Register for ALL relevant Bluetooth events
    DisposableEffect(Unit) {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }
        context.registerReceiver(classicReceiver, filter)
        onDispose {
            try {
                context.unregisterReceiver(classicReceiver)
                if (bluetoothAdapter?.isDiscovering == true) {
                    if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                        bluetoothAdapter.cancelDiscovery()
                    }
                }
                scanCallback?.let { callback ->
                    if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                        bluetoothAdapter?.bluetoothLeScanner?.stopScan(callback)
                    }
                }
            } catch (e: Exception) {
                // Handle any exceptions during cleanup
            }
        }
    }

    // Permission handling with LaunchedEffect
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    var permissionsRequested by remember { mutableStateOf(false) }
    val allGranted = permissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (permissions.all { result[it] == true }) {
            if (scanRequested) {
                startScanning()
                scanRequested = false
            }
        }
    }

    LaunchedEffect(allGranted, permissionsRequested, scanRequested) {
        if (!allGranted && !permissionsRequested && context is android.app.Activity) {
            permissionsRequested = true
            launcher.launch(permissions)
        } else if (allGranted && scanRequested) {
            startScanning()
            scanRequested = false
        }
    }

    // UI Content
    WearScaffold {
        TimeText()
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                WearText("Connect to Device", modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Show debug info
            item {
                if (scanDebugInfo.isNotEmpty()) {
                    WearText("Status: $scanDebugInfo",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.caption2
                    )
                }
            }

            item {
                when (state) {
                    is ConnectionState.Idle -> {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            WearButton(
                                onClick = {
                                    scanRequested = true
                                    startScanning()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                WearText("Scan for Devices", modifier = Modifier.padding(8.dp))
                            }
                        }
                    }
                    is ConnectionState.Searching -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            WearText("Scanning for devices...", modifier = Modifier.padding(bottom = 4.dp))
                            WearCircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    is ConnectionState.Connecting -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            WearText("Connecting to device...", modifier = Modifier.padding(vertical = 8.dp))
                            WearCircularProgressIndicator(modifier = Modifier.size(32.dp))
                        }
                    }
                    is ConnectionState.Connected -> {
                        val connectedState = state as ConnectionState.Connected
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            WearText("Connected to ${connectedState.deviceName}", modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                            WearText("Opening controls...", modifier = Modifier.padding(vertical = 8.dp))
                            WearCircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }

                        // Auto-navigate immediately when connected
                        LaunchedEffect(connectedState.deviceName) {
                            delay(1000) // Brief delay to show connection success
                            onOpenControls()
                        }
                    }
                    is ConnectionState.Error -> {
                        val errorState = state as ConnectionState.Error
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                WearText("Error: ${errorState.message}",
                                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                // FIXED: Show "Enable Bluetooth" button for Bluetooth-related errors
                                if (errorState.message.contains("Bluetooth", ignoreCase = true) &&
                                    bluetoothAdapter != null && !bluetoothAdapter.isEnabled) {
                                    WearButton(
                                        onClick = {
                                            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                                            bluetoothEnableLauncher.launch(enableBtIntent)
                                        },
                                        modifier = Modifier
                                            .widthIn(max = 150.dp)
                                            .padding(vertical = 4.dp)
                                    ) {
                                        WearText("Enable Bluetooth")
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                WearButton(
                                    onClick = {
                                        scanRequested = true
                                        startScanning()
                                    },
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

            // Show device list only when scanning and devices are found
            if (state is ConnectionState.Searching && deviceList.isNotEmpty()) {
                items(deviceList.size) { idx ->
                    val deviceTriple = deviceList[idx]
                    val (name, rssi, _) = deviceTriple
                    val isPaired = deviceMap[name]?.bondState == BluetoothDevice.BOND_BONDED

                    WearButton(
                        onClick = {
                            // Stop scanning when user selects a device
                            scanCallback?.let { callback ->
                                try {
                                    bluetoothAdapter?.bluetoothLeScanner?.stopScan(callback)
                                } catch (e: Exception) {
                                    // Ignore errors when stopping scan
                                }
                            }
                            if (bluetoothAdapter?.isDiscovering == true) {
                                try {
                                    bluetoothAdapter.cancelDiscovery()
                                } catch (e: Exception) {
                                    // Ignore errors when canceling discovery
                                }
                            }

                            if (!isPaired) {
                                try {
                                    deviceMap[name]?.createBond()
                                    state = ConnectionState.Connecting
                                } catch (e: Exception) {
                                    // Continue to connect even if pairing fails
                                }
                            }

                            val device = deviceMap[name]
                            if (device != null) {
                                state = ConnectionState.Connecting
                                connectionStateViewModel.setConnecting()

                                // FIXED: Use enhanced connection with validation
                                connectToDeviceWithValidation(
                                    device,
                                    name,
                                    hidService,
                                    connectionStateViewModel,
                                    settingsStore,
                                    {
                                        state = ConnectionState.Connected(name)
                                        coroutineScope.launch {
                                            delay(1000)
                                            onOpenControls()
                                        }
                                    },
                                    { errorMessage: String ->
                                        state = ConnectionState.Error("Connection failed: $errorMessage")
                                    },
                                    {
                                        state = ConnectionState.Error("Connection timed out after 5 seconds")
                                    }
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isPaired) {
                                WearIcon(Icons.Filled.Bluetooth, contentDescription = "Paired", modifier = Modifier.size(20.dp))
                            } else {
                                WearIcon(Icons.Filled.BluetoothSearching, contentDescription = "Discovered", modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            WearText(name, modifier = Modifier.weight(1f))
                            Spacer(modifier = Modifier.width(4.dp))
                            WearText("$rssi dBm", style = MaterialTheme.typography.caption2)
                        }
                    }
                }
            }
        }
    }
}
