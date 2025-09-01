package com.example.inmocontrol_v2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun TouchpadScreen() {
    val coroutineScope = rememberCoroutineScope()
    val tapCount = remember { mutableStateOf(0) }
    val lastAction = remember { mutableStateOf("None") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .background(Color.LightGray.copy(alpha = 0.3f))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            coroutineScope.launch {
                                tapCount.value++
                                lastAction.value = "Tap"
                            }
                        },
                        onDoubleTap = {
                            coroutineScope.launch {
                                lastAction.value = "Double Tap"
                            }
                        },
                        onLongPress = {
                            coroutineScope.launch {
                                lastAction.value = "Long Press"
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, rotation ->
                        if (zoom != 1f) {
                            lastAction.value = if (zoom > 1f) "Zoom In" else "Zoom Out"
                        } else if (pan != Offset.Zero) {
                            lastAction.value = "Pan"
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Last Action: ${lastAction.value}\nTaps: ${tapCount.value}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}