package com.example.inmocontrol_v2.ui.screens

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.*
import androidx.wear.compose.material.Text as WearText
import com.example.inmocontrol_v2.data.SettingsStore
import kotlinx.coroutines.launch
import java.util.Locale
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    onNavigateToMouseCalibration: () -> Unit = {}
) {
    val context = LocalContext.current
    val settingsStore = remember { SettingsStore.get(context) }
    val scope = rememberCoroutineScope()
    val sensitivity by settingsStore.sensitivity.collectAsState(initial = 0.5f)
    val remoteBackDoubleClick by settingsStore.remoteBackDoubleClick.collectAsState(initial = false)
    val scrollSensitivity by settingsStore.scrollSensitivity.collectAsState(initial = 1.0f)

    Scaffold(
        timeText = { TimeText() }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = 32.dp,
                start = 16.dp,
                end = 16.dp,
                bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = {
                                    // TODO: Show scroll popup menu
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    WearText(
                        text = "Settings",
                        style = MaterialTheme.typography.title2,
                        color = MaterialTheme.colors.onSurface
                    )
                }
            }

            // Pointer Sensitivity
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        WearText(
                            text = "Pointer Sensitivity",
                            style = MaterialTheme.typography.body2,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        WearText(
                            text = String.format(Locale.US, "%.2f", sensitivity),
                            style = MaterialTheme.typography.title3,
                            color = MaterialTheme.colors.primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(
                                onClick = {
                                    val newValue = (sensitivity - 0.1f).coerceAtLeast(0.1f)
                                    scope.launch { settingsStore.setSensitivity(newValue) }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                WearText("-", fontSize = 14.sp)
                            }

                            Button(
                                onClick = {
                                    val newValue = (sensitivity + 0.1f).coerceAtMost(2.0f)
                                    scope.launch { settingsStore.setSensitivity(newValue) }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                WearText("+", fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            // Scroll Sensitivity
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        WearText(
                            text = "Scroll Sensitivity",
                            style = MaterialTheme.typography.body2,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        WearText(
                            text = String.format(Locale.US, "%.2f", scrollSensitivity),
                            style = MaterialTheme.typography.title3,
                            color = MaterialTheme.colors.primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(
                                onClick = {
                                    val newValue = (scrollSensitivity - 0.1f).coerceAtLeast(0.1f)
                                    scope.launch { settingsStore.setScrollSensitivity(newValue) }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                WearText("-", fontSize = 14.sp)
                            }

                            Button(
                                onClick = {
                                    val newValue = (scrollSensitivity + 0.1f).coerceAtMost(2.0f)
                                    scope.launch { settingsStore.setScrollSensitivity(newValue) }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                WearText("+", fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            // Remote Back Double Click Toggle
            item {
                ToggleChip(
                    checked = remoteBackDoubleClick,
                    onCheckedChange = {
                        scope.launch { settingsStore.setRemoteBackDoubleClick(it) }
                    },
                    label = {
                        WearText(
                            text = "Remote Back Double Click",
                            textAlign = TextAlign.Center
                        )
                    },
                    toggleControl = {
                        Switch(
                            checked = remoteBackDoubleClick,
                            onCheckedChange = null
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Mouse Calibration Button
            item {
                Button(
                    onClick = { onNavigateToMouseCalibration() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    WearText("Mouse Calibration")
                }
            }

            // Back Button
            item {
                Button(
                    onClick = { onBack() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.secondaryButtonColors()
                ) {
                    WearText("Back")
                }
            }
        }
    }
}

@Preview(device = "id:wearos_small_round", showSystemUi = true)
@Composable
fun SettingsScreenPreview() {
    SettingsScreen()
}
