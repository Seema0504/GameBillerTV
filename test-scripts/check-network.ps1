# GameBillerTV Network Connectivity Test
# Usage: .\check-network.ps1

Write-Host "=== Network Connectivity Test ===" -ForegroundColor Cyan
Write-Host ""

# Test 1: Check if emulator can reach localhost
Write-Host "1. Testing connectivity to 10.0.2.2:3002 (host machine)..." -ForegroundColor Yellow
Write-Host "   Note: 10.0.2.2 is the emulator's alias for host localhost" -ForegroundColor Gray
adb shell "curl -v http://10.0.2.2:3002/health 2>&1 || echo 'Connection failed'"
Write-Host ""

# Test 2: Check network interfaces
Write-Host "2. Checking emulator network interfaces..." -ForegroundColor Yellow
adb shell "ip addr show"
Write-Host ""

# Test 3: Check if backend server is running on host
Write-Host "3. Checking if backend server is running on host machine..." -ForegroundColor Yellow
Write-Host "   Testing localhost:3002..." -ForegroundColor Gray
try {
    $response = Invoke-WebRequest -Uri "http://localhost:3002/health" -TimeoutSec 5 -ErrorAction Stop
    Write-Host "   ✓ Backend server is running!" -ForegroundColor Green
    Write-Host "   Status: $($response.StatusCode)" -ForegroundColor Green
} catch {
    Write-Host "   ✗ Backend server is NOT running on localhost:3002" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# Test 4: Check app's network permissions
Write-Host "4. Verifying app has INTERNET permission..." -ForegroundColor Yellow
$permissions = adb shell dumpsys package com.gamebiller.tvlock | Select-String "android.permission.INTERNET"
if ($permissions) {
    Write-Host "   ✓ INTERNET permission granted" -ForegroundColor Green
} else {
    Write-Host "   ✗ INTERNET permission NOT found" -ForegroundColor Red
}
Write-Host ""

# Test 5: Show current BASE_URL from BuildConfig
Write-Host "5. Current API_BASE_URL configuration:" -ForegroundColor Yellow
Write-Host "   Debug build: http://10.0.2.2:3002/" -ForegroundColor Cyan
Write-Host "   (Configured in app/build.gradle.kts)" -ForegroundColor Gray
Write-Host ""

Write-Host "=== Network Test Complete ===" -ForegroundColor Green
