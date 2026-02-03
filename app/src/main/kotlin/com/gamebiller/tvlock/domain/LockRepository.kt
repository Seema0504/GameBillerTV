package com.gamebiller.tvlock.domain

import com.gamebiller.tvlock.data.remote.AuditEventRequest
import com.gamebiller.tvlock.data.remote.PairDeviceRequest
import com.gamebiller.tvlock.domain.model.AuditEvent
import com.gamebiller.tvlock.domain.model.DeviceInfo
import com.gamebiller.tvlock.domain.model.StationStatus
import com.gamebiller.tvlock.data.remote.ApiService
import com.gamebiller.tvlock.data.local.DevicePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LockRepository @Inject constructor(
    private val apiService: ApiService,
    private val devicePreferences: DevicePreferences,
    private val auditDao: com.gamebiller.tvlock.data.local.dao.AuditDao,
    private val moshi: com.squareup.moshi.Moshi,
    @com.gamebiller.tvlock.di.IoDispatcher private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher
) {
    
    private val metadataAdapter = moshi.adapter(com.gamebiller.tvlock.domain.model.AuditMetadata::class.java)
    private val auditFlushMutex = kotlinx.coroutines.sync.Mutex()

    /**
     * Get device information as Flow
     */
    fun getDeviceInfo(): Flow<DeviceInfo?> {
        return devicePreferences.getDeviceInfo()
    }
    
    /**
     * Check if device is paired
     */
    fun isPaired(): Flow<Boolean> {
        return devicePreferences.isPaired()
    }
    
    /**
     * Pair device with a station using station code
     */
    suspend fun pairDevice(stationCode: String): Result<DeviceInfo> {
        return kotlinx.coroutines.withContext(ioDispatcher) {
             try {
                val deviceId = devicePreferences.getDeviceId()
                val request = PairDeviceRequest(
                    stationCode = stationCode,
                    deviceId = deviceId,
                    deviceName = android.os.Build.MODEL ?: "Unknown Android TV"
                )
                
                Timber.d("Pairing device $deviceId with station code: $stationCode")
                val response = apiService.pairDevice(request)
            
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    // Handle potential nulls from backend
                    val deviceInfo = DeviceInfo(
                        deviceId = body.deviceId ?: deviceId, // Use response ID or fallback to local ID
                        shopId = body.shopId ?: 0, // Default to 0 if missing
                        stationId = body.stationId,
                        shopName = body.shopName ?: "Game Shop", // Default name
                        stationName = body.stationName,
                        token = body.token,
                        isPaired = true
                    )
                    
                    // Save pairing info locally
                    devicePreferences.savePairingInfo(deviceInfo)
                    
                    // Send audit event
                    sendAuditEvent(
                        AuditEvent(
                            event = com.gamebiller.tvlock.domain.model.AuditEventType.DEVICE_PAIRED,
                            stationId = deviceInfo.stationId,
                            deviceId = deviceId,
                            timestamp = Instant.now().toString(),
                            metadata = com.gamebiller.tvlock.domain.model.AuditMetadata.DevicePaired(
                                shopName = deviceInfo.shopName,
                                stationName = deviceInfo.stationName
                            )
                        )
                    )
                    
                    Timber.d("Device paired successfully: ${deviceInfo.shopName} - ${deviceInfo.stationName}")
                    Result.success(deviceInfo)
                } else {
                    val errorMsg = "Pairing failed: ${response.code()}"
                    Timber.e(errorMsg)
                    
                    // Auto-fix for Duplicate Key (400): Reset Device ID so next attempt uses a fresh one
                    if (response.code() == 400) {
                        Timber.w("Collision detected (400). Resetting local Device ID to resolve unique constraint.")
                        devicePreferences.clearPairingData()
                    }
                    
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error pairing device")
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get current station status from backend
     * Note: This no longer triggers side-effect flushing internally.
     * The Caller (ViewModel) must call flushAuditLogs() separately.
     * 
     * BACKEND CONTRACT (CRITICAL - DO NOT MODIFY):
     * - HTTP 200 → Parse status (RUNNING/STOPPED/PAUSED/NOT_STARTED)
     * - HTTP 401 → TokenInvalid → MUST unpair device
     * - HTTP 403 → FeatureDisabled → MUST remain paired, just lock
     * - HTTP 429 → RateLimited → Back off, remain paired
     * - HTTP 5xx → Unknown → Fail-safe lock, remain paired
     */
    suspend fun getStationStatus(): StationStatus {
        return kotlinx.coroutines.withContext(ioDispatcher) {
            try {
                val deviceInfo = devicePreferences.getDeviceInfo().first()
                
                if (deviceInfo == null || !deviceInfo.isPaired || deviceInfo.token.isBlank()) {
                    Timber.w("Device not paired or missing token")
                    return@withContext StationStatus.Unknown
                }
                
                val authHeader = "Bearer ${deviceInfo.token}"
                val response = apiService.getStationStatus(token = authHeader)
                
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val status = StationStatus.fromString(body.status)
                    
                    // Sync names if provided (Dynamic updates)
                    if (!body.shopName.isNullOrBlank() && !body.stationName.isNullOrBlank()) {
                         if (body.shopName != deviceInfo.shopName || body.stationName != deviceInfo.stationName) {
                             Timber.d("Syncing updated station info: ${body.shopName} - ${body.stationName}")
                             devicePreferences.updateStationNames(body.shopName, body.stationName)
                         }
                    }
                    
                    Timber.d("Station status: $status")
                    status
                } else {
                    // CRITICAL: Proper HTTP error handling per backend contract
                    val httpCode = response.code()
                    Timber.e("Failed to get station status: $httpCode ${response.message()}")
                    
                    when (httpCode) {
                        // HTTP 401: Token revoked/invalid - MUST trigger unpair
                        401 -> {
                            Timber.w("HTTP 401: Token invalid/revoked - device must unpair")
                            StationStatus.TokenInvalid
                        }
                        
                        // HTTP 403: Feature disabled (subscription lapsed, etc.)
                        // MUST remain paired - NEVER clear token on 403
                        403 -> {
                            Timber.w("HTTP 403: Feature disabled - device remains paired, locking screen")
                            StationStatus.FeatureDisabled
                        }
                        
                        // HTTP 429: Rate limited - back off polling
                        429 -> {
                            val retryAfter = parseRetryAfterHeader(response.headers()["Retry-After"])
                            Timber.w("HTTP 429: Rate limited, retry after ${retryAfter}s")
                            StationStatus.RateLimited(retryAfter)
                        }
                        
                        // All other errors (5xx, network, etc.) - fail-safe lock
                        else -> {
                            Timber.w("HTTP $httpCode: Unknown error - fail-safe lock")
                            StationStatus.Unknown
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error getting station status")
                StationStatus.Unknown
            }
        }
    }
    
    /**
     * Parse Retry-After header from rate limit response
     * Returns seconds to wait, defaults to 60 if header missing/invalid
     */
    private fun parseRetryAfterHeader(retryAfter: String?): Int {
        if (retryAfter.isNullOrBlank()) return 60
        
        return try {
            // Try parsing as seconds (e.g., "120")
            retryAfter.toIntOrNull() ?: 60
        } catch (e: Exception) {
            Timber.w("Failed to parse Retry-After header: $retryAfter")
            60
        }
    }
    
    /**
     * Store audit event in local DB and attempt to flush queue
     */
    suspend fun sendAuditEvent(event: AuditEvent) {
         kotlinx.coroutines.withContext(ioDispatcher) {
            try {
                // 1. Save to local DB first (Queue)
                val metadataJson = if (event.metadata != null) {
                    metadataAdapter.toJson(event.metadata)
                } else null

                val entity = com.gamebiller.tvlock.data.local.entity.AuditLogEntity(
                    event = event.event.eventName,
                    stationId = event.stationId,
                    deviceId = event.deviceId,
                    timestamp = event.timestamp,
                    metadataJson = metadataJson
                )
                
                auditDao.insert(entity)
                Timber.d("Audit event queued locally: ${event.event.eventName}")
                
                // 2. Attempt to flush queue immediately (blocking in this context)
                flushPendingLogsInternal()
                
            } catch (e: Exception) {
                Timber.e(e, "Error saving audit event locally")
            }
        }
    }

    /**
     * Public method for ViewModel to trigger sync
     */
    suspend fun flushAuditLogs() {
        kotlinx.coroutines.withContext(ioDispatcher) {
             // Guard against overlapping flushes
             auditFlushMutex.withLock {
                 flushPendingLogsInternal()
             }
        }
    }

    /**
     * Internal sync logic
     */
    private suspend fun flushPendingLogsInternal() {
        try {
            val pendingLogs = auditDao.getAllLogs()
            if (pendingLogs.isEmpty()) return

            // Fetch token for API calls
            val deviceInfo = devicePreferences.getDeviceInfo().first()
            if (deviceInfo == null || deviceInfo.token.isBlank()) {
               Timber.w("Cannot flush logs: No token available")
               return
            }
            val authHeader = "Bearer ${deviceInfo.token}"

            Timber.d("Flushing ${pendingLogs.size} pending audit logs")
// ... logic continues ...

            val logsToDelete = mutableListOf<Long>()

            for (log in pendingLogs) {
                val metadata = if (log.metadataJson != null) {
                    try {
                        metadataAdapter.fromJson(log.metadataJson)
                    } catch (e: Exception) { null }
                } else null

                val request = AuditEventRequest(
                    event = log.event,
                    stationId = log.stationId,
                    deviceId = log.deviceId,
                    timestamp = log.timestamp,
                    metadata = metadata
                )

                try {
                    val response = apiService.sendAuditEvent(authHeader, request)
                    if (response.isSuccessful) {
                        logsToDelete.add(log.id)
                    } else {
                        // Stop trying if we hit an error (likely network down or server issue)
                        Timber.w("Failed to send log ${log.id}, stopping flush. Code: ${response.code()}")
                        break 
                    }
                } catch (e: Exception) {
                    Timber.w("Network error sending log ${log.id}, stopping flush")
                    break
                }
            }

            // Remove successfully sent logs
            if (logsToDelete.isNotEmpty()) {
                auditDao.deleteByIds(logsToDelete)
                Timber.d("Successfully synced ${logsToDelete.size} audit logs")
            }
            
        } catch (e: Exception) {
            Timber.e(e, "Error flushing audit logs")
        }
    }
    
    /**
     * Clear pairing data (for testing/reset)
     */
    suspend fun unpairDevice() {
        kotlinx.coroutines.withContext(ioDispatcher) {
            devicePreferences.clearPairingData()
            Timber.d("Device unpaired")
        }
    }
}
