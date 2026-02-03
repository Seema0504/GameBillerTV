package com.gamebiller.tvlock.domain.model

/**
 * Represents the current status of a GameBiller station
 * Determines whether the TV should be locked or unlocked
 * 
 * BACKEND CONTRACT:
 * - 200 → Running/Stopped/Paused/NotStarted (normal operation)
 * - 401 → TokenInvalid (device revoked, MUST unpair)
 * - 403 → FeatureDisabled (subscription lapsed, MUST remain paired)
 * - 429 → RateLimited (back off polling)
 * - 5xx → Unknown (fail-safe lock)
 */
sealed class StationStatus {
    /**
     * Station is actively running - TV should be UNLOCKED
     */
    data object Running : StationStatus()
    
    /**
     * Station is stopped - TV should be LOCKED
     */
    data object Stopped : StationStatus()
    
    /**
     * Station is paused - TV should be LOCKED
     */
    data object Paused : StationStatus()
    
    /**
     * Station has not started yet - TV should be LOCKED
     */
    data object NotStarted : StationStatus()
    
    /**
     * Status is unknown (network error, parsing error, etc.) - TV should be LOCKED (fail-safe)
     */
    data object Unknown : StationStatus()
    
    /**
     * HTTP 401: The authentication token is invalid or revoked
     * MUST trigger unpair - device needs to re-pair
     */
    data object TokenInvalid : StationStatus()
    
    /**
     * HTTP 403: Feature is disabled (e.g., subscription lapsed)
     * MUST remain paired - device should lock but NEVER unpair
     * Token remains valid, polling continues
     */
    data object FeatureDisabled : StationStatus()
    
    /**
     * HTTP 429: Rate limited by backend
     * Device should back off polling but remain paired
     */
    data class RateLimited(val retryAfterSeconds: Int = 60) : StationStatus()
    
    /**
     * Determines if the TV should be locked based on this status
     * CRITICAL: Fail-safe approach - only RUNNING unlocks the TV
     */
    fun shouldLock(): Boolean = this !is Running
    
    companion object {
        /**
         * Parse status string from API response
         * Returns Unknown for any unrecognized status (fail-safe)
         */
        fun fromString(status: String?): StationStatus {
            return when (status?.uppercase()) {
                "RUNNING" -> Running
                "STOPPED" -> Stopped
                "PAUSED" -> Paused
                "NOT_STARTED" -> NotStarted
                else -> Unknown
            }
        }
    }
}
