# GameBillerTV - Pairing Test Script
# Usage: .\pair-device.ps1 -Code "YOUR_PAIRING_CODE"

param(
    [Parameter(Mandatory=$true)]
    [string]$Code
)

$env:Path += ";C:\Users\racin\AppData\Local\Android\Sdk\platform-tools"

Write-Host "=== GameBillerTV Device Pairing ===" -ForegroundColor Cyan
Write-Host ""

# Step 1: Focus on input field
Write-Host "1. Focusing on Station Code input field..." -ForegroundColor Yellow
adb shell input keyevent KEYCODE_DPAD_DOWN
Start-Sleep -Milliseconds 500
adb shell input keyevent KEYCODE_DPAD_CENTER
Start-Sleep -Milliseconds 500

# Step 2: Enter code
Write-Host "2. Entering pairing code: $Code" -ForegroundColor Yellow
adb shell input text "$Code"
Start-Sleep -Milliseconds 500

# Step 3: Navigate to button
Write-Host "3. Navigating to Pair Device button..." -ForegroundColor Yellow
adb shell input keyevent KEYCODE_DPAD_DOWN
Start-Sleep -Milliseconds 500

# Step 4: Click button
Write-Host "4. Clicking Pair Device button..." -ForegroundColor Green
adb shell input keyevent KEYCODE_DPAD_CENTER

Write-Host ""
Write-Host "=== Pairing Request Sent ===" -ForegroundColor Green
Write-Host ""
Write-Host "Monitoring logs for response..." -ForegroundColor Yellow
Write-Host ""

# Monitor logs for 10 seconds
Start-Sleep -Seconds 2
adb logcat -d | Select-String "Pairing|paired|error" -CaseInsensitive | Select-Object -Last 10

Write-Host ""
Write-Host "Check the emulator screen for:" -ForegroundColor Cyan
Write-Host "  Success message (green)" -ForegroundColor Green
Write-Host "  Error message (red)" -ForegroundColor Red
Write-Host ""
