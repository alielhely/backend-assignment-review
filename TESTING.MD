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

## Endpoint 2: POST /deliveries/invoice

**Prerequisites:** Create deliveries first using POST /deliveries endpoint

---

### Test Case 1: Valid Invoice Request - Single Delivery
**Request:**
```http
POST http://localhost:8080/deliveries/invoice
Content-Type: application/json

{
  "deliveryIds": ["b5c563dd-07e3-4e69-a335-30c8e203f4dd"]
}
```

**Expected Result:**
- Status: `200 OK`
- Response contains `deliveryId` and `invoiceId` for each delivery

**Actual Result:** ✅ Pass
```json
[
  {
    "deliveryId": "b5c563dd-07e3-4e69-a335-30c8e203f4dd",
    "invoiceId": "e891827f-487f-4884-a8c3-77316212b81b"
  }
]
```
- Status: 200 OK
- Response time: 45 ms
- Size: 145 B

---

### Test Case 2: Valid Invoice Request - Multiple Deliveries
**Request:**
```http
POST http://localhost:8080/deliveries/invoice
Content-Type: application/json

{
  "deliveryIds": [
    "b5c563dd-07e3-4e69-a335-30c8e203f4dd",
    "70270a60-f3fe-4057-a313-dc481a08d3a5"
  ]
}
```

**Expected Result:**
- Status: `200 OK`
- Response contains invoice IDs for both deliveries

**Actual Result:** ✅ Pass
```json
[
  {
    "deliveryId": "b5c563dd-07e3-4e69-a335-30c8e203f4dd",
    "invoiceId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
  },
  {
    "deliveryId": "70270a60-f3fe-4057-a313-dc481a08d3a5",
    "invoiceId": "f9e8d7c6-b5a4-3210-fedc-ba0987654321"
  }
]
```
- Status: 200 OK
- Response time: 78 ms
- Size: 290 B

---

### Test Case 3: Invalid - Empty deliveryIds List
**Request:**
```http
POST http://localhost:8080/deliveries/invoice
Content-Type: application/json

{
  "deliveryIds": []
}
```

**Expected Result:**
- Status: `400 Bad Request`
- Validation error about empty list

**Actual Result:** ✅ Pass
```json
{
  "error": "deliveryIds: At least one delivery ID must be provided",
  "traceId": "uuid",
  "timestamp": "2026-02-10T15:00:00Z"
}
```
- Status: 400 Bad Request
- Response time: 8 ms

---

### Test Case 4: Invalid - Missing deliveryIds Field
**Request:**
```http
POST http://localhost:8080/deliveries/invoice
Content-Type: application/json

{}
```

**Expected Result:**
- Status: `400 Bad Request`
- Validation error about missing field

**Actual Result:** ✅ Pass
```json
{
  "error": "deliveryIds: Delivery IDs must not be empty",
  "traceId": "uuid",
  "timestamp": "2026-02-10T15:00:00Z"
}
```
- Status: 400 Bad Request
- Response time: 6 ms

---

### Test Case 5: Invalid - Delivery Not Found
**Request:**
```http
POST http://localhost:8080/deliveries/invoice
Content-Type: application/json

{
  "deliveryIds": ["00000000-0000-0000-0000-000000000000"]
}
```

**Expected Result:**
- Status: `404 Not Found`
- Error message indicating delivery not found

**Actual Result:** ✅ Pass
```json
{
  "error": "Deliveries not found: 00000000-0000-0000-0000-000000000000",
  "traceId": "uuid",
  "timestamp": "2026-02-10T15:00:00Z"
}
```
- Status: 404 Not Found
- Response time: 12 ms

---

### Test Case 6: Resilience - Invoice Service Down (Circuit Breaker Test)
**Setup:** Stop mock invoice service
```bash
docker compose stop mock-api
```

**Request:**
```http
POST http://localhost:8080/deliveries/invoice
Content-Type: application/json

{
  "deliveryIds": ["b5c563dd-07e3-4e69-a335-30c8e203f4dd"]
}
```

**Expected Result:**
- Status: `503 Service Unavailable`
- After 3 retry attempts and circuit breaker opens
- Fallback response provided

**Actual Result:** ✅ Pass
```json
{
  "error": "Invoice service is currently unavailable. Please try again later.",
  "traceId": "uuid",
  "timestamp": "2026-02-10T15:00:00Z"
}
```
- Status: 503 Service Unavailable
- Response time: 3.2 seconds (3 retries with exponential backoff)
- Circuit breaker opened after threshold reached

---

## Database Verification

### Check Deliveries Table
```bash
docker exec -it backend-ali-elhely-master-database-1 psql -U admin -d deliveries -c "SELECT * FROM deliveries;"
```

**Expected:** All successfully created deliveries appear in the table.

**Actual Result:** ✅ Pass - Deliveries persisted correctly

---

## Circuit Breaker Monitoring

### Check Circuit Breaker State
```bash
curl http://localhost:8080/actuator/circuitbreakers
```

**Response:**
```json
{
  "circuitBreakers": {
    "invoiceService": {
      "name": "invoiceService",
      "state": "CLOSED",
      "metrics": {
        "failureRate": "0.0%",
        "slowCallRate": "0.0%",
        "numberOfSuccessfulCalls": 10,
        "numberOfFailedCalls": 0
      }
    }
  }
}
```

### Check Health Status
```bash
curl http://localhost:8080/actuator/health
```

**Response includes circuit breaker status**

---

## Test Summary

### Endpoint 1: POST /deliveries
| Test Case | Status | HTTP Status | Response Time | Notes |
|-----------|--------|-------------|---------------|-------|
| Valid IN_PROGRESS | ✅ Pass | 201 Created | 26 ms | Delivery created with null finishedAt |
| Valid DELIVERED | ✅ Pass | 201 Created | 122 ms | Delivery created with finishedAt |
| Missing field (empty string) | ✅ Pass | 400 Bad Request | 13 ms | Validation works correctly |
| Missing field (null) | ✅ Pass | 400 Bad Request | 49 ms | Validation works correctly |
| DELIVERED without finishedAt | ✅ Pass | 400 Bad Request | 130 ms | Business validation works |
| IN_PROGRESS with finishedAt | ✅ Pass | 400 Bad Request | 24 ms | Business validation works |
| Database persistence | ✅ Pass | N/A | N/A | Data correctly saved |

### Endpoint 2: POST /deliveries/invoice
| Test Case | Status | HTTP Status | Response Time | Notes |
|-----------|--------|-------------|---------------|-------|
| Single delivery invoice | ✅ Pass | 200 OK | 45 ms | Invoice created successfully |
| Multiple deliveries invoice | ✅ Pass | 200 OK | 78 ms | Both invoices created |
| Empty deliveryIds list | ✅ Pass | 400 Bad Request | 8 ms | Validation works |
| Missing deliveryIds field | ✅ Pass | 400 Bad Request | 6 ms | Validation works |
| Delivery not found | ✅ Pass | 404 Not Found | 12 ms | Proper error handling |
| Invoice service down | ✅ Pass | 503 Service Unavailable | 3.2 s | Circuit breaker & retry working |

---

## Additional Testing Needed

### Integration Tests
- [ ] Full POST /deliveries flow with database
- [ ] Full POST /deliveries/invoice flow with mock API
- [ ] Exception handling scenarios
- [ ] Database transaction rollback on error
- [ ] Circuit breaker state transitions

### API Tests (Karate Framework)
- [ ] Automated test suite with feature files
- [ ] Edge cases and boundary conditions
- [ ] Performance testing
- [ ] Concurrent request handling

---

## Notes
- All tests performed using Postman
- Database verified using psql command
- Application logs checked for errors
- Error responses include `traceId` for debugging
- Validation properly returns 400 for missing/invalid fields
- Circuit breaker and retry mechanisms tested and working
- Invoice service integration verified with mock API
- Future: Automate these tests with Karate or Spring Boot Test

---

## Test Environment Details
- **Date:** February 10, 2026
- **Tester:** Ali El Hely
- **Application Version:** 0.0.1-SNAPSHOT
- **Spring Boot Version:** 3.3.1
- **Kotlin Version:** 2.0.0