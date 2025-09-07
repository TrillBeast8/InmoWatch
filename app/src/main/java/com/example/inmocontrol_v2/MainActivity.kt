package com.example.inmocontrol_v2

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.inmocontrol_v2.ui.screens.*
import com.example.inmocontrol_v2.ui.theme.InmoTheme
import androidx.core.app.ActivityCompat
import com.example.inmocontrol_v2.hid.HidService
import com.example.inmocontrol_v2.hid.HidClient
import com.example.inmocontrol_v2.data.SettingsStore
import kotlinx.coroutines.delay

/**
 * Clean MainActivity following simplified pattern
 */
class MainActivity : ComponentActivity() {
    private val doubleClickThreshold = 400L

    // Service connection
    private var hidServiceBound = false
    private val hidServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            try {
                val service = (binder as HidService.LocalBinder).getService()
                HidClient.setService(service)
                hidServiceBound = true
            } catch (_: Exception) {
                // Handle silently
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            HidClient.setService(null)
            hidServiceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize HidClient
        HidClient.initialize(this)

        // Request permissions and bind service
        if (hasAllRequiredPermissions()) {
            bindHidService()
        } else {
            requestPermissions()
        }

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

    private fun hasAllRequiredPermissions(): Boolean {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            listOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        return permissions.all { permission ->
            ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        ActivityCompat.requestPermissions(this, permissions, 1001)
    }

    private fun bindHidService() {
        try {
            val intent = Intent(this, HidService::class.java)
            startService(intent)
            bindService(intent, hidServiceConnection, BIND_AUTO_CREATE) // Removed redundant qualifier
        } catch (_: Exception) {
            // Handle silently
        }
    }

    override fun onResume() {
        super.onResume()

        if (!hidServiceBound) {
            bindHidService()
        }

        HidClient.refreshConnectionState()
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
        } catch (_: Exception) {
            // Handle silently
        }
    }

    @Suppress("DEPRECATION")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1001) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                bindHidService()
            }
        }
    }

    @Composable
    private fun AppNavigation() {
        val context = LocalContext.current
        val navController = rememberNavController()
        val settingsStore = remember { SettingsStore.get(context) }

        // Get back button remap setting
        val remoteBackDoubleClick by settingsStore.remoteBackDoubleClick.collectAsState(initial = false)

        // Back button handler state
        var lastBackPressTime by remember { mutableStateOf(0L) }
        var pendingBackAction by remember { mutableStateOf(false) }

        // Handle pending single back press
        LaunchedEffect(pendingBackAction, lastBackPressTime) {
            if (pendingBackAction) {
                delay(doubleClickThreshold)
                if (pendingBackAction) { // Still pending, execute single press action
                    pendingBackAction = false
                    if (remoteBackDoubleClick && HidClient.isConnected.value) {
                        // Send ESC/back to connected device
                        HidClient.sendKey(27) // ESC key
                    } else {
                        // Normal back navigation
                        if (navController.previousBackStackEntry != null) {
                            navController.popBackStack()
                        }
                    }
                }
            }
        }

        // Custom back handler for remap functionality
        BackHandler(enabled = true) {
            val currentTime = System.currentTimeMillis()

            if (remoteBackDoubleClick) {
                // Back remap is enabled
                if (currentTime - lastBackPressTime < doubleClickThreshold) {
                    // Double press - navigate to previous screen in watch app
                    pendingBackAction = false // Cancel pending single press
                    lastBackPressTime = 0L
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    }
                } else {
                    // First press - set up for potential single press (send to device)
                    lastBackPressTime = currentTime
                    pendingBackAction = true
                }
            } else {
                // Back remap is disabled - normal behavior
                if (navController.previousBackStackEntry != null) {
                    navController.popBackStack()
                }
            }
        }

        NavHost(
            navController = navController,
            startDestination = "connect_device"
        ) {
            composable("connect_device") {
                ConnectToDeviceScreen(
                    onDeviceConnected = {
                        navController.navigate("main") {
                            popUpTo("connect_device") { inclusive = true }
                        }
                    }
                )
            }

            composable("main") {
                MainMenuScreen(
                    onNavigateToConnect = {
                        navController.navigate("connect_device") {
                            popUpTo("main") { inclusive = true }
                        }
                    },
                    onNavigateToTouchpad = {
                        navController.navigate("touchpad")
                    },
                    onNavigateToDpad = {
                        navController.navigate("dpad")
                    },
                    onNavigateToMouse = {
                        navController.navigate("mouse")
                    },
                    onNavigateToKeyboard = {
                        navController.navigate("keyboard")
                    },
                    onNavigateToMedia = {
                        navController.navigate("media")
                    },
                    onNavigateToSettings = {
                        navController.navigate("settings")
                    }
                )
            }

            composable("touchpad") {
                TouchpadScreen(
                    mode = "touchpad"
                )
            }

            composable("dpad") {
                DpadScreen()
            }

            composable("mouse") {
                TouchpadScreen(
                    mode = "mouse"
                )
            }

            composable("keyboard") {
                KeyboardScreen()
            }

            composable("media") {
                MediaScreen()
            }

            composable("settings") {
                SettingsScreen()
            }
        }
    }
}