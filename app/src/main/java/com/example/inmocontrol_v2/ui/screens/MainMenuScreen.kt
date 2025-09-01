package com.example.inmocontrol_v2.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material.Button as WearButton
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.ScalingLazyListState
import androidx.wear.compose.material.Text as WearText
import androidx.wear.compose.material.TimeText
import androidx.compose.runtime.remember

@Composable
fun MainMenuScreen(onNavigate: (String) -> Unit) {
    val menuItems = listOf(
        "Mouse" to "mouse",
        "Keyboard" to "keyboard",
        "Touchpad" to "touchpad",
        "D-Pad" to "dpad",
        "Media" to "media",
        "Settings" to "settings"
    )
    val scalingLazyListState = remember { ScalingLazyListState() }
    androidx.wear.compose.material.Scaffold {
        TimeText()
        ScalingLazyColumn(
            state = scalingLazyListState,
            modifier = Modifier.fillMaxSize()
        ) {
            items(menuItems.size) { index ->
                val (label, route) = menuItems[index]
                WearButton(onClick = { onNavigate(route) }, modifier = Modifier.fillMaxWidth()) {
                    WearText(label)
                }
            }
        }
    }
}

@Preview(device = "id:wearos_small_round", showBackground = true)
@Composable
fun MainMenuScreenPreview() {
    MainMenuScreen(onNavigate = {})
}
