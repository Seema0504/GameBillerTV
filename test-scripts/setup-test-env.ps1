# GameBillerTV - Setup Test Environment
# Run this script once before starting tests

Write-Host "=== GameBillerTV Test Environment Setup ===" -ForegroundColor Cyan
Write-Host ""

# 1. Create screenshots directory
Write-Host "1. Creating screenshots directory..." -ForegroundColor Yellow
$screenshotsDir = Join-Path $PSScriptRoot "..\screenshots"
if (!(Test-Path $screenshotsDir)) {
    New-Item -ItemType Directory -Path $screenshotsDir -Force | Out-Null
    Write-Host "   ✓ Created: $screenshotsDir" -ForegroundColor Green
} else {
    Write-Host "   ✓ Already exists: $screenshotsDir" -ForegroundColor Green
}
Write-Host ""

# 2. Check ADB availability
Write-Host "2. Checking ADB availability..." -ForegroundColor Yellow
try {
    $adbVersion = adb version 2>&1
    Write-Host "   ✓ ADB is available" -ForegroundColor Green
    Write-Host "   Version: $($adbVersion[0])" -ForegroundColor Gray
} catch {
    Write-Host "   ✗ ADB not found in PATH" -ForegroundColor Red
    Write-Host ""
    Write-Host "   To fix this, run:" -ForegroundColor Yellow
    Write-Host "   `$env:Path += `";C:\Users\<YourUsername>\AppData\Local\Android\Sdk\platform-tools`"" -ForegroundColor White
    Write-Host ""
    Write-Host "   Find your SDK path in Android Studio:" -ForegroundColor Gray
    Write-Host "   File -> Settings -> System Settings -> Android SDK" -ForegroundColor Gray
}
Write-Host ""

# 3. Check if emulator is running
Write-Host "3. Checking for connected devices..." -ForegroundColor Yellow
try {
    $devices = adb devices 2>&1 | Select-String "device$"
    if ($devices) {
        Write-Host "   ✓ Device(s) connected:" -ForegroundColor Green
        adb devices | Select-String "device$" | ForEach-Object { Write-Host "     $_" -ForegroundColor Gray }
    } else {
        Write-Host "   ⚠ No devices connected" -ForegroundColor Yellow
        Write-Host "   Start your Android TV emulator in Android Studio" -ForegroundColor Gray
    }
} catch {
    Write-Host "   ✗ Could not check devices (ADB not available)" -ForegroundColor Red
}
Write-Host ""

# 4. Check if backend is running
Write-Host "4. Checking backend server..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:3002/health" -TimeoutSec 3 -ErrorAction Stop
    Write-Host "   ✓ Backend server is running on localhost:3002" -ForegroundColor Green
    Write-Host "   Status: $($response.StatusCode)" -ForegroundColor Gray
} catch {
    Write-Host "   ✗ Backend server is NOT running on localhost:3002" -ForegroundColor Red
    Write-Host "   Start your backend server before testing" -ForegroundColor Gray
}
Write-Host ""

# 5. Check if app is built
Write-Host "5. Checking if app is built..." -ForegroundColor Yellow
$apkPath = Join-Path $PSScriptRoot "..\app\build\outputs\apk\debug\app-debug.apk"
if (Test-Path $apkPath) {
    $apkInfo = Get-Item $apkPath
    Write-Host "   ✓ APK found: app-debug.apk" -ForegroundColor Green
    Write-Host "   Size: $([math]::Round($apkInfo.Length / 1MB, 2)) MB" -ForegroundColor Gray
    Write-Host "   Modified: $($apkInfo.LastWriteTime)" -ForegroundColor Gray
} else {
    Write-Host "   ⚠ APK not found" -ForegroundColor Yellow
    Write-Host "   Run: .\gradlew.bat assembleDebug" -ForegroundColor Gray
}
Write-Host ""

# 6. Summary
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Setup Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "NEXT STEPS:" -ForegroundColor Yellow
Write-Host "1. Ensure Android TV emulator is running" -ForegroundColor White
Write-Host "2. Ensure backend server is running on localhost:3002" -ForegroundColor White
Write-Host "3. Build the app if needed: .\gradlew.bat assembleDebug" -ForegroundColor White
Write-Host "4. Install the app: adb install -r app\build\outputs\apk\debug\app-debug.apk" -ForegroundColor White
Write-Host "5. Run test scripts from .\test-scripts\" -ForegroundColor White
Write-Host ""
Write-Host "Quick Start:" -ForegroundColor Cyan
Write-Host "  cd test-scripts" -ForegroundColor White
Write-Host "  .\launch-app.ps1" -ForegroundColor White
Write-Host ""
Write-Host "Full Guide: See TESTING_GUIDE.md" -ForegroundColor Gray
Write-Host ""
