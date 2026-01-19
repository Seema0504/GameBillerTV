# API Model Adjustments (Critical Fixes)

## Overview
During integration testing (Jan 2026), we discovered mismatches between the Android App's expected JSON structure and the actual Backend API response.

To prevent crashes and "JSON Parse Errors", we relaxed the strictness of the Android Data Models.

## Changes Made

### 1. `PairDeviceResponse` (ApiModels.kt)
The backend does NOT return `shop_id`, `device_id`, or `shop_name` during the pairing handshake.
- **Change:** Fields made **nullable** (`Int?`, `String?`).
- **Logic:** `LockRepository.kt` handles nulls by using safe defaults (e.g., Shop ID = 0, Default Name = "Game Shop").

### 2. `StationStatusResponse` (ApiModels.kt)
The backend `GET /status` endpoint only returns `{ "status": "..." }`. It omits `station_id` and names.
- **Change:** `station_id`, `shop_name`, `station_name` made **nullable**.
- **Logic:** `StationStatus` enum mapping relies ONLY on the `status` string, so missing IDs are ignored safely.

## Recommendation for Future
If possible, update the Backend API to return the full objects. Once updated, you can revert `ApiModels.kt` to non-nullable types for stricter type safety.
