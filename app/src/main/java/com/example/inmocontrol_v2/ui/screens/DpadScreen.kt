package com.example.inmocontrol_v2.ui.screens

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text as WearText
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.*
import com.example.inmocontrol_v2.hid.HidClient
import com.example.inmocontrol_v2.data.DeviceProfile
import kotlinx.coroutines.delay

@Composable
fun DpadScreen(
    onBack: () -> Unit = {},
    onScrollPopup: () -> Unit = {}
) {
    LaunchedEffect(Unit) {
        HidClient.currentDeviceProfile = DeviceProfile.Generic
    }

    var pressedButton by remember { mutableStateOf<String?>(null) }
    var isScrollMode by remember { mutableStateOf(false) }

    // Use real-time connection state from HidClient
    val isConnected by HidClient.isConnected.collectAsState()
    val connectionError by HidClient.connectionError.collectAsState()

    // Auto-clear feedback after 150ms
    LaunchedEffect(pressedButton) {
        if (pressedButton != null) {
            delay(150)
            pressedButton = null
        }
    }

    // Handle center button - single click for confirm/select, double click for right-click
    fun handleCenterButtonClick() {
        if (!isConnected) return
        pressedButton = "CENTER"

        if (isScrollMode) {
            HidClient.mouseLeftClick() // Left click in scroll mode
        } else {
            HidClient.sendInmoConfirm() // Confirm/select in D-pad mode (KEYCODE_DPAD_CENTER)
        }
    }

    // Handle center button double-click for right-click/context menu
    fun handleCenterButtonDoubleClick() {
        if (!isConnected) return
        pressedButton = "CENTER"
        HidClient.mouseRightClick() // Right click for context menu in both modes
    }

    fun onButtonPress(buttonName: String, direction: Int) {
        if (isConnected) {
            pressedButton = buttonName
            if (isScrollMode) {
                // Scroll mode - only UP, DOWN, LEFT, RIGHT work for scrolling
                when (direction) {
                    0 -> HidClient.mouseScroll(0, -3) // UP
                    1 -> HidClient.mouseScroll(0, 3)  // DOWN
                    2 -> HidClient.mouseScroll(-3, 0) // LEFT
                    3 -> HidClient.mouseScroll(3, 0)  // RIGHT
                }
            } else {
                // Normal 8-way D-pad mode with corrected direction mapping
                HidClient.dpad(direction)
            }
        }
    }

    Scaffold(
        timeText = { TimeText() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Simple mode indicator
            WearText(
                text = if (isScrollMode) "Scroll Mode" else "D-Pad Mode",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.primary,
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            )

            // Connection status
            if (!isConnected) {
                WearText(
                    text = connectionError ?: "Not connected",
                    style = MaterialTheme.typography.caption1,
                    color = MaterialTheme.colors.error,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // 8-way D-pad layout
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top row: UP-LEFT, UP, UP-RIGHT
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // UP-LEFT (7)
                    Button(
                        onClick = { if (!isScrollMode) onButtonPress("UP_LEFT", 7) },
                        modifier = Modifier.size(36.dp),
                        enabled = isConnected && !isScrollMode,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (pressedButton == "UP_LEFT") MaterialTheme.colors.secondary
                            else if (isScrollMode) MaterialTheme.colors.surface.copy(alpha = 0.3f)
                            else MaterialTheme.colors.primary
                        )
                    ) {
                        WearText("↖", fontSize = 12.sp)
                    }

                    // UP (0)
                    Button(
                        onClick = { onButtonPress("UP", 0) },
                        modifier = Modifier.size(36.dp),
                        enabled = isConnected,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (pressedButton == "UP") MaterialTheme.colors.secondary
                            else MaterialTheme.colors.primary
                        )
                    ) {
                        WearText("↑", fontSize = 14.sp)
                    }

                    // UP-RIGHT (8)
                    Button(
                        onClick = { if (!isScrollMode) onButtonPress("UP_RIGHT", 8) },
                        modifier = Modifier.size(36.dp),
                        enabled = isConnected && !isScrollMode,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (pressedButton == "UP_RIGHT") MaterialTheme.colors.secondary
                            else if (isScrollMode) MaterialTheme.colors.surface.copy(alpha = 0.3f)
                            else MaterialTheme.colors.primary
                        )
                    ) {
                        WearText("↗", fontSize = 12.sp)
                    }
                }

                // Middle row: LEFT, CENTER, RIGHT
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // LEFT (2)
                    Button(
                        onClick = { onButtonPress("LEFT", 2) },
                        modifier = Modifier.size(36.dp),
                        enabled = isConnected,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (pressedButton == "LEFT") MaterialTheme.colors.secondary
                            else MaterialTheme.colors.primary
                        )
                    ) {
                        WearText("←", fontSize = 14.sp)
                    }

                    // CENTER (OK/Enter) - Simple click for confirm, double-click for right-click
                    Button(
                        onClick = { handleCenterButtonClick() },
                        modifier = Modifier
                            .size(36.dp)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = { handleCenterButtonDoubleClick() }
                                )
                            },
                        enabled = isConnected,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (pressedButton == "CENTER") MaterialTheme.colors.secondary
                            else MaterialTheme.colors.primary
                        )
                    ) {
                        WearText("●", fontSize = 14.sp)
                    }

                    // RIGHT (3)
                    Button(
                        onClick = { onButtonPress("RIGHT", 3) },
                        modifier = Modifier.size(36.dp),
                        enabled = isConnected,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (pressedButton == "RIGHT") MaterialTheme.colors.secondary
                            else MaterialTheme.colors.primary
                        )
                    ) {
                        WearText("→", fontSize = 14.sp)
                    }
                }

                // Bottom row: DOWN-LEFT, DOWN, DOWN-RIGHT
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // DOWN-LEFT (5)
                    Button(
                        onClick = { if (!isScrollMode) onButtonPress("DOWN_LEFT", 5) },
                        modifier = Modifier.size(36.dp),
                        enabled = isConnected && !isScrollMode,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (pressedButton == "DOWN_LEFT") MaterialTheme.colors.secondary
                            else if (isScrollMode) MaterialTheme.colors.surface.copy(alpha = 0.3f)
                            else MaterialTheme.colors.primary
                        )
                    ) {
                        WearText("↙", fontSize = 12.sp)
                    }

                    // DOWN (1)
                    Button(
                        onClick = { onButtonPress("DOWN", 1) },
                        modifier = Modifier.size(36.dp),
                        enabled = isConnected,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (pressedButton == "DOWN") MaterialTheme.colors.secondary
                            else MaterialTheme.colors.primary
                        )
                    ) {
                        WearText("↓", fontSize = 14.sp)
                    }

                    // DOWN-RIGHT (6)
                    Button(
                        onClick = { if (!isScrollMode) onButtonPress("DOWN_RIGHT", 6) },
                        modifier = Modifier.size(36.dp),
                        enabled = isConnected && !isScrollMode,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (pressedButton == "DOWN_RIGHT") MaterialTheme.colors.secondary
                            else if (isScrollMode) MaterialTheme.colors.surface.copy(alpha = 0.3f)
                            else MaterialTheme.colors.primary
                        )
                    ) {
                        WearText("↘", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Control buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mode toggle
                Button(
                    onClick = { isScrollMode = !isScrollMode },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (isScrollMode) MaterialTheme.colors.secondary else MaterialTheme.colors.surface
                    )
                ) {
                    WearText(
                        text = if (isScrollMode) "D-Pad" else "Scroll",
                        fontSize = 10.sp
                    )
                }

                // Scroll popup button
                Button(
                    onClick = onScrollPopup,
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.surface)
                ) {
                    WearText("Scroll", fontSize = 10.sp)
                }

                // Back button
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.surface)
                ) {
                    WearText("Back", fontSize = 10.sp)
                }
            }
        }
    }
}

@Preview(device = "id:wearos_small_round", showSystemUi = true)
@Composable
fun DpadScreenPreview() {
    DpadScreen()
}
