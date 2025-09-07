package com.example.inmocontrol_v2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
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

@Composable
fun MainMenuScreen(
    onNavigateToConnect: () -> Unit = {},
    onNavigateToMouse: () -> Unit = {},
    onNavigateToTouchpad: () -> Unit = {},
    onNavigateToKeyboard: () -> Unit = {},
    onNavigateToMedia: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToDpad: () -> Unit = {}
) {
    val context = LocalContext.current

    // Use real-time connection state from HidClient
    val isConnected by HidClient.isConnected.collectAsState()

    // Get last device for quick reconnection
    val lastDevice by BluetoothSettingsStore.lastDeviceFlow(context).collectAsState(initial = null)

    // Clear any previous errors when screen loads
    LaunchedEffect(Unit) {
        HidClient.clearError()
    }

    // Collect connection error state
    val connectionError by HidClient.connectionError.collectAsState()

    Scaffold(
        timeText = { TimeText() }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = 32.dp,
                start = 16.dp,
                end = 16.dp,
                bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                if (connectionError != null) {
                    Text(
                        text = connectionError ?: "",
                        color = MaterialTheme.colors.error,
                        style = MaterialTheme.typography.body2,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }

            item {
                Text(
                    text = "InmoControl",
                    style = MaterialTheme.typography.title3,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            if (isConnected) {
                // Show current connection status when connected
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { /* Maybe show device info or disconnect option */ },
                        backgroundPainter = CardDefaults.cardBackgroundPainter(
                            startBackgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.BluetoothConnected,
                                contentDescription = "Connected",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Connected",
                                style = MaterialTheme.typography.body2
                            )
                        }
                    }
                }
                // Control Buttons when connected
                item {
                    Button(
                        onClick = onNavigateToMouse,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Mouse", style = MaterialTheme.typography.body2)
                    }
                }
                item {
                    Button(
                        onClick = onNavigateToTouchpad,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Touchpad", style = MaterialTheme.typography.body2)
                    }
                }
                item {
                    Button(
                        onClick = onNavigateToKeyboard,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Keyboard", style = MaterialTheme.typography.body2)
                    }
                }
                item {
                    Button(
                        onClick = onNavigateToMedia,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Media", style = MaterialTheme.typography.body2)
                    }
                }
                item {
                    Button(
                        onClick = onNavigateToDpad,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("D-Pad", style = MaterialTheme.typography.body2)
                    }
                }
            } else {
                // Show connection status without error logs
                lastDevice?.let { deviceMac ->
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                onNavigateToConnect()
                            },
                            backgroundPainter = CardDefaults.cardBackgroundPainter(
                                startBackgroundColor = MaterialTheme.colors.secondary.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bluetooth,
                                    contentDescription = "Device",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Last Device",
                                        style = MaterialTheme.typography.body2
                                    )
                                    Text(
                                        text = "••${deviceMac.takeLast(4)}", // Show last 4 chars
                                        style = MaterialTheme.typography.caption1
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Connect",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colors.primary
                                )
                            }
                        }
                    }
                }
                // If no last device exists, show connect button
                if (lastDevice == null) {
                    item {
                        Button(
                            onClick = onNavigateToConnect,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = MaterialTheme.colors.surface
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Device",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Connect Device",
                                    style = MaterialTheme.typography.body2
                                )
                            }
                        }
                    }
                }
            }

            // Single settings button - always available at the bottom
            item {
                Button(
                    onClick = onNavigateToSettings,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.surface
                    )
                ) {
                    Text("Settings", style = MaterialTheme.typography.body2)
                }
            }
        }
    }
}
