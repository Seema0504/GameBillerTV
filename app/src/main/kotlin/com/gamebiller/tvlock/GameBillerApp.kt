package com.gamebiller.tvlock

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Main Application class for GameBiller TV Lock
 * Initializes Hilt dependency injection and global configurations
 */
@HiltAndroidApp
class GameBillerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        
        Timber.d("GameBiller TV Lock App initialized")
        
        // Set up global exception handler
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Timber.e(throwable, "Uncaught exception in thread: ${thread.name}")
            // In production, you might want to restart the app here
            // to maintain the lock screen
        }
    }
}
