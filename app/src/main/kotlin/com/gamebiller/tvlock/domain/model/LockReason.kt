package com.gamebiller.tvlock.domain.model

enum class LockReason {
    NETWORK_FAILURE,
    SESSION_STOPPED,
    SESSION_PAUSED,
    SESSION_NOT_STARTED,
    SESSION_NOT_ACTIVE,
    TOKEN_INVALID,
    FEATURE_DISABLED,  // HTTP 403 - subscription lapsed, remain paired
    RATE_LIMITED,      // HTTP 429 - backend rate limit
    APP_RESTART
}

fun LockReason.toDisplayText(): String = when (this) {
    LockReason.NETWORK_FAILURE -> "Network Unavailable"
    LockReason.SESSION_STOPPED -> "Session Stopped"
    LockReason.SESSION_PAUSED -> "Session Paused"
    LockReason.SESSION_NOT_STARTED -> "Session Not Started"
    LockReason.SESSION_NOT_ACTIVE -> "Session Not Active"
    LockReason.TOKEN_INVALID -> "Authorization Error"
    LockReason.FEATURE_DISABLED -> "Feature Temporarily Unavailable"
    LockReason.RATE_LIMITED -> "Please Wait..."
    LockReason.APP_RESTART -> "System Restarted"
}
