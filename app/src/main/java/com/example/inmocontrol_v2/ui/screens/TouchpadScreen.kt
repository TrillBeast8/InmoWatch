package com.example.inmocontrol_v2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text as WearText
import androidx.wear.compose.material.TimeText
import com.example.inmocontrol_v2.hid.HidClient
import java.util.Locale
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun TouchpadScreen() {
    val coroutineScope = rememberCoroutineScope()
    val lastAction = remember { mutableStateOf("None") }
    val isDragging = remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val settingsStore = remember { com.example.inmocontrol_v2.data.SettingsStore.get(context) }
    val scrollSensitivity = settingsStore.scrollSensitivity.collectAsState(initial = 1.0f).value
    androidx.wear.compose.material.Scaffold {
        TimeText()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp) // Fallback to 16.dp for safe area
                .background(Color.LightGray.copy(alpha = 0.3f))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            coroutineScope.launch {
                                lastAction.value = "Left Click"
                                try {
                                    HidClient.instance()?.mouseLeftClick()
                                } catch (e: Exception) {}
                            }
                        },
                        onLongPress = {
                            coroutineScope.launch {
                                lastAction.value = "Right Click"
                                try {
                                    HidClient.instance()?.mouseRightClick()
                                } catch (e: Exception) {}
                            }
                        },
                        onDoubleTap = {
                            coroutineScope.launch {
                                lastAction.value = "Right Click (Double Tap)"
                                try {
                                    HidClient.instance()?.mouseRightClick()
                                } catch (e: Exception) {}
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            isDragging.value = true
                            lastAction.value = "Drag"
                        },
                        onDragEnd = {
                            isDragging.value = false
                            lastAction.value = "Drag End"
                        },
                        onDragCancel = {
                            isDragging.value = false
                            lastAction.value = "Drag Null"
                        },
                        onDrag = { change: PointerInputChange, dragAmount: androidx.compose.ui.geometry.Offset ->
                            coroutineScope.launch {
                                // Bidirectional scrolling with sensitivity
                                if (abs(dragAmount.y) > abs(dragAmount.x) && abs(dragAmount.y) > 10f) {
                                    val scaledY = (dragAmount.y * scrollSensitivity).toInt()
                                    val direction = if (scaledY < 0) "Scroll Up" else "Scroll Down"
                                    lastAction.value = direction
                                    try {
                                        HidClient.instance()?.mouseScroll(0, scaledY)
                                    } catch (e: Exception) {
                                        // Handle scroll error
                                    }
                                } else if (abs(dragAmount.x) > abs(dragAmount.y) && abs(dragAmount.x) > 10f) {
                                    val scaledX = (dragAmount.x * scrollSensitivity).toInt()
                                    val direction = if (scaledX < 0) "Scroll Left" else "Scroll Right"
                                    lastAction.value = direction
                                    try {
                                        HidClient.instance()?.mouseScroll(scaledX, 0)
                                    } catch (e: Exception) {
                                        // Handle scroll error
                                    }
                                } else {
                                    lastAction.value = "Dragging"
                                    try {
                                        HidClient.instance()?.mouseDragMove(dragAmount.x.toInt(), dragAmount.y.toInt())
                                    } catch (e: Exception) {
                                        // Handle drag error
                                    }
                                }
                            }
                        }
                    )
                }
        ) {
            WearText(
                text = "Touchpad Area",
                modifier = Modifier.align(androidx.compose.ui.Alignment.TopCenter).padding(top = 16.dp),
                fontSize = 16.sp
            )
            WearText(
                text = "Last Action: ${lastAction.value}",
                modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter).padding(bottom = 16.dp),
                fontSize = 14.sp
            )
        }
    }
}

@Preview(showBackground = true, device = "id:wearos_small_round")
@Composable
fun TouchpadScreenPreview() {
    TouchpadScreen()
}
