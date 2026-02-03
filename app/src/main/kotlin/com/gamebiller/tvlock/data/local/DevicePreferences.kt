package com.gamebiller.tvlock.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.gamebiller.tvlock.domain.model.DeviceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages ENCRYPTED local storage for device pairing information.
 * Uses EncryptedSharedPreferences backed by Android Keystore for security.
 * 
 * TOKEN HANDLING CONTRACT:
 * - Tokens are PERMANENT device keys, NOT sessions
 * - Tokens NEVER expire
 * - Tokens are ONLY cleared on:
 *   1. HTTP 401 response (TokenInvalid - device revoked)
 *   2. Manual admin reset (10-tap + PIN)
 * - Tokens are NEVER cleared on HTTP 403 (FeatureDisabled)
 * 
 * SECURITY:
 * - Uses AES-256 encryption via MasterKey
 * - Keys stored in Android Keystore (hardware-backed if available)
 * - File: encrypted_device_prefs (encrypted on disk)
 */
@Singleton
class DevicePreferences @Inject constructor(
    private val context: Context
) {
    
    // In-memory cache for reactive updates
    private val _deviceInfoCache = MutableStateFlow<DeviceInfo?>(null)
    private val _isPairedCache = MutableStateFlow(false)
    
    companion object {
        private const val PREFS_FILE_NAME = "encrypted_device_prefs"
        
        // Keys for SharedPreferences
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_SHOP_ID = "shop_id"
        private const val KEY_STATION_ID = "station_id"
        private const val KEY_SHOP_NAME = "shop_name"
        private const val KEY_STATION_NAME = "station_name"
        private const val KEY_TOKEN = "token"
        private const val KEY_IS_PAIRED = "is_paired"
    }
    
    /**
     * Lazily initialized EncryptedSharedPreferences instance.
     * Uses MasterKey backed by Android Keystore for AES-256 encryption.
     */
    private val encryptedPrefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            EncryptedSharedPreferences.create(
                context,
                PREFS_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to create EncryptedSharedPreferences, falling back to standard prefs")
            // Fallback to standard SharedPreferences if encryption fails (should never happen in production)
            context.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)
        }
    }
    
    init {
        // Initialize cache from stored values
        refreshCache()
    }
    
    /**
     * Refresh in-memory cache from encrypted storage
     */
    private fun refreshCache() {
        try {
            val isPaired = encryptedPrefs.getBoolean(KEY_IS_PAIRED, false)
            _isPairedCache.value = isPaired
            
            if (isPaired) {
                _deviceInfoCache.value = DeviceInfo(
                    deviceId = encryptedPrefs.getString(KEY_DEVICE_ID, "") ?: "",
                    shopId = encryptedPrefs.getInt(KEY_SHOP_ID, 0),
                    stationId = encryptedPrefs.getInt(KEY_STATION_ID, 0),
                    shopName = encryptedPrefs.getString(KEY_SHOP_NAME, "") ?: "",
                    stationName = encryptedPrefs.getString(KEY_STATION_NAME, "") ?: "",
                    token = encryptedPrefs.getString(KEY_TOKEN, "") ?: "",
                    isPaired = true
                )
            } else {
                _deviceInfoCache.value = null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error refreshing preferences cache")
            _isPairedCache.value = false
            _deviceInfoCache.value = null
        }
    }
    
    /**
     * Get or generate device ID
     * Device ID is generated once and NEVER changes (survives unpair)
     */
    suspend fun getDeviceId(): String = withContext(Dispatchers.IO) {
        var deviceId = encryptedPrefs.getString(KEY_DEVICE_ID, null)
        
        if (deviceId.isNullOrBlank()) {
            deviceId = "TV-${UUID.randomUUID().toString().take(8).uppercase()}"
            encryptedPrefs.edit()
                .putString(KEY_DEVICE_ID, deviceId)
                .apply()
            Timber.d("Generated new device ID: $deviceId")
        }
        
        deviceId
    }
    
    /**
     * Save device pairing information (encrypted)
     * Called ONLY after successful pairing with backend
     */
    suspend fun savePairingInfo(deviceInfo: DeviceInfo) = withContext(Dispatchers.IO) {
        encryptedPrefs.edit()
            .putString(KEY_DEVICE_ID, deviceInfo.deviceId)
            .putInt(KEY_SHOP_ID, deviceInfo.shopId)
            .putInt(KEY_STATION_ID, deviceInfo.stationId)
            .putString(KEY_SHOP_NAME, deviceInfo.shopName)
            .putString(KEY_STATION_NAME, deviceInfo.stationName)
            .putString(KEY_TOKEN, deviceInfo.token)
            .putBoolean(KEY_IS_PAIRED, true)
            .commit() // Use commit() for immediate persistence
        
        // Update cache
        _deviceInfoCache.value = deviceInfo.copy(isPaired = true)
        _isPairedCache.value = true
        
        Timber.d("Saved pairing info (encrypted): ${deviceInfo.shopName} - ${deviceInfo.stationName}")
    }

    /**
     * Update shop and station names only (from backend sync)
     */
    suspend fun updateStationNames(shopName: String, stationName: String) = withContext(Dispatchers.IO) {
        encryptedPrefs.edit()
            .putString(KEY_SHOP_NAME, shopName)
            .putString(KEY_STATION_NAME, stationName)
            .apply()
        
        // Update cache
        _deviceInfoCache.value?.let { current ->
            _deviceInfoCache.value = current.copy(shopName = shopName, stationName = stationName)
        }
    }
    
    /**
     * Get device pairing information as Flow (reactive)
     */
    fun getDeviceInfo(): Flow<DeviceInfo?> {
        return _deviceInfoCache
    }
    
    /**
     * Check if device is paired (reactive)
     */
    fun isPaired(): Flow<Boolean> {
        return _isPairedCache
    }
    
    /**
     * Clear all pairing data (for testing or admin reset)
     * 
     * CRITICAL: This should ONLY be called from:
     * 1. LockRepository.unpairDevice() triggered by HTTP 401
     * 2. Manual admin reset (10-tap + PIN = "1100")
     * 
     * NEVER call this on HTTP 403 (FeatureDisabled)
     */
    suspend fun clearPairingData() = withContext(Dispatchers.IO) {
        encryptedPrefs.edit()
            .remove(KEY_DEVICE_ID) // Generate new ID on next run
            .remove(KEY_TOKEN)
            .remove(KEY_SHOP_ID)
            .remove(KEY_STATION_ID)
            .remove(KEY_SHOP_NAME)
            .remove(KEY_STATION_NAME)
            .putBoolean(KEY_IS_PAIRED, false)
            .commit()
        
        // Update cache
        _deviceInfoCache.value = null
        _isPairedCache.value = false
        
        Timber.d("Cleared pairing data (Factory Reset) - TRIGGERED BY: HTTP 401 or Admin Reset")
    }
}
