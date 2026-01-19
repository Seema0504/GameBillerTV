# GameBillerTV App Monitoring Script
# Usage: .\monitor-app.ps1

Write-Host "=== GameBillerTV App Monitor ===" -ForegroundColor Cyan
Write-Host ""

# Check if device is connected
Write-Host "Checking connected devices..." -ForegroundColor Yellow
adb devices

Write-Host ""
Write-Host "Clearing logcat buffer..." -ForegroundColor Yellow
adb logcat -c

Write-Host ""
Write-Host "Starting filtered logcat (Ctrl+C to stop)..." -ForegroundColor Green
Write-Host "Filtering for: MainActivity, LockViewModel, Compose, and Errors" -ForegroundColor Gray
Write-Host ""

# Monitor logcat with filters
adb logcat -v time `
    MainActivity:D `
    LockViewModel:D `
    PairingViewModel:D `
    LockRepository:D `
    StatusPollingService:D `
    Compose:D `
    AndroidRuntime:E `
    *:S
