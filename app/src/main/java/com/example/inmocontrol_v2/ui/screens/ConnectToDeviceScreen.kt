package com.example.inmocontrol_v2.ui.screens

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
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
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.*
import com.example.inmocontrol_v2.hid.HidClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Simple and working ConnectToDeviceScreen for Wear OS
 * Fixed permission handling and simplified UI flow
 */
@Composable
fun ConnectToDeviceScreen(
    onDeviceConnected: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Bluetooth state management
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val bluetoothAdapter = bluetoothManager.adapter

    // Connection states from HidClient
    val isConnected by HidClient.isConnected.collectAsState()
    val isServiceReady by HidClient.isServiceReady.collectAsState()
    val connectionError by HidClient.connectionError.collectAsState()

    // Local UI states
    var bluetoothEnabled by remember { mutableStateOf(bluetoothAdapter?.isEnabled == true) }
    var pairedDevices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var discoveredDevices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var showPermissionPrompt by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }

    // Helper functions
    fun loadPairedDevices() {
        if (!bluetoothEnabled) return
        try {
            val bondedDevices = bluetoothAdapter?.bondedDevices
            Log.d("ConnectScreen", "Found ${bondedDevices?.size ?: 0} bonded devices")

            // Get all bonded devices and filter out any null values
            val devices = bondedDevices?.filterNotNull() ?: emptyList()

            // Log each device for debugging with more details
            devices.forEach { device ->
                try {
                    val deviceName = device.name ?: "Unknown Device"
                    val deviceType = when (device.type) {
                        BluetoothDevice.DEVICE_TYPE_CLASSIC -> "Classic"
                        BluetoothDevice.DEVICE_TYPE_LE -> "Low Energy"
                        BluetoothDevice.DEVICE_TYPE_DUAL -> "Dual Mode"
                        else -> "Unknown"
                    }
                    val bondState = when (device.bondState) {
                        BluetoothDevice.BOND_BONDED -> "Bonded"
                        BluetoothDevice.BOND_BONDING -> "Bonding"
                        BluetoothDevice.BOND_NONE -> "Not Bonded"
                        else -> "Unknown"
                    }
                    Log.d("ConnectScreen", "Bonded Device: $deviceName - ${device.address} - Type: $deviceType - Bond: $bondState")
                } catch (e: SecurityException) {
                    Log.d("ConnectScreen", "Bonded Device: Permission denied - ${device.address}")
                }
            }

            // Include ALL bonded devices regardless of type
            pairedDevices = devices
            showPermissionPrompt = false

            Log.d("ConnectScreen", "Loaded ${devices.size} bonded devices into UI")
        } catch (e: SecurityException) {
            // Permissions not granted, show prompt
            Log.w("ConnectScreen", "Security exception loading paired devices", e)
            showPermissionPrompt = true
        } catch (e: Exception) {
            Log.e("ConnectScreen", "Error loading paired devices", e)
            HidClient.setConnectionError("Failed to load devices: ${e.message}")
        }
    }

    fun startDeviceDiscovery() {
        if (!bluetoothEnabled) return
        try {
            if (isScanning) {
                bluetoothAdapter?.cancelDiscovery()
                return
            }

            discoveredDevices = emptyList()
            Log.d("ConnectScreen", "Starting Bluetooth device discovery...")

            val success = bluetoothAdapter?.startDiscovery() ?: false
            if (success) {
                isScanning = true
                Log.d("ConnectScreen", "Bluetooth discovery started successfully")
            } else {
                Log.w("ConnectScreen", "Failed to start Bluetooth discovery")
                HidClient.setConnectionError("Failed to start device discovery")
            }
        } catch (e: SecurityException) {
            Log.w("ConnectScreen", "Permission denied for device discovery", e)
            showPermissionPrompt = true
        } catch (e: Exception) {
            Log.e("ConnectScreen", "Error starting device discovery", e)
            HidClient.setConnectionError("Discovery error: ${e.message}")
        }
    }

    fun connectToDevice(device: BluetoothDevice) {
        scope.launch {
            try {
                HidClient.clearError()
                val success = HidClient.connect(device)
                if (success) {
                    delay(500)
                    if (HidClient.isConnected.value) {
                        onDeviceConnected()
                    }
                }
            } catch (e: Exception) {
                HidClient.setConnectionError("Connection failed: ${e.message}")
            }
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            showPermissionPrompt = false
            loadPairedDevices()
        }
    }

    fun requestPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
        permissionLauncher.launch(permissions)
    }

    // Monitor Bluetooth state
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    bluetoothEnabled = state == BluetoothAdapter.STATE_ON
                    if (bluetoothEnabled) {
                        loadPairedDevices()
                    }
                } else if (intent.action == BluetoothDevice.ACTION_FOUND) {
                    // Discovery result
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    if (device != null && !pairedDevices.contains(device)) {
                        // Add to discovered devices if not already paired
                        discoveredDevices = discoveredDevices + device
                        Log.d("ConnectScreen", "Discovered device: ${device.name} - ${device.address}")
                    }
                } else if (intent.action == BluetoothAdapter.ACTION_DISCOVERY_FINISHED) {
                    // Discovery finished
                    isScanning = false
                    Log.d("ConnectScreen", "Bluetooth discovery finished")
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        context.registerReceiver(receiver, filter)
        onDispose { context.unregisterReceiver(receiver) }
    }

    // Initialize
    LaunchedEffect(Unit) {
        loadPairedDevices()
    }

    // Main UI
    Scaffold(
        timeText = { TimeText() },
        modifier = Modifier.fillMaxSize()
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            item {
                Text(
                    text = "Connect Device",
                    style = MaterialTheme.typography.title3,
                    textAlign = TextAlign.Center
                )
            }

            // Connection status
            item {
                Card(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            if (isConnected) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                            contentDescription = null,
                            tint = if (isConnected) Color.Green else MaterialTheme.colors.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = when {
                                isConnected -> "Device Connected"
                                isServiceReady -> "Ready to Connect"
                                else -> "Starting Service..."
                            },
                            style = MaterialTheme.typography.body1,
                            textAlign = TextAlign.Center
                        )

                        connectionError?.let { error ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = error,
                                style = MaterialTheme.typography.caption1,
                                color = Color.Red,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Handle different states
            when {
                !bluetoothEnabled -> {
                    item {
                        Card(
                            onClick = { },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.BluetoothDisabled,
                                    contentDescription = null,
                                    tint = Color.Red,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Bluetooth is disabled",
                                    style = MaterialTheme.typography.body2,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Enable Bluetooth to continue",
                                    style = MaterialTheme.typography.caption1,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                showPermissionPrompt -> {
                    item {
                        Button(
                            onClick = { requestPermissions() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Grant Bluetooth Permission")
                        }
                    }
                    item {
                        Text(
                            text = "Bluetooth permissions are required to connect to devices",
                            style = MaterialTheme.typography.caption1,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                isConnected -> {
                    item {
                        Button(
                            onClick = {
                                HidClient.disconnect()
                                HidClient.refreshConnectionState()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red)
                        ) {
                            Text("Disconnect", color = Color.White)
                        }
                    }
                    item {
                        Button(
                            onClick = { onDeviceConnected() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Use Controls")
                        }
                    }
                }

                else -> {
                    // Show paired devices
                    if (pairedDevices.isNotEmpty()) {
                        item {
                            Text(
                                text = "Paired Devices",
                                style = MaterialTheme.typography.title3,
                                textAlign = TextAlign.Center
                            )
                        }

                        items(pairedDevices) { device ->
                            Chip(
                                onClick = { connectToDevice(device) },
                                label = {
                                    Text(
                                        text = try {
                                            device.name ?: "Unknown Device"
                                        } catch (e: SecurityException) {
                                            "Unknown Device"
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                secondaryLabel = {
                                    Text(
                                        text = "Tap to connect",
                                        style = MaterialTheme.typography.caption1
                                    )
                                },
                                icon = {
                                    Icon(
                                        Icons.Default.Bluetooth,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                enabled = isServiceReady
                            )
                        }
                    } else {
                        item {
                            Card(
                                onClick = { },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.Settings,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp),
                                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No Paired Devices",
                                        style = MaterialTheme.typography.body2,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "Pair a device in phone Bluetooth settings first",
                                        style = MaterialTheme.typography.caption1,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }

                    // Show discovered devices from scanning
                    if (discoveredDevices.isNotEmpty()) {
                        item {
                            Text(
                                text = "Discovered Devices",
                                style = MaterialTheme.typography.title3,
                                textAlign = TextAlign.Center
                            )
                        }

                        items(discoveredDevices) { device ->
                            Chip(
                                onClick = { connectToDevice(device) },
                                label = {
                                    Text(
                                        text = try {
                                            device.name ?: "Unknown Device"
                                        } catch (e: SecurityException) {
                                            "Unknown Device"
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                secondaryLabel = {
                                    Text(
                                        text = "Tap to pair & connect",
                                        style = MaterialTheme.typography.caption1
                                    )
                                },
                                icon = {
                                    Icon(
                                        Icons.Default.Bluetooth,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                enabled = true // Always enable discovered device selection
                            )
                        }
                    }

                    // Discovery button
                    item {
                        Button(
                            onClick = { startDeviceDiscovery() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = if (isScanning)
                                ButtonDefaults.buttonColors(backgroundColor = Color.Red)
                                else ButtonDefaults.buttonColors()
                        ) {
                            if (isScanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Cancel Scan", color = Color.White)
                            } else {
                                Text("Discover Devices")
                            }
                        }
                    }

                    // Refresh paired devices button
                    item {
                        Button(
                            onClick = { loadPairedDevices() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Refresh Paired")
                        }
                    }
                }
            }
        }
    }
}
