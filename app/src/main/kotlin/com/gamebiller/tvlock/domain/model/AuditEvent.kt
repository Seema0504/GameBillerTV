package com.gamebiller.tvlock.domain.model

/**
 * Audit event types sent to backend
 */
enum class AuditEventType(val eventName: String) {
    APP_STARTED("APP_STARTED"),
    DEVICE_PAIRED("DEVICE_PAIRED"),
    TV_UNLOCKED("TV_UNLOCKED"),
    TV_LOCKED("TV_LOCKED"),
    NETWORK_LOST("NETWORK_LOST"),
    NETWORK_RESTORED("NETWORK_RESTORED"),
    GRACE_PERIOD_STARTED("GRACE_PERIOD_STARTED"),
    GRACE_PERIOD_EXPIRED("GRACE_PERIOD_EXPIRED")
}

/**
 * Audit event to be sent to backend
 */
data class AuditEvent(
    val event: AuditEventType,
    val stationId: Int?,
    val deviceId: String,
    val timestamp: String, // ISO-8601 format
    val metadata: AuditMetadata? = null
)
