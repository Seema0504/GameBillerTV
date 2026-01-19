package com.gamebiller.tvlock.ui

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.gamebiller.tvlock.domain.model.LockState
import com.gamebiller.tvlock.ui.screens.LockScreen
import com.gamebiller.tvlock.ui.screens.PairingScreen
import com.gamebiller.tvlock.ui.theme.GameBillerTVTheme
import com.gamebiller.tvlock.ui.viewmodel.LockViewModel
import com.gamebiller.tvlock.ui.viewmodel.PairingState
import com.gamebiller.tvlock.ui.viewmodel.PairingViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * Main Activity - Single full-screen activity for the app
 * Manages navigation between pairing and lock screens
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private val lockViewModel: LockViewModel by viewModels()
    private val pairingViewModel: PairingViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Timber.d("MainActivity created")
        
        // Enable immersive mode (hide system UI)
        setupImmersiveMode()
        
        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
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
            PairingScreen(
                pairingState = pairingState,
                onPairClick = { code ->
                    pairingViewModel.pairDevice(code)
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        
        is LockState.Locked,
        is LockState.GracePeriod -> {
            LockScreen(
                lockState = lockState,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        is LockState.Unlocked -> {
            // TV is unlocked - show nothing (HDMI input visible)
            // In production, this could show a minimal overlay with shop/station info
            Timber.d("TV unlocked - showing HDMI input")
        }
    }
}
