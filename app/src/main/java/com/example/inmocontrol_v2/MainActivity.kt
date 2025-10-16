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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
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
        private const val HOLD_THRESHOLD_MS = 3000L // 3 seconds for Windows/Home key
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
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
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
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
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

        // Remote Back Button feature state
        var backPressStartTime by remember { mutableLongStateOf(0L) }
        var feedbackText by remember { mutableStateOf<String?>(null) }
        
        val remoteBackDoubleClick by remember {
            SettingsStore.get(context).remoteBackDoubleClick
        }.collectAsState(initial = false)
        
        val isConnected by HidClient.isConnected.collectAsState()

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

        // Remote Back Button Handler - Press duration detection
        BackHandler(enabled = remoteBackDoubleClick && isConnected) {
            val pressDuration = System.currentTimeMillis() - backPressStartTime
            
            if (backPressStartTime == 0L) {
                // First press - record start time
                backPressStartTime = System.currentTimeMillis()
            } else {
                // Release - check duration
                if (pressDuration < 500) {
                    // Single press - Send ESC
                    HidClient.sendEscapeKey()
                    feedbackText = "ESC"
                } else if (pressDuration >= HOLD_THRESHOLD_MS) {
                    // 3-second hold - Send Windows/Home key
                    HidClient.sendWindowsKey()
                    feedbackText = "Home"
                }
                
                // Reset for next press
                backPressStartTime = 0L
            }
        }

        // Default navigation back handler (when remote back disabled or not connected)
        BackHandler(enabled = !remoteBackDoubleClick || !isConnected) {
            if (navController.currentBackStackEntry?.destination?.route != NavRoutes.MainMenu) {
                navController.popBackStack()
            } else {
                finish()
            }
        }

        // Visual Feedback Text Overlay
        Box(modifier = Modifier.fillMaxSize()) {
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
                    onNavigateToMouseCalibration = { navController.navigate(NavRoutes.MouseCalibration) },
                    onNavigateToConnect = { navController.navigate(NavRoutes.ConnectDevice) }
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
        
        // Text feedback overlay (appears in center of screen)
        feedbackText?.let { text ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.title1,
                    color = Color.White,
                    modifier = Modifier
                        .background(
                            color = Color.Black.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }
            
            LaunchedEffect(text) {
                kotlinx.coroutines.delay(1000) // Show for 1 second
                feedbackText = null
            }
        }
    }
    }
}
