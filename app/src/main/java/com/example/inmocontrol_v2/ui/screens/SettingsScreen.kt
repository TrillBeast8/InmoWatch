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
import java.util.Locale
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
    val scrollSensitivity by settingsStore.scrollSensitivity.collectAsState(initial = 1.0f)
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
                // Local state for the text field, initialized from sensitivity
                var sensitivityText by remember { mutableStateOf(sensitivity.toString()) }
                // Synchronize text field when slider changes
                LaunchedEffect(sensitivity) {
                    sensitivityText = String.format(Locale.US, "%.2f", sensitivity)
                }
                Slider(
                    value = sensitivity,
                    onValueChange = {
                        scope.launch { settingsStore.setSensitivity(it) }
                        sensitivityText = String.format(Locale.US, "%.2f", it)
                    },
                    valueRange = 0.1f..2.0f,
                    steps = 19,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = sensitivityText,
                    onValueChange = { newText ->
                        sensitivityText = newText
                        val floatValue = newText.toFloatOrNull()
                        if (floatValue != null && floatValue in 0.1f..2.0f) {
                            scope.launch { settingsStore.setSensitivity(floatValue) }
                        }
                    },
                    label = { Text("Set value") },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = MaterialTheme.typography.bodySmall.fontSize),
                    modifier = Modifier.width(80.dp).height(40.dp).padding(horizontal = 16.dp)
                )
            }
            item {
                WearText("Scroll Sensitivity", modifier = Modifier.padding(bottom = 8.dp))
                var scrollSensitivityText by remember { mutableStateOf(scrollSensitivity.toString()) }
                LaunchedEffect(scrollSensitivity) {
                    scrollSensitivityText = String.format(Locale.US, "%.2f", scrollSensitivity)
                }
                Slider(
                    value = scrollSensitivity,
                    onValueChange = {
                        scope.launch { settingsStore.setScrollSensitivity(it) }
                        scrollSensitivityText = String.format(Locale.US, "%.2f", it)
                    },
                    valueRange = 0.1f..2.0f,
                    steps = 19,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = scrollSensitivityText,
                    onValueChange = { newText ->
                        scrollSensitivityText = newText
                        val parsed = newText.toFloatOrNull()
                        if (parsed != null && parsed in 0.1f..2.0f) {
                            scope.launch { settingsStore.setScrollSensitivity(parsed) }
                        }
                    },
                    label = { Text("Set value") },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = MaterialTheme.typography.bodySmall.fontSize),
                    modifier = Modifier.width(80.dp).height(40.dp).padding(horizontal = 16.dp)
                )
            }
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    WearText("Remote Back Double Click", modifier = Modifier.weight(1f))
                    Switch(
                        checked = remoteBackDoubleClick,
                        onCheckedChange = {
                            scope.launch { settingsStore.setRemoteBackDoubleClick(it) }
                        }
                    )
                }
            }
            item {
                androidx.wear.compose.material.Button(
                    onClick = { onNavigate("mouse_calibration") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    WearText("Mouse Calibration")
                }
            }
        }
    }
}

@Preview(device = "id:wearos_small_round", showSystemUi = true)
@Composable
fun SettingsScreenPreview() {
    SettingsScreen()
}
