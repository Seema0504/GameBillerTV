# GameBiller TV Lock - Android TV Application

## Overview

GameBiller TV Lock is a kiosk-style Android TV application that enforces session-based access control for gaming stations. The TV screen remains locked unless an active GameBiller station session is running, preventing revenue leakage and unauthorized usage.

## Core Features

- **Fail-safe Lock Mechanism**: TV is locked by default; unlocks only when station status = RUNNING
- **Device Pairing**: One-time setup to link TV with a specific GameBiller station
- **Real-time Status Polling**: Checks station status every 12 seconds
- **Grace Period**: 30-second buffer before locking when network is lost
- **Auto-restart**: App relaunches on boot and after crashes
- **Audit Logging**: Tracks all lock/unlock events and sends to backend

## Architecture

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose for TV
- **Architecture**: MVVM with Hilt dependency injection
- **Networking**: Retrofit + OkHttp
- **Local Storage**: DataStore (encrypted)
- **Minimum SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)

## Project Structure

```
app/src/main/kotlin/com/gamebiller/tvlock/
├── GameBillerApp.kt                    # Application class
├── data/
│   ├── local/
│   │   └── DevicePreferences.kt        # Encrypted local storage
│   └── remote/
│       ├── ApiModels.kt                # API request/response models
│       └── ApiService.kt               # Retrofit interface
├── di/
│   └── NetworkModule.kt                # Hilt dependency injection
├── domain/
│   ├── LockRepository.kt               # Business logic layer
│   └── model/
│       ├── AuditEvent.kt               # Audit event models
│       ├── DeviceInfo.kt               # Device pairing info
│       ├── LockState.kt                # Lock state sealed class
│       └── StationStatus.kt            # Station status sealed class
├── receiver/
│   ├── AppRestartReceiver.kt           # App restart handler
│   └── BootReceiver.kt                 # Boot auto-launch
├── service/
│   └── StatusPollingService.kt         # Foreground service
└── ui/
    ├── MainActivity.kt                 # Main activity
    ├── screens/
    │   ├── LockScreen.kt               # Lock overlay UI
    │   └── PairingScreen.kt            # Device pairing UI
    ├── theme/
    │   └── Theme.kt                    # Material3 theme
    └── viewmodel/
        ├── LockViewModel.kt            # Lock state management
        └── PairingViewModel.kt         # Pairing logic
```

## Backend API Requirements

The app requires the following endpoints on the GameBiller backend:

### 1. Device Pairing
```
POST /api/tv-devices/pair
Request: {
  "station_code": "ABC123",
  "device_id": "TV-XYZ"
}
Response: {
  "shop_id": 1,
  "station_id": 3,
  "device_id": "TV-XYZ",
  "shop_name": "ABC Gaming Zone",
  "station_name": "PS-1"
}
```

### 2. Station Status
```
GET /api/stations/{station_id}/status
Response: {
  "station_id": 3,
  "status": "RUNNING" | "STOPPED" | "PAUSED" | "NOT_STARTED",
  "shop_name": "ABC Gaming Zone",
  "station_name": "PS-1"
}
```

### 3. Audit Events
```
POST /api/tv-devices/audit
Request: {
  "event": "TV_UNLOCKED",
  "station_id": 3,
  "device_id": "TV-XYZ",
  "timestamp": "2026-01-19T04:50:00Z",
  "metadata": { ... }
}
```

## Configuration

Edit `app/build.gradle.kts` to configure:

```kotlin
buildConfigField("String", "API_BASE_URL", "\"https://your-backend.com\"")
buildConfigField("int", "POLLING_INTERVAL_SECONDS", "12")
buildConfigField("int", "GRACE_PERIOD_SECONDS", "30")
```

## Building the App

### Debug Build (for testing)
```bash
./gradlew assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

### Release Build (for production)
```bash
./gradlew assembleRelease
```
Output: `app/build/outputs/apk/release/app-release.apk`

## Installation

### Via ADB
```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

### Via USB Drive
1. Copy APK to USB drive
2. Insert USB into Android TV
3. Use file manager to install APK

## First-Time Setup

1. Install and launch the app
2. App shows "Device Pairing" screen
3. Enter station code provided by staff
4. Click "Pair Device"
5. App saves pairing info and starts monitoring

## Testing Scenarios

### Test 1: Fresh Install
- Install APK → Should show pairing screen
- Enter valid code → Should pair and show lock screen

### Test 2: Station Running
- Backend: Set station status to `RUNNING`
- App: Lock screen should disappear within 12 seconds

### Test 3: Station Stopped
- Backend: Set station status to `STOPPED`
- App: Lock screen should appear within 12 seconds

### Test 4: Network Loss
- Disconnect TV from network
- App: Should show grace period countdown (30s)
- After 30s: Should show lock screen

### Test 5: Reboot
- Reboot Android TV
- App: Should auto-launch and show lock screen

### Test 6: Back Button
- Press back button on remote
- App: Should remain on current screen (no exit)

## Security Features

### Phase 1 (MVP - Implemented)
- ✅ Full-screen immersive mode
- ✅ Back button disabled
- ✅ Auto-restart on boot
- ✅ Fail-safe lock logic
- ✅ Encrypted local storage

### Phase 2 (Future Enhancement)
- ⏳ Kiosk mode (requires Device Owner)
- ⏳ HOME button blocking
- ⏳ PIN-protected admin exit
- ⏳ Settings access restriction

## Troubleshooting

### App doesn't auto-launch on boot
- Check `RECEIVE_BOOT_COMPLETED` permission in manifest
- Verify BootReceiver is registered

### Lock screen doesn't appear
- Check network connectivity
- Verify backend API is accessible
- Check Logcat for errors: `adb logcat | grep GameBiller`

### Pairing fails
- Verify station code is correct
- Check backend API endpoint is working
- Ensure TV has internet connection

## Logging

View logs in real-time:
```bash
adb logcat | grep -E "GameBiller|OkHttp"
```

## License

Proprietary - GameBiller (Gamers Spot)

## Support

For technical support, contact the GameBiller development team.
