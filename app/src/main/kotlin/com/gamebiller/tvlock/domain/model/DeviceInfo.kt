package com.gamebiller.tvlock.domain.model

/**
 * Represents a paired device with station information
 */
data class DeviceInfo(
    val deviceId: String,
    val shopId: Int,
    val stationId: Int,
    val shopName: String,
    val stationName: String,
    val token: String,
    val isPaired: Boolean = true
)
