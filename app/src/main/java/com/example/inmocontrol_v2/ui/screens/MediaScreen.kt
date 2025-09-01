package com.example.inmocontrol_v2.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times

@Composable
fun MediaScreen() {
    val volume = remember { mutableStateOf(50) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onRotaryScrollEvent { event ->
                    // Handle rotary events
                    val scrollAmount = event.verticalScrollPixels
                    // Update volume based on scroll
                    val newVolume = (volume.value + (scrollAmount / 10).toInt()).coerceIn(0, 100)
                    volume.value = newVolume
                    // Return true to indicate the event was consumed
                    true
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Volume: ${volume.value}%",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}