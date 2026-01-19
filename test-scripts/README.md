# Test Scripts - Quick Reference

This directory contains automated PowerShell scripts to help you test the GameBillerTV Android TV app.

## 📁 Available Scripts

### 1. `launch-app.ps1`
**Purpose:** Launch the app and verify it's running

**Usage:**
```powershell
.\launch-app.ps1
```

**What it does:**
- Checks device connection
- Stops any existing app instance
- Launches MainActivity
- Verifies app is in foreground
- Shows activity stack

---

### 2. `monitor-app.ps1`
**Purpose:** Monitor app logs in real-time

**Usage:**
```powershell
.\monitor-app.ps1
```

**What it does:**
- Clears logcat buffer
- Monitors filtered logs for:
  - MainActivity
  - LockViewModel
  - PairingViewModel
  - Compose rendering
  - Errors and crashes

**Tip:** Run this in a separate PowerShell window while testing

---

### 3. `debug-black-screen.ps1`
**Purpose:** Diagnose black screen issues

**Usage:**
```powershell
.\debug-black-screen.ps1
```

**What it does:**
- Checks if MainActivity was created
- Verifies Compose UI rendering
- Looks for crashes/exceptions
- Checks window focus
- Verifies app is in foreground
- Checks LockViewModel state
- Verifies pairing status
- **Takes a screenshot** (saved to `.\screenshots\`)
- Checks GPU rendering

**Output:** Screenshot saved to `.\screenshots\screenshot_YYYYMMDD_HHMMSS.png`

---

### 4. `check-network.ps1`
**Purpose:** Verify network connectivity

**Usage:**
```powershell
.\check-network.ps1
```

**What it does:**
- Tests connectivity to `10.0.2.2:3002` from emulator
- Checks emulator network interfaces
- Verifies backend server is running on host
- Checks app's INTERNET permission
- Shows current BASE_URL configuration

---

### 5. `test-business-rules.ps1`
**Purpose:** Interactive guide to test all business rules

**Usage:**
```powershell
.\test-business-rules.ps1
```

**What it does:**
- Guides you through 7 comprehensive tests:
  1. App starts in UNPAIRED state
  2. Device pairing
  3. Backend STOPPED → Locked
  4. Backend RUNNING → Unlocked
  5. Network failure → Grace period → Lock
  6. Network recovery during grace period
  7. Token invalid → Immediate lock

**Duration:** ~15-20 minutes (interactive)

---

## 🚀 Quick Start

### First Time Setup

1. **Add ADB to PATH:**
   ```powershell
   $env:Path += ";C:\Users\<YourUsername>\AppData\Local\Android\Sdk\platform-tools"
   ```

2. **Verify ADB:**
   ```powershell
   adb version
   ```

3. **Start Android TV Emulator** in Android Studio

4. **Start Backend Server** on `localhost:3002`

---

### Typical Testing Workflow

```powershell
# Navigate to test scripts directory
cd "c:\Dev\GameBiller Smart Lock\Version 1\GameBillerTV\test-scripts"

# 1. Launch the app
.\launch-app.ps1

# 2. Start monitoring logs (in a separate window)
.\monitor-app.ps1

# 3. If you see a black screen
.\debug-black-screen.ps1

# 4. Verify network connectivity
.\check-network.ps1

# 5. Run comprehensive business rules tests
.\test-business-rules.ps1
```

---

## 📋 Prerequisites

Before running any script, ensure:

- [ ] Android TV emulator is running (API 36, 1080p)
- [ ] ADB is in your PATH
- [ ] Backend server is running on `localhost:3002`
- [ ] App is built (`.\gradlew.bat assembleDebug`)
- [ ] App is installed on emulator

---

## 🛠️ Troubleshooting

### "adb is not recognized"

**Solution:**
```powershell
# Find your Android SDK path
# Usually: C:\Users\<YourUsername>\AppData\Local\Android\Sdk\platform-tools

# Add to PATH
$env:Path += ";C:\Users\<YourUsername>\AppData\Local\Android\Sdk\platform-tools"

# Verify
adb version
```

---

### "No devices/emulators found"

**Solution:**
1. Start Android TV emulator in Android Studio
2. Wait for it to fully boot
3. Run `adb devices` to verify connection

---

### "Connection refused" when testing network

**Solution:**
1. Verify backend is running: `Invoke-WebRequest -Uri "http://localhost:3002/health"`
2. Check Windows Firewall settings
3. Ensure backend is listening on `0.0.0.0:3002` (not just `localhost`)

---

### Screenshots not saving

**Solution:**
```powershell
# Create screenshots directory manually
New-Item -ItemType Directory -Path ".\screenshots" -Force
```

---

## 📖 Full Documentation

For complete testing guide, see: `../TESTING_GUIDE.md`

---

## 🎯 Quick Commands

```powershell
# Launch app
adb shell am start -n com.gamebiller.tvlock/.ui.MainActivity

# Stop app
adb shell am force-stop com.gamebiller.tvlock

# Clear app data
adb shell pm clear com.gamebiller.tvlock

# Take screenshot
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png .\screenshot.png

# Monitor logs
adb logcat -s MainActivity:D LockViewModel:D AndroidRuntime:E

# Test network from emulator
adb shell "curl http://10.0.2.2:3002/health"
```

---

## 📞 Support

If you encounter issues:

1. Check `../TESTING_GUIDE.md` for detailed troubleshooting
2. Review logs with `.\monitor-app.ps1`
3. Take a screenshot with `.\debug-black-screen.ps1`
4. Check network with `.\check-network.ps1`

---

**Happy Testing! 🚀**
