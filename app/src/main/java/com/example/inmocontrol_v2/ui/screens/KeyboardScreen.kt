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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun KeyboardScreen() {
    val text = remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    // Request focus only once when the screen opens
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
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
                    onValueChange = { text.value = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .focusRequester(focusRequester),
                    textStyle = TextStyle(fontSize = MaterialTheme.typography.bodyLarge.fontSize),
                    maxLines = 5,
                    placeholder = { Text("Type your message...") }
                )
            }
            item {
                Button(
                    onClick = {
                        HidClient.instance()?.sendKeyboardReport(text.value.encodeToByteArray())
                        keyboardController?.hide()
                        text.value = ""
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text("Send", fontSize = 16.sp)
                }
            }
        }
    }
}

@Preview(showBackground = true, device = "id:wearos_small_round")
@Composable
fun KeyboardScreenPreview() {
    KeyboardScreen()
}
