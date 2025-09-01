package com.example.inmocontrol_v2.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

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
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(menuItems) { (label, route) ->
            Button(onClick = { onNavigate(route) }, modifier = Modifier.fillMaxWidth()) {
                Text(label)
            }
        }
    }
}

@Preview(device = "id:wearos_small_round", showBackground = true)
@Composable
fun MainMenuScreenPreview() {
    MainMenuScreen(onNavigate = {})
}
