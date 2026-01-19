package com.gamebiller.tvlock.domain.model

/**
 * Strongly typed audit metadata
 */
sealed class AuditMetadata {
    data class NetworkLost(val retryCount: Int) : AuditMetadata()
    data class GracePeriodExpired(val durationSeconds: Int) : AuditMetadata()
    data class ManualLock(val reason: String) : AuditMetadata()
    data class AppRestarted(val boot: Boolean) : AuditMetadata()
    data class DevicePaired(val shopName: String, val stationName: String) : AuditMetadata()
    data class Generic(val info: Map<String, String>) : AuditMetadata()
}
