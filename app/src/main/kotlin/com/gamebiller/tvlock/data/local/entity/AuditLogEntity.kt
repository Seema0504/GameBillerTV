package com.gamebiller.tvlock.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val event: String,
    val stationId: Int?,
    val deviceId: String,
    val timestamp: String,
    val metadataJson: String? // Store map as JSON string
)
