package com.gamebiller.tvlock.ui.viewmodel

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import com.gamebiller.tvlock.domain.LockRepository
import com.gamebiller.tvlock.domain.model.AuditEvent
import com.gamebiller.tvlock.domain.model.AuditMetadata
import com.gamebiller.tvlock.domain.model.AuditEventType
import com.gamebiller.tvlock.domain.model.LockState
import com.gamebiller.tvlock.domain.model.StationStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject

/**
 * ViewModel managing lock screen state and status polling
 */
@HiltViewModel
class LockViewModel @Inject constructor(
    private val repository: LockRepository
) : ViewModel() {
    
    private val _lockState = MutableStateFlow<LockState>(LockState.Unpaired)
    val lockState: StateFlow<LockState> = _lockState.asStateFlow()
    
    private var gracePeriodActive = false
    private var lastKnownStatus: StationStatus = StationStatus.Unknown
    private var consecutiveFailures = 0
    
    companion object {
        private const val POLLING_INTERVAL_MS = 12_000L // 12 seconds
        private const val GRACE_PERIOD_SECONDS = 30
        private const val MAX_CONSECUTIVE_FAILURES = 3
    }
    
    init {
        // Check pairing status initially
        viewModelScope.launch {
            val isPaired = repository.isPaired().first()
            if (isPaired) {
                val deviceInfo = repository.getDeviceInfo().first()
                if (deviceInfo != null) {
                    // Send app started event
                    repository.sendAuditEvent(
                        AuditEvent(
                            event = AuditEventType.APP_STARTED,
                            stationId = deviceInfo.stationId,
                            deviceId = deviceInfo.deviceId,
                            timestamp = Instant.now().toString(),
                            metadata = AuditMetadata.AppRestarted(boot = true)
                        )
                    )
                }
            } else {
                Timber.d("Device not paired")
                _lockState.value = LockState.Unpaired
            }
        }
    }
    
    /**
     * Start polling station status using Lifecycle-aware scope
     * This ensures polling stops when the UI is not visible (Activity stopped)
     */
    fun startStatusPolling(lifecycleOwner: LifecycleOwner) {
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                Timber.d("Polling lifecycle started")
                
                while (isActive) {
                    val isPaired = repository.isPaired().first()
                    
                    if (isPaired) {
                        // Device is paired - poll status
                        pollStationStatus()
                        delay(POLLING_INTERVAL_MS)
                    } else {
                        // Device not paired - ensure state is updated
                        if (_lockState.value !is LockState.Unpaired) {
                             _lockState.value = LockState.Unpaired
                        }
                        // Check again frequently to catch pairing event quickly
                        delay(1000) 
                    }
                }
            }
            Timber.d("Polling lifecycle stopped")
        }
    }
    
    /**
     * Poll station status once
     */
    private suspend fun pollStationStatus() {
        val deviceInfo = repository.getDeviceInfo().first() ?: return
        val status = repository.getStationStatus()
        
        // Check if we got a valid response
        if (status == StationStatus.TokenInvalid) {
            Timber.w("Token invalid or device deleted from backend - Unpairing device")
            repository.unpairDevice()
            return
        } else if (status == StationStatus.Unknown) {
            consecutiveFailures++
            Timber.w("Failed to get station status (failure $consecutiveFailures/$MAX_CONSECUTIVE_FAILURES)")
                        if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                // LOCK immediately if not running, otherwise try grace period
                if (lastKnownStatus !is StationStatus.Running) {
                    updateLockState(
                        StationStatus.Unknown,
                        deviceInfo.shopName,
                        deviceInfo.stationName,
                        deviceInfo.deviceId,
                        deviceInfo.stationId
                    )
                } else {
                    // Start grace period only if not already active
                    startGracePeriod(deviceInfo.shopName, deviceInfo.stationName, deviceInfo.deviceId, deviceInfo.stationId)
                }
            }
        } else {
            // Valid response received - RESET failure counters
            resetFailure()
            
            updateLockState(status, deviceInfo.shopName, deviceInfo.stationName, deviceInfo.deviceId, deviceInfo.stationId)
            lastKnownStatus = status
            
            // Explicitly trigger flush since Repository no longer does it
            viewModelScope.launch {
                repository.flushAuditLogs()
            }
        }
    }
    
    private fun resetFailure() {
        consecutiveFailures = 0
        gracePeriodActive = false 
    }
    
    /**
     * Start grace period countdown with Single-Source-of-Truth guard
     */
    private fun startGracePeriod(shopName: String, stationName: String, deviceId: String, stationId: Int) {
        if (gracePeriodActive) return
        
        gracePeriodActive = true
        Timber.d("Starting grace period")
        
        viewModelScope.launch {
            repository.sendAuditEvent(
                AuditEvent(
                    event = AuditEventType.GRACE_PERIOD_STARTED,
                    stationId = stationId,
                    deviceId = deviceId,
                    timestamp = Instant.now().toString(),
                    metadata = AuditMetadata.NetworkLost(retryCount = consecutiveFailures)
                )
            )

            for (secondsRemaining in GRACE_PERIOD_SECONDS downTo 0) {
                // If we recovered in the meantime (gracePeriodActive set to false by success), abort
                if (!gracePeriodActive) {
                    Timber.d("Grace period aborted due to recovery")
                    return@launch
                }

                _lockState.value = LockState.GracePeriod(
                    secondsRemaining = secondsRemaining,
                    shopName = shopName,
                    stationName = stationName
                )
                
                if (secondsRemaining > 0) {
                    delay(1000)
                }
            }
            
            // If still active at end, LOCK
            if (gracePeriodActive) {
                Timber.d("Grace period expired, locking TV")
                _lockState.value = LockState.Locked(
                    reason = com.gamebiller.tvlock.domain.model.LockReason.NETWORK_FAILURE,
                    shopName = shopName,
                    stationName = stationName
                )
                
                repository.sendAuditEvent(
                    AuditEvent(
                        event = AuditEventType.GRACE_PERIOD_EXPIRED,
                        stationId = stationId,
                        deviceId = deviceId,
                        timestamp = Instant.now().toString(),
                        metadata = AuditMetadata.GracePeriodExpired(durationSeconds = GRACE_PERIOD_SECONDS)
                    )
                )
                gracePeriodActive = false
            }
        }
    }
    
    /**
     * Update lock state based on station status
     */
    private fun updateLockState(
        status: StationStatus,
        shopName: String,
        stationName: String,
        deviceId: String,
        stationId: Int
    ) {
        val currentState = _lockState.value
        val newState = if (status.shouldLock()) {
            val reason = when (status) {
                is StationStatus.Stopped -> com.gamebiller.tvlock.domain.model.LockReason.SESSION_STOPPED
                is StationStatus.Paused -> com.gamebiller.tvlock.domain.model.LockReason.SESSION_PAUSED
                is StationStatus.NotStarted -> com.gamebiller.tvlock.domain.model.LockReason.SESSION_NOT_STARTED
                is StationStatus.TokenInvalid -> com.gamebiller.tvlock.domain.model.LockReason.TOKEN_INVALID
                else -> com.gamebiller.tvlock.domain.model.LockReason.SESSION_NOT_ACTIVE
            }
            LockState.Locked(reason, shopName, stationName)
        } else {
            LockState.Unlocked(shopName, stationName)
        }
        
        // Avoid state churn if identical
        if (currentState == newState) return

        _lockState.value = newState

        // Send audit events on state transitions
        if (currentState is LockState.Locked && newState is LockState.Unlocked) {
            viewModelScope.launch {
                repository.sendAuditEvent(
                    AuditEvent(
                        event = AuditEventType.TV_UNLOCKED,
                        stationId = stationId,
                        deviceId = deviceId,
                        timestamp = Instant.now().toString(),
                        metadata = AuditMetadata.Generic(mapOf("status" to "RUNNING"))
                    )
                )
            }
        } else if (currentState is LockState.Unlocked && newState is LockState.Locked) {
           viewModelScope.launch {
               // Determine reason for metadata
               val meta = if (newState is LockState.Locked) AuditMetadata.ManualLock(newState.reason.toString()) else null 
               
               repository.sendAuditEvent(
                    AuditEvent(
                        event = AuditEventType.TV_LOCKED,
                        stationId = stationId,
                        deviceId = deviceId,
                        timestamp = Instant.now().toString(),
                        metadata = meta
                    )
                )
           }
        }
    }
}
