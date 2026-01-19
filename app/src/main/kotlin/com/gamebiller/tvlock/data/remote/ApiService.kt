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
     * POST /api/tv-devices/pair (Hybrid Routing: Explicit path for pairing)
     */
    @POST("api/tv-devices/pair")
    suspend fun pairDevice(
        @Body request: PairDeviceRequest
    ): Response<PairDeviceResponse>
    
    /**
     * Get the current status of a station
     * GET /api/tv-devices?action=status&station_id={id} (Hybrid Routing: Query param for status)
     */
    @GET("api/tv-devices")
    suspend fun getStationStatus(
        @retrofit2.http.Query("action") action: String = "status",
        @retrofit2.http.Query("station_id") stationId: Int,
        @retrofit2.http.Header("Authorization") token: String
    ): Response<StationStatusResponse>
    
    /**
     * Send an audit event to the backend
     * POST /api/tv-devices?action=audit
     */
    @POST("api/tv-devices?action=audit")
    suspend fun sendAuditEvent(
        @Body event: AuditEventRequest
    ): Response<Unit>
}
