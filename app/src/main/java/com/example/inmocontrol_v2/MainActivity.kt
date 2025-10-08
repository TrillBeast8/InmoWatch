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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import com.example.inmocontrol_v2.ui.screens.*
import com.example.inmocontrol_v2.ui.theme.InmoTheme
import com.example.inmocontrol_v2.hid.HidService
import com.example.inmocontrol_v2.hid.HidClient
import com.example.inmocontrol_v2.data.SettingsStore

/**
 * Optimized MainActivity with modern permission handling and performance improvements
 */
class MainActivity : ComponentActivity() {
    companion object {
        private const val DOUBLE_CLICK_THRESHOLD = 400L
    }

    // Modern permission launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            startAndBindHidService()
        }
    }

    // Service connection management
    private var hidServiceBound = false
    private val hidServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            try {
                val service = (binder as? HidService.LocalBinder)?.getService()
                HidClient.setService(service)
                hidServiceBound = true
            } catch (_: Exception) {
                hidServiceBound = false
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            HidClient.setService(null)
            hidServiceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request permissions and start service
        requestBluetoothPermissions()

        setContent {
            InmoTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    AppNavigation()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        bindHidServiceIfNeeded()
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindHidService()
    }

    private fun requestBluetoothPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            startAndBindHidService()
        }
    }

    private fun startAndBindHidService() {
        val serviceIntent = Intent(this, HidService::class.java)
        startForegroundService(serviceIntent)
        bindHidServiceIfNeeded()
    }

    private fun bindHidServiceIfNeeded() {
        if (!hidServiceBound) {
            val serviceIntent = Intent(this, HidService::class.java)
            bindService(serviceIntent, hidServiceConnection, BIND_AUTO_CREATE)
        }
    }

    private fun unbindHidService() {
        if (hidServiceBound) {
            try {
                unbindService(hidServiceConnection)
            } catch (_: Exception) {
                // Service may already be unbound
            }
            hidServiceBound = false
        }
    }

    @Composable
    private fun AppNavigation() {
        val navController = rememberNavController()
        val context = LocalContext.current
        val settingsStore = remember { SettingsStore.get(context) }

        // Optimized back button handling
        var lastBackPressTime by remember { mutableLongStateOf(0L) }
        val remoteBackDoubleClick by settingsStore.remoteBackDoubleClick.collectAsState(initial = false)

        BackHandler {
            val currentTime = System.currentTimeMillis()
            if (remoteBackDoubleClick && (currentTime - lastBackPressTime) < DOUBLE_CLICK_THRESHOLD) {
                finish()
            } else {
                lastBackPressTime = currentTime
                if (navController.previousBackStackEntry != null) {
                    navController.popBackStack()
                } else if (!remoteBackDoubleClick) {
                    finish()
                }
            }
        }

        NavHost(
            navController = navController,
            startDestination = "main"
        ) {
            // Main menu
            composable("main") {
                MainMenuScreen(
                    onNavigateToConnect = { navController.navigate("connect") },
                    onNavigateToMouse = { navController.navigate("mouse") },
                    onNavigateToTouchpad = { navController.navigate("touchpad") },
                    onNavigateToKeyboard = { navController.navigate("keyboard") },
                    onNavigateToMedia = { navController.navigate("media") },
                    onNavigateToSettings = { navController.navigate("settings") },
                    onNavigateToDpad = { navController.navigate("dpad") }
                )
            }

            // Input modes - optimized navigation
            composable("keyboard") {
                KeyboardScreen(
                    onBack = { navController.popBackStack() },
                    onScrollPopup = { navController.navigate("scrollPopup/keyboard") }
                )
            }

            composable("touchpad") {
                TouchpadScreen(
                    onBack = { navController.popBackStack() },
                    onScrollPopup = { navController.navigate("scrollPopup/touchpad") }
                )
            }

            composable("mouse") {
                MouseScreen(
                    onBack = { navController.popBackStack() },
                    onScrollPopup = { navController.navigate("scrollPopup/mouse") }
                )
            }

            composable("dpad") {
                DpadScreen(
                    onBack = { navController.popBackStack() },
                    onScrollPopup = { navController.navigate("scrollPopup/dpad") }
                )
            }

            composable("media") {
                MediaScreen(
                    onBack = { navController.popBackStack() },
                    onScrollPopup = { navController.navigate("scrollPopup/media") }
                )
            }

            // Settings and configuration
            composable("settings") {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToMouseCalibration = { navController.navigate("mouseCalibration") }
                )
            }

            composable("connect") {
                ConnectToDeviceScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable("mouseCalibration") {
                MouseCalibrationScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            // Scroll popup with parent parameter
            composable(
                "scrollPopup/{parent}",
                arguments = listOf(navArgument("parent") { type = NavType.StringType })
            ) { backStackEntry ->
                val parent = backStackEntry.arguments?.getString("parent") ?: "main"
                ScrollPopupScreen(
                    parentScreen = parent,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
