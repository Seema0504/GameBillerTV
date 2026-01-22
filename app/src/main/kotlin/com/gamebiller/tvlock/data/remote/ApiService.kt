package com.gamebiller.tvlock.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Retrofit API service interface for GameBiller backend
 */
interface ApiService {
    
    /**
     * Pair a device with a station using a station code
     * POST /api/tvdevices/tvsettings?action=pair
     */
    @POST("api/tvdevices/tvsettings?action=pair")
    suspend fun pairDevice(
        @Body request: PairDeviceRequest
    ): Response<PairDeviceResponse>
    
    /**
     * Get the current status of a station
     * GET /api/tvdevices/tvsettings?action=status
     */
    @GET("api/tvdevices/tvsettings")
    suspend fun getStationStatus(
        @retrofit2.http.Query("action") action: String = "status",
        @retrofit2.http.Header("Authorization") token: String
    ): Response<StationStatusResponse>
    
    /**
     * Send an audit event to the backend
     * POST /api/tvdevices/tvsettings?action=audit
     */
    @POST("api/tvdevices/tvsettings?action=audit")
    suspend fun sendAuditEvent(
        @retrofit2.http.Header("Authorization") token: String,
        @Body event: AuditEventRequest
    ): Response<Unit>
}
