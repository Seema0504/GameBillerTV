package com.gamebiller.tvlock.domain.model

/**
 * Represents the current status of a GameBiller station
 * Determines whether the TV should be locked or unlocked
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
     * The authentication token is invalid or expired - TV should be LOCKED
     */
    data object TokenInvalid : StationStatus()
    
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
