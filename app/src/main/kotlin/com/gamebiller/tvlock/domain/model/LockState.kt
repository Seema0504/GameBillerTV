package com.gamebiller.tvlock.domain.model

/**
 * Represents the current lock state of the TV
 */
sealed class LockState {
    /**
     * Device is not yet paired with a station
     */
    data object Unpaired : LockState()
    
    /**
     * TV is locked - show lock screen
     */
    data class Locked(
        val reason: LockReason,
        val shopName: String? = null,
        val stationName: String? = null
    ) : LockState()
    
    /**
     * TV is unlocked - hide lock screen
     */
    data class Unlocked(
        val shopName: String,
        val stationName: String
    ) : LockState()
    
    /**
     * Grace period active - network lost but still unlocked temporarily
     */
    data class GracePeriod(
        val secondsRemaining: Int,
        val shopName: String,
        val stationName: String
    ) : LockState()
}
