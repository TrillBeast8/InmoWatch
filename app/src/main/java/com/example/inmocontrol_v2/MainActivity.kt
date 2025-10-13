package com.example.inmocontrol_v2

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.Scaffold
import com.example.inmocontrol_v2.nav.NavRoutes
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable StrictMode in debug builds to catch performance issues
        if (BuildConfig.DEBUG) {
            enableStrictMode()
        }

        // Defer permission request until after UI is rendered to reduce startup frame skips
        setContent {
            InmoTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    AppNavigation()
                }
            }
        }

        // Request permissions AFTER setContent to avoid blocking UI rendering
        // This prevents the 238 frame skip issue
        requestBluetoothPermissions()
    }

    override fun onStart() {
        super.onStart()
        // Remove duplicate binding - already handled in onCreate/requestBluetoothPermissions
    }

    override fun onDestroy() {
        super.onDestroy()
        HidClient.unbindService(this)
    }

    private fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
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
        HidClient.bindService(this)
    }

    private fun enableStrictMode() {
        android.os.StrictMode.setThreadPolicy(
            android.os.StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .build()
        )
        android.os.StrictMode.setVmPolicy(
            android.os.StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .build()
        )
    }

    @Composable
    private fun AppNavigation() {
        val navController = rememberNavController()
        val context = LocalContext.current

        // Optimized back button handling - defer settings access until needed
        var lastBackPressTime by remember { mutableLongStateOf(0L) }
        val remoteBackDoubleClick by remember {
            SettingsStore.get(context).remoteBackDoubleClick
        }.collectAsState(initial = false)

        val screens = listOf(
            NavRoutes.Mouse,
            NavRoutes.Touchpad,
            NavRoutes.Keyboard,
            NavRoutes.Media,
            NavRoutes.DPad,
            NavRoutes.Settings
        )

        val navigateLeft: (String) -> Unit = { currentRoute ->
            val currentIndex = screens.indexOf(currentRoute)
            val nextIndex = if (currentIndex > 0) currentIndex - 1 else screens.size - 1
            navController.navigate(screens[nextIndex]) {
                popUpTo(NavRoutes.MainMenu)
            }
        }

        val navigateRight: (String) -> Unit = { currentRoute ->
            val currentIndex = screens.indexOf(currentRoute)
            val nextIndex = (currentIndex + 1) % screens.size
            navController.navigate(screens[nextIndex]) {
                popUpTo(NavRoutes.MainMenu)
            }
        }

        BackHandler(enabled = true) {
            val currentTime = System.currentTimeMillis()
            if (remoteBackDoubleClick) {
                if (currentTime - lastBackPressTime < DOUBLE_CLICK_THRESHOLD) {
                    // Exit app on double-press
                    finish()
                } else {
                    lastBackPressTime = currentTime
                    // On single press, navigate back if not on main menu
                    if (navController.currentBackStackEntry?.destination?.route != NavRoutes.MainMenu) {
                        navController.popBackStack()
                    }
                }
            } else {
                // Default behavior: navigate back or exit if at start
                if (navController.currentBackStackEntry?.destination?.route != NavRoutes.MainMenu) {
                    navController.popBackStack()
                } else {
                    finish()
                }
            }
        }

        NavHost(navController = navController, startDestination = NavRoutes.MainMenu) {
            composable(NavRoutes.MainMenu) {
                MainMenuScreen(
                    onNavigateToMouse = { navController.navigate(NavRoutes.Mouse) },
                    onNavigateToTouchpad = { navController.navigate(NavRoutes.Touchpad) },
                    onNavigateToKeyboard = { navController.navigate(NavRoutes.Keyboard) },
                    onNavigateToMedia = { navController.navigate(NavRoutes.Media) },
                    onNavigateToDpad = { navController.navigate(NavRoutes.DPad) },
                    onNavigateToSettings = { navController.navigate(NavRoutes.Settings) },
                    onNavigateToConnect = { navController.navigate(NavRoutes.ConnectDevice) }
                )
            }
            composable(NavRoutes.Mouse) {
                MouseScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateTo = { route -> navController.navigate(route) }
                )
            }
            composable(NavRoutes.Touchpad) {
                TouchpadScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateTo = { route -> navController.navigate(route) }
                )
            }
            composable(NavRoutes.Keyboard) {
                KeyboardScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateTo = { route -> navController.navigate(route) }
                )
            }
            composable(NavRoutes.Media) {
                MediaScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateTo = { route -> navController.navigate(route) }
                )
            }
            composable(NavRoutes.DPad) {
                DpadScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateTo = { route -> navController.navigate(route) },
                    onScrollPopup = { /* Scroll mode is built into D-pad screen */ }
                )
            }
            composable(NavRoutes.Settings) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToMouseCalibration = { navController.navigate(NavRoutes.MouseCalibration) }
                )
            }
            composable(NavRoutes.MouseCalibration) {
                MouseCalibrationScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(NavRoutes.ConnectDevice) {
                ConnectToDeviceScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
