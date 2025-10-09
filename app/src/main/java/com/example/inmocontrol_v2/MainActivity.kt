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
import androidx.navigation.NavType
import androidx.navigation.navArgument
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
        // Bind to HidClient if permissions are granted
        if (hasBluetoothPermissions()) {
            HidClient.bindService(this)
        }
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

    @Composable
    private fun AppNavigation() {
        val navController = rememberNavController()
        val context = LocalContext.current
        val settingsStore = remember { SettingsStore.get(context) }

        // Optimized back button handling
        var lastBackPressTime by remember { mutableLongStateOf(0L) }
        val remoteBackDoubleClick by settingsStore.remoteBackDoubleClick.collectAsState(initial = false)

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
                    onScrollPopup = { navController.navigate("scrollPopup/mouse") },
                    onSwipeLeft = { navigateLeft(NavRoutes.Mouse) },
                    onSwipeRight = { navigateRight(NavRoutes.Mouse) }
                )
            }
            composable(NavRoutes.Touchpad) {
                TouchpadScreen(
                    onBack = { navController.popBackStack() },
                    onScrollPopup = { navController.navigate("scrollPopup/touchpad") },
                    onSwipeLeft = { navigateLeft(NavRoutes.Touchpad) },
                    onSwipeRight = { navigateRight(NavRoutes.Touchpad) }
                )
            }
            composable(NavRoutes.Keyboard) {
                KeyboardScreen(
                    onBack = { navController.popBackStack() },
                    onScrollPopup = { navController.navigate("scrollPopup/keyboard") },
                    onSwipeLeft = { navigateLeft(NavRoutes.Keyboard) },
                    onSwipeRight = { navigateRight(NavRoutes.Keyboard) }
                )
            }
            composable(NavRoutes.Media) {
                MediaScreen(
                    onBack = { navController.popBackStack() },
                    onScrollPopup = { navController.navigate("scrollPopup/media") },
                    onSwipeLeft = { navigateLeft(NavRoutes.Media) },
                    onSwipeRight = { navigateRight(NavRoutes.Media) }
                )
            }
            composable(NavRoutes.DPad) {
                DpadScreen(
                    onBack = { navController.popBackStack() },
                    onScrollPopup = { navController.navigate("scrollPopup/dpad") },
                    onSwipeLeft = { navigateLeft(NavRoutes.DPad) },
                    onSwipeRight = { navigateRight(NavRoutes.DPad) }
                )
            }
            composable(NavRoutes.Settings) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToMouseCalibration = { navController.navigate(NavRoutes.MouseCalibration) },
                    onSwipeLeft = { navigateLeft(NavRoutes.Settings) },
                    onSwipeRight = { navigateRight(NavRoutes.Settings) }
                )
            }
            composable(NavRoutes.MouseCalibration) {
                MouseCalibrationScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "scrollPopup/{parent}",
                arguments = listOf(navArgument("parent") { type = NavType.StringType })
            ) { backStackEntry ->
                ScrollPopupScreen(
                    parentScreen = backStackEntry.arguments?.getString("parent") ?: "unknown",
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
