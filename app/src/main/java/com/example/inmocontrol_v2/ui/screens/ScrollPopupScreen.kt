package com.example.inmocontrol_v2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.example.inmocontrol_v2.hid.HidClient
import kotlinx.coroutines.delay

enum class PopupMode {
    SCROLL, DPAD
}

@Composable
fun ScrollPopupScreen(
    parentScreen: String, // "mouse", "touchpad", or "dpad"
    onBack: () -> Unit = {}
) {
    // Mode state - starts in scroll mode for all parent screens
    var currentMode by remember { mutableStateOf(PopupMode.SCROLL) }

    // Local sensitivity states - separate for each mode in this popup
    var scrollSensitivity by remember { mutableStateOf(50.0f) } // 1-100%
    var dpadSensitivity by remember { mutableStateOf(50.0f) } // 1-100%
    var sensitivityText by remember { mutableStateOf("50.0") }

    // UI state
    var pressedButton by remember { mutableStateOf<String?>(null) }

    // Mouse click detection state for center button
    var lastCenterClickTime by remember { mutableStateOf(0L) }
    var pendingClick by remember { mutableStateOf(false) }

    // Use real-time connection state from HidClient
    val isConnected by HidClient.isConnected.collectAsState()
    val connectionError by HidClient.connectionError.collectAsState()

    // Auto-clear button feedback after 150ms
    LaunchedEffect(pressedButton) {
        if (pressedButton != null) {
            delay(150)
            pressedButton = null
        }
    }

    // Handle pending single clicks
    LaunchedEffect(pendingClick, lastCenterClickTime) {
        if (pendingClick) {
            delay(300) // Wait for potential double click
            if (pendingClick) { // Still pending, execute single click
                pendingClick = false
                when (currentMode) {
                    PopupMode.SCROLL -> HidClient.mouseLeftClick() // Left click in scroll mode
                    PopupMode.DPAD -> HidClient.dpad(4) // D-pad center/confirm in D-pad mode
                }
            }
        }
    }

    // Update sensitivity text when mode changes
    LaunchedEffect(currentMode) {
        sensitivityText = when (currentMode) {
            PopupMode.SCROLL -> scrollSensitivity.toString()
            PopupMode.DPAD -> dpadSensitivity.toString()
        }
    }

    fun onScrollAction(direction: String, deltaX: Int, deltaY: Int) {
        if (isConnected) {
            pressedButton = direction
            val scrollAmount = (scrollSensitivity / 10.0f).toInt().coerceAtLeast(1)
            HidClient.mouseScroll(deltaX * scrollAmount, deltaY * scrollAmount)
        }
    }

    fun onDpadAction(buttonName: String, direction: Int) {
        if (isConnected) {
            pressedButton = buttonName
            HidClient.dpad(direction)
        }
    }

    fun updateSensitivity(newValue: String) {
        val value = newValue.toFloatOrNull() ?: return
        val clampedValue = value.coerceIn(1.0f, 100.0f)
        when (currentMode) {
            PopupMode.SCROLL -> scrollSensitivity = clampedValue
            PopupMode.DPAD -> dpadSensitivity = clampedValue
        }
    }

    // Handle center button double-click detection
    fun handleCenterButtonClick() {
        if (!isConnected) return

        val currentTime = System.currentTimeMillis()
        pressedButton = "CENTER"

        if (currentTime - lastCenterClickTime < 300) { // Double click within 300ms
            // Double click = right click in both modes
            HidClient.mouseRightClick()
        } else {
            // Register pending click for single click action
            pendingClick = true
        }
        lastCenterClickTime = currentTime
    }

    Scaffold(
        timeText = { TimeText() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title with click-to-return functionality
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = onBack,
                    modifier = Modifier.size(120.dp, 32.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color.Transparent
                    )
                ) {
                    Text(
                        text = "${parentScreen.replaceFirstChar { it.uppercaseChar() }} ${currentMode.name.lowercase().replaceFirstChar { it.uppercaseChar() }}",
                        style = MaterialTheme.typography.title3,
                        color = Color(0xFF9C27B0),
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp
                    )
                }
            }

            // Connection status
            if (!isConnected) {
                Text(
                    text = connectionError ?: "Not connected",
                    style = MaterialTheme.typography.caption1,
                    color = MaterialTheme.colors.error,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // Instructions
            Text(
                text = if (currentMode == PopupMode.SCROLL)
                    "Use arrows to scroll (4-way only)"
                else
                    "8-way D-pad navigation",
                style = MaterialTheme.typography.caption1,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                fontSize = 9.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )

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
                    // UP-LEFT (7) - ESC/Back key in both modes
                    Button(
                        onClick = {
                            if (isConnected) {
                                pressedButton = "UP_LEFT"
                                // Use universal ESC key that works across all devices
                                HidClient.sendKey(0x29) // HID scan code for ESC key
                            }
                        },
                        modifier = Modifier.size(36.dp),
                        enabled = isConnected,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (pressedButton == "UP_LEFT") MaterialTheme.colors.secondary
                            else Color(0xFFFF5722) // Orange color to indicate special ESC function
                        )
                    ) {
                        Text("ESC", fontSize = 8.sp, color = Color.White)
                    }

                    // UP (0) - works in both modes
                    Button(
                        onClick = {
                            when (currentMode) {
                                PopupMode.SCROLL -> onScrollAction("UP", 0, -1)
                                PopupMode.DPAD -> onDpadAction("UP", 0)
                            }
                        },
                        modifier = Modifier.size(36.dp),
                        enabled = isConnected,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (pressedButton == "UP") MaterialTheme.colors.secondary
                            else if (currentMode == PopupMode.SCROLL) Color(0xFF9C27B0) else MaterialTheme.colors.primary
                        )
                    ) {
                        Text(
                            if (currentMode == PopupMode.SCROLL) "⬆" else "↑",
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }

                    // UP-RIGHT (8) - only works in DPAD mode
                    Button(
                        onClick = { if (currentMode == PopupMode.DPAD) onDpadAction("UP_RIGHT", 8) },
                        modifier = Modifier.size(36.dp),
                        enabled = isConnected && currentMode == PopupMode.DPAD,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (pressedButton == "UP_RIGHT") MaterialTheme.colors.secondary
                            else if (currentMode == PopupMode.SCROLL) MaterialTheme.colors.surface.copy(alpha = 0.3f)
                            else MaterialTheme.colors.primary
                        )
                    ) {
                        Text("↗", fontSize = 12.sp, color = Color.White)
                    }
                }

                // Middle row: LEFT, CENTER, RIGHT
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // LEFT (2) - works in both modes
                    Button(
                        onClick = {
                            when (currentMode) {
                                PopupMode.SCROLL -> onScrollAction("LEFT", -1, 0)
                                PopupMode.DPAD -> onDpadAction("LEFT", 2)
                            }
                        },
                        modifier = Modifier.size(36.dp),
                        enabled = isConnected,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (pressedButton == "LEFT") MaterialTheme.colors.secondary
                            else if (currentMode == PopupMode.SCROLL) Color(0xFF9C27B0) else MaterialTheme.colors.primary
                        )
                    ) {
                        Text(
                            if (currentMode == PopupMode.SCROLL) "⬅" else "←",
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }

                    // CENTER - OK/Enter button (works in both modes)
                    Button(
                        onClick = {
                            if (isConnected) {
                                handleCenterButtonClick()
                            }
                        },
                        modifier = Modifier.size(36.dp),
                        enabled = isConnected,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (pressedButton == "CENTER") MaterialTheme.colors.secondary
                            else if (currentMode == PopupMode.SCROLL) Color(0xFF9C27B0) else MaterialTheme.colors.primary
                        )
                    ) {
                        Text("●", fontSize = 14.sp, color = Color.White)
                    }

                    // RIGHT (3) - works in both modes
                    Button(
                        onClick = {
                            when (currentMode) {
                                PopupMode.SCROLL -> onScrollAction("RIGHT", 1, 0)
                                PopupMode.DPAD -> onDpadAction("RIGHT", 3)
                            }
                        },
                        modifier = Modifier.size(36.dp),
                        enabled = isConnected,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (pressedButton == "RIGHT") MaterialTheme.colors.secondary
                            else if (currentMode == PopupMode.SCROLL) Color(0xFF9C27B0) else MaterialTheme.colors.primary
                        )
                    ) {
                        Text(
                            if (currentMode == PopupMode.SCROLL) "➡" else "→",
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }

                // Bottom row: DOWN-LEFT, DOWN, DOWN-RIGHT
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // DOWN-LEFT (5) - only works in DPAD mode
                    Button(
                        onClick = { if (currentMode == PopupMode.DPAD) onDpadAction("DOWN_LEFT", 5) },
                        modifier = Modifier.size(36.dp),
                        enabled = isConnected && currentMode == PopupMode.DPAD,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (pressedButton == "DOWN_LEFT") MaterialTheme.colors.secondary
                            else if (currentMode == PopupMode.SCROLL) MaterialTheme.colors.surface.copy(alpha = 0.3f)
                            else MaterialTheme.colors.primary
                        )
                    ) {
                        Text("↙", fontSize = 12.sp, color = Color.White)
                    }

                    // DOWN (1) - works in both modes
                    Button(
                        onClick = {
                            when (currentMode) {
                                PopupMode.SCROLL -> onScrollAction("DOWN", 0, 1)
                                PopupMode.DPAD -> onDpadAction("DOWN", 1)
                            }
                        },
                        modifier = Modifier.size(36.dp),
                        enabled = isConnected,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (pressedButton == "DOWN") MaterialTheme.colors.secondary
                            else if (currentMode == PopupMode.SCROLL) Color(0xFF9C27B0) else MaterialTheme.colors.primary
                        )
                    ) {
                        Text(
                            if (currentMode == PopupMode.SCROLL) "⬇" else "↓",
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }

                    // DOWN-RIGHT (6) - only works in DPAD mode
                    Button(
                        onClick = { if (currentMode == PopupMode.DPAD) onDpadAction("DOWN_RIGHT", 6) },
                        modifier = Modifier.size(36.dp),
                        enabled = isConnected && currentMode == PopupMode.DPAD,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (pressedButton == "DOWN_RIGHT") MaterialTheme.colors.secondary
                            else if (currentMode == PopupMode.SCROLL) MaterialTheme.colors.surface.copy(alpha = 0.3f)
                            else MaterialTheme.colors.primary
                        )
                    ) {
                        Text("↘", fontSize = 12.sp, color = Color.White)
                    }
                }
            }

            // Sensitivity control (visible in both modes)
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Sens:",
                    style = MaterialTheme.typography.caption1,
                    fontSize = 10.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )

                // Use Wear Compose Card instead of Material 3 TextField
                Card(
                    onClick = { updateSensitivity(sensitivityText) },
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp),
                    backgroundPainter = CardDefaults.cardBackgroundPainter(
                        startBackgroundColor = MaterialTheme.colors.surface,
                        endBackgroundColor = MaterialTheme.colors.surface
                    )
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = sensitivityText,
                            style = MaterialTheme.typography.caption1,
                            fontSize = 10.sp,
                            color = Color.White
                        )
                    }
                }

                Text(
                    text = "%",
                    style = MaterialTheme.typography.caption1,
                    fontSize = 10.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }

            // Toggle and Back buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Mode Toggle button
                Button(
                    onClick = {
                        currentMode = if (currentMode == PopupMode.SCROLL) PopupMode.DPAD else PopupMode.SCROLL
                    },
                    modifier = Modifier.weight(1f).height(32.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.secondary
                    )
                ) {
                    Text(
                        text = if (currentMode == PopupMode.SCROLL) "D-Pad" else "Scroll",
                        style = MaterialTheme.typography.caption1,
                        fontSize = 10.sp
                    )
                }

                // Back button
                Button(
                    onClick = onBack,
                    modifier = Modifier.weight(1f).height(32.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.surface
                    )
                ) {
                    Text(
                        text = "Back",
                        style = MaterialTheme.typography.caption1,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
