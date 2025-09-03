package com.example.inmocontrol_v2

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.inmocontrol_v2.ui.screens.*
import com.example.inmocontrol_v2.ui.theme.InmoTheme
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import com.example.inmocontrol_v2.hid.HidService
import com.example.inmocontrol_v2.hid.HidClient
import androidx.compose.runtime.mutableStateOf

class MainActivity : ComponentActivity() {
    private var lastBackPressTime: Long = 0
    private val doubleClickThreshold = 400 // ms

    // ServiceConnection for HidService
    private var hidServiceBound = false
    private val hidServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val service = (binder as HidService.LocalBinder).getService()
            HidClient.setService(service)
            hidServiceBound = true
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            HidClient.setService(null)
            hidServiceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Request Bluetooth and location permissions
        val permissions = mutableListOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        }
        if (permissions.any { ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 1001)
        }
        // Bind to HidService
        val intent = Intent(this, HidService::class.java)
        bindService(intent, hidServiceConnection, Context.BIND_AUTO_CREATE)
        setContent {
            InmoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (hidServiceBound) {
            unbindService(hidServiceConnection)
            hidServiceBound = false
        }
    }

    @Deprecated("onBackPressed is deprecated")
    override fun onBackPressed() {
        val currentTime = System.currentTimeMillis()
        val context = this
        val settingsStore = com.example.inmocontrol_v2.data.SettingsStore.get(context)
        // Use runBlocking directly instead of GlobalScope.launch
        val featureEnabled = runBlocking {
            settingsStore.remoteBackDoubleClick.first()
        }
        if (featureEnabled) {
            if (currentTime - lastBackPressTime < doubleClickThreshold) {
                // Double click detected, send remote back command
                sendRemoteBackCommand()
                lastBackPressTime = 0
            } else {
                // Single click, go back as usual
                super.onBackPressed()
                lastBackPressTime = currentTime
            }
        } else {
            // Feature disabled, default behavior
            super.onBackPressed()
        }
    }

    private fun sendRemoteBackCommand() {
        HidClient.sendBack()
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val settingsStore = remember { com.example.inmocontrol_v2.data.SettingsStore.get(context) }
    val autoReconnectEnabled = settingsStore.autoReconnectEnabled.collectAsState(initial = true)

    // Flag to avoid repeated navigation - fix delegation issue
    val hasNavigatedToConnect = remember { mutableStateOf(false) }

    NavHost(navController = navController, startDestination = "main_menu") {
        composable("main_menu") {
            // Check device connection state
            val isDeviceConnected = HidClient.isConnected()
            if (!isDeviceConnected && !hasNavigatedToConnect.value && autoReconnectEnabled.value) {
                hasNavigatedToConnect.value = true
                navController.navigate("connect")
            } else {
                // Reset navigation flag when we're connected or auto-reconnect is disabled
                hasNavigatedToConnect.value = false
                MainMenuScreen(onNavigate = { route: String ->
                    navController.navigate(route)
                })
            }
        }
        composable("mouse") { MouseScreen() }
        composable("keyboard") { KeyboardScreen() }
        composable("touchpad") { TouchpadScreen() }
        composable("media") { MediaScreen(isDeviceConnected = HidClient.isConnected()) }
        composable("dpad") {
            DpadScreen()
        }
        composable("settings") {
            SettingsScreen(onNavigate = { route ->
                navController.navigate(route)
            })
        }
        composable("connect") {
            ConnectToDeviceScreen(
                onOpenControls = {
                    // Clear the navigation flag and go to main menu
                    hasNavigatedToConnect.value = false
                    navController.navigate("main_menu") {
                        // Clear the back stack so user can't go back to connect screen
                        popUpTo("main_menu") { inclusive = true }
                    }
                }
            )
        }
        composable("connect_device") {
            ConnectToDeviceScreen(
                onOpenControls = {
                    hasNavigatedToConnect.value = false
                    navController.navigate("main_menu") {
                        popUpTo("main_menu") { inclusive = true }
                    }
                }
            )
        }
        composable("mouse_calibration") {
            MouseCalibrationScreen(onNavigate = { route: String ->
                navController.navigate(route)
            })
        }
    }
}