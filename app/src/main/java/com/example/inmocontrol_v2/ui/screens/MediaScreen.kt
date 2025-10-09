package com.example.inmocontrol_v2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text as WearText
import com.example.inmocontrol_v2.hid.HidClient
import com.example.inmocontrol_v2.ui.gestures.detectTwoFingerSwipe
import kotlinx.coroutines.delay

/**
 * Modern Media Control Screen
 * Improved UI with proper volume controls and device output switching
 */
@Composable
fun MediaScreen(
    onBack: () -> Unit = {},
    onScrollPopup: () -> Unit = {},
    onSwipeLeft: () -> Unit = {},
    onSwipeRight: () -> Unit = {}
) {
    // State management
    var isPlaying by remember { mutableStateOf(false) }
    var currentVolume by remember { mutableStateOf(50) }
    var pressedButton by remember { mutableStateOf<String?>(null) }

    // Use real-time connection state from HidClient
    val isConnected by HidClient.isConnected.collectAsState()
    val connectionError by HidClient.connectionError.collectAsState()

    // Auto-clear button feedback after 200ms
    LaunchedEffect(pressedButton) {
        if (pressedButton != null) {
            delay(200)
            pressedButton = null
        }
    }

    // Media control functions
    fun handlePlayPause() {
        if (isConnected) {
            pressedButton = "PLAY_PAUSE"
            isPlaying = !isPlaying
            HidClient.playPause()
        }
    }

    fun handlePrevious() {
        if (isConnected) {
            pressedButton = "PREVIOUS"
            HidClient.previousTrack()
        }
    }

    fun handleNext() {
        if (isConnected) {
            pressedButton = "NEXT"
            HidClient.nextTrack()
        }
    }

    fun handleVolumeUp() {
        if (isConnected) {
            pressedButton = "VOLUME_UP"
            currentVolume = (currentVolume + 5).coerceAtMost(100)
            HidClient.volumeUp()
        }
    }

    fun handleVolumeDown() {
        if (isConnected) {
            pressedButton = "VOLUME_DOWN"
            currentVolume = (currentVolume - 5).coerceAtLeast(0)
            HidClient.volumeDown()
        }
    }

    Scaffold(
        timeText = { TimeText() },
        modifier = Modifier.detectTwoFingerSwipe(
            onSwipeLeft = onSwipeLeft,
            onSwipeRight = onSwipeRight
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Connection status
                if (!isConnected) {
                    WearText(
                        text = connectionError ?: "Not connected",
                        style = MaterialTheme.typography.caption1,
                        color = MaterialTheme.colors.error,
                        textAlign = TextAlign.Center,
                        fontSize = 10.sp
                    )
                }

                // Media info area - more realistic media control display
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    WearText(
                        text = "Now Playing",
                        style = MaterialTheme.typography.caption1,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                    WearText(
                        text = "Media Player",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    WearText(
                        text = if (isPlaying) "♪ Playing" else "⏸ Paused",
                        style = MaterialTheme.typography.caption1,
                        color = if (isPlaying) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                        fontSize = 9.sp
                    )
                }

                // Main playback controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous track
                    MediaButton(
                        icon = Icons.Default.SkipPrevious,
                        onClick = { handlePrevious() },
                        enabled = isConnected,
                        isPressed = pressedButton == "PREVIOUS",
                        size = 40.dp
                    )

                    // Play/Pause (larger, center)
                    MediaButton(
                        icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        onClick = { handlePlayPause() },
                        enabled = isConnected,
                        isPressed = pressedButton == "PLAY_PAUSE",
                        size = 52.dp,
                        backgroundColor = MaterialTheme.colors.primary
                    )

                    // Next track
                    MediaButton(
                        icon = Icons.Default.SkipNext,
                        onClick = { handleNext() },
                        enabled = isConnected,
                        isPressed = pressedButton == "NEXT",
                        size = 40.dp
                    )
                }

                // Volume controls with single volume control
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    // Volume down
                    MediaButton(
                        icon = Icons.AutoMirrored.Filled.VolumeDown,
                        onClick = { handleVolumeDown() },
                        enabled = isConnected && currentVolume > 0,
                        isPressed = pressedButton == "VOLUME_DOWN",
                        size = 32.dp
                    )

                    // Volume display
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(50.dp)
                    ) {
                        WearText(
                            text = "$currentVolume%",
                            style = MaterialTheme.typography.caption1,
                            color = MaterialTheme.colors.onSurface,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )

                        // Volume bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(
                                    MaterialTheme.colors.surface,
                                    CircleShape
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(currentVolume / 100f)
                                    .background(
                                        MaterialTheme.colors.primary,
                                        CircleShape
                                    )
                            )
                        }
                    }

                    // Volume up
                    MediaButton(
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        onClick = { handleVolumeUp() },
                        enabled = isConnected && currentVolume < 100,
                        isPressed = pressedButton == "VOLUME_UP",
                        size = 32.dp
                    )
                }

                // Bottom controls row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    // Back button
                    MediaButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = onBack,
                        enabled = true,
                        isPressed = pressedButton == "BACK",
                        size = 36.dp
                    )

                    // Scroll popup button
                    MediaButton(
                        icon = Icons.Default.Keyboard,
                        onClick = onScrollPopup,
                        enabled = true,
                        isPressed = pressedButton == "SCROLL",
                        size = 36.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaButton(
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean,
    isPressed: Boolean,
    size: Dp,
    backgroundColor: Color = MaterialTheme.colors.surface
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                if (isPressed) MaterialTheme.colors.secondary
                else if (enabled) backgroundColor
                else backgroundColor.copy(alpha = 0.3f)
            )
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) {
                if (backgroundColor == MaterialTheme.colors.primary) Color.White
                else MaterialTheme.colors.onSurface
            } else {
                MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
            },
            modifier = Modifier.size(size * 0.6f)
        )
    }
}
