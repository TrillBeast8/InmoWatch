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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.inmocontrol_v2.ui.screens.*
import com.example.inmocontrol_v2.ui.theme.InmoTheme
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.flow.first
import androidx.compose.ui.platform.LocalContext
import com.example.inmocontrol_v2.hid.HidService
import com.example.inmocontrol_v2.hid.HidClient

class MainActivity : ComponentActivity() {
    private var lastBackPressTime: Long = 0
    private val doubleClickThreshold = 400L

    // Optimized service connection with proper cleanup
    private var hidServiceBound = false
    private val hidServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            try {
                val service = (binder as HidService.LocalBinder).getService()
                HidClient.setService(service)
                hidServiceBound = true
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Service connection error", e)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            HidClient.setService(null)
            hidServiceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissions()
        bindHidService()

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

    private fun requestPermissions() {
        val permissions = buildList {
            add(Manifest.permission.BLUETOOTH)
            add(Manifest.permission.BLUETOOTH_ADMIN)
            add(Manifest.permission.ACCESS_FINE_LOCATION)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.BLUETOOTH_ADVERTISE)
                add(Manifest.permission.BLUETOOTH_SCAN)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val missingPermissions = permissions.filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), 1001)
        }
    }

    private fun bindHidService() {
        try {
            val intent = Intent(this, HidService::class.java)
            bindService(intent, hidServiceConnection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to bind service", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanup()
    }

    private fun cleanup() {
        try {
            if (hidServiceBound) {
                unbindService(hidServiceConnection)
                hidServiceBound = false
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Cleanup error", e)
        }
    }

    @Deprecated("onBackPressed is deprecated")
    override fun onBackPressed() {
        val currentTime = System.currentTimeMillis()
        val settingsStore = com.example.inmocontrol_v2.data.SettingsStore.get(this)

        // Use runBlocking for settings access (optimized for single use)
        val featureEnabled = try {
            kotlinx.coroutines.runBlocking {
                settingsStore.remoteBackDoubleClick.first()
            }
        } catch (e: Exception) {
            false // Default to disabled if error
        }

        if (featureEnabled && currentTime - lastBackPressTime < doubleClickThreshold) {
            HidClient.sendBack()
            lastBackPressTime = 0
        } else {
            super.onBackPressed()
            lastBackPressTime = currentTime
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val settingsStore = remember { com.example.inmocontrol_v2.data.SettingsStore.get(context) }
    val autoReconnectEnabled by settingsStore.autoReconnectEnabled.collectAsState(initial = true)

    // Simplified navigation flag management
    var hasNavigatedToConnect by remember { mutableStateOf(false) }

    NavHost(navController = navController, startDestination = "main_menu") {
        composable("main_menu") {
            val isDeviceConnected = HidClient.isConnected()

            if (!isDeviceConnected && !hasNavigatedToConnect && autoReconnectEnabled) {
                hasNavigatedToConnect = true
                LaunchedEffect(Unit) {
                    navController.navigate("connect")
                }
            } else {
                hasNavigatedToConnect = false
                MainMenuScreen { route -> navController.navigate(route) }
            }
        }

        composable("mouse") { MouseScreen() }
        composable("keyboard") { KeyboardScreen() }
        composable("touchpad") { TouchpadScreen() }
        composable("media") { MediaScreen(isDeviceConnected = HidClient.isConnected()) }
        composable("dpad") { DpadScreen() }

        composable("settings") {
            SettingsScreen { route -> navController.navigate(route) }
        }

        composable("connect") {
            ConnectToDeviceScreen {
                hasNavigatedToConnect = false
                navController.navigate("main_menu") {
                    popUpTo("main_menu") { inclusive = true }
                }
            }
        }

        composable("connect_device") {
            ConnectToDeviceScreen {
                hasNavigatedToConnect = false
                navController.navigate("main_menu") {
                    popUpTo("main_menu") { inclusive = true }
                }
            }
        }

        composable("mouse_calibration") {
            MouseCalibrationScreen { route -> navController.navigate(route) }
        }
    }
}