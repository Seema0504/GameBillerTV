# GameBiller Android TV Lock Project - Comprehensive Documentation (v2.2)

**Generated Date:** 2026-01-19
**Project Name:** GameBillerTV
**Platform:** Android TV (minSdk 26, targetSdk 34)

---

## 1. Project Overview

**GameBillerTV** is a kiosk-style enforcement application designed for GameBiller gaming stations. its primary purpose is to **prevent revenue leakage** by locking the TV screen (blocking HDMI/Game input) unless a valid session is running on the backend.

### Core Business Rules
1.  **Fail-Safe Locking:** The TV is LOCKED by default. It only unlocks if the backend explicitly returns `status: "RUNNING"`.
2.  **Offline Protection:**
    - If network fails: Enters a 30-second grace period, then locks.
    - If active: Blocks "Back" button navigation.
3.  **Audit Compliance:** Every lock/unlock event is logged locally (**ordered**) and synced to the backend.
4.  **Security:** 
    - **Token-based Auth:** All API calls require `Authorization: Bearer <token>`.
    - **Explicit Auth Failure Handling:** Backend 401/403 errors immediately lock the screen with specific "Authorization Error" reason.

---

## 2. Technical Architecture

The app follows a modern Android **MVVM (Model-View-ViewModel)** architecture:

*   **UI:** Jetpack Compose for TV (Modern, declarative UI).
*   **Dependency Injection:** Hilt (Dagger) - including **@IoDispatcher**.
*   **Async Operations:** 
    - Lifecycle-aware Polling (`repeatOnLifecycle`).
    - **Structured Concurrency** (injected Dispatchers, no detached scopes).
    - **Mutex Guards** (prevent overlapping audit flushes).
*   **Networking:** Retrofit + OkHttp (with **Polymorphic Moshi** JSON parsing).
*   **Local Persistence:**
    - Encrypted DataStore (Device ID, Tokens).
    - Room Database (Ordered Offline Queue).

### System Diagram
```
[Android TV OS] -> [BootReceiver] -> [MainActivity]
                                          |
                                    [LockViewModel] <--> [StatusPollingService]
                                          |
                                    [LockRepository] (Mutex Protected)
                                     /           \
                           [ApiService]         [Database/DataStore]
                                |                     |
                        [GameBiller Backend]    [Ordered Offline Queue]
```

---

## 3. Backend Integration Requirements

### A. Station Status (Critical)
`GET /api/stations/{station_id}/status`
- **Headers:** `Authorization: Bearer <TOKEN>`
- **Poll Interval:** Every 12 seconds.
- **Fail-Safe Behavior:**
  - `200 OK` + `RUNNING` -> **UNLOCK**
  - `401/403` -> **LOCK** (Reason: `Authorization Error`)
  - Network Error -> **LOCK** (after 30s grace)

### B. Device Pairing
`POST /api/tv-devices/pair`
- **Input:** `{ "station_code": "ABC123", "device_id": "TV-UUID" }`
- **Output:** `{ "station_id": 5, "token": "SECURE_TOKEN_XYZ", ... }`

---

## 4. Critical Source Code

### 4.1. Lock Repository (Concurrency & Safety)
*Features: Structured concurrency, Mutex locking, and Error Mapping.*

```kotlin
// com.gamebiller.tvlock.domain.LockRepository.kt

private val auditFlushMutex = Mutex()

suspend fun getStationStatus(): StationStatus {
    return withContext(ioDispatcher) {
        // ... auth header setup ...
        val response = apiService.getStationStatus(...)
        
        if (response.isSuccessful) {
            StationStatus.fromString(response.body()!!.status)
        } else {
            // Explicitly map authorization failures
            if (response.code() == 401 || response.code() == 403) {
                StationStatus.TokenInvalid
            } else {
                StationStatus.Unknown
            }
        }
    }
}

suspend fun flushAuditLogs() {
    withContext(ioDispatcher) {
        // Prevent overlapping flushes
        if (auditFlushMutex.tryLock()) {
            try {
               // ... flush logic ...
            } finally {
               auditFlushMutex.unlock()
            }
        }
    }
}
```

### 4.2. Lock Reason Enum
*Strongly typed state handling.*

```kotlin
enum class LockReason {
    NETWORK_FAILURE,
    SESSION_STOPPED,
    TOKEN_INVALID, // Mapped to "Authorization Error"
    APP_RESTART,
    // ...
}
```

---

## 5. Deployment Instructions

1.  **Build Signed APK:** `gradlew assembleRelease`
2.  **Install via ADB:** `adb install -r app-release.apk`
3.  **Kiosk Mode (Optional Phase 2):**
    `adb shell dpm set-device-owner com.gamebiller.tvlock/.receiver.DeviceAdminReceiver`

---

**End of Documentation**
