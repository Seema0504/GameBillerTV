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
                    deviceId = deviceId
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
                    val errorMsg = "Pairing failed: ${response.code()} ${response.message()}"
                    Timber.e(errorMsg)
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
                val response = apiService.getStationStatus(deviceInfo.stationId, authHeader)
                
                if (response.isSuccessful && response.body() != null) {
                    val status = StationStatus.fromString(response.body()!!.status)
                    Timber.d("Station status: $status")
                    status
                } else {
                    Timber.e("Failed to get station status: ${response.code()} ${response.message()}")
                    // Explicitly map auth errors
                    if (response.code() == 401 || response.code() == 403) {
                         StationStatus.TokenInvalid
                    } else {
                         StationStatus.Unknown
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error getting station status")
                StationStatus.Unknown
            }
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
                    val response = apiService.sendAuditEvent(request)
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
