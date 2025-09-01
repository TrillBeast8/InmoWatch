package com.example.inmocontrol_v2.ui.screens

import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
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
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.wear.compose.material.Button as WearButton
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text as WearText
import androidx.wear.compose.material.TimeText
import com.example.inmocontrol_v2.hid.HidClient

@Composable
fun MediaScreen() {
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
    androidx.wear.compose.material.Scaffold {
        TimeText()
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
            Box(modifier = Modifier.size(180.dp)) {
                // Center Play/Pause
                WearButton(
                    onClick = {
                        isPlaying.value = !isPlaying.value
                        HidClient.playPause()
                    },
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Image(
                        imageVector = if (isPlaying.value) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying.value) "Pause" else "Play"
                    )
                }
                // Left Previous/Rewind
                WearButton(
                    onClick = { HidClient.previousTrack() },
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Image(imageVector = Icons.Filled.SkipPrevious, contentDescription = "Previous")
                }
                // Right Next/Forward
                WearButton(
                    onClick = { HidClient.nextTrack() },
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Image(imageVector = Icons.Filled.SkipNext, contentDescription = "Next")
                }
                // Grouped output and volume controls below play/pause
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xAA000000))
                        .clickable { showVolumeOverlay.value = false },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        WearText("Volume", modifier = Modifier.padding(bottom = 8.dp))
                        Slider(
                            value = volume.value.toFloat(),
                            onValueChange = {
                                volume.value = it.toInt()
                                HidClient.setVolume(volume.value)
                            },
                            valueRange = 0f..100f,
                            modifier = Modifier.width(120.dp)
                        )
                        WearText("${volume.value}%", modifier = Modifier.padding(top = 8.dp))
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
        } catch (e: SecurityException) {
            trackName.value = "Permission required"
            artistName.value = ""
            albumName.value = ""
        } catch (e: Exception) {
            trackName.value = "Media error"
            artistName.value = ""
            albumName.value = ""
        }
    }
}

// Helper to map output device string to int
fun outputDeviceToInt(device: String): Int = when (device) {
    "headphones" -> 0
    "speaker" -> 1
    "phone" -> 2
    else -> 0
}

@Preview(device = "id:wearos_small_round", showBackground = true)
@Composable
fun MediaScreenPreview() {
    MediaScreen()
}
