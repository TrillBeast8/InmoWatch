package com.example.inmocontrol_v2.ui.screens

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.wear.compose.material.Button as WearButton
import androidx.wear.compose.material.Text as WearText
import androidx.wear.compose.material.TimeText
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.inmocontrol_v2.data.SettingsStore
import com.example.inmocontrol_v2.hid.HidClient

@Composable
fun DpadScreen() {
    androidx.wear.compose.material.Scaffold {
        TimeText()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WearText("D-pad")
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                DpadWearButton("●", "ok", Modifier.align(Alignment.Center).size(40.dp))
                DpadWearButton("▲", "up", Modifier.align(Alignment.TopCenter).size(40.dp))
                DpadWearButton("▼", "down", Modifier.align(Alignment.BottomCenter).size(40.dp))
                DpadWearButton("◀", "left", Modifier.align(Alignment.CenterStart).size(40.dp))
                DpadWearButton("▶", "right", Modifier.align(Alignment.CenterEnd).size(40.dp))
                DpadWearButton("↖", "upleft", Modifier.align(Alignment.TopStart).size(32.dp))
                DpadWearButton("↗", "upright", Modifier.align(Alignment.TopEnd).size(32.dp))
                DpadWearButton("↘", "downright", Modifier.align(Alignment.BottomEnd).size(32.dp))
            }
        }
    }
}

@Composable
private fun DpadWearButton(label: String, direction: String, modifier: Modifier = Modifier) {
    var isPressed by remember { mutableStateOf(false) }
    val backgroundColor = if (isPressed) androidx.wear.compose.material.MaterialTheme.colors.primary else androidx.wear.compose.material.MaterialTheme.colors.surface
    val contentColor = if (isPressed) androidx.wear.compose.material.MaterialTheme.colors.onPrimary else androidx.wear.compose.material.MaterialTheme.colors.onSurface

    WearButton(
        onClick = {
            when (direction) {
                "up" -> HidClient.dpad(0)
                "down" -> HidClient.dpad(1)
                "left" -> HidClient.dpad(2)
                "right" -> HidClient.dpad(3)
                "ok" -> HidClient.dpad(8)
                else -> {}
            }
        },
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            },
        colors = androidx.wear.compose.material.ButtonDefaults.buttonColors(
            backgroundColor = backgroundColor,
            contentColor = contentColor
        )
    ) {
        WearText(label)
    }
}

@Preview(device = "id:wearos_small_round", showSystemUi = true)
@Composable
fun DpadScreenPreview() {
    DpadScreen()
}
