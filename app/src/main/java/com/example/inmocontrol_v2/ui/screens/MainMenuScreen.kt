package com.example.inmocontrol_v2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.*
import com.example.inmocontrol_v2.data.BluetoothSettingsStore
import com.example.inmocontrol_v2.hid.HidClient

/**
 * Main menu screen, optimized for performance and clarity (2025)
 */
@Composable
fun MainMenuScreen(
    onNavigateToConnect: () -> Unit,
    onNavigateToMouse: () -> Unit,
    onNavigateToTouchpad: () -> Unit,
    onNavigateToKeyboard: () -> Unit,
    onNavigateToMedia: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDpad: () -> Unit
) {
    val context = LocalContext.current
    val isConnected by HidClient.isConnected.collectAsState()
    val lastDevice by BluetoothSettingsStore.lastDeviceFlow(context).collectAsState(initial = null)
    val connectionError by HidClient.connectionError.collectAsState()

    LaunchedEffect(Unit) {
        HidClient.clearError()
    }

    Scaffold(
        timeText = { TimeText(modifier = Modifier.padding(top = 8.dp)) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 24.dp, start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Text(
                    text = "InmoControl",
                    style = MaterialTheme.typography.title3,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Connection status and error handling
            item {
                if (connectionError != null) {
                    Text(
                        text = connectionError!!,
                        color = MaterialTheme.colors.error,
                        style = MaterialTheme.typography.body2,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                } else if (isConnected) {
                    Text(
                        text = "Connected",
                        color = MaterialTheme.colors.primary,
                        style = MaterialTheme.typography.body2,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }

            // Main action buttons
            if (isConnected) {
                item { ControlButton(text = "Mouse", icon = Icons.Default.Mouse, onClick = onNavigateToMouse) }
                item { ControlButton(text = "Touchpad", icon = Icons.Default.TouchApp, onClick = onNavigateToTouchpad) }
                item { ControlButton(text = "Keyboard", icon = Icons.Default.Keyboard, onClick = onNavigateToKeyboard) }
                item { ControlButton(text = "Media", icon = Icons.Default.PlayArrow, onClick = onNavigateToMedia) }
                item { ControlButton(text = "D-Pad", icon = Icons.Default.Gamepad, onClick = onNavigateToDpad) }
                item { ControlButton(text = "Settings", icon = Icons.Default.Settings, onClick = onNavigateToSettings) }
            } else {
                // Connection buttons
                item {
                    Button(
                        onClick = onNavigateToConnect,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Bluetooth, contentDescription = "Connect")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connect")
                        }
                    }
                }
                lastDevice?.let { device ->
                    item {
                        Button(
                            onClick = {
                                try {
                                    HidClient.connectToDevice(device)
                                } catch (e: SecurityException) {
                                    HidClient.setConnectionError("Permission denied")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.secondaryButtonColors()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.BluetoothConnected, contentDescription = "Reconnect")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Reconnect")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Chip(
        label = { Text(text) },
        icon = { Icon(imageVector = icon, contentDescription = text) },
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    )
}
