# GameBillerTV - Complete Project Documentation
**Version:** 1.0.0  
**Last Updated:** January 20, 2026  
**Platform:** Android TV (API 26+)

---

## 📋 Table of Contents

1. [Project Overview](#project-overview)
2. [Architecture & Technology Stack](#architecture--technology-stack)
3. [Project Structure](#project-structure)
4. [Core Components](#core-components)
5. [Backend Integration](#backend-integration)
6. [Configuration & Build](#configuration--build)
7. [Installation & Deployment](#installation--deployment)
8. [Features & Functionality](#features--functionality)
9. [Security Implementation](#security-implementation)
10. [Testing Guide](#testing-guide)
11. [Troubleshooting](#troubleshooting)
12. [Development History](#development-history)

---

## 🎯 Project Overview

### Purpose
GameBillerTV is a **kiosk-style Android TV application** designed to enforce session-based access control for gaming stations in gaming cafés. The TV screen remains locked unless an active GameBiller station session is running, preventing revenue leakage and unauthorized usage.

### Key Business Problem Solved
- **Revenue Protection**: Prevents customers from using gaming stations without paying
- **Automated Control**: No manual intervention needed to lock/unlock TVs
- **Real-time Synchronization**: TV status updates automatically based on backend session status
- **Fail-safe Design**: TV defaults to locked state if network is lost or app crashes

### Target Deployment
- **Environment**: Gaming cafés with multiple gaming stations (PS5, PC, VR)
- **Hardware**: Android TV devices (Fire TV, Mi Box, etc.)
- **Network**: Local WiFi with internet connectivity to GameBiller backend

---

## 🏗️ Architecture & Technology Stack

### Language & Framework
- **Primary Language**: Kotlin 1.9.20
- **UI Framework**: Jetpack Compose for TV
- **Minimum SDK**: 26 (Android 8.0 Oreo)
- **Target SDK**: 34 (Android 14)
- **Compile SDK**: 34

### Architecture Pattern
**MVVM (Model-View-ViewModel)** with Clean Architecture principles:

```
┌─────────────────────────────────────────┐
│           UI Layer (Compose)            │
│  ┌─────────────┐    ┌────────────────┐ │
│  │ MainActivity│◄───│  LockViewModel │ │
│  └─────────────┘    └────────────────┘ │
└─────────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────┐
│         Domain Layer (Business Logic)   │
│  ┌──────────────────────────────────┐  │
│  │      LockRepository              │  │
│  │  (Pairing, Status, Audit)        │  │
│  └──────────────────────────────────┘  │
└─────────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────┐
│            Data Layer                   │
│  ┌─────────────┐    ┌────────────────┐ │
│  │ ApiService  │    │DevicePreferences│ │
│  │ (Retrofit)  │    │  (DataStore)   │ │
│  └─────────────┘    └────────────────┘ │
└─────────────────────────────────────────┘
```

### Key Libraries & Dependencies

#### Networking
- **Retrofit 2.9.0**: REST API communication
- **OkHttp 4.12.0**: HTTP client with logging interceptor
- **Moshi 1.15.0**: JSON parsing with Kotlin support

#### Dependency Injection
- **Hilt 2.48**: Compile-time dependency injection
- **Hilt Navigation Compose 1.1.0**: ViewModel integration

#### Local Storage
- **DataStore Preferences 1.0.0**: Key-value storage
- **Security Crypto 1.1.0-alpha06**: Encrypted SharedPreferences
- **Room 2.6.1**: Local database for audit logs

#### UI & Compose
- **Compose BOM 2023.10.01**: Jetpack Compose libraries
- **Material3**: Material Design 3 components
- **TV Foundation 1.0.0-alpha10**: Android TV-specific components
- **TV Material 1.0.0-alpha10**: TV-optimized Material components

#### Concurrency
- **Kotlin Coroutines 1.7.3**: Asynchronous programming
- **Lifecycle Runtime Compose 2.7.0**: Lifecycle-aware coroutines

#### Logging
- **Timber 5.0.1**: Structured logging

---

## 📁 Project Structure

```
GameBillerTV/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── kotlin/com/gamebiller/tvlock/
│   │   │   │   ├── GameBillerApp.kt              # Application class (Hilt, Timber)
│   │   │   │   ├── data/
│   │   │   │   │   ├── local/
│   │   │   │   │   │   ├── AppDatabase.kt        # Room database
│   │   │   │   │   │   ├── DevicePreferences.kt  # Encrypted DataStore
│   │   │   │   │   │   ├── dao/
│   │   │   │   │   │   │   └── AuditDao.kt       # Audit log DAO
│   │   │   │   │   │   └── entity/
│   │   │   │   │   │       └── AuditLogEntity.kt # Audit log entity
│   │   │   │   │   └── remote/
│   │   │   │   │       ├── ApiModels.kt          # Request/Response DTOs
│   │   │   │   │       └── ApiService.kt         # Retrofit interface
│   │   │   │   ├── di/
│   │   │   │   │   ├── CoroutinesModule.kt       # Coroutine dispatchers
│   │   │   │   │   ├── DatabaseModule.kt         # Room & DataStore
│   │   │   │   │   └── NetworkModule.kt          # Retrofit & OkHttp
│   │   │   │   ├── domain/
│   │   │   │   │   ├── LockRepository.kt         # Business logic
│   │   │   │   │   └── model/
│   │   │   │   │       ├── AuditEvent.kt         # Audit event types
│   │   │   │   │       ├── AuditMetadata.kt      # Event metadata
│   │   │   │   │       ├── DeviceInfo.kt         # Pairing info
│   │   │   │   │       ├── LockReason.kt         # Lock reason enum
│   │   │   │   │       ├── LockState.kt          # Lock state sealed class
│   │   │   │   │       └── StationStatus.kt      # Station status sealed class
│   │   │   │   ├── receiver/
│   │   │   │   │   ├── AppRestartReceiver.kt     # Crash recovery
│   │   │   │   │   └── BootReceiver.kt           # Auto-launch on boot
│   │   │   │   ├── service/
│   │   │   │   │   └── StatusPollingService.kt   # Foreground service
│   │   │   │   └── ui/
│   │   │   │       ├── MainActivity.kt           # Single activity
│   │   │   │       ├── screens/
│   │   │   │       │   ├── LockScreen.kt         # Lock overlay UI
│   │   │   │       │   └── PairingScreen.kt      # Pairing UI
│   │   │   │       ├── theme/
│   │   │   │       │   └── Theme.kt              # Material3 theme
│   │   │   │       └── viewmodel/
│   │   │   │           ├── LockViewModel.kt      # Lock state logic
│   │   │   │           └── PairingViewModel.kt   # Pairing logic
│   │   │   ├── AndroidManifest.xml               # App manifest
│   │   │   └── res/                              # Resources
│   │   └── test/                                 # Unit tests
│   └── build.gradle.kts                          # App-level Gradle
├── build.gradle.kts                              # Project-level Gradle
├── gradle.properties                             # Gradle properties
├── settings.gradle.kts                           # Gradle settings
├── DEPLOYMENT_GUIDE.md                           # Deployment instructions
├── BACKEND_INTEGRATION.md                        # Backend API docs
├── TESTING_GUIDE.md                              # Testing procedures
└── README.md                                     # Project README
```

---

## 🔧 Core Components

### 1. GameBillerApp.kt
**Purpose**: Application entry point

**Responsibilities**:
- Initialize Hilt dependency injection
- Configure Timber logging (Debug only)
- Set up global exception handlers

**Key Code**:
```kotlin
@HiltAndroidApp
class GameBillerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
```

---

### 2. MainActivity.kt
**Purpose**: Single activity hosting all screens

**Responsibilities**:
- Enable immersive full-screen mode
- Manage navigation between Pairing and Lock screens
- Start/stop status polling service
- Handle lifecycle events

**Key Features**:
- **Immersive Mode**: Hides system UI (status bar, navigation bar)
- **Back Button Disabled**: Prevents user exit
- **State-based Navigation**: Shows PairingScreen if unpaired, LockScreen if paired

**Navigation Logic**:
```kotlin
when (lockState) {
    LockState.Unpaired -> PairingScreen(...)
    else -> LockScreen(...)
}
```

---

### 3. LockViewModel.kt
**Purpose**: Manages lock state and polling logic

**State Management**:
```kotlin
sealed class LockState {
    object Unpaired : LockState()
    data class Locked(val reason: LockReason) : LockState()
    object Unlocked : LockState()
    data class GracePeriod(val secondsRemaining: Int) : LockState()
}
```

**Polling Flow**:
1. Check if device is paired
2. If paired, poll station status every 12 seconds
3. Update lock state based on response:
   - `RUNNING` → Unlocked
   - `STOPPED/PAUSED/NOT_STARTED` → Locked
   - Network error → Grace Period (30s countdown)
4. Send audit events on state transitions

**Failure Handling**:
- **Grace Period**: 30-second buffer before locking on network loss
- **Failure Counter**: Tracks consecutive failures (max 3)
- **Token Invalid**: Auto-unpairs device if backend rejects token

---

### 4. LockRepository.kt
**Purpose**: Business logic layer for pairing, status checks, and audit logging

**Key Methods**:

#### `pairDevice(stationCode: String)`
- Calls `POST /api/tv-devices/pair`
- Saves pairing info to encrypted DataStore
- Returns `DeviceInfo` on success

#### `getStationStatus()`
- Calls `GET /api/tv-devices?action=status&station_id={id}`
- Maps response to `StationStatus` sealed class
- Handles 401/403 as `TokenInvalid`

#### `sendAuditEvent(event: AuditEvent)`
- Queues event in local Room database
- Attempts immediate flush to backend
- Retries failed events on next successful network call

**Audit Event Types**:
- `DEVICE_PAIRED`
- `DEVICE_UNPAIRED`
- `TV_LOCKED`
- `TV_UNLOCKED`
- `NETWORK_ISSUE`
- `TOKEN_INVALID`

---

### 5. DevicePreferences.kt
**Purpose**: Encrypted local storage for pairing data

**Stored Data**:
```kotlin
data class DeviceInfo(
    val deviceId: String,
    val shopId: Int,
    val stationId: Int,
    val shopName: String,
    val stationName: String,
    val token: String,
    val isPaired: Boolean
)
```

**Security**:
- Uses `EncryptedSharedPreferences` with AES256-GCM encryption
- Master key stored in Android Keystore

---

### 6. ApiService.kt
**Purpose**: Retrofit interface for backend communication

**Hybrid Routing Strategy**:
The app uses a **hybrid routing approach** to support both Localhost (Express server) and Vercel (Serverless functions):

```kotlin
interface ApiService {
    // Path-based routing (works on Localhost)
    @POST("api/tv-devices/pair")
    suspend fun pairDevice(@Body request: PairDeviceRequest): Response<PairDeviceResponse>
    
    // Query parameter routing (works on Vercel)
    @GET("api/tv-devices")
    suspend fun getStationStatus(
        @Query("action") action: String = "status",
        @Query("station_id") stationId: Int,
        @Header("Authorization") token: String
    ): Response<StationStatusResponse>
    
    // Query parameter routing
    @POST("api/tv-devices?action=audit")
    suspend fun sendAuditEvent(@Body event: AuditEventRequest): Response<Unit>
}
```

**Why Hybrid Routing?**
- **Localhost (Express)**: Uses explicit path `/api/tv-devices/pair`
- **Vercel (Serverless)**: Uses query params `?action=status` for dynamic routing
- This ensures compatibility with both development and production environments

---

### 7. PairingScreen.kt & LockScreen.kt
**Purpose**: Compose UI screens

#### PairingScreen
- Input field for station code
- "Pair Device" button
- Loading/error/success states
- **Debug-only Exit button** (top-right corner, only in Debug builds)

#### LockScreen
- Full-screen lock overlay
- Lock icon with 5-click emergency exit trigger
- Station info display
- Session status message
- **Debug-only Exit button** (top-right corner, only in Debug builds)

**Emergency Exit Feature**:
- Click lock icon 5 times rapidly
- PIN dialog appears (PIN: `5555`)
- On correct PIN, opens Android Settings
- Allows shop owner to access system for maintenance

---

### 8. BootReceiver.kt & AppRestartReceiver.kt
**Purpose**: Auto-launch and crash recovery

#### BootReceiver
- Listens for `BOOT_COMPLETED` broadcast
- Launches MainActivity on device boot
- Ensures kiosk mode persists after reboot

#### AppRestartReceiver
- Restarts app if it crashes or is force-stopped
- Uses `AlarmManager` to schedule restart

---

## 🌐 Backend Integration

### API Base URL Configuration
Located in `app/build.gradle.kts`:

```kotlin
buildConfigField("String", "API_BASE_URL", "\"https://gamerspot-g5ucj66zy-seemabharanis-projects.vercel.app/\"")
```

**Environment-specific URLs**:
- **Localhost**: `http://10.0.2.2:3002/` (Android Emulator)
- **Vercel Preview**: `https://gamerspot-{hash}-seemabharanis-projects.vercel.app/`
- **Production**: `https://www.gamebiller.com/`

### Required Backend Endpoints

#### 1. Device Pairing
**Endpoint**: `POST /api/tv-devices/pair`

**Request**:
```json
{
  "station_code": "ST91-E421",
  "device_id": "android-tv-12345",
  "device_name": "Fire TV Stick 4K"
}
```

**Response** (200 OK):
```json
{
  "shopId": 1,
  "deviceId": "android-tv-12345",
  "stationId": 3,
  "shopName": "GameZone Pro",
  "stationName": "PS5 - Seat 1",
  "token": "tvk_9c21b866f20a22084231b36e89f3a3692e81c22f8b7ee97e7091c201e27e9083"
}
```

**Error Responses**:
- `400 Bad Request`: Invalid station code or missing fields
- `404 Not Found`: Station code not found
- `409 Conflict`: Device already paired to another station

---

#### 2. Station Status Check
**Endpoint**: `GET /api/tv-devices?action=status&station_id={id}`

**Headers**:
```
Authorization: Bearer tvk_9c21b866f20a22084231b36e89f3a3692e81c22f8b7ee97e7091c201e27e9083
```

**Response** (200 OK):
```json
{
  "stationId": 3,
  "status": "RUNNING",
  "shopName": "GameZone Pro",
  "stationName": "PS5 - Seat 1"
}
```

**Status Values**:
- `RUNNING`: Session active, TV should be unlocked
- `STOPPED`: Session ended, TV should be locked
- `PAUSED`: Session paused, TV should be locked
- `NOT_STARTED`: No session, TV should be locked

**Error Responses**:
- `401 Unauthorized`: Invalid or expired token (triggers auto-unpair)
- `403 Forbidden`: Token valid but access denied (triggers auto-unpair)
- `404 Not Found`: Station not found

---

#### 3. Audit Event Logging
**Endpoint**: `POST /api/tv-devices?action=audit`

**Request**:
```json
{
  "event": "TV_UNLOCKED",
  "stationId": 3,
  "deviceId": "android-tv-12345",
  "timestamp": "2026-01-20T16:47:00Z",
  "reason": null,
  "metadata": {
    "type": "TV_UNLOCKED",
    "shopName": "GameZone Pro",
    "stationName": "PS5 - Seat 1"
  }
}
```

**Event Types**:
- `DEVICE_PAIRED`
- `DEVICE_UNPAIRED`
- `TV_LOCKED`
- `TV_UNLOCKED`
- `NETWORK_ISSUE`
- `TOKEN_INVALID`

**Response** (200 OK):
```json
{
  "success": true
}
```

---

### Backend Routing Requirements

The backend must support **both** routing patterns:

1. **Path-based** (for Pairing):
   - `/api/tv-devices/pair` → Handled by `api/tv-devices/pair.js`

2. **Query parameter-based** (for Status & Audit):
   - `/api/tv-devices?action=status` → Handled by `api/tv-devices/index.js`
   - `/api/tv-devices?action=audit` → Handled by `api/tv-devices/index.js`

**Localhost (Express) Setup**:
```javascript
// server.js
import tvPairingHandler from './api/tv-devices/pair.js';
import tvDevicesHandler from './api/tv-devices/index.js';

app.all('/api/tv-devices/pair', adaptHandler(tvPairingHandler));
app.all('/api/tv-devices', adaptHandler(tvDevicesHandler));
```

**Vercel Setup**:
- File structure automatically maps to routes
- `api/tv-devices/pair.js` → `/api/tv-devices/pair`
- `api/tv-devices/index.js` → `/api/tv-devices`

---

## ⚙️ Configuration & Build

### Build Configuration
**File**: `app/build.gradle.kts`

**Key Settings**:
```kotlin
android {
    namespace = "com.gamebiller.tvlock"
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.gamebiller.tvlock"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        
        // Backend API configuration
        buildConfigField("String", "API_BASE_URL", "\"https://gamerspot-g5ucj66zy-seemabharanis-projects.vercel.app/\"")
        buildConfigField("int", "POLLING_INTERVAL_SECONDS", "12")
        buildConfigField("int", "GRACE_PERIOD_SECONDS", "30")
    }
    
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
            buildConfigField("String", "API_BASE_URL", "\"https://gamerspot-g5ucj66zy-seemabharanis-projects.vercel.app/\"")
        }
    }
}
```

### Environment Variables
Access in code via `BuildConfig`:
```kotlin
val apiUrl = BuildConfig.API_BASE_URL
val pollingInterval = BuildConfig.POLLING_INTERVAL_SECONDS
val gracePeriod = BuildConfig.GRACE_PERIOD_SECONDS
```

### Building the App

#### Debug Build (for testing)
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleDebug
```
**Output**: `app\build\outputs\apk\debug\app-debug.apk`

**Debug Build Features**:
- ✅ Detailed logging enabled
- ✅ Debug Exit button visible (top-right corner)
- ✅ No code optimization
- ✅ Larger APK size (~8-10 MB)

#### Release Build (for production)
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleRelease
```
**Output**: `app\build\outputs\apk\release\app-release.apk`

**Release Build Features**:
- ✅ Code obfuscation (ProGuard)
- ✅ Resource shrinking
- ✅ Debug Exit button removed
- ✅ Optimized performance
- ✅ Smaller APK size (~4-5 MB)

---

## 📦 Installation & Deployment

### Method 1: ADB over Network (Recommended for Testing)

**Prerequisites**:
- Fire TV and PC on same WiFi network
- ADB debugging enabled on Fire TV

**Steps**:
1. **On Fire TV**:
   - Settings → My Fire TV → Developer Options
   - Enable "ADB Debugging"
   - Settings → My Fire TV → About → Network
   - Note IP address (e.g., `192.168.1.100`)

2. **On PC**:
```powershell
$env:Path += ";C:\Users\racin\AppData\Local\Android\Sdk\platform-tools"
adb connect 192.168.1.100:5555
adb install -r "app\build\outputs\apk\debug\app-debug.apk"
```

---

### Method 2: Downloader App (Recommended for Shop Owners)

**Prerequisites**:
- Fire TV with internet connection
- APK uploaded to file hosting service

**Steps**:
1. **Upload APK**:
   - Upload to GitHub Releases, MediaFire, or similar
   - Get direct download link

2. **On Fire TV**:
   - Open **Downloader** app
   - Enter direct download URL
   - Click **Go** → **Install** → **Done**

**Supported File Hosts**:
- ✅ GitHub Releases (best for versioning)
- ✅ MediaFire (free, reliable)
- ✅ Dropbox (change `dl=0` to `dl=1`)
- ❌ Google Drive (doesn't work with Downloader)

---

### Method 3: USB Sideloading

**Steps**:
1. Copy APK to USB drive
2. Insert USB into Fire TV
3. Install file manager (e.g., X-plore)
4. Browse to USB drive
5. Click APK to install

---

### Post-Installation Setup

#### 1. Enable Unknown Sources
Settings → My Fire TV → Developer Options → Apps from Unknown Sources → ON

#### 2. Set as Launcher (Optional for Kiosk Mode)
Settings → Applications → Manage Installed Applications → GameBiller TV Lock → Launch by Default → Set as Home

#### 3. First-Time Pairing
1. Launch app
2. Enter station code from admin panel
3. Click "Pair Device"
4. Verify lock screen appears

---

## 🎯 Features & Functionality

### Core Features

#### 1. Device Pairing
- **One-time setup** to link TV with specific station
- **Station code validation** via backend API
- **Encrypted storage** of pairing credentials
- **Auto-unpair** on token invalidation

#### 2. Real-time Lock/Unlock
- **Polling interval**: 12 seconds
- **Unlock trigger**: Station status = `RUNNING`
- **Lock trigger**: Station status = `STOPPED`, `PAUSED`, or `NOT_STARTED`
- **Smooth transitions**: Fade animations between states

#### 3. Grace Period
- **Duration**: 30 seconds
- **Trigger**: Network connectivity lost
- **Countdown display**: Shows remaining seconds
- **Auto-lock**: After grace period expires

#### 4. Emergency Exit
- **Trigger**: Click lock icon 5 times rapidly
- **PIN Protection**: Requires PIN `5555`
- **Action**: Opens Android Settings
- **Use Case**: Shop owner maintenance access

#### 5. Debug Exit Button (Debug Build Only)
- **Location**: Top-right corner of all screens
- **Label**: "Exit (Debug)"
- **Action**: Calls `Activity.finish()` to exit app
- **Visibility**: Only in Debug builds (`BuildConfig.DEBUG == true`)
- **Production**: Automatically removed in Release builds

#### 6. Audit Logging
- **Local Queue**: Events stored in Room database
- **Auto-flush**: Attempts immediate sync to backend
- **Retry Logic**: Failed events retried on next successful network call
- **Event Types**: Pairing, unpairing, lock/unlock, network issues

#### 7. Auto-restart
- **Boot Launch**: App starts automatically on device boot
- **Crash Recovery**: App restarts if force-stopped or crashed
- **Persistent Kiosk**: Ensures continuous operation

---

### Security Features

#### Implemented (Phase 1)
- ✅ **Full-screen Immersive Mode**: Hides system UI
- ✅ **Back Button Disabled**: Prevents accidental exit
- ✅ **Encrypted Storage**: AES256-GCM for pairing data
- ✅ **Token-based Auth**: Secure backend communication
- ✅ **Fail-safe Lock**: Defaults to locked on errors
- ✅ **Emergency Exit**: PIN-protected admin access

#### Future Enhancements (Phase 2)
- ⏳ **Device Owner Mode**: Full kiosk lockdown
- ⏳ **HOME Button Blocking**: Prevent launcher access
- ⏳ **Settings Restriction**: Block system settings
- ⏳ **Remote Management**: Backend-controlled app config

---

## 🧪 Testing Guide

### Test Scenarios

#### Test 1: Fresh Install & Pairing
**Steps**:
1. Install APK on Android TV
2. Launch app
3. Verify "Device Pairing" screen appears
4. Enter valid station code
5. Click "Pair Device"

**Expected Result**:
- ✅ Pairing succeeds
- ✅ Lock screen appears
- ✅ Station name displayed correctly

---

#### Test 2: Lock/Unlock on Session Start
**Steps**:
1. Ensure app is paired and showing lock screen
2. In backend admin panel, start a session for the station
3. Wait up to 12 seconds

**Expected Result**:
- ✅ Lock screen disappears
- ✅ TV content visible
- ✅ Audit event `TV_UNLOCKED` sent to backend

---

#### Test 3: Lock on Session Stop
**Steps**:
1. Ensure session is running and TV is unlocked
2. In backend admin panel, stop the session
3. Wait up to 12 seconds

**Expected Result**:
- ✅ Lock screen appears
- ✅ Message: "Session Not Active"
- ✅ Audit event `TV_LOCKED` sent to backend

---

#### Test 4: Grace Period on Network Loss
**Steps**:
1. Ensure session is running and TV is unlocked
2. Disconnect TV from WiFi
3. Observe countdown

**Expected Result**:
- ✅ Grace period countdown starts (30s)
- ✅ Message: "Network Issue - Locking in Xs"
- ✅ After 30s, lock screen appears
- ✅ Audit event `NETWORK_ISSUE` sent when network restored

---

#### Test 5: Emergency Exit
**Steps**:
1. On lock screen, click lock icon 5 times rapidly
2. Enter PIN: `5555`
3. Click "Exit"

**Expected Result**:
- ✅ PIN dialog appears after 5 clicks
- ✅ Android Settings opens on correct PIN
- ✅ Can navigate to app settings or uninstall

---

#### Test 6: Debug Exit Button (Debug Build Only)
**Steps**:
1. Install Debug APK
2. Launch app
3. Look for "Exit (Debug)" button in top-right corner
4. Click the button

**Expected Result**:
- ✅ Button visible in Debug build
- ✅ App exits immediately when clicked
- ✅ Can restart app normally

**Verification**:
- Install Release APK
- Verify "Exit (Debug)" button is NOT visible

---

#### Test 7: Auto-restart on Boot
**Steps**:
1. Ensure app is installed and paired
2. Reboot Android TV
3. Wait for boot to complete

**Expected Result**:
- ✅ App launches automatically
- ✅ Lock screen appears
- ✅ Polling resumes

---

#### Test 8: Token Invalidation
**Steps**:
1. Ensure app is paired
2. In backend, delete the TV device or invalidate token
3. Wait for next status poll (12s)

**Expected Result**:
- ✅ App detects 401/403 response
- ✅ Auto-unpairs device
- ✅ Pairing screen appears
- ✅ Audit event `TOKEN_INVALID` sent

---

### Logging & Debugging

#### View Real-time Logs
```powershell
adb logcat | Select-String "GameBiller|OkHttp"
```

#### Filter by Component
```powershell
# ViewModel logs
adb logcat | Select-String "LockViewModel"

# Network logs
adb logcat | Select-String "OkHttp"

# Repository logs
adb logcat | Select-String "LockRepository"
```

#### Export Logs to File
```powershell
adb logcat -d > app_logs.txt
```

---

## 🐛 Troubleshooting

### Issue: App doesn't auto-launch on boot

**Possible Causes**:
- `RECEIVE_BOOT_COMPLETED` permission missing
- BootReceiver not registered in manifest
- Battery optimization blocking background launch

**Solution**:
1. Verify `AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<receiver android:name=".receiver.BootReceiver"
    android:enabled="true"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
    </intent-filter>
</receiver>
```

2. Disable battery optimization:
   - Settings → Apps → GameBiller TV Lock → Battery → Unrestricted

---

### Issue: Lock screen doesn't appear

**Possible Causes**:
- Network connectivity lost
- Backend API unreachable
- Incorrect API URL configuration

**Solution**:
1. Check network: `adb shell ping 8.8.8.8`
2. Verify API URL in `build.gradle.kts`
3. Test backend endpoint manually:
```powershell
Invoke-RestMethod -Uri "https://your-backend.com/api/tv-devices?action=status&station_id=1" -Headers @{"Authorization"="Bearer YOUR_TOKEN"}
```
4. Check logs: `adb logcat | Select-String "OkHttp"`

---

### Issue: Pairing fails with 404

**Possible Causes**:
- Invalid station code
- Backend endpoint not configured
- Network routing issue

**Solution**:
1. Verify station code in admin panel
2. Test pairing endpoint:
```powershell
$body = @{station_code="ST91-E421"; device_id="test"} | ConvertTo-Json
Invoke-RestMethod -Uri "https://your-backend.com/api/tv-devices/pair" -Method Post -Body $body -ContentType "application/json"
```
3. Check backend logs for routing errors

---

### Issue: App exits when pressing Back button

**Possible Causes**:
- Back button handling not implemented
- Debug build with Exit button visible

**Solution**:
1. Verify `MainActivity.kt` overrides `onBackPressed()`:
```kotlin
override fun onBackPressed() {
    // Do nothing - prevent exit
}
```
2. If using Debug build, Exit button is intentional for testing

---

### Issue: Grace period countdown not showing

**Possible Causes**:
- Network loss not detected
- Polling service stopped

**Solution**:
1. Check service status: `adb shell dumpsys activity services | Select-String "StatusPollingService"`
2. Verify grace period config: `BuildConfig.GRACE_PERIOD_SECONDS`
3. Check logs: `adb logcat | Select-String "Grace"`

---

## 📚 Development History

### Version 1.0.0 (January 20, 2026)

#### Features Implemented
- ✅ Device pairing with station code
- ✅ Real-time lock/unlock based on session status
- ✅ Grace period on network loss
- ✅ Emergency exit (5-click + PIN)
- ✅ Debug exit button (Debug builds only)
- ✅ Audit event logging with offline queue
- ✅ Auto-restart on boot and crash
- ✅ Encrypted local storage
- ✅ Hybrid routing (Localhost + Vercel compatibility)

#### Backend Integration
- ✅ Hybrid routing strategy for Localhost and Vercel
- ✅ Path-based pairing: `/api/tv-devices/pair`
- ✅ Query-based status: `/api/tv-devices?action=status`
- ✅ Query-based audit: `/api/tv-devices?action=audit`
- ✅ Token-based authentication
- ✅ Auto-unpair on token invalidation

#### Known Issues & Limitations
- ⚠️ HOME button can still be pressed (requires Device Owner mode)
- ⚠️ Settings accessible via emergency exit (intentional for shop owner)
- ⚠️ Polling interval fixed at 12s (not configurable at runtime)

#### Deployment Environments
- **Development**: Localhost (`http://10.0.2.2:3002/`)
- **Staging**: Vercel Preview (`https://gamerspot-g5ucj66zy-seemabharanis-projects.vercel.app/`)
- **Production**: Vercel Production (`https://www.gamebiller.com/`)

---

## 🔐 Security Considerations

### Data Protection
- **Pairing Credentials**: Encrypted with AES256-GCM
- **Master Key**: Stored in Android Keystore (hardware-backed)
- **Network Communication**: HTTPS only
- **Token Storage**: Never logged or exposed

### Attack Vectors & Mitigations
| Attack Vector | Mitigation |
|---------------|------------|
| Physical access to Settings | Emergency exit requires PIN |
| Network interception | HTTPS with certificate pinning (future) |
| Token theft | Short-lived tokens, auto-unpair on invalid |
| App uninstall | Requires Settings access (PIN-protected) |
| Force stop | Auto-restart via AlarmManager |

### Compliance
- **GDPR**: No personal data collected
- **Data Retention**: Audit logs stored locally, synced to backend
- **User Privacy**: Device ID is anonymized UUID

---

## 📞 Support & Contact

### Technical Support
- **Repository**: https://github.com/Seema0504/GameBillerTV
- **Branch**: `test`
- **Issue Tracker**: GitHub Issues

### Documentation
- **Deployment Guide**: `DEPLOYMENT_GUIDE.md`
- **Backend Integration**: `BACKEND_INTEGRATION.md`
- **Testing Guide**: `TESTING_GUIDE.md`
- **API Fixes**: `README_API_FIXES.md`

### Development Team
- **Project**: GameBiller (Gamers Spot)
- **Platform**: Android TV
- **License**: Proprietary

---

## 📝 Appendix

### A. Build Commands Reference

```powershell
# Set environment variables
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:Path += ";C:\Users\racin\AppData\Local\Android\Sdk\platform-tools"

# Clean build
.\gradlew.bat clean

# Debug build
.\gradlew.bat assembleDebug

# Release build
.\gradlew.bat assembleRelease

# Install via ADB
adb install -r "app\build\outputs\apk\debug\app-debug.apk"

# Uninstall
adb uninstall com.gamebiller.tvlock

# View logs
adb logcat | Select-String "GameBiller"

# Connect via network
adb connect 192.168.1.100:5555
```

---

### B. API Endpoint Summary

| Endpoint | Method | Purpose | Auth Required |
|----------|--------|---------|---------------|
| `/api/tv-devices/pair` | POST | Device pairing | No |
| `/api/tv-devices?action=status` | GET | Station status | Yes (Bearer token) |
| `/api/tv-devices?action=audit` | POST | Audit logging | No |

---

### C. Configuration Constants

| Constant | Default Value | Location | Purpose |
|----------|---------------|----------|---------|
| `API_BASE_URL` | Vercel URL | `build.gradle.kts` | Backend endpoint |
| `POLLING_INTERVAL_SECONDS` | 12 | `build.gradle.kts` | Status check frequency |
| `GRACE_PERIOD_SECONDS` | 30 | `build.gradle.kts` | Network loss buffer |
| `EMERGENCY_PIN` | 5555 | `LockScreen.kt` | Emergency exit PIN |
| `CLICK_COUNT_TRIGGER` | 5 | `LockScreen.kt` | Emergency exit clicks |

---

### D. File Locations

| File | Path | Purpose |
|------|------|---------|
| Debug APK | `app\build\outputs\apk\debug\app-debug.apk` | Testing build |
| Release APK | `app\build\outputs\apk\release\app-release.apk` | Production build |
| Build Config | `app\build.gradle.kts` | App configuration |
| Manifest | `app\src\main\AndroidManifest.xml` | App permissions |
| ProGuard Rules | `app\proguard-rules.pro` | Code obfuscation |

---

**End of Documentation**

*This document is intended for developers, QA testers, and AI assistants (ChatGPT, Claude, etc.) who need comprehensive context about the GameBillerTV project.*
