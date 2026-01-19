# GameBiller TV Lock - Backend Integration Guide

This document outlines the backend changes needed to support the Android TV lock application.

## Required API Endpoints

### 1. Device Pairing Endpoint

**Endpoint**: `POST /api/tv-devices/pair`

**Purpose**: Pair an Android TV device with a specific station using a station code.

**Request Body**:
```json
{
  "station_code": "ABC123",
  "device_id": "TV-A1B2C3D4"
}
```

**Response** (Success - 200):
```json
{
  "shop_id": 1,
  "station_id": 3,
  "device_id": "TV-A1B2C3D4",
  "shop_name": "ABC Gaming Zone",
  "station_name": "PS-1"
}
```

**Response** (Error - 400/404):
```json
{
  "error": "Invalid station code",
  "message": "Station code not found or already paired"
}
```

**Implementation Notes**:
- Generate unique station codes for each station (e.g., 6-character alphanumeric)
- Store station code in `stations` table or create new `station_codes` table
- Validate that station code is active and not already paired
- Create record in `tv_devices` table
- Return shop and station details for display on TV

---

### 2. Station Status Endpoint

**Endpoint**: `GET /api/stations/{station_id}/status`

**Purpose**: Get current status of a station to determine if TV should be locked or unlocked.

**Response** (Success - 200):
```json
{
  "station_id": 3,
  "status": "RUNNING",
  "shop_name": "ABC Gaming Zone",
  "station_name": "PS-1"
}
```

**Status Values**:
- `"RUNNING"` - Station is actively running (TV unlocked)
- `"STOPPED"` - Station is stopped (TV locked)
- `"PAUSED"` - Station is paused (TV locked)
- `"NOT_STARTED"` - Station has not started (TV locked)

**Implementation Notes**:
- Map existing `stations` table fields to status:
  - `is_running = true` → `"RUNNING"`
  - `is_paused = true` → `"PAUSED"`
  - `is_done = true` → `"STOPPED"`
  - Otherwise → `"NOT_STARTED"`
- This endpoint should be lightweight (no auth required for MVP)
- Consider caching to reduce database load

**Example Implementation** (Node.js/Express):
```javascript
// In api/stations.js
app.get('/api/stations/:station_id/status', async (req, res) => {
  const { station_id } = req.params;
  
  const result = await db.query(
    `SELECT s.id, s.name as station_name, s.is_running, s.is_paused, s.is_done,
            sh.name as shop_name
     FROM stations s
     JOIN shops sh ON s.shop_id = sh.id
     WHERE s.id = $1`,
    [station_id]
  );
  
  if (result.rows.length === 0) {
    return res.status(404).json({ error: 'Station not found' });
  }
  
  const station = result.rows[0];
  let status = 'NOT_STARTED';
  
  if (station.is_running) {
    status = 'RUNNING';
  } else if (station.is_paused) {
    status = 'PAUSED';
  } else if (station.is_done) {
    status = 'STOPPED';
  }
  
  res.json({
    station_id: station.id,
    status: status,
    shop_name: station.shop_name,
    station_name: station.station_name
  });
});
```

---

### 3. Audit Event Endpoint

**Endpoint**: `POST /api/tv-devices/audit`

**Purpose**: Log audit events from TV devices for monitoring and compliance.

**Request Body**:
```json
{
  "event": "TV_UNLOCKED",
  "station_id": 3,
  "device_id": "TV-A1B2C3D4",
  "timestamp": "2026-01-19T04:50:00Z",
  "metadata": {
    "shop_name": "ABC Gaming Zone",
    "station_name": "PS-1"
  }
}
```

**Event Types**:
- `APP_STARTED` - TV app launched
- `DEVICE_PAIRED` - Device paired with station
- `TV_UNLOCKED` - TV unlocked (station started)
- `TV_LOCKED` - TV locked (station stopped)
- `NETWORK_LOST` - Network connection lost
- `NETWORK_RESTORED` - Network connection restored
- `GRACE_PERIOD_STARTED` - Grace period countdown started
- `GRACE_PERIOD_EXPIRED` - Grace period expired, TV locked

**Response** (Success - 200):
```json
{
  "success": true
}
```

**Implementation Notes**:
- Store events in `tv_audit_logs` table
- Events can be queued and sent in batches
- Endpoint should accept events even if network was temporarily down
- Use for compliance reporting and troubleshooting

---

## Database Schema

### New Table: `tv_devices`

```sql
CREATE TABLE tv_devices (
  id SERIAL PRIMARY KEY,
  shop_id INTEGER NOT NULL REFERENCES shops(id),
  station_id INTEGER NOT NULL REFERENCES stations(id),
  device_id VARCHAR(50) NOT NULL UNIQUE,
  paired_at TIMESTAMP DEFAULT NOW(),
  last_seen_at TIMESTAMP DEFAULT NOW(),
  is_active BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_tv_devices_station ON tv_devices(station_id);
CREATE INDEX idx_tv_devices_device ON tv_devices(device_id);
```

### New Table: `tv_audit_logs`

```sql
CREATE TABLE tv_audit_logs (
  id SERIAL PRIMARY KEY,
  device_id VARCHAR(50) NOT NULL,
  station_id INTEGER REFERENCES stations(id),
  event_type VARCHAR(50) NOT NULL,
  event_timestamp TIMESTAMP NOT NULL,
  metadata JSONB,
  created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_tv_audit_device ON tv_audit_logs(device_id);
CREATE INDEX idx_tv_audit_station ON tv_audit_logs(station_id);
CREATE INDEX idx_tv_audit_timestamp ON tv_audit_logs(event_timestamp);
```

### Optional Table: `station_codes`

```sql
CREATE TABLE station_codes (
  id SERIAL PRIMARY KEY,
  station_id INTEGER NOT NULL REFERENCES stations(id),
  code VARCHAR(10) NOT NULL UNIQUE,
  is_active BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMP DEFAULT NOW(),
  expires_at TIMESTAMP
);

CREATE INDEX idx_station_codes_code ON station_codes(code);
```

---

## Security Considerations

### MVP (Phase 1)
- No authentication required for status endpoint (read-only, non-sensitive)
- Pairing endpoint should validate station codes
- Audit endpoint can be unauthenticated (write-only logs)

### Future Enhancements (Phase 2)
- Add API key authentication for TV devices
- Implement rate limiting on status endpoint
- Add device revocation mechanism
- Encrypt audit event metadata

---

## Testing the Backend

### Test Pairing
```bash
curl -X POST http://localhost:3000/api/tv-devices/pair \
  -H "Content-Type: application/json" \
  -d '{
    "station_code": "ABC123",
    "device_id": "TV-TEST001"
  }'
```

### Test Status
```bash
curl http://localhost:3000/api/stations/3/status
```

### Test Audit
```bash
curl -X POST http://localhost:3000/api/tv-devices/audit \
  -H "Content-Type: application/json" \
  -d '{
    "event": "TV_UNLOCKED",
    "station_id": 3,
    "device_id": "TV-TEST001",
    "timestamp": "2026-01-19T04:50:00Z"
  }'
```

---

## Deployment Checklist

- [ ] Create `tv_devices` table
- [ ] Create `tv_audit_logs` table
- [ ] Implement pairing endpoint
- [ ] Implement status endpoint
- [ ] Implement audit endpoint
- [ ] Generate station codes for existing stations
- [ ] Test all endpoints with sample data
- [ ] Deploy to production
- [ ] Update Android TV app with production API URL

---

## Support

For questions about backend integration, contact the GameBiller backend team.
