package com.example.inmocontrol_v2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.inmocontrol_v2.hid.HidClient
import com.example.inmocontrol_v2.data.SettingsStore
import kotlinx.coroutines.launch

@Composable
fun DpadScreen() {
    val ctx = LocalContext.current
    val store = remember { SettingsStore.get(ctx) }
    val scope = rememberCoroutineScope()
    val eightWay by store.dpadEightWay.collectAsState(initial = true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // Top row
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (eightWay) {
                DpadButton("↖", "upleft")
            }
            DpadButton("▲", "up")
            if (eightWay) {
                DpadButton("↗", "upright")
            }
        }
        // Middle row
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            DpadButton("◀", "left")
            DpadButton("●", "ok")
            DpadButton("▶", "right")
        }
        // Bottom row
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (eightWay) {
                DpadButton("↙", "downleft")
            }
            DpadButton("▼", "down")
            if (eightWay) {
                DpadButton("↘", "downright")
            }
        }
        // Toggle switch for 8-way/4-way
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text("8-way D-pad")
            Switch(
                checked = eightWay,
                onCheckedChange = { checked ->
                    scope.launch { store.setDpadEightWay(checked) }
                }
            )
        }
    }
}

@Composable
private fun DpadButton(label: String, direction: String) {
    Button(
        onClick = { HidClient.instance()?.dpad(direction) },
        modifier = Modifier.size(56.dp)
    ) {
        Text(label)
    }
}