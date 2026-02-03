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
    private var rateLimitBackoffSeconds = 0  // For HTTP 429 handling
    
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
                        
                        // Respect rate limit backoff if set (HTTP 429)
                        val delayMs = if (rateLimitBackoffSeconds > 0) {
                            Timber.d("Rate limit active, backing off for ${rateLimitBackoffSeconds}s")
                            rateLimitBackoffSeconds * 1000L
                        } else {
                            POLLING_INTERVAL_MS
                        }
                        delay(delayMs)
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
     * 
     * BACKEND CONTRACT HANDLING:
     * - TokenInvalid (401) → MUST unpair device
     * - FeatureDisabled (403) → Lock screen, NEVER unpair, continue polling
     * - RateLimited (429) → Lock screen, back off polling, NEVER unpair
     * - Unknown (5xx/network) → Grace period then lock, NEVER unpair
     * - Normal statuses → Lock/unlock as appropriate
     */
    private suspend fun pollStationStatus() {
        val deviceInfo = repository.getDeviceInfo().first() ?: return
        val status = repository.getStationStatus()
        
        when (status) {
            // HTTP 401: Token revoked - ONLY case that triggers unpair
            is StationStatus.TokenInvalid -> {
                Timber.w("HTTP 401: Token invalid/revoked - Unpairing device")
                repository.unpairDevice()
                _lockState.value = LockState.Unpaired
                return
            }
            
            // HTTP 403: Feature disabled (subscription lapsed, etc.)
            // CRITICAL: NEVER unpair on 403 - device stays paired, just lock
            is StationStatus.FeatureDisabled -> {
                Timber.w("HTTP 403: Feature disabled - Locking but STAYING PAIRED")
                resetFailure() // Not a failure, just a controlled state
                updateLockState(
                    status,
                    deviceInfo.shopName,
                    deviceInfo.stationName,
                    deviceInfo.deviceId,
                    deviceInfo.stationId
                )
                lastKnownStatus = status
                // Continue normal polling - feature may be re-enabled
            }
            
            // HTTP 429: Rate limited - back off polling
            // CRITICAL: NEVER unpair on 429
            is StationStatus.RateLimited -> {
                Timber.w("HTTP 429: Rate limited - Backing off for ${status.retryAfterSeconds}s")
                resetFailure() // Not a failure, just rate limiting
                updateLockState(
                    status,
                    deviceInfo.shopName,
                    deviceInfo.stationName,
                    deviceInfo.deviceId,
                    deviceInfo.stationId
                )
                // Back off polling for the specified duration
                rateLimitBackoffSeconds = status.retryAfterSeconds
            }
            
            // Network error / Unknown status - use grace period logic
            is StationStatus.Unknown -> {
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
            }
            
            // Normal status responses (RUNNING, STOPPED, PAUSED, NOT_STARTED)
            else -> {
                // Valid response received - RESET failure counters and rate limit
                resetFailure()
                rateLimitBackoffSeconds = 0
                
                updateLockState(status, deviceInfo.shopName, deviceInfo.stationName, deviceInfo.deviceId, deviceInfo.stationId)
                lastKnownStatus = status
                
                // Explicitly trigger flush since Repository no longer does it
                viewModelScope.launch {
                    repository.flushAuditLogs()
                }
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
     * 
     * NOTE: This function ONLY handles lock screen display.
     * It does NOT trigger unpair - that is handled separately in pollStationStatus()
     * for TokenInvalid ONLY.
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
                // HTTP 403: Feature disabled - lock but NEVER unpair
                is StationStatus.FeatureDisabled -> com.gamebiller.tvlock.domain.model.LockReason.FEATURE_DISABLED
                // HTTP 429: Rate limited - lock temporarily, NEVER unpair
                is StationStatus.RateLimited -> com.gamebiller.tvlock.domain.model.LockReason.RATE_LIMITED
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
    
    /**
     * Trigger manual unpair from UI (Admin Reset)
     */
    fun triggerUnpair() {
        viewModelScope.launch {
            repository.unpairDevice()
            _lockState.value = LockState.Unpaired
            Timber.d("Manual unpair triggered")
        }
    }
}
