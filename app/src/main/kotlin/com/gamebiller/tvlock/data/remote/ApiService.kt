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
     * POST /api/tv-devices?action=pair
     */
    @POST("api/tv-devices?action=pair")
    suspend fun pairDevice(
        @Body request: PairDeviceRequest
    ): Response<PairDeviceResponse>
    
    /**
     * Get the current status of a station
     * GET /api/stations/{station_id}/status
     */
    @GET("api/stations/{station_id}/status")
    suspend fun getStationStatus(
        @Path("station_id") stationId: Int,
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
