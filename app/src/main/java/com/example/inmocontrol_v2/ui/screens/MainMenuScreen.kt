package com.example.inmocontrol_v2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.wear.compose.material.Button as WearButton
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.material.Text as WearText
import androidx.wear.compose.material.TimeText
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun MainMenuScreen(onNavigate: (String) -> Unit = {}) {
    val connectionStateViewModel: ConnectionStateViewModel = viewModel()
    val isConnected = connectionStateViewModel.isConnected
    // Auto-launch connect screen if not connected
    androidx.compose.runtime.LaunchedEffect(isConnected) {
        if (!isConnected) {
            onNavigate("connect")
        }
    }
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
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.safeDrawing.asPaddingValues()),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            items(menuItems.size) { index ->
                val (label, route) = menuItems[index]
                WearButton(
                    onClick = { onNavigate(route) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    WearText(
                        label,
                        color = Color.White,
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Preview(device = "id:wearos_small_round", showSystemUi = true)
@Composable
fun MainMenuScreenPreview() {
    MainMenuScreen()
}
