# GameBillerTV - Business Rules Test Guide
# Test all business logic scenarios step-by-step

Write-Host "=== GameBillerTV Business Rules Test ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "This script will guide you through testing all business rules." -ForegroundColor Gray
Write-Host "You'll need to control the backend responses to simulate different scenarios." -ForegroundColor Gray
Write-Host ""

# Function to wait for user confirmation
function Wait-UserConfirmation {
    param([string]$message)
    Write-Host ""
    Write-Host $message -ForegroundColor Yellow
    Read-Host "Press Enter to continue"
}

# Function to show current app state
function Show-AppState {
    Write-Host ""
    Write-Host "--- Current App State ---" -ForegroundColor Cyan
    adb logcat -d -s LockViewModel:D | Select-Object -Last 3
    Write-Host "-------------------------" -ForegroundColor Cyan
    Write-Host ""
}

Write-Host "PREREQUISITES:" -ForegroundColor Yellow
Write-Host "1. Backend server running on localhost:3002" -ForegroundColor White
Write-Host "2. Android TV emulator running" -ForegroundColor White
Write-Host "3. App installed and ready to launch" -ForegroundColor White
Write-Host ""

Wait-UserConfirmation "Ensure prerequisites are met, then press Enter to start"

# ============================================
# TEST 1: Initial LOCKED State (Unpaired)
# ============================================
Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "TEST 1: App Starts in UNPAIRED State" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Expected Behavior:" -ForegroundColor Yellow
Write-Host "- App should show Pairing Screen" -ForegroundColor White
Write-Host "- User should see pairing code input" -ForegroundColor White
Write-Host ""

# Clear app data to ensure unpaired state
Write-Host "Clearing app data to simulate first launch..." -ForegroundColor Gray
adb shell pm clear com.gamebiller.tvlock
Start-Sleep -Seconds 1

# Launch app
Write-Host "Launching app..." -ForegroundColor Gray
adb shell am start -n com.gamebiller.tvlock/.ui.MainActivity
Start-Sleep -Seconds 3

# Show logs
Write-Host ""
Write-Host "Checking logs for 'Unpaired' state..." -ForegroundColor Gray
adb logcat -d | Select-String "Unpaired|PairingScreen" | Select-Object -Last 5

Wait-UserConfirmation "Verify Pairing Screen is visible on emulator, then press Enter"

# ============================================
# TEST 2: Device Pairing
# ============================================
Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "TEST 2: Device Pairing" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "MANUAL STEP REQUIRED:" -ForegroundColor Yellow
Write-Host "1. On your backend, generate a pairing code for a test station" -ForegroundColor White
Write-Host "2. Enter the pairing code in the app using D-pad" -ForegroundColor White
Write-Host "3. Observe the pairing process" -ForegroundColor White
Write-Host ""
Write-Host "Expected Behavior:" -ForegroundColor Yellow
Write-Host "- App sends pairing request to backend" -ForegroundColor White
Write-Host "- Backend validates code and returns device info" -ForegroundColor White
Write-Host "- App saves device info and transitions to Lock Screen" -ForegroundColor White
Write-Host ""

Wait-UserConfirmation "Complete pairing on the emulator, then press Enter"

# Verify pairing
Write-Host ""
Write-Host "Verifying pairing..." -ForegroundColor Gray
adb logcat -d | Select-String "Pairing successful|paired" | Select-Object -Last 5

# ============================================
# TEST 3: Backend Returns STOPPED → Locked
# ============================================
Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "TEST 3: Backend Returns STOPPED" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "BACKEND CONFIGURATION:" -ForegroundColor Yellow
Write-Host "Ensure your backend returns status: 'STOPPED' for this station" -ForegroundColor White
Write-Host ""
Write-Host "Expected Behavior:" -ForegroundColor Yellow
Write-Host "- App polls backend every 12 seconds" -ForegroundColor White
Write-Host "- Backend returns STOPPED status" -ForegroundColor White
Write-Host "- App shows LOCKED screen with reason: SESSION_STOPPED" -ForegroundColor White
Write-Host ""

Wait-UserConfirmation "Configure backend to return STOPPED, then press Enter"

# Monitor polling
Write-Host ""
Write-Host "Monitoring next 3 polling cycles (36 seconds)..." -ForegroundColor Gray
Write-Host "Watch for 'SESSION_STOPPED' in logs..." -ForegroundColor Gray
Write-Host ""

# Clear logcat and monitor
adb logcat -c
Start-Sleep -Seconds 40

adb logcat -d | Select-String "pollStationStatus|SESSION_STOPPED|Locked" | Select-Object -Last 10

Wait-UserConfirmation "Verify LOCKED screen is showing on emulator, then press Enter"

# ============================================
# TEST 4: Backend Returns RUNNING → Unlocked
# ============================================
Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "TEST 4: Backend Returns RUNNING" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "BACKEND CONFIGURATION:" -ForegroundColor Yellow
Write-Host "Change backend to return status: 'RUNNING' for this station" -ForegroundColor White
Write-Host ""
Write-Host "Expected Behavior:" -ForegroundColor Yellow
Write-Host "- App polls backend and receives RUNNING status" -ForegroundColor White
Write-Host "- App transitions to UNLOCKED state" -ForegroundColor White
Write-Host "- Screen shows minimal/no UI (HDMI input visible)" -ForegroundColor White
Write-Host "- Logs show 'TV unlocked'" -ForegroundColor White
Write-Host ""

Wait-UserConfirmation "Configure backend to return RUNNING, then press Enter"

# Monitor polling
Write-Host ""
Write-Host "Monitoring next polling cycle (15 seconds)..." -ForegroundColor Gray
adb logcat -c
Start-Sleep -Seconds 15

adb logcat -d | Select-String "RUNNING|Unlocked|TV unlocked" | Select-Object -Last 10

Wait-UserConfirmation "Verify screen is unlocked (minimal UI), then press Enter"

# ============================================
# TEST 5: Network Failure → Grace Period
# ============================================
Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "TEST 5: Network Failure → Grace Period" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "BACKEND CONFIGURATION:" -ForegroundColor Yellow
Write-Host "STOP your backend server to simulate network failure" -ForegroundColor White
Write-Host ""
Write-Host "Expected Behavior:" -ForegroundColor Yellow
Write-Host "- App fails to poll backend (3 consecutive failures)" -ForegroundColor White
Write-Host "- Grace period starts (30 seconds countdown)" -ForegroundColor White
Write-Host "- Screen shows 'Connection Lost' with countdown" -ForegroundColor White
Write-Host "- After 30 seconds, screen locks with reason: NETWORK_FAILURE" -ForegroundColor White
Write-Host ""

Wait-UserConfirmation "STOP the backend server, then press Enter"

# Monitor grace period
Write-Host ""
Write-Host "Monitoring grace period (will take ~60 seconds)..." -ForegroundColor Gray
Write-Host "Waiting for 3 consecutive failures (36 seconds)..." -ForegroundColor Gray
adb logcat -c
Start-Sleep -Seconds 40

Write-Host ""
Write-Host "Checking for grace period start..." -ForegroundColor Gray
adb logcat -d | Select-String "grace|Grace|GRACE" | Select-Object -Last 5

Write-Host ""
Write-Host "Waiting for grace period to expire (30 seconds)..." -ForegroundColor Gray
Start-Sleep -Seconds 35

adb logcat -d | Select-String "Grace period expired|NETWORK_FAILURE|Locked" | Select-Object -Last 5

Wait-UserConfirmation "Verify screen shows LOCKED with network failure message, then press Enter"

# ============================================
# TEST 6: Network Recovery During Grace Period
# ============================================
Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "TEST 6: Network Recovery During Grace" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "BACKEND CONFIGURATION:" -ForegroundColor Yellow
Write-Host "1. Keep backend STOPPED initially" -ForegroundColor White
Write-Host "2. Wait for grace period to start" -ForegroundColor White
Write-Host "3. START backend (returning RUNNING) during grace period" -ForegroundColor White
Write-Host ""
Write-Host "Expected Behavior:" -ForegroundColor Yellow
Write-Host "- Grace period starts" -ForegroundColor White
Write-Host "- Backend comes back online" -ForegroundColor White
Write-Host "- Grace period is aborted" -ForegroundColor White
Write-Host "- App returns to UNLOCKED state" -ForegroundColor White
Write-Host ""

# Restart app to reset state
Write-Host "Restarting app..." -ForegroundColor Gray
adb shell am force-stop com.gamebiller.tvlock
Start-Sleep -Seconds 2
adb shell am start -n com.gamebiller.tvlock/.ui.MainActivity
Start-Sleep -Seconds 5

Wait-UserConfirmation "Ensure backend is STOPPED, then press Enter to trigger grace period"

Write-Host ""
Write-Host "Waiting for grace period to start (40 seconds)..." -ForegroundColor Gray
adb logcat -c
Start-Sleep -Seconds 40

Write-Host ""
Write-Host "Grace period should be active now." -ForegroundColor Yellow
Write-Host "START YOUR BACKEND NOW (returning RUNNING status)" -ForegroundColor Yellow
Write-Host ""

Wait-UserConfirmation "Start backend, wait 15 seconds, then press Enter"

adb logcat -d | Select-String "Grace period aborted|recovered|Unlocked" | Select-Object -Last 10

Wait-UserConfirmation "Verify app returned to UNLOCKED state, then press Enter"

# ============================================
# TEST 7: Token Invalid → Immediate Lock
# ============================================
Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "TEST 7: Token Invalid → Immediate Lock" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "BACKEND CONFIGURATION:" -ForegroundColor Yellow
Write-Host "Configure backend to return status: 'TOKEN_INVALID' (401 or specific error)" -ForegroundColor White
Write-Host ""
Write-Host "Expected Behavior:" -ForegroundColor Yellow
Write-Host "- App receives TOKEN_INVALID status" -ForegroundColor White
Write-Host "- App IMMEDIATELY locks (no grace period)" -ForegroundColor White
Write-Host "- Screen shows LOCKED with reason: TOKEN_INVALID" -ForegroundColor White
Write-Host ""

Wait-UserConfirmation "Configure backend to return TOKEN_INVALID, then press Enter"

Write-Host ""
Write-Host "Monitoring next polling cycle (15 seconds)..." -ForegroundColor Gray
adb logcat -c
Start-Sleep -Seconds 15

adb logcat -d | Select-String "TOKEN_INVALID|Locked" | Select-Object -Last 10

Wait-UserConfirmation "Verify screen shows LOCKED with token invalid message, then press Enter"

# ============================================
# TEST COMPLETE
# ============================================
Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "ALL BUSINESS RULES TESTS COMPLETE!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "SUMMARY OF TESTS:" -ForegroundColor Cyan
Write-Host "✓ TEST 1: App starts in UNPAIRED state" -ForegroundColor Green
Write-Host "✓ TEST 2: Device pairing successful" -ForegroundColor Green
Write-Host "✓ TEST 3: Backend STOPPED → Locked" -ForegroundColor Green
Write-Host "✓ TEST 4: Backend RUNNING → Unlocked" -ForegroundColor Green
Write-Host "✓ TEST 5: Network failure → Grace period → Lock" -ForegroundColor Green
Write-Host "✓ TEST 6: Network recovery during grace period" -ForegroundColor Green
Write-Host "✓ TEST 7: Token invalid → Immediate lock" -ForegroundColor Green
Write-Host ""
Write-Host "Next: Review the final checklists in TESTING_GUIDE.md" -ForegroundColor Yellow
