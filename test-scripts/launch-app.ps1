# GameBillerTV App Launcher Script
# Usage: .\launch-app.ps1

Write-Host "=== GameBillerTV App Launcher ===" -ForegroundColor Cyan
Write-Host ""

# Check device connection
Write-Host "1. Checking connected devices..." -ForegroundColor Yellow
$devices = adb devices
Write-Host $devices
Write-Host ""

# Force stop existing instance
Write-Host "2. Stopping any existing app instance..." -ForegroundColor Yellow
adb shell am force-stop com.gamebiller.tvlock
Start-Sleep -Seconds 1

# Clear app data (optional - uncomment if needed)
# Write-Host "3. Clearing app data..." -ForegroundColor Yellow
# adb shell pm clear com.gamebiller.tvlock

# Launch MainActivity
Write-Host "3. Launching MainActivity..." -ForegroundColor Green
adb shell am start -n com.gamebiller.tvlock/.ui.MainActivity
Start-Sleep -Seconds 2

# Verify app is running
Write-Host ""
Write-Host "4. Verifying app is running..." -ForegroundColor Yellow
$focus = adb shell dumpsys window windows | Select-String "mCurrentFocus"
Write-Host $focus

Write-Host ""
Write-Host "5. Checking Activity stack..." -ForegroundColor Yellow
$activities = adb shell dumpsys activity activities | Select-String "gamebiller"
Write-Host $activities

Write-Host ""
Write-Host "=== Launch Complete ===" -ForegroundColor Green
Write-Host "Run .\monitor-app.ps1 to view logs" -ForegroundColor Gray
