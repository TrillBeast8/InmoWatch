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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.asPaddingValues

@Composable
fun MainMenuScreen(onNavigate: (String) -> Unit = {}) {
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
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
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
