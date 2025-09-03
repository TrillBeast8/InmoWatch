package com.example.inmocontrol_v2.ui.screens

import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button as WearButton
import androidx.wear.compose.material.Text as WearText
import androidx.wear.compose.material.TimeText
import com.example.inmocontrol_v2.hid.HidClient
import kotlinx.coroutines.delay
import kotlin.math.min

@Composable
fun MediaScreen(isDeviceConnected: Boolean = false) {
    val context = LocalContext.current
    val volume = remember { mutableStateOf(50) }
    val isPlaying = remember { mutableStateOf(false) }
    val trackName = remember { mutableStateOf("") }
    val artistName = remember { mutableStateOf("") }
    val albumName = remember { mutableStateOf("") }
    val showVolumeOverlay = remember { mutableStateOf(false) }
    val outputDevice = remember { mutableStateOf("headphones") } // "headphones", "speaker", "phone"
    val errorMessage = remember { mutableStateOf("") }
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
    val bluetoothAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
    val isBluetoothAudioConnected = bluetoothAdapter?.getProfileConnectionState(android.bluetooth.BluetoothProfile.A2DP) == android.bluetooth.BluetoothProfile.STATE_CONNECTED
    val systemVolume = remember { mutableStateOf(audioManager?.getStreamVolume(android.media.AudioManager.STREAM_MUSIC) ?: 5) }
    val maxSystemVolume = audioManager?.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC) ?: 15
    val outputIcon = when (outputDevice.value) {
        "headphones" -> Icons.Filled.Headphones
        "speaker" -> Icons.Filled.Speaker
        else -> Icons.Filled.Speaker
    }
    val volumeIcon = if (volume.value == 0) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp

    val coroutineScope = rememberCoroutineScope()
    var lastAction by remember { mutableStateOf("") }
    var actionFeedbackVisible by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val minDimension = min(configuration.screenWidthDp, configuration.screenHeightDp)
    val buttonSize = (minDimension / 4).dp

    // Set input mode to MEDIA when screen loads
    LaunchedEffect(Unit) {
        HidClient.instance()?.setInputMode(com.example.inmocontrol_v2.hid.HidService.InputMode.MEDIA)
    }

    // Auto-hide action feedback after 1 second
    LaunchedEffect(actionFeedbackVisible) {
        if (actionFeedbackVisible) {
            delay(1000)
            actionFeedbackVisible = false
        }
    }

    fun performMediaAction(action: String, hidAction: () -> Unit) {
        try {
            // First try system media controller
            val sessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            val controllers = sessionManager?.getActiveSessions(null)

            if (controllers != null && controllers.isNotEmpty()) {
                val controller = controllers[0]
                // Use system media controller
                when (action) {
                    "Play/Pause" -> {
                        if (controller.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING) {
                            controller.transportControls.pause()
                            isPlaying.value = false
                        } else {
                            controller.transportControls.play()
                            isPlaying.value = true
                        }
                    }
                    "Previous" -> controller.transportControls.skipToPrevious()
                    "Next" -> controller.transportControls.skipToNext()
                }
            } else {
                // Fallback to HID if no system media session available
                HidClient.instance()?.setInputMode(com.example.inmocontrol_v2.hid.HidService.InputMode.MEDIA)
                hidAction()
            }

            lastAction = action
            actionFeedbackVisible = true
        } catch (e: SecurityException) {
            // If we don't have permission, fallback to HID
            try {
                HidClient.instance()?.setInputMode(com.example.inmocontrol_v2.hid.HidService.InputMode.MEDIA)
                hidAction()
                lastAction = action
                actionFeedbackVisible = true
            } catch (e2: Exception) {
                lastAction = "Error"
                actionFeedbackVisible = true
            }
        } catch (e: Exception) {
            // General error, try HID fallback
            try {
                HidClient.instance()?.setInputMode(com.example.inmocontrol_v2.hid.HidService.InputMode.MEDIA)
                hidAction()
                lastAction = action
                actionFeedbackVisible = true
            } catch (e2: Exception) {
                lastAction = "Error"
                actionFeedbackVisible = true
            }
        }
    }

    androidx.wear.compose.material.Scaffold {
        TimeText()
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isDeviceConnected) {
                Text(text = "No device connected. Please connect a device first.", color = Color.Red, modifier = Modifier.padding(16.dp))
                // Show system Bluetooth output info
                val outputText = if (isBluetoothAudioConnected) "Bluetooth headphones connected" else "Speaker/other output"
                Text(text = "System Output: $outputText", modifier = Modifier.padding(8.dp))
                // System volume control
                Text(text = "System Volume", color = Color.White)
                Slider(
                    value = systemVolume.value.toFloat(),
                    onValueChange = {
                        val newVol = it.toInt().coerceIn(0, maxSystemVolume)
                        systemVolume.value = newVol
                        audioManager?.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, newVol, 0)
                    },
                    valueRange = 0f..maxSystemVolume.toFloat()
                )
                Text(text = "${systemVolume.value}", color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                androidx.wear.compose.material.Button(onClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_MAIN)
                    intent.addCategory(android.content.Intent.CATEGORY_APP_MUSIC)
                    context.startActivity(intent)
                }) {
                    WearText("Open System Media Controls")
                }
            } else {
                if (errorMessage.value.isNotEmpty()) {
                    Text(text = errorMessage.value, color = Color.Red, modifier = Modifier.padding(8.dp))
                }
                // Show current output device below controls
                Text(text = "Output: ${outputDevice.value}", modifier = Modifier.padding(8.dp))

                // Media control buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    WearButton(onClick = {
                        performMediaAction("Previous") { HidClient.previousTrack() }
                    }) {
                        WearText("⏮")
                    }

                    WearButton(onClick = {
                        performMediaAction("Play/Pause") { HidClient.playPause() }
                    }) {
                        WearText(if (isPlaying.value) "⏸" else "▶")
                    }

                    WearButton(onClick = {
                        performMediaAction("Next") { HidClient.nextTrack() }
                    }) {
                        WearText("⏭")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Volume controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    WearButton(onClick = {
                        HidClient.setVolume(-1)
                        lastAction = "Volume Down"
                        actionFeedbackVisible = true
                    }) {
                        WearText("🔉")
                    }

                    WearButton(onClick = {
                        HidClient.setVolume(1)
                        lastAction = "Volume Up"
                        actionFeedbackVisible = true
                    }) {
                        WearText("🔊")
                    }
                }

                if (actionFeedbackVisible) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = lastAction, color = Color.Green)
                }
            }
        }
    }

    // MediaSession listener to update metadata
    LaunchedEffect(Unit) {
        try {
            val sessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            val controllers = sessionManager?.getActiveSessions(null)
            if (controllers != null && controllers.isNotEmpty()) {
                val controller = controllers[0]
                val metadata = controller.metadata
                trackName.value = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE) ?: ""
                artistName.value = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST) ?: ""
                albumName.value = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_ALBUM) ?: ""
                controller.registerCallback(object : MediaController.Callback() {
                    override fun onMetadataChanged(metadata: android.media.MediaMetadata?) {
                        trackName.value = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE) ?: ""
                        artistName.value = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST) ?: ""
                        albumName.value = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_ALBUM) ?: ""
                    }
                })
            } else {
                trackName.value = "No media info available"
                artistName.value = ""
                albumName.value = ""
            }
        } catch (_: SecurityException) {
            trackName.value = "Permission required"
            artistName.value = ""
            albumName.value = ""
        } catch (_: Exception) {
            trackName.value = "Media error"
            artistName.value = ""
            albumName.value = ""
        }
    }
}

// Helper function for output device int
fun outputDeviceToInt(device: String): Int = when (device) {
    "headphones" -> 0
    "speaker" -> 1
    "phone" -> 2
    else -> 0
}

// Preview for small Wear OS circular screen
@Preview(device = "id:wearos_small_round", showSystemUi = true)
@Composable
fun MediaScreenPreview() {
    MediaScreen(isDeviceConnected = true)
}
