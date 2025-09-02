package com.example.inmocontrol_v2.ui.screens

import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button as WearButton
import androidx.wear.compose.material.Text as WearText
import com.example.inmocontrol_v2.hid.HidClient

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
    val outputIcon = when (outputDevice.value) {
        "headphones" -> Icons.Filled.Headphones
        "speaker" -> Icons.Filled.Speaker
        else -> Icons.Filled.Speaker
    }
    val volumeIcon = if (volume.value == 0) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!isDeviceConnected) {
            Text(text = "No device connected. Please connect a device first.", color = Color.Red, modifier = Modifier.padding(16.dp))
            return@Column
        }

        // Removed top row of three output buttons
        // Show current output device below controls
        Text(text = "Output: ${outputDevice.value}", modifier = Modifier.padding(8.dp))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .onRotaryScrollEvent { event ->
                    if (showVolumeOverlay.value) {
                        val scrollAmount = event.verticalScrollPixels
                        val newVolume = (volume.value + (scrollAmount / 10).toInt()).coerceIn(0, 100)
                        volume.value = newVolume
                        HidClient.setVolume(volume.value)
                        true
                    } else false
                },
            contentAlignment = Alignment.Center
        ) {
            // Track info at top
            Column(
                modifier = Modifier.align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                WearText(trackName.value, modifier = Modifier.padding(top = 16.dp, bottom = 2.dp))
                WearText(artistName.value, modifier = Modifier.padding(bottom = 2.dp))
                WearText(albumName.value, modifier = Modifier.padding(bottom = 8.dp))
            }
            // Radial controls
            Box(modifier = Modifier.size(220.dp).offset(y = (-24).dp)) { // Increased size and moved up
                // Center Play/Pause
                WearButton(
                    onClick = {
                        isPlaying.value = !isPlaying.value
                        HidClient.playPause()
                    },
                    modifier = Modifier.align(Alignment.Center).size(32.dp) // Reduced button size
                ) {
                    Image(
                        imageVector = if (isPlaying.value) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying.value) "Pause" else "Play"
                    )
                }
                // Left Previous/Rewind
                WearButton(
                    onClick = { HidClient.previousTrack() },
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp).size(32.dp) // Reduced button size
                ) {
                    Image(imageVector = Icons.Filled.SkipPrevious, contentDescription = "Previous")
                }
                // Right Next/Forward
                WearButton(
                    onClick = { HidClient.nextTrack() },
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp).size(32.dp) // Reduced button size
                ) {
                    Image(imageVector = Icons.Filled.SkipNext, contentDescription = "Next")
                }
                // Output and volume controls below play/pause
                Row(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    WearButton(
                        onClick = {
                            outputDevice.value = when (outputDevice.value) {
                                "headphones" -> "speaker"
                                "speaker" -> "phone"
                                else -> "headphones"
                            }
                            HidClient.switchOutput(outputDeviceToInt(outputDevice.value))
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Image(imageVector = outputIcon, contentDescription = "Output Device")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    WearButton(
                        onClick = { showVolumeOverlay.value = !showVolumeOverlay.value },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Image(imageVector = volumeIcon, contentDescription = "Volume")
                    }
                }
            }
            // Volume overlay
            if (showVolumeOverlay.value) {
                Surface(
                    modifier = Modifier.align(Alignment.Center).size(120.dp),
                    color = Color.DarkGray,
                    shape = CircleShape
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Volume", color = Color.White)
                        Slider(
                            value = volume.value.toFloat(),
                            onValueChange = {
                                volume.value = it.toInt()
                                HidClient.setVolume(volume.value)
                            },
                            valueRange = 0f..100f
                        )
                        Text(text = "${volume.value}", color = Color.White)
                    }
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
@Preview(device = "id:wearos_small_round", showBackground = true)
@Composable
fun MediaScreenWearPreview() {
    MediaScreen(isDeviceConnected = true)
}
