package com.gamebiller.tvlock.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.gamebiller.tvlock.domain.model.LockState
import com.gamebiller.tvlock.ui.screens.LockScreen
import com.gamebiller.tvlock.ui.screens.PairingScreen
import com.gamebiller.tvlock.ui.theme.GameBillerTVTheme
import com.gamebiller.tvlock.ui.viewmodel.LockViewModel
import com.gamebiller.tvlock.ui.viewmodel.PairingState
import com.gamebiller.tvlock.ui.viewmodel.PairingViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Main Activity - Single full-screen activity for the app
 * Manages navigation between pairing and lock screens
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private val lockViewModel: LockViewModel by viewModels()
    private val pairingViewModel: PairingViewModel by viewModels()

    // Permission launcher for Location (required for FGS on Android 14+)
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Timber.d("Permission granted, starting service")
            startLockMonitoringService()
        } else {
            Timber.w("Permission denied, service cannot run properly")
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Timber.d("MainActivity created")
        
        // Enable immersive mode (hide system UI)
        setupImmersiveMode()
        
        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // Check for Overlay Permission (Required for background activity start on Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(this)) {
            Timber.w("Overlay permission missing - requesting")
            val intent = Intent(
                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }

        // Start foreground service (request permission if needed)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                startLockMonitoringService()
            } else {
                Timber.d("Requesting location permission for foreground service")
                permissionLauncher.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        } else {
            startLockMonitoringService()
        }
        
        // Observe lock state changes and handle foreground/background
        lifecycleScope.launch {
            lockViewModel.lockState.collect { state ->
                handleLockStateChange(state)
            }
        }
        
        setContent {
            GameBillerTVTheme {
                MainScreen(
                    lockViewModel = lockViewModel,
                    pairingViewModel = pairingViewModel
                )
            }
        }
    }
    
    /**
     * Start foreground service to monitor lock state
     */
    private fun startLockMonitoringService() {
        Timber.d("Starting StatusPollingService...")
        try {
            val serviceIntent = Intent(this, com.gamebiller.tvlock.service.StatusPollingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Timber.d("Starting foreground service (API ${Build.VERSION.SDK_INT})")
                startForegroundService(serviceIntent)
            } else {
                Timber.d("Starting service (API ${Build.VERSION.SDK_INT})")
                startService(serviceIntent)
            }
            Timber.d("StatusPollingService start command sent")
        } catch (e: Exception) {
            Timber.e(e, "Failed to start StatusPollingService")
        }
    }
    
    /**
     * Handle lock state changes - launch Fire TV launcher when unlocked
     */
    private fun handleLockStateChange(state: LockState) {
        when (state) {
            is LockState.Unlocked -> {
                Timber.d("TV unlocked - finishing activity")
                // Finish the activity to completely remove it from the screen.
                // The StatusPollingService will automatically restart it when the state becomes Locked.
                finish()
            }
            is LockState.Locked, is LockState.GracePeriod -> {
                Timber.d("TV locked - bringing app to foreground")
                // Bring this activity to foreground
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(intent)
            }
            else -> {
                Timber.d("Unpaired state")
            }
        }
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Timber.d("onNewIntent called")
        setIntent(intent)
    }
    
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        Timber.d("onUserLeaveHint called - attempting to intercept Home press")
        val state = lockViewModel.lockState.value
        if (state is LockState.Locked || state is LockState.GracePeriod) {
            Timber.d("App is locked - forcing back to foreground")
            // Immediately bring activity to front to cancel the "Leave"
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
            startActivity(intent)
        }
    }
    
    override fun onStart() {
        super.onStart()
        Timber.d("onStart called")
        // Re-enable immersive mode
        setupImmersiveMode()
    }
    
    /**
     * Setup immersive mode to hide system UI
     */
    private fun setupImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = 
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
    
    /**
     * Disable back button
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Do nothing - back button is disabled
        Timber.d("Back button pressed (ignored)")
    }
    
    override fun onResume() {
        super.onResume()
        // Re-enable immersive mode when app resumes
        setupImmersiveMode()
    }
}

/**
 * Main screen composable
 * Shows pairing screen if not paired, otherwise shows lock screen
 */
@Composable
fun MainScreen(
    lockViewModel: LockViewModel,
    pairingViewModel: PairingViewModel
) {
    val lockState by lockViewModel.lockState.collectAsState()
    val pairingState by pairingViewModel.pairingState.collectAsState()
    val activity = androidx.compose.ui.platform.LocalContext.current as? ComponentActivity
    
    // Handle pairing success - start polling
    LaunchedEffect(pairingState) {
        if (pairingState is PairingState.Success) {
            Timber.d("Pairing successful, starting status polling")
            // In a real app we might trigger a re-check or just let the Loop in Activity handle it.
            // But since the loop in Activity starts on CREATE, it will be running.
            // It checks "isPaired". So if we just paired, we need to ensure the loop picks it up.
            // The loop checks isPaired() inside.
        }
    }
    
    // Start lifecycle-aware polling
    // This connects the UI lifecycle to the data polling
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
         lockViewModel.startStatusPolling(lifecycleOwner)
    }
    
    // Show appropriate screen based on state
    when (lockState) {
        is LockState.Unpaired -> {
            // Reset pairing state to Idle whenever we enter this screen
            LaunchedEffect(Unit) {
                pairingViewModel.resetState()
            }
            
            val context = androidx.compose.ui.platform.LocalContext.current
            PairingScreen(
                pairingState = pairingState,
                onPairClick = { code ->
                    pairingViewModel.pairDevice(code)
                },
                onExit = {
                     Timber.d("Kill switch activated")
                     try {
                         // 1. Stop the lock service
                         val intent = Intent(context, com.gamebiller.tvlock.service.StatusPollingService::class.java)
                         context.stopService(intent)
                         
                         // 2. Clear flags and Kill app
                         (context as? android.app.Activity)?.finishAndRemoveTask()
                         System.exit(0) // Ensure process death to stop all receivers/jobs
                     } catch (e: Exception) {
                         Timber.e(e, "Error during kill switch")
                     }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        
        is LockState.Locked,
        is LockState.GracePeriod -> {
            LockScreen(
                lockState = lockState,
                onUnpair = { lockViewModel.triggerUnpair() },
                modifier = Modifier.fillMaxSize()
            )
        }
        
        is LockState.Unlocked -> {
            // TV is unlocked - app moved to background
            // Show minimal transparent overlay (user can access TV normally)
            Timber.d("TV unlocked - app in background")
        }
    }
}
