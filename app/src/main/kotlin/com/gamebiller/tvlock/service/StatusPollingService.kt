package com.gamebiller.tvlock.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.gamebiller.tvlock.R
import com.gamebiller.tvlock.domain.LockRepository
import com.gamebiller.tvlock.domain.model.LockState
import com.gamebiller.tvlock.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject

/**
 * Foreground service for status polling and lock enforcement
 * Monitors lock state and brings MainActivity to foreground when locked
 */
@AndroidEntryPoint
class StatusPollingService : Service() {
    
    @Inject
    lateinit var repository: LockRepository
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var statusPollingJob: Job? = null
    private var enforcementJob: Job? = null
    private val isLocked = java.util.concurrent.atomic.AtomicBoolean(false)
    
    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "gamebiller_lock_service"
        private const val STATUS_POLL_INTERVAL_MS = 15000L // 15 seconds (Network/DB)
        private const val ENFORCEMENT_INTERVAL_MS = 2000L // 2 seconds (UI Enforcement)
    }
    
    override fun onCreate() {
        super.onCreate()
        Timber.d("StatusPollingService created")
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("StatusPollingService started")
        
        // Start foreground service with notification
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        
        // Start monitoring (Split into Polling and Enforcement)
        startMonitoring()
        
        return START_STICKY // Restart service if killed
    }
    
    private fun startMonitoring() {
        // 1. Status Polling Job (Slower, Network/DB heavy)
        statusPollingJob?.cancel()
        statusPollingJob = serviceScope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val deviceInfo = repository.getDeviceInfo().first()
                    if (deviceInfo != null && deviceInfo.isPaired) {
                        val status = repository.getStationStatus()
                        
                        // Default to Locked if status check fails? 
                        // Current logic: status != Running means Locked.
                        // If Unknown (network fail), it's != Running, so it LOCKS. This is safe.
                        val shouldBeLocked = status != com.gamebiller.tvlock.domain.model.StationStatus.Running
                        isLocked.set(shouldBeLocked)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error in status polling")
                }
                delay(STATUS_POLL_INTERVAL_MS)
            }
        }

        // 2. Lock Enforcement Job (Fast, UI driven)
        enforcementJob?.cancel()
        enforcementJob = serviceScope.launch(Dispatchers.Main) {
            while (isActive) {
                if (isLocked.get()) {
                    bringActivityToForeground()
                }
                delay(ENFORCEMENT_INTERVAL_MS)
            }
        }
    }
    
    /**
     * Bring MainActivity to foreground
     */
    private fun bringActivityToForeground() {
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
        } catch (e: Exception) {
            Timber.e(e, "Failed to bring activity to foreground")
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        statusPollingJob?.cancel()
        enforcementJob?.cancel()
        Timber.d("StatusPollingService destroyed")
    }
    
    /**
     * Create notification channel for Android O+
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "GameBiller Lock Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitoring station status"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Create foreground service notification
     */
    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.service_notification_title))
            .setContentText(getString(R.string.service_notification_text))
            .setSmallIcon(R.drawable.ic_lock_notification)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
}
