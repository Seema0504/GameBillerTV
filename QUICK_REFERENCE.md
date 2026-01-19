# 🎯 GameBillerTV - Quick Reference Card

## 📱 App Information
- **Package:** `com.gamebiller.tvlock`
- **Main Activity:** `com.gamebiller.tvlock.ui.MainActivity`
- **Backend URL (Debug):** `http://10.0.2.2:3002/`
- **Polling Interval:** 12 seconds
- **Grace Period:** 30 seconds

---

## 🚀 Essential ADB Commands

### Launch & Control
```powershell
# Launch app
adb shell am start -n com.gamebiller.tvlock/.ui.MainActivity

# Stop app
adb shell am force-stop com.gamebiller.tvlock

# Clear app data (reset to unpaired)
adb shell pm clear com.gamebiller.tvlock

# Reinstall app
.\gradlew.bat assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

### Debugging
```powershell
# Check if app is running
adb shell dumpsys window windows | findstr "mCurrentFocus"

# Monitor logs (filtered)
adb logcat -s MainActivity:D LockViewModel:D AndroidRuntime:E

# Take screenshot
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png

# Test network from emulator
adb shell "curl -v http://10.0.2.2:3002/health"
```

---

## 🧪 Test Scripts

```powershell
cd "c:\Dev\GameBiller Smart Lock\Version 1\GameBillerTV\test-scripts"

# Launch app and verify
.\launch-app.ps1

# Monitor logs in real-time
.\monitor-app.ps1

# Debug black screen
.\debug-black-screen.ps1

# Check network connectivity
.\check-network.ps1

# Run all business rule tests
.\test-business-rules.ps1
```

---

## 📊 App States

| State | Description | UI Display |
|-------|-------------|------------|
| **Unpaired** | Device not paired with backend | Pairing Screen |
| **Locked** | Station STOPPED/PAUSED/NOT_STARTED | Lock Screen with reason |
| **Unlocked** | Station RUNNING | Minimal/No UI (HDMI visible) |
| **Grace Period** | Network failure, countdown active | Lock Screen with countdown |

---

## 🔄 Business Rules

### Lock Conditions
- ✅ Station status: `STOPPED`, `PAUSED`, `NOT_STARTED`
- ✅ Token invalid (401)
- ✅ Network failure after grace period expires

### Unlock Conditions
- ✅ Station status: `RUNNING`
- ✅ Valid token
- ✅ Successful backend polling

### Grace Period
- **Trigger:** 3 consecutive network failures (~36 seconds)
- **Duration:** 30 seconds
- **Can be aborted:** Yes, if network recovers
- **After expiry:** TV locks with reason `NETWORK_FAILURE`

---

## 🌐 Backend Endpoints

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/health` | GET | Health check |
| `/api/devices/pair` | POST | Pair device with code |
| `/api/stations/{id}/status` | GET | Get station status |
| `/api/audit-events` | POST | Send audit events |

---

## 🔍 Expected Status Values

| Status | App Behavior |
|--------|--------------|
| `RUNNING` | Unlock (show HDMI) |
| `STOPPED` | Lock (SESSION_STOPPED) |
| `PAUSED` | Lock (SESSION_PAUSED) |
| `NOT_STARTED` | Lock (SESSION_NOT_STARTED) |
| `TOKEN_INVALID` | Lock immediately (TOKEN_INVALID) |
| Network error | Start grace period |

---

## 🐛 Troubleshooting Quick Fixes

### Black Screen
```powershell
.\test-scripts\debug-black-screen.ps1
# Check screenshot in .\screenshots\
```

### Network Issues
```powershell
# Test backend
Invoke-WebRequest -Uri "http://localhost:3002/health"

# Test from emulator
adb shell "curl http://10.0.2.2:3002/health"
```

### App Not Launching
```powershell
# Check device connection
adb devices

# Force stop and relaunch
adb shell am force-stop com.gamebiller.tvlock
adb shell am start -n com.gamebiller.tvlock/.ui.MainActivity
```

### Polling Not Working
```powershell
# Check pairing status
adb logcat -d | Select-String "paired|Unpaired"

# Monitor polling
adb logcat -c
# Wait 15 seconds
adb logcat -d | Select-String "pollStationStatus"
```

---

## ✅ Pre-Flight Checklist

Before testing:
- [ ] Android TV emulator running (API 36, 1080p)
- [ ] ADB in PATH (`adb version` works)
- [ ] Backend running on `localhost:3002`
- [ ] App built (`.\gradlew.bat assembleDebug`)
- [ ] App installed on emulator

---

## 📞 Quick Diagnostics

```powershell
# All-in-one diagnostic
adb devices                                          # Check connection
adb shell dumpsys window windows | findstr "mCurrentFocus"  # Check app focus
adb logcat -d -s AndroidRuntime:E                   # Check crashes
adb shell "curl http://10.0.2.2:3002/health"        # Check network
adb shell screencap -p /sdcard/screenshot.png       # Take screenshot
adb pull /sdcard/screenshot.png
```

---

## 🎓 Key Files

| File | Purpose |
|------|---------|
| `AndroidManifest.xml` | App configuration, launcher setup |
| `MainActivity.kt` | Main entry point, UI lifecycle |
| `LockViewModel.kt` | Business logic, polling, state management |
| `LockRepository.kt` | Backend API calls, data persistence |
| `app/build.gradle.kts` | Build config, BASE_URL |

---

## 📖 Full Documentation

See `TESTING_GUIDE.md` for comprehensive testing instructions.

---

**Print this card and keep it handy! 📋**
