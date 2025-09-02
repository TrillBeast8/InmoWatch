package com.example.inmocontrol_v2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
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
import androidx.compose.ui.unit.sp
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
                .padding(16.dp) // Fallback to 16.dp for safe area
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
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            isDragging.value = true
                            lastAction.value = "Drag Start"
                        },
                        onDragEnd = {
                            isDragging.value = false
                            lastAction.value = "Drag End"
                        },
                        onDragCancel = {
                            isDragging.value = false
                            lastAction.value = "Drag Cancel"
                        },
                        onDrag = { change, dragAmount ->
                            coroutineScope.launch {
                                lastAction.value = "Dragging"
                                HidClient.instance()?.mouseDragMove(dragAmount.x.toInt(), dragAmount.y.toInt())
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
