# 📚 GameBillerTV Testing Resources - Index

Welcome to the GameBillerTV Android TV App Testing Suite!

This index will help you navigate all the testing resources and get started quickly.

---

## 🎯 Start Here

### **New to Testing This App?**
1. Read: **[TESTING_SUMMARY.md](TESTING_SUMMARY.md)** (5 min read)
2. Review: **[TESTING_FLOW_DIAGRAM.txt](TESTING_FLOW_DIAGRAM.txt)** (Visual overview)
3. Run: `.\test-scripts\setup-test-env.ps1` (Setup environment)
4. Follow: **[TESTING_GUIDE.md](TESTING_GUIDE.md)** (Step-by-step)

### **Need Quick Commands?**
- See: **[QUICK_REFERENCE.md](QUICK_REFERENCE.md)** (Print this!)

### **Ready to Test?**
- Run: `.\test-scripts\test-business-rules.ps1` (Interactive testing)

---

## 📄 Documentation Files

| File | Purpose | When to Use |
|------|---------|-------------|
| **[TESTING_SUMMARY.md](TESTING_SUMMARY.md)** | Executive summary of all testing deliverables | First read, overview |
| **[TESTING_GUIDE.md](TESTING_GUIDE.md)** | Comprehensive 500+ line testing guide | Main reference, detailed procedures |
| **[QUICK_REFERENCE.md](QUICK_REFERENCE.md)** | Quick reference card with essential commands | Daily use, quick lookup |
| **[TESTING_FLOW_DIAGRAM.txt](TESTING_FLOW_DIAGRAM.txt)** | Visual diagrams of testing flow and app states | Understanding architecture |
| **[test-scripts/README.md](test-scripts/README.md)** | Documentation for all test scripts | Script usage reference |

---

## 🔧 Test Scripts

| Script | Purpose | Usage |
|--------|---------|-------|
| **[setup-test-env.ps1](test-scripts/setup-test-env.ps1)** | Environment setup & verification | Run once before testing |
| **[launch-app.ps1](test-scripts/launch-app.ps1)** | Launch app and verify it's running | Daily, before each test session |
| **[monitor-app.ps1](test-scripts/monitor-app.ps1)** | Real-time log monitoring | Run in separate window during testing |
| **[debug-black-screen.ps1](test-scripts/debug-black-screen.ps1)** | Black screen diagnostics + screenshots | When app shows black screen |
| **[check-network.ps1](test-scripts/check-network.ps1)** | Network connectivity verification | When polling doesn't work |
| **[test-business-rules.ps1](test-scripts/test-business-rules.ps1)** | Interactive business rules testing (7 tests) | Comprehensive testing session |

---

## 🗂️ Directory Structure

```
GameBillerTV/
│
├── 📚 Documentation
│   ├── TESTING_SUMMARY.md          ← Executive summary
│   ├── TESTING_GUIDE.md            ← Main comprehensive guide
│   ├── QUICK_REFERENCE.md          ← Quick reference card
│   ├── TESTING_FLOW_DIAGRAM.txt    ← Visual diagrams
│   └── INDEX.md                    ← This file
│
├── 🔧 Test Scripts
│   └── test-scripts/
│       ├── README.md               ← Scripts documentation
│       ├── setup-test-env.ps1      ← Environment setup
│       ├── launch-app.ps1          ← Launch app
│       ├── monitor-app.ps1         ← Real-time logs
│       ├── debug-black-screen.ps1  ← Black screen diagnostics
│       ├── check-network.ps1       ← Network verification
│       └── test-business-rules.ps1 ← Business rules testing
│
├── 📸 Screenshots
│   └── screenshots/                ← Auto-generated screenshots
│
└── 📱 App Source
    └── app/
        ├── src/main/
        │   ├── AndroidManifest.xml
        │   └── kotlin/com/gamebiller/tvlock/
        └── build.gradle.kts
```

---

## 🚀 Quick Start Workflows

### **First-Time Setup (15 minutes)**

```powershell
# 1. Navigate to project
cd "c:\Dev\GameBiller Smart Lock\Version 1\GameBillerTV"

# 2. Read summary
# Open TESTING_SUMMARY.md

# 3. Setup environment
.\test-scripts\setup-test-env.ps1

# 4. Build app
.\gradlew.bat assembleDebug

# 5. Install app
adb install -r app\build\outputs\apk\debug\app-debug.apk

# 6. Launch app
.\test-scripts\launch-app.ps1
```

---

### **Daily Testing Workflow (5 minutes)**

```powershell
# 1. Start emulator (in Android Studio)
# 2. Start backend (localhost:3002)

# 3. Launch app
.\test-scripts\launch-app.ps1

# 4. Monitor logs (separate window)
.\test-scripts\monitor-app.ps1

# 5. Test manually or run business rules
.\test-scripts\test-business-rules.ps1
```

---

### **Debugging Black Screen (2 minutes)**

```powershell
# 1. Run diagnostic script
.\test-scripts\debug-black-screen.ps1

# 2. Check screenshot in .\screenshots\

# 3. Review logs in output

# 4. If needed, monitor in real-time
.\test-scripts\monitor-app.ps1
```

---

### **Network Connectivity Issues (2 minutes)**

```powershell
# 1. Run network check
.\test-scripts\check-network.ps1

# 2. Verify backend is running
Invoke-WebRequest -Uri "http://localhost:3002/health"

# 3. Test from emulator
adb shell "curl http://10.0.2.2:3002/health"
```

---

### **Comprehensive Testing Session (20 minutes)**

```powershell
# 1. Setup (if first time)
.\test-scripts\setup-test-env.ps1

# 2. Launch app
.\test-scripts\launch-app.ps1

# 3. Start monitoring (separate window)
.\test-scripts\monitor-app.ps1

# 4. Run all business rules tests
.\test-scripts\test-business-rules.ps1

# 5. Review checklists in TESTING_GUIDE.md
```

---

## 📋 Testing Checklists

All checklists are in **[TESTING_GUIDE.md](TESTING_GUIDE.md)**, Section 6:

1. **Emulator Checklist** (6 items)
   - Device connection, network, UI rendering, etc.

2. **App Checklist** (11 items)
   - Build, installation, permissions, configuration, etc.

3. **Backend Checklist** (8 items)
   - Server running, endpoints, status values, etc.

4. **Go-Live Confidence Checklist** (30 items)
   - Functionality, UI/UX, Stability, Security, Performance

---

## 🎯 Testing Scenarios

All scenarios detailed in **[TESTING_GUIDE.md](TESTING_GUIDE.md)**, Section 5:

| Test | Scenario | Expected Behavior |
|------|----------|-------------------|
| **1** | App starts unpaired | Shows Pairing Screen |
| **2** | Device pairing | Successful pairing → Lock Screen |
| **3** | Backend STOPPED | App locks (SESSION_STOPPED) |
| **4** | Backend RUNNING | App unlocks (HDMI visible) |
| **5** | Network failure | Grace period → Lock |
| **6** | Network recovery | Grace aborted → Unlock |
| **7** | Token invalid | Immediate lock (no grace) |

---

## 🔍 Key Configuration

### Backend URL
- **Debug:** `http://10.0.2.2:3002/`
- **Configured in:** `app/build.gradle.kts` (line 38)

### Polling Interval
- **Interval:** 12 seconds
- **Configured in:** `app/src/main/kotlin/com/gamebiller/tvlock/ui/viewmodel/LockViewModel.kt` (line 44)

### Grace Period
- **Duration:** 30 seconds
- **Trigger:** 3 consecutive network failures (~36 seconds)
- **Configured in:** `LockViewModel.kt` (line 45)

---

## 🛠️ Essential ADB Commands

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
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png

# Test network
adb shell "curl http://10.0.2.2:3002/health"
```

Full command reference in **[QUICK_REFERENCE.md](QUICK_REFERENCE.md)**

---

## 📞 Troubleshooting

### Common Issues

| Issue | Solution |
|-------|----------|
| **ADB not recognized** | Add to PATH: `$env:Path += ";C:\Users\<User>\AppData\Local\Android\Sdk\platform-tools"` |
| **No devices found** | Start Android TV emulator in Android Studio |
| **Black screen** | Run `.\test-scripts\debug-black-screen.ps1` |
| **Network errors** | Run `.\test-scripts\check-network.ps1` |
| **Polling not working** | Check pairing: `adb logcat -d \| Select-String "paired"` |

Full troubleshooting guide in **[TESTING_GUIDE.md](TESTING_GUIDE.md)**, Section "Troubleshooting"

---

## 📊 Testing Metrics

### Documentation
- **Total lines:** ~2,500+
- **Files:** 9
- **Sections:** 30+

### Automation
- **Scripts:** 6
- **Total script lines:** ~800+
- **Automation coverage:** ~90%

### Test Coverage
- **Business rules:** 7/7 (100%)
- **Checklists:** 4 (55 total items)
- **Scenarios:** 7 comprehensive tests

---

## 🎓 Learning Path

### Beginner (Never tested Android TV apps)
1. Read **TESTING_SUMMARY.md** (overview)
2. Review **TESTING_FLOW_DIAGRAM.txt** (visual understanding)
3. Run `setup-test-env.ps1` (hands-on setup)
4. Follow **TESTING_GUIDE.md** Section 2 (force launch)
5. Use **QUICK_REFERENCE.md** as needed

### Intermediate (Familiar with ADB)
1. Skim **TESTING_SUMMARY.md** (quick overview)
2. Run `launch-app.ps1` (get started)
3. Run `test-business-rules.ps1` (comprehensive testing)
4. Use **QUICK_REFERENCE.md** for commands

### Advanced (Ready for production)
1. Review **Go-Live Confidence Checklist** in TESTING_GUIDE.md
2. Run all test scripts
3. Document results using template in TESTING_GUIDE.md
4. Perform load testing and security audit

---

## 📚 Additional Resources

### Android TV Documentation
- [Android TV Developer Guide](https://developer.android.com/training/tv)
- [Jetpack Compose for TV](https://developer.android.com/jetpack/compose/tv)
- [Leanback Library](https://developer.android.com/training/tv/start/start)

### ADB Documentation
- [ADB Command Reference](https://developer.android.com/studio/command-line/adb)
- [Logcat Documentation](https://developer.android.com/studio/command-line/logcat)

### Project-Specific
- **Main Activity:** `app/src/main/kotlin/com/gamebiller/tvlock/ui/MainActivity.kt`
- **Business Logic:** `app/src/main/kotlin/com/gamebiller/tvlock/ui/viewmodel/LockViewModel.kt`
- **Backend API:** `app/src/main/kotlin/com/gamebiller/tvlock/domain/LockRepository.kt`

---

## ✅ Success Criteria

Your app is **PRODUCTION READY** when all items in the **Go-Live Confidence Checklist** pass:

- ✅ All 7 business rule tests pass
- ✅ No black screen issues
- ✅ Network connectivity reliable
- ✅ Polling works consistently
- ✅ Grace period behaves correctly
- ✅ UI responsive and clear
- ✅ Auto-launch on boot works
- ✅ No crashes or errors

Full checklist in **[TESTING_GUIDE.md](TESTING_GUIDE.md)**, Section 6

---

## 🎉 Next Steps

1. **Start with setup:**
   ```powershell
   .\test-scripts\setup-test-env.ps1
   ```

2. **Launch and test:**
   ```powershell
   .\test-scripts\launch-app.ps1
   .\test-scripts\test-business-rules.ps1
   ```

3. **Review results** using template in TESTING_GUIDE.md

4. **Prepare for production** using Go-Live checklist

---

## 📞 Support

If you need help:

1. Check **[TESTING_GUIDE.md](TESTING_GUIDE.md)** Troubleshooting section
2. Run diagnostic scripts (`debug-black-screen.ps1`, `check-network.ps1`)
3. Review logs with `monitor-app.ps1`
4. Take screenshots with `debug-black-screen.ps1`

---

**Happy Testing! 🚀**

---

*Last updated: 2026-01-19*  
*Version: 1.0*
