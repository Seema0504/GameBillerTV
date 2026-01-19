# GameBillerTV - Android TV App Testing Guide

## 📋 Table of Contents
1. [Prerequisites](#prerequisites)
2. [Launcher Configuration Verification](#1-launcher-configuration-verification)
3. [Force-Launch the App](#2-force-launch-the-app)
4. [Debug Black Screen Issues](#3-debug-black-screen-issues)
5. [Network Connectivity Verification](#4-network-connectivity-verification)
6. [Business Rules Testing](#5-business-rules-testing)
7. [Final Checklists](#6-final-checklists)

---

## Prerequisites

### 1. Set up ADB (Android Debug Bridge)

**Locate Android SDK Platform Tools:**
- Usually at: `C:\Users\<YourUsername>\AppData\Local\Android\Sdk\platform-tools`
- Or in Android Studio: `File → Settings → Appearance & Behavior → System Settings → Android SDK`

**Add ADB to PATH (temporary):**
```powershell
$env:Path += ";C:\Users\<YourUsername>\AppData\Local\Android\Sdk\platform-tools"
```

**Verify ADB:**
```powershell
adb version
```

### 2. Start Android TV Emulator

In Android Studio:
- **Tools → Device Manager**
- Start your **Android TV (1080p), API 36** emulator
- Wait for it to fully boot

### 3. Ensure Backend is Running

```powershell
# Test if backend is accessible
Invoke-WebRequest -Uri "http://localhost:3002/health"
```

---

## 1️⃣ Launcher Configuration Verification

### ✅ Status: VERIFIED

Your `AndroidManifest.xml` is correctly configured:

```xml
<activity
    android:name=".ui.MainActivity"
    android:exported="true"
    android:launchMode="singleTask"
    android:screenOrientation="landscape">
    
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LEANBACK_LAUNCHER" />
    </intent-filter>
</activity>
```

**Confirmed:**
- ✅ `android.intent.action.MAIN`
- ✅ `android.intent.category.LEANBACK_LAUNCHER`
- ✅ `android:exported="true"`
- ✅ Leanback feature required
- ✅ Landscape orientation enforced
- ✅ Fullscreen theme applied

**No changes needed!**

---

## 2️⃣ Force-Launch the App

### Using PowerShell Script (Recommended)

```powershell
cd "c:\Dev\GameBiller Smart Lock\Version 1\GameBillerTV\test-scripts"
.\launch-app.ps1
```

### Manual Commands

```powershell
# 1. Check connected devices
adb devices

# Expected output:
# List of devices attached
# emulator-5554   device

# 2. Force stop any existing instance
adb shell am force-stop com.gamebiller.tvlock

# 3. Launch MainActivity
adb shell am start -n com.gamebiller.tvlock/.ui.MainActivity

# Expected output:
# Starting: Intent { cmp=com.gamebiller.tvlock/.ui.MainActivity }

# 4. Verify app is running
adb shell dumpsys window windows | findstr "mCurrentFocus"

# Expected output:
# mCurrentFocus=Window{... com.gamebiller.tvlock/com.gamebiller.tvlock.ui.MainActivity}
```

### Expected UI Output

**If NOT paired:**
- Pairing Screen with code input field
- Instructions to enter pairing code

**If paired:**
- Lock Screen (if station is STOPPED)
- OR minimal/no UI (if station is RUNNING)

---

## 3️⃣ Debug Black Screen Issues

### Quick Diagnostic Script

```powershell
cd "c:\Dev\GameBiller Smart Lock\Version 1\GameBillerTV\test-scripts"
.\debug-black-screen.ps1
```

This script will:
1. Check if MainActivity was created
2. Verify Compose UI rendering
3. Look for crashes/exceptions
4. Check window focus
5. Verify app is in foreground
6. Check LockViewModel state
7. Verify pairing status
8. **Take a screenshot** (saved to `.\screenshots\`)
9. Check GPU rendering

### Manual Debugging

#### Check Activity Launch
```powershell
adb logcat -d -s MainActivity:D | Select-String "created"
```
**Expected:** `MainActivity created`

#### Check Compose Rendering
```powershell
adb logcat -d | Select-String "Compose|setContent"
```
**Expected:** Compose initialization logs

#### Check for Crashes
```powershell
adb logcat -d -s AndroidRuntime:E
```
**Expected:** No output (no crashes)

#### Take Screenshot
```powershell
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png .\screenshot.png
```

#### Monitor Real-Time Logs
```powershell
cd "c:\Dev\GameBiller Smart Lock\Version 1\GameBillerTV\test-scripts"
.\monitor-app.ps1
```

---

## 4️⃣ Network Connectivity Verification

### Using Script (Recommended)

```powershell
cd "c:\Dev\GameBiller Smart Lock\Version 1\GameBillerTV\test-scripts"
.\check-network.ps1
```

### Manual Verification

#### Test Backend Connectivity from Emulator
```powershell
adb shell "curl -v http://10.0.2.2:3002/health"
```

**Expected:** HTTP 200 response from backend

**Note:** `10.0.2.2` is the emulator's alias for `localhost` on the host machine.

#### Verify BASE_URL Configuration

**Current configuration** (from `app/build.gradle.kts`):
```kotlin
debug {
    buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:3002/\"")
}
```

**Polling interval:** 12 seconds (configured in `LockViewModel.kt`)

#### Check App Network Permissions
```powershell
adb shell dumpsys package com.gamebiller.tvlock | Select-String "INTERNET"
```

**Expected:** `android.permission.INTERNET: granted=true`

---

## 5️⃣ Business Rules Testing

### Interactive Test Script

```powershell
cd "c:\Dev\GameBiller Smart Lock\Version 1\GameBillerTV\test-scripts"
.\test-business-rules.ps1
```

This script will guide you through testing all business rules step-by-step.

### Manual Testing Guide

#### TEST 1: App Starts in LOCKED State (Unpaired)

**Setup:**
```powershell
# Clear app data to simulate first launch
adb shell pm clear com.gamebiller.tvlock

# Launch app
adb shell am start -n com.gamebiller.tvlock/.ui.MainActivity
```

**Expected Behavior:**
- ✅ App shows **Pairing Screen**
- ✅ User can enter pairing code
- ✅ Logs show `LockState.Unpaired`

---

#### TEST 2: Device Pairing

**Setup:**
1. Generate a pairing code on your backend
2. Enter the code in the app using the D-pad

**Expected Behavior:**
- ✅ App sends pairing request to `POST /api/devices/pair`
- ✅ Backend validates code and returns device info
- ✅ App saves device info to DataStore
- ✅ App transitions to Lock Screen
- ✅ Logs show `Pairing successful, starting status polling`

**Verify:**
```powershell
adb logcat -d | Select-String "Pairing successful|paired"
```

---

#### TEST 3: Backend Returns STOPPED → Remains Locked

**Setup:**
- Ensure backend returns `status: "STOPPED"` for the paired station

**Expected Behavior:**
- ✅ App polls backend every **12 seconds**
- ✅ Backend returns `STOPPED` status
- ✅ App shows **LOCKED** screen
- ✅ Lock reason: `SESSION_STOPPED`
- ✅ Logs show `updateLockState` with `STOPPED`

**Verify:**
```powershell
# Monitor polling
adb logcat -c
# Wait 15 seconds
adb logcat -d | Select-String "pollStationStatus|SESSION_STOPPED"
```

---

#### TEST 4: Backend Returns RUNNING → Unlock UI

**Setup:**
- Change backend to return `status: "RUNNING"` for the paired station

**Expected Behavior:**
- ✅ App polls backend and receives `RUNNING` status
- ✅ App transitions to **UNLOCKED** state
- ✅ Screen shows minimal/no UI (HDMI input visible)
- ✅ Logs show `TV unlocked - showing HDMI input`
- ✅ Audit event `TV_UNLOCKED` sent to backend

**Verify:**
```powershell
adb logcat -d | Select-String "RUNNING|Unlocked|TV unlocked"
```

---

#### TEST 5: Network Failure → Grace Period → Lock

**Setup:**
1. Ensure station is in `RUNNING` state (unlocked)
2. **STOP the backend server** to simulate network failure

**Expected Behavior:**
- ✅ App fails to poll backend (3 consecutive failures = 36 seconds)
- ✅ Grace period starts (30 seconds countdown)
- ✅ Screen shows **"Connection Lost"** with countdown
- ✅ Logs show `Starting grace period`
- ✅ After 30 seconds, screen **LOCKS**
- ✅ Lock reason: `NETWORK_FAILURE`
- ✅ Audit events: `GRACE_PERIOD_STARTED` → `GRACE_PERIOD_EXPIRED`

**Verify:**
```powershell
# Monitor grace period (takes ~66 seconds total)
adb logcat -c
# Wait for grace period to start and expire
adb logcat -d | Select-String "grace|Grace|GRACE"
```

**Timeline:**
- 0-36s: 3 consecutive polling failures
- 36s: Grace period starts
- 36-66s: Grace period countdown
- 66s: Grace period expires, TV locks

---

#### TEST 6: Network Recovery During Grace Period

**Setup:**
1. Trigger grace period (stop backend)
2. **START backend** during the grace period (before 30s expires)
3. Backend returns `RUNNING` status

**Expected Behavior:**
- ✅ Grace period starts
- ✅ Backend comes back online
- ✅ App successfully polls and receives `RUNNING`
- ✅ Grace period is **aborted**
- ✅ App returns to **UNLOCKED** state
- ✅ Logs show `Grace period aborted due to recovery`

**Verify:**
```powershell
adb logcat -d | Select-String "Grace period aborted|recovered"
```

---

#### TEST 7: Token Invalid → Immediate Lock

**Setup:**
- Configure backend to return `401 Unauthorized` or `status: "TOKEN_INVALID"`

**Expected Behavior:**
- ✅ App receives `TOKEN_INVALID` status
- ✅ App **IMMEDIATELY locks** (no grace period)
- ✅ Lock reason: `TOKEN_INVALID`
- ✅ Screen shows error message about invalid token
- ✅ Logs show `TOKEN_INVALID`

**Verify:**
```powershell
adb logcat -d | Select-String "TOKEN_INVALID|Locked"
```

---

## 6️⃣ Final Checklists

### 🖥️ Emulator Checklist

- [ ] Android TV emulator is running (API 36, 1080p)
- [ ] Emulator has network connectivity
- [ ] Emulator can reach `10.0.2.2:3002` (host backend)
- [ ] Emulator displays UI correctly (not black screen)
- [ ] D-pad navigation works
- [ ] Screenshots can be captured

**Verify:**
```powershell
adb devices
adb shell "curl http://10.0.2.2:3002/health"
```

---

### 📱 App Checklist

- [ ] App builds successfully (`.\gradlew.bat assembleDebug`)
- [ ] App installs on emulator
- [ ] App launches from Android TV launcher
- [ ] App can be force-launched via ADB
- [ ] MainActivity is exported and has correct intent filters
- [ ] App has INTERNET permission
- [ ] App uses correct BASE_URL (`http://10.0.2.2:3002/`)
- [ ] Compose UI renders correctly
- [ ] Immersive mode hides system bars
- [ ] Screen stays on (FLAG_KEEP_SCREEN_ON)
- [ ] Back button is disabled

**Verify:**
```powershell
.\gradlew.bat assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.gamebiller.tvlock/.ui.MainActivity
```

---

### 🌐 Backend Checklist

- [ ] Backend server is running on `localhost:3002`
- [ ] Backend has `/health` endpoint
- [ ] Backend has `/api/devices/pair` endpoint
- [ ] Backend has `/api/stations/{id}/status` endpoint
- [ ] Backend can generate pairing codes
- [ ] Backend returns correct status values:
  - `RUNNING`
  - `STOPPED`
  - `PAUSED`
  - `NOT_STARTED`
  - `TOKEN_INVALID`
- [ ] Backend accepts audit events
- [ ] Backend handles 401 Unauthorized correctly

**Verify:**
```powershell
Invoke-WebRequest -Uri "http://localhost:3002/health"
```

---

### 🚀 Go-Live Confidence Checklist

#### Functionality
- [ ] Device pairing works correctly
- [ ] Status polling occurs every 12 seconds
- [ ] Lock screen displays when station is STOPPED
- [ ] Unlock (HDMI passthrough) works when station is RUNNING
- [ ] Grace period activates on network failure
- [ ] Grace period can be aborted on network recovery
- [ ] Token invalid triggers immediate lock
- [ ] Audit events are sent to backend
- [ ] App auto-launches on device boot (BootReceiver)

#### UI/UX
- [ ] Pairing screen is clear and usable with D-pad
- [ ] Lock screen shows shop name, station name, and reason
- [ ] Grace period countdown is visible and accurate
- [ ] Unlocked state shows minimal/no UI
- [ ] All screens are landscape-oriented
- [ ] System bars are hidden (immersive mode)
- [ ] No navigation away from app is possible

#### Stability
- [ ] App doesn't crash on launch
- [ ] App doesn't crash on network failure
- [ ] App doesn't crash on invalid backend responses
- [ ] App recovers gracefully from errors
- [ ] Polling continues reliably in background
- [ ] App restarts if killed (AppRestartReceiver)

#### Security
- [ ] Device token is stored securely (Encrypted DataStore)
- [ ] Network traffic uses HTTPS in production
- [ ] Token is sent in Authorization header
- [ ] Invalid tokens are handled correctly

#### Performance
- [ ] App launches quickly (< 3 seconds)
- [ ] UI is responsive to D-pad input
- [ ] Polling doesn't cause UI lag
- [ ] Memory usage is stable over time
- [ ] No memory leaks

---

## 🛠️ Troubleshooting

### Black Screen Issues

**Possible Causes:**
1. Compose UI not rendering
2. Activity not created
3. Emulator GPU issue
4. Theme/styling issue

**Solutions:**
```powershell
# 1. Check logs
.\test-scripts\debug-black-screen.ps1

# 2. Take screenshot
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png

# 3. Restart emulator
# In Android Studio: Device Manager → Stop → Start

# 4. Clear app data and reinstall
adb shell pm clear com.gamebiller.tvlock
adb uninstall com.gamebiller.tvlock
.\gradlew.bat assembleDebug
adb install app\build\outputs\apk\debug\app-debug.apk
```

---

### Network Connectivity Issues

**Possible Causes:**
1. Backend not running
2. Firewall blocking connection
3. Incorrect BASE_URL
4. Emulator network issue

**Solutions:**
```powershell
# 1. Verify backend is running
Invoke-WebRequest -Uri "http://localhost:3002/health"

# 2. Test from emulator
adb shell "curl -v http://10.0.2.2:3002/health"

# 3. Check firewall
# Windows Defender Firewall → Allow an app → Node.js

# 4. Restart emulator networking
adb shell "svc wifi disable"
adb shell "svc wifi enable"
```

---

### Polling Not Working

**Possible Causes:**
1. Device not paired
2. Lifecycle not STARTED
3. Coroutine cancelled
4. Backend returning errors

**Solutions:**
```powershell
# 1. Check pairing status
adb logcat -d | Select-String "paired|Unpaired"

# 2. Check polling logs
adb logcat -c
# Wait 15 seconds
adb logcat -d | Select-String "pollStationStatus|Polling"

# 3. Check for errors
adb logcat -d -s LockViewModel:E

# 4. Restart app
adb shell am force-stop com.gamebiller.tvlock
adb shell am start -n com.gamebiller.tvlock/.ui.MainActivity
```

---

## 📞 Quick Reference Commands

```powershell
# Launch app
adb shell am start -n com.gamebiller.tvlock/.ui.MainActivity

# Stop app
adb shell am force-stop com.gamebiller.tvlock

# Clear app data
adb shell pm clear com.gamebiller.tvlock

# Monitor logs
adb logcat -s MainActivity:D LockViewModel:D AndroidRuntime:E

# Take screenshot
adb shell screencap -p /sdcard/screenshot.png && adb pull /sdcard/screenshot.png

# Test network
adb shell "curl http://10.0.2.2:3002/health"

# Check current focus
adb shell dumpsys window windows | findstr "mCurrentFocus"

# Reinstall app
.\gradlew.bat assembleDebug && adb install -r app\build\outputs\apk\debug\app-debug.apk
```

---

## 📝 Test Results Template

Use this template to document your test results:

```markdown
# GameBillerTV Test Results
Date: YYYY-MM-DD
Tester: [Your Name]
Build: [Version]

## Emulator Configuration
- [ ] Android TV (1080p), API 36
- [ ] Network: Connected
- [ ] Backend: localhost:3002

## Test Results

### 1. Launcher Configuration
- Status: ✅ PASS / ❌ FAIL
- Notes:

### 2. Force Launch
- Status: ✅ PASS / ❌ FAIL
- Notes:

### 3. Black Screen Debug
- Status: ✅ PASS / ❌ FAIL
- Screenshot: [Attach]
- Notes:

### 4. Network Connectivity
- Status: ✅ PASS / ❌ FAIL
- Notes:

### 5. Business Rules
- TEST 1 (Unpaired): ✅ PASS / ❌ FAIL
- TEST 2 (Pairing): ✅ PASS / ❌ FAIL
- TEST 3 (STOPPED → Locked): ✅ PASS / ❌ FAIL
- TEST 4 (RUNNING → Unlocked): ✅ PASS / ❌ FAIL
- TEST 5 (Network Failure → Grace → Lock): ✅ PASS / ❌ FAIL
- TEST 6 (Network Recovery): ✅ PASS / ❌ FAIL
- TEST 7 (Token Invalid): ✅ PASS / ❌ FAIL

## Issues Found
1. [Issue description]
2. [Issue description]

## Overall Status
- [ ] Ready for production
- [ ] Needs fixes
```

---

## 🎉 Success Criteria

Your app is ready for production when:

✅ All 7 business rule tests pass  
✅ No black screen issues  
✅ Network connectivity is reliable  
✅ Polling works consistently  
✅ Grace period behaves correctly  
✅ UI is responsive and clear  
✅ App auto-launches on boot  
✅ App restarts if killed  
✅ No crashes or errors in logs  
✅ Backend integration is stable  

---

**Good luck with your testing! 🚀**
