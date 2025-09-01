package com.example.inmocontrol_v2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.Text as WearText
import androidx.wear.compose.material.TimeText
import com.example.inmocontrol_v2.hid.HidClient
import kotlinx.coroutines.launch

@Composable
fun TouchpadScreen() {
    val coroutineScope = rememberCoroutineScope()
    val lastAction = remember { mutableStateOf("None") }
    val isDragging = remember { mutableStateOf(false) }
    androidx.wear.compose.material.Scaffold {
        TimeText()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .background(Color.LightGray.copy(alpha = 0.3f))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            coroutineScope.launch {
                                lastAction.value = "Left Click"
                                HidClient.instance()?.mouseLeftClick()
                            }
                        },
                        onLongPress = {
                            coroutineScope.launch {
                                lastAction.value = "Right Click"
                                HidClient.instance()?.mouseRightClick()
                            }
                        },
                        onDoubleTap = {
                            coroutineScope.launch {
                                lastAction.value = "Double Click"
                                HidClient.instance()?.mouseDoubleClick()
                            }
                        }
                    )
                }
                .pointerInput(isDragging.value) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            coroutineScope.launch {
                                lastAction.value = "Drag Start"
                                isDragging.value = true
                                HidClient.instance()?.mouseDragMove(0, 0)
                            }
                        },
                        onDrag = { change, dragAmount ->
                            coroutineScope.launch {
                                lastAction.value = "Dragging"
                                HidClient.instance()?.mouseDragMove(dragAmount.x.toInt(), dragAmount.y.toInt())
                            }
                        },
                        onDragEnd = {
                            coroutineScope.launch {
                                lastAction.value = "Drag End"
                                isDragging.value = false
                                HidClient.instance()?.mouseDragEnd()
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, rotation ->
                        if (zoom == 1f && rotation == 0f) {
                            // Two-finger drag for scroll
                            coroutineScope.launch {
                                lastAction.value = "Scroll"
                                HidClient.instance()?.mouseScroll(pan.x.toInt(), pan.y.toInt())
                            }
                        }
                    }
                }
        ) {
            ScalingLazyColumn {
                item {
                    WearText("Touchpad", modifier = Modifier.padding(8.dp))
                }
                item {
                    WearText("Last Action: ${lastAction.value}", modifier = Modifier.padding(8.dp))
                }
            }
        }
    }
}

@Preview(device = "id:wearos_small_round", showBackground = true)
@Composable
fun TouchpadScreenPreview() {
    TouchpadScreen()
}
