package com.example.inmocontrol_v2.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Button
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.Text as WearText
import androidx.wear.compose.material.TimeText
import com.example.inmocontrol_v2.hid.HidClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun KeyboardScreen() {
    val text = rememberSaveable { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    // TODO: In the future, support receiving text updates from the paired device and update 'text.value' here.
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val showSentMessage = remember { mutableStateOf(false) }

    // Request focus only once when the screen is first shown
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Auto-hide "Sent" message after 1 second
    LaunchedEffect(showSentMessage.value) {
        if (showSentMessage.value) {
            delay(1000)
            showSentMessage.value = false
        }
    }

    // Function to convert character to HID key code
    fun charToHidKeyCode(char: Char): Int {
        return when (char.uppercaseChar()) {
            'A' -> 0x04; 'B' -> 0x05; 'C' -> 0x06; 'D' -> 0x07; 'E' -> 0x08
            'F' -> 0x09; 'G' -> 0x0A; 'H' -> 0x0B; 'I' -> 0x0C; 'J' -> 0x0D
            'K' -> 0x0E; 'L' -> 0x0F; 'M' -> 0x10; 'N' -> 0x11; 'O' -> 0x12
            'P' -> 0x13; 'Q' -> 0x14; 'R' -> 0x15; 'S' -> 0x16; 'T' -> 0x17
            'U' -> 0x18; 'V' -> 0x19; 'W' -> 0x1A; 'X' -> 0x1B; 'Y' -> 0x1C
            'Z' -> 0x1D
            '1' -> 0x1E; '2' -> 0x1F; '3' -> 0x20; '4' -> 0x21; '5' -> 0x22
            '6' -> 0x23; '7' -> 0x24; '8' -> 0x25; '9' -> 0x26; '0' -> 0x27
            ' ' -> 0x2C // Space
            '\n' -> 0x28 // Enter
            '.' -> 0x37; ',' -> 0x36; '!' -> 0x1E; '?' -> 0x38
            else -> 0x2C // Default to space for unsupported characters
        }
    }

    // Function to send text character by character with proper HID codes
    suspend fun sendTextAsHidKeys(textToSend: String) {
        val hidClient = HidClient.instance()

        // Switch to keyboard mode
        hidClient?.setInputMode(com.example.inmocontrol_v2.hid.HidService.InputMode.KEYBOARD)

        textToSend.forEach { char ->
            try {
                val hidKeyCode = charToHidKeyCode(char)
                hidClient?.sendKey(hidKeyCode)
                delay(30)
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }

    androidx.wear.compose.material.Scaffold {
        TimeText()
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.safeDrawing.asPaddingValues()),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
        ) {
            item {
                WearText(
                    "Keyboard",
                    modifier = Modifier.padding(8.dp),
                    fontSize = 18.sp
                )
            }
            item {
                TextField(
                    value = text.value,
                    onValueChange = {
                        text.value = it
                        showSentMessage.value = false // Hide sent message when typing
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .focusRequester(focusRequester),
                    textStyle = TextStyle(fontSize = MaterialTheme.typography.bodyLarge.fontSize),
                    maxLines = 5,
                    placeholder = { Text("Type your message...") }
                )
            }

            // Simple "Sent" message that appears briefly
            if (showSentMessage.value) {
                item {
                    WearText(
                        "Sent",
                        modifier = Modifier.padding(4.dp),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        if (text.value.isNotEmpty()) {
                            coroutineScope.launch {
                                sendTextAsHidKeys(text.value)
                                keyboardController?.hide()
                                showSentMessage.value = true
                                text.value = "" // Automatically clear text after sending
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    enabled = text.value.isNotEmpty()
                ) {
                    Text("Send")
                }
            }
        }
    }

    // Set input mode to KEYBOARD when screen loads
    LaunchedEffect(Unit) {
        HidClient.instance()?.setInputMode(com.example.inmocontrol_v2.hid.HidService.InputMode.KEYBOARD)
    }
}

@Preview(device = "id:wearos_small_round", showSystemUi = true)
@Composable
fun KeyboardScreenPreview() {
    KeyboardScreen()
}
