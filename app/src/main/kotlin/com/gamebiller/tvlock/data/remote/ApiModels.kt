package com.gamebiller.tvlock.data.remote

import com.gamebiller.tvlock.domain.model.AuditMetadata
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * API request to pair a device with a station
 */
@JsonClass(generateAdapter = true)
data class PairDeviceRequest(
    @Json(name = "station_code")
    val stationCode: String,
    
    @Json(name = "device_id")
    val deviceId: String,

    @Json(name = "device_name")
    val deviceName: String? = null
)

/**
 * API response from pairing endpoint
 */
@JsonClass(generateAdapter = true)
data class PairDeviceResponse(
    @Json(name = "shop_id")
    val shopId: Int? = null,
    
    @Json(name = "station_id")
    val stationId: Int,
    
    @Json(name = "device_id")
    val deviceId: String? = null,
    
    @Json(name = "shop_name")
    val shopName: String? = null,
    
    @Json(name = "station_name")
    val stationName: String,

    @Json(name = "token")
    val token: String
)

/**
 * API response from station status endpoint
 */
@JsonClass(generateAdapter = true)
data class StationStatusResponse(
    @Json(name = "station_id")
    val stationId: Int? = null,
    
    @Json(name = "status")
    val status: String, // "RUNNING", "STOPPED", "PAUSED", "NOT_STARTED"
    
    @Json(name = "shop_name")
    val shopName: String? = null,
    
    @Json(name = "station_name")
    val stationName: String? = null
)

/**
 * API request to send audit event
 */
@JsonClass(generateAdapter = true)
data class AuditEventRequest(
    @Json(name = "event")
    val event: String,
    
    @Json(name = "station_id")
    val stationId: Int?,
    
    @Json(name = "device_id")
    val deviceId: String,
    
    @Json(name = "timestamp")
    val timestamp: String,
    
    @Json(name = "metadata")
    val metadata: AuditMetadata? = null
)

/**
 * Generic API error response
 */
@JsonClass(generateAdapter = true)
data class ApiErrorResponse(
    @Json(name = "error")
    val error: String,
    
    @Json(name = "message")
    val message: String? = null
)
