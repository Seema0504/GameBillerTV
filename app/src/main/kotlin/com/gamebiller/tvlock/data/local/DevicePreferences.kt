package com.gamebiller.tvlock.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gamebiller.tvlock.domain.model.DeviceInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extension property to create DataStore instance
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "device_preferences")

/**
 * Manages encrypted local storage for device pairing information
 * Uses DataStore for persistence
 */
@Singleton
class DevicePreferences @Inject constructor(
    private val context: Context
) {
    
    private val dataStore = context.dataStore
    
    companion object {
        private val KEY_DEVICE_ID = stringPreferencesKey("device_id")
        private val KEY_SHOP_ID = intPreferencesKey("shop_id")
        private val KEY_STATION_ID = intPreferencesKey("station_id")
        private val KEY_SHOP_NAME = stringPreferencesKey("shop_name")
        private val KEY_STATION_NAME = stringPreferencesKey("station_name")
        private val KEY_TOKEN = stringPreferencesKey("token")
        private val KEY_IS_PAIRED = booleanPreferencesKey("is_paired")
    }
    
    /**
     * Get or generate device ID
     * Device ID is generated once and never changes
     */
    suspend fun getDeviceId(): String {
        val preferences = dataStore.data.map { it[KEY_DEVICE_ID] }
        var deviceId = preferences.first()
        
        if (deviceId == null) {
            deviceId = "TV-${UUID.randomUUID().toString().take(8).uppercase()}"
            dataStore.edit { prefs ->
                prefs[KEY_DEVICE_ID] = deviceId
            }
            Timber.d("Generated new device ID: $deviceId")
        }
        
        return deviceId
    }
    
    /**
     * Save device pairing information
     */
    suspend fun savePairingInfo(deviceInfo: DeviceInfo) {
        dataStore.edit { prefs ->
            prefs[KEY_DEVICE_ID] = deviceInfo.deviceId
            prefs[KEY_SHOP_ID] = deviceInfo.shopId
            prefs[KEY_STATION_ID] = deviceInfo.stationId
            prefs[KEY_SHOP_NAME] = deviceInfo.shopName
            prefs[KEY_STATION_NAME] = deviceInfo.stationName
            prefs[KEY_TOKEN] = deviceInfo.token
            prefs[KEY_IS_PAIRED] = true
        }
        Timber.d("Saved pairing info: ${deviceInfo.shopName} - ${deviceInfo.stationName}")
    }

    /**
     * Update shop and station names only
     */
    suspend fun updateStationNames(shopName: String, stationName: String) {
        dataStore.edit { prefs ->
            prefs[KEY_SHOP_NAME] = shopName
            prefs[KEY_STATION_NAME] = stationName
        }
    }
    
    /**
     * Get device pairing information as Flow
     */
    fun getDeviceInfo(): Flow<DeviceInfo?> {
        return dataStore.data.map { prefs ->
            val isPaired = prefs[KEY_IS_PAIRED] ?: false
            if (!isPaired) {
                null
            } else {
                DeviceInfo(
                    deviceId = prefs[KEY_DEVICE_ID] ?: "",
                    shopId = prefs[KEY_SHOP_ID] ?: 0,
                    stationId = prefs[KEY_STATION_ID] ?: 0,
                    shopName = prefs[KEY_SHOP_NAME] ?: "",
                    stationName = prefs[KEY_STATION_NAME] ?: "",
                    token = prefs[KEY_TOKEN] ?: "",
                    isPaired = true
                )
            }
        }
    }
    
    /**
     * Check if device is paired
     */
    fun isPaired(): Flow<Boolean> {
        return dataStore.data.map { prefs ->
            prefs[KEY_IS_PAIRED] ?: false
        }
    }
    
    /**
     * Clear all pairing data (for testing or reset)
     */
    suspend fun clearPairingData() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_DEVICE_ID) // Generate new ID on next run
            prefs.remove(KEY_TOKEN) // Clear token
            prefs.remove(KEY_SHOP_ID)
            prefs.remove(KEY_STATION_ID)
            prefs.remove(KEY_SHOP_NAME)
            prefs.remove(KEY_STATION_NAME)
            prefs[KEY_IS_PAIRED] = false
        }
        Timber.d("Cleared pairing data (Factory Reset)")
    }
}
