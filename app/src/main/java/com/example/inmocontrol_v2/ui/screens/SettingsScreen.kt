package com.example.inmocontrol_v2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.inmocontrol_v2.data.SettingsStore
import kotlinx.coroutines.launch
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.Text as WearText
import androidx.wear.compose.material.TimeText

@Composable
fun SettingsScreen(onNavigate: (String) -> Unit = {}) {
    val context = LocalContext.current
    val settingsStore = remember { SettingsStore.get(context) }
    val scope = rememberCoroutineScope()
    val sensitivity by settingsStore.sensitivity.collectAsState(initial = 0.5f)
    val remoteBackDoubleClick by settingsStore.remoteBackDoubleClick.collectAsState(initial = false)
    val dpadEnabled by settingsStore.dpadEnabled.collectAsState(initial = false)
    var feedbackMessage by remember { mutableStateOf("") }
    if (feedbackMessage.isNotEmpty()) {
        Snackbar(
            modifier = Modifier.padding(8.dp),
            action = {
                TextButton(onClick = { feedbackMessage = "" }) { Text("OK") }
            }
        ) { Text(feedbackMessage) }
    }
    androidx.wear.compose.material.Scaffold {
        TimeText()
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                WearText("Settings", modifier = Modifier.padding(bottom = 12.dp))
            }
            item {
                WearText("Pointer Sensitivity", modifier = Modifier.padding(bottom = 8.dp))
                Slider(
                    value = sensitivity,
                    onValueChange = {
                        scope.launch { settingsStore.setSensitivity(it) }
                    },
                    valueRange = 0.1f..2.0f,
                    steps = 19,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )
            }
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    WearText("Remote Back Button Double-Click", modifier = Modifier.weight(1f))
                    Switch(
                        checked = remoteBackDoubleClick,
                        onCheckedChange = {
                            scope.launch { settingsStore.setRemoteBackDoubleClick(it) }
                        }
                    )
                }
            }
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    WearText("Enable D-pad", modifier = Modifier.weight(1f))
                    Switch(
                        checked = dpadEnabled,
                        onCheckedChange = {
                            scope.launch { settingsStore.setDpadEnabled(it) }
                        }
                    )
                }
            }
            item {
                Button(
                    onClick = {
                        onNavigate("connect")
                        feedbackMessage = "Connecting to device..."
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    WearText("Connect to Device")
                }
            }
            item {
                Button(
                    onClick = {
                        onNavigate("mouse_calibration")
                        feedbackMessage = "Starting mouse calibration..."
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    WearText("Mouse Calibration")
                }
            }
        }
    }
}

@Preview(device = "id:wearos_small_round", showBackground = true)
@Composable
fun SettingsScreenPreview() {
    SettingsScreen()
}
