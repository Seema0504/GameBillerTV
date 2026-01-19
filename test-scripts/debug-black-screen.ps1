# GameBillerTV Black Screen Debugger
# Usage: .\debug-black-screen.ps1

Write-Host "=== Black Screen Diagnostic Tool ===" -ForegroundColor Cyan
Write-Host ""

# Test 1: Check if Activity is created
Write-Host "1. Checking if MainActivity was created..." -ForegroundColor Yellow
adb logcat -d -s MainActivity:D | Select-String "created" | Select-Object -Last 5
Write-Host ""

# Test 2: Check for Compose rendering
Write-Host "2. Checking Compose UI rendering..." -ForegroundColor Yellow
adb logcat -d | Select-String "Compose|setContent" | Select-Object -Last 10
Write-Host ""

# Test 3: Check for crashes
Write-Host "3. Checking for crashes/exceptions..." -ForegroundColor Yellow
$crashes = adb logcat -d -s AndroidRuntime:E | Select-Object -Last 20
if ($crashes) {
    Write-Host $crashes -ForegroundColor Red
} else {
    Write-Host "   ✓ No crashes detected" -ForegroundColor Green
}
Write-Host ""

# Test 4: Check window focus
Write-Host "4. Checking window focus..." -ForegroundColor Yellow
adb shell dumpsys window windows | Select-String "mCurrentFocus"
Write-Host ""

# Test 5: Check if app is in foreground
Write-Host "5. Checking if app is in foreground..." -ForegroundColor Yellow
adb shell dumpsys activity activities | Select-String "ResumedActivity"
Write-Host ""

# Test 6: Check LockViewModel state
Write-Host "6. Checking LockViewModel state..." -ForegroundColor Yellow
adb logcat -d -s LockViewModel:D | Select-Object -Last 10
Write-Host ""

# Test 7: Check if device is paired
Write-Host "7. Checking pairing status..." -ForegroundColor Yellow
adb logcat -d | Select-String "paired|Unpaired" | Select-Object -Last 5
Write-Host ""

# Test 8: Take screenshot
Write-Host "8. Taking screenshot..." -ForegroundColor Yellow
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$screenshotPath = "/sdcard/screenshot_$timestamp.png"
adb shell screencap -p $screenshotPath
adb pull $screenshotPath ".\screenshots\screenshot_$timestamp.png"
Write-Host "   Screenshot saved to: .\screenshots\screenshot_$timestamp.png" -ForegroundColor Green
Write-Host ""

# Test 9: Check GPU rendering
Write-Host "9. Checking GPU rendering profile..." -ForegroundColor Yellow
adb shell dumpsys gfxinfo com.gamebiller.tvlock | Select-String "Total frames rendered" -Context 0,3
Write-Host ""

Write-Host "=== Diagnostic Complete ===" -ForegroundColor Green
Write-Host ""
Write-Host "NEXT STEPS:" -ForegroundColor Yellow
Write-Host "1. Review the screenshot in .\screenshots\" -ForegroundColor White
Write-Host "2. If black screen persists, run .\monitor-app.ps1 and launch app again" -ForegroundColor White
Write-Host "3. Check for Compose rendering errors in logs" -ForegroundColor White
