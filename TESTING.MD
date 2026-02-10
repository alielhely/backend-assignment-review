# Manual Testing Documentation

## Test Environment
- **Application URL:** http://localhost:8080
- **Database:** PostgreSQL 15 (Docker container)
- **Mock Invoice Service:** http://localhost:8000 (Wiremock)
- **Testing Tool:** Postman

## How to Run Tests

### Start the Application
```bash
docker compose up --build
```

Wait for the application to start (check logs for "Started SandboxApplication").

---

## Endpoint 1: POST /deliveries

### Test Case 1: Valid IN_PROGRESS Delivery
**Request:**
```http
POST http://localhost:8080/deliveries
Content-Type: application/json

{
  "vehicleId": "AHV-123",
  "address": "Test Street 1",
  "startedAt": "2024-02-04T10:00:00Z",
  "status": "IN_PROGRESS"
}
```

**Expected Result:**
- Status: `201 Created`
- Response contains: `id`, `vehicleId`, `address`, `startedAt`, `finishedAt: null`, `status: IN_PROGRESS`

**Actual Result:** ✅ Pass
```json
{
  "id": "b5c563dd-07e3-4e69-a335-30c8e203f4dd",
  "vehicleId": "AHV-123",
  "address": "Test Street 1",
  "startedAt": "2024-02-04T10:00:00Z",
  "finishedAt": null,
  "status": "IN_PROGRESS"
}
```
- Status: 201 Created
- Response time: 26 ms
- Size: 338 B

---

### Test Case 2: Valid DELIVERED Delivery
**Request:**
```http
POST http://localhost:8080/deliveries
Content-Type: application/json

{
  "vehicleId": "AHV-456",
  "address": "Test Street 2",
  "startedAt": "2024-02-03T14:30:00Z",
  "finishedAt": "2024-02-03T15:00:00Z",
  "status": "DELIVERED"
}
```

**Expected Result:**
- Status: `201 Created`
- Response contains delivery with `finishedAt` populated

**Actual Result:** ✅ Pass
```json
{
  "id": "70270a60-f3fe-4057-a313-dc481a08d3a5",
  "vehicleId": "AHV-456",
  "address": "Test Street 2",
  "startedAt": "2024-02-03T14:30:00Z",
  "finishedAt": "2024-02-03T15:00:00Z",
  "status": "DELIVERED"
}
```
- Status: 201 Created
- Response time: 122 ms
- Size: 354 B

---

### Test Case 3: Invalid - Missing Required Field (address)
**Request:**
```http
POST http://localhost:8080/deliveries
Content-Type: application/json

{
  "vehicleId": "AHV-123",
  "address": "",
  "startedAt": "2024-02-04T10:00:00Z",
  "status": "IN_PROGRESS"
}
```

**Expected Result:**
- Status: `400 Bad Request`
- Validation error: "address: Address must not be null nor blank"
- Response includes `traceId` and `timestamp`

**Actual Result:** ✅ Pass
```json
{
  "error": "address: Address must not be null nor blank",
  "traceId": "57a29a8c-f511-4228-8ae0-976cf2d70619",
  "timestamp": "2026-02-09T12:14:15.204578093Z"
}
```
- Status: 400 Bad Request
- Response time: 13 ms
- Size: 293 B

---

### Test Case 4: Invalid - Missing Required Field (address completely absent)
**Request:**
```http
POST http://localhost:8080/deliveries
Content-Type: application/json

{
  "vehicleId": "AHV-123",
  "startedAt": "2024-02-04T10:00:00Z",
  "status": "IN_PROGRESS"
}
```

**Expected Result:**
- Status: `400 Bad Request`
- Validation error: "address: Address must not be null nor blank"
- Response includes `traceId` and `timestamp`

**Actual Result:** ✅ Pass
```json
{
  "error": "address: Address must not be null nor blank",
  "traceId": "fa3ca335-c525-4a54-8f05-c885315f821f",
  "timestamp": "2026-02-09T12:14:40.544211008Z"
}
```
- Status: 400 Bad Request
- Response time: 49 ms
- Size: 293 B

---

### Test Case 5: Invalid - DELIVERED without finishedAt
**Request:**
```http
POST http://localhost:8080/deliveries
Content-Type: application/json

{
  "vehicleId": "AHV-999",
  "address": "Test Street 4",
  "startedAt": "2024-02-04T10:00:00Z",
  "status": "DELIVERED"
}
```

**Expected Result:**
- Status: `400 Bad Request`
- Error: "DELIVERED deliveries must have finishedAt"
- Response includes `traceId` and `timestamp`

**Actual Result:** ✅ Pass
```json
{
  "error": "DELIVERED deliveries must have finishedAt",
  "traceId": "ce97dd6a-81c1-4748-b3c1-0d963faa819b",
  "timestamp": "2026-02-09T11:09:38.111048514Z"
}
```
- Status: 400 Bad Request
- Response time: 130 ms
- Size: 291 B

---

### Test Case 6: Invalid - IN_PROGRESS with finishedAt
**Request:**
```http
POST http://localhost:8080/deliveries
Content-Type: application/json

{
  "vehicleId": "AHV-789",
  "address": "Test Street 3",
  "startedAt": "2024-02-04T10:00:00Z",
  "finishedAt": "2024-02-04T11:00:00Z",
  "status": "IN_PROGRESS"
}
```

**Expected Result:**
- Status: `400 Bad Request`
- Error: "IN_PROGRESS deliveries must not have finishedAt"
- Response includes `traceId` and `timestamp`

**Actual Result:** ✅ Pass
```json
{
  "error": "IN_PROGRESS deliveries must not have finishedAt",
  "traceId": "947fdbab-add3-4b36-8b96-1e44e48f5620",
  "timestamp": "2026-02-09T11:09:50.544139880Z"
}
```
- Status: 400 Bad Request
- Response time: 24 ms
- Size: 297 B

---

## Database Verification

### Check Deliveries Table
```bash
docker exec -it backend-ali-elhely-master-database-1 psql -U admin -d deliveries -c "SELECT * FROM deliveries;"
```

**Expected:** All successfully created deliveries appear in the table.

**Actual Result:** ✅ Pass - Deliveries persisted correctly

---

## Test Summary

| Test Case | Status | HTTP Status | Response Time | Notes |
|-----------|--------|-------------|---------------|-------|
| Valid IN_PROGRESS | ✅ Pass | 201 Created | 26 ms | Delivery created with null finishedAt |
| Valid DELIVERED | ✅ Pass | 201 Created | 122 ms | Delivery created with finishedAt |
| Missing field (empty string) | ✅ Pass | 400 Bad Request | 13 ms | Validation works correctly |
| Missing field (null) | ✅ Pass | 400 Bad Request | 49 ms | Validation works correctly |
| DELIVERED without finishedAt | ✅ Pass | 400 Bad Request | 130 ms | Business validation works |
| IN_PROGRESS with finishedAt | ✅ Pass | 400 Bad Request | 24 ms | Business validation works |
| Database persistence | ✅ Pass | N/A | N/A | Data correctly saved |

---

## Additional Testing Needed

### Integration Tests
- [ ] Full POST /deliveries flow with database
- [ ] Exception handling scenarios
- [ ] Database transaction rollback on error

### API Tests (Karate Framework)
- [ ] Automated test suite with feature files
- [ ] Edge cases and boundary conditions
- [ ] Performance testing

---

## Notes
- All tests performed using Postman
- Database verified using psql command
- Application logs checked for errors
- Error responses include `traceId` for debugging
- Validation now properly returns 400 for missing/invalid fields
- Future: Automate these tests with Karate or Spring Boot Test

---

## Test Environment Details
- **Date:** February 9, 2026
- **Tester:** Ali El Hely
- **Application Version:** 0.0.1-SNAPSHOT
- **Spring Boot Version:** 3.3.1
- **Kotlin Version:** 2.0.0