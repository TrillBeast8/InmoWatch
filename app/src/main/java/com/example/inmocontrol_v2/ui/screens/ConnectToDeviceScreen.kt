package com.example.inmocontrol_v2.ui.screens

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.*
import com.example.inmocontrol_v2.data.BluetoothSettingsStore
import kotlinx.coroutines.launch

/**
 * Optimized ConnectToDeviceScreen with a ViewModel for state management (2025)
 */
@Composable
fun ConnectToDeviceScreen(
    onBack: () -> Unit,
    viewModel: ConnectViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            viewModel.startScan()
        }
    }

    // Lifecycle events
    LaunchedEffect(Unit) {
        viewModel.registerReceiver()
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        permissionLauncher.launch(requiredPermissions)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.unregisterReceiver()
        }
    }

    Scaffold(
        timeText = { TimeText(modifier = Modifier.padding(top = 8.dp)) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 24.dp, start = 16.dp, end = 16.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Text("Connect to Device", style = MaterialTheme.typography.title3)
            }

            // Bluetooth status
            if (!uiState.bluetoothEnabled) {
                item { BluetoothDisabledState(context) }
            } else {
                // Scanning status
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Scan", style = MaterialTheme.typography.body1)
                        if (uiState.isScanning) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Button(onClick = { viewModel.startScan() }) {
                                Text("Start")
                            }
                        }
                    }
                }

                // Discovered devices
                if (uiState.discoveredDevices.isNotEmpty()) {
                    item { ListHeader { Text("Discovered") } }
                    items(uiState.discoveredDevices) { device ->
                        DeviceChip(device = device) {
                            viewModel.connect(device)
                            scope.launch {
                                BluetoothSettingsStore.saveLastDevice(context, device)
                                onBack()
                            }
                        }
                    }
                }

                // Paired devices
                if (uiState.pairedDevices.isNotEmpty()) {
                    item { ListHeader { Text("Paired") } }
                    items(uiState.pairedDevices) { device ->
                        DeviceChip(device = device) {
                            viewModel.connect(device)
                            scope.launch {
                                BluetoothSettingsStore.saveLastDevice(context, device)
                                onBack()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceChip(device: BluetoothDevice, onClick: () -> Unit) {
    Chip(
        modifier = Modifier.fillMaxWidth(),
        icon = { Icon(Icons.Default.Bluetooth, contentDescription = null) },
        label = {
            Text(
                text = try {
                    device.name ?: "Unknown Device"
                } catch (e: SecurityException) {
                    "Permission Denied"
                }
            )
        },
        onClick = onClick
    )
}

@Composable
private fun BluetoothDisabledState(context: Context) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.BluetoothDisabled,
            contentDescription = "Bluetooth Disabled",
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Bluetooth is disabled.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.body1
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            try {
                context.startActivity(intent)
            } catch (_: SecurityException) {
                // Permission may be denied at runtime; ignore and keep UI state
            }
        }) {
            Text("Enable")
        }
    }
}
