package com.example.inmocontrol_v2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.inmocontrol_v2.data.SettingsStore
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val settingsStore = remember { SettingsStore.get(context) }
    val scope = rememberCoroutineScope()

    val keyboardMode by settingsStore.keyboardMode.collectAsState(initial = false)
    val mouseMode by settingsStore.mouseMode.collectAsState(initial = false)
    val touchpadMode by settingsStore.touchpadMode.collectAsState(initial = false)
    val dpadMode by settingsStore.dpadMode.collectAsState(initial = false)
    val mediaMode by settingsStore.mediaMode.collectAsState(initial = false)
    val dpadEightWay by settingsStore.dpadEightWay.collectAsState(initial = true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "Settings",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        SettingSwitch("Keyboard", keyboardMode) { scope.launch { settingsStore.setKeyboardMode(it) } }
        SettingSwitch("Mouse", mouseMode) { scope.launch { settingsStore.setMouseMode(it) } }
        SettingSwitch("Touchpad", touchpadMode) { scope.launch { settingsStore.setTouchpadMode(it) } }
        SettingSwitch("D-pad", dpadMode) { scope.launch { settingsStore.setDpadMode(it) } }
        SettingSwitch("Media", mediaMode) { scope.launch { settingsStore.setMediaMode(it) } }
        SettingSwitch("8-way D-pad", dpadEightWay) { scope.launch { settingsStore.setDpadEightWay(it) } }
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}