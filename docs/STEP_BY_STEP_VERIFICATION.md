# EWB Standing Order Processor: Step-by-Step Verification & Screenshot Guide

This guide provides a comprehensive, step-by-step walkthrough to verify all deliverables for **Member 4 (Platform, Gateway/IdP, Notifications & Acceptance Lead)** and system-wide acceptance criteria.

Each step includes the exact command, expected JSON responses, and a designated **Screenshot Placeholder** ready for your documentation submissions.

---

## Prerequisites

- **Java 21 LTS** installed (`java -version`)
- **Apache Maven 3.9+** installed (`mvn -version`)
- **PowerShell 7+** or Windows PowerShell
- Terminal working directory set to project root: `c:\Users\MSB83776\Documents\antigravity\day-29-neo`

---

## Step 1: Run Full Automated Test Suite (100% Green Build)

Run Maven across the modules to verify that `common`, `gateway-service`, and `notification-service` tests pass with zero failures:

```powershell
mvn test
```

### Expected Output:
```
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0 -- in com.ewb.gateway.SecurityRbacTest & AuthControllerTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0 -- in com.ewb.notification.NotificationServiceTest & NotificationControllerTest
[INFO] BUILD SUCCESS
```

> 📸 **SCREENSHOT #1: Maven Test Suite Success**
> - **Target:** Terminal output of `mvn test`
> - **What to capture:** The `[INFO] BUILD SUCCESS` message showing all tests passing.
> 
> ```
> [PASTE SCREENSHOT 1 HERE: Maven Test Suite Success]
> ```

---

## Step 2: Start Infrastructure Services (Config & Eureka)

Open a new PowerShell terminal and start the Spring Cloud Config Server:

```powershell
mvn spring-boot:run -pl config-server
```

Open a second PowerShell terminal and start the Netflix Eureka Discovery Server:

```powershell
mvn spring-boot:run -pl eureka-server
```

Open your browser and navigate to:
**`http://localhost:8761`**

### Expected Result:
The Spring Cloud Netflix Eureka dashboard displays with System Status and instance registry.

> 📸 **SCREENSHOT #2: Eureka Discovery Dashboard**
> - **Target:** Browser window at `http://localhost:8761`
> - **What to capture:** Eureka web console displaying running environment.
> 
> ```
> [PASTE SCREENSHOT 2 HERE: Eureka Dashboard]
> ```

---

## Step 3: Verify Centralized Configuration Server

Open your browser or run the following PowerShell command to test Config Server native profile delivery:

```powershell
Invoke-RestMethod -Uri "http://localhost:8888/gateway-service/default" -Method Get | ConvertTo-Json -Depth 5
```

### Expected Result:
HTTP 200 OK returning property sources including `gateway-service.yml` and `ewb.routes` mapping.

> 📸 **SCREENSHOT #3: Config Server Profile Response**
> - **Target:** Browser or terminal at `http://localhost:8888/gateway-service/default`
> - **What to capture:** JSON output showing the distributed configuration profile.
> 
> ```
> [PASTE SCREENSHOT 3 HERE: Config Server Response]
> ```

---

## Step 4: Start Gateway Service & Notification Service

Open two additional terminals and start the services owned by Member 4:

**Terminal 3 (Gateway Service - Port 8080):**
```powershell
mvn spring-boot:run -pl gateway-service
```

**Terminal 4 (Notification Service - Port 8084):**
```powershell
mvn spring-boot:run -pl notification-service
```

---

## Step 5: Test Mock IdP Authentication (`POST /auth/login`)

Authenticate as **Asuna Yuuki** (`ROLE_CUSTOMER`):

```powershell
$body = @{ username = "asuna" } | ConvertTo-Json
$authResponse = Invoke-RestMethod -Uri "http://localhost:8080/auth/login" -Method Post -ContentType "application/json" -Body $body
$authResponse | Format-List
```

### Expected JSON Response (`200 OK`):
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "username": "asuna",
  "fullName": "Asuna Yuuki",
  "role": "ROLE_CUSTOMER",
  "accountIds": [
    "EWB-ASU-1001",
    "EWB-ASU-2001"
  ]
}
```

Authenticate as an unknown persona to verify rejection:

```powershell
$unknownBody = @{ username = "stranger" } | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8080/auth/login" -Method Post -ContentType "application/json" -Body $unknownBody
```

### Expected JSON Response (`401 Unauthorized`):
```json
{
  "status": "UNAUTHORIZED",
  "message": "Unknown persona: stranger"
}
```

> 📸 **SCREENSHOT #4: Mock IdP Login & JWT Generation**
> - **Target:** Postman or Terminal executing `POST /auth/login`
> - **What to capture:** Asuna's generated JWT token, role `ROLE_CUSTOMER`, and assigned accounts.
> 
> ```
> [PASTE SCREENSHOT 4 HERE: Mock IdP Login]
> ```

---

## Step 6: Test Gateway Security & RBAC Enforcement

### 6.1. External Block on Internal Inter-Service Endpoints
Attempt to directly call `/internal/standing-orders/due` from outside the perimeter:

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/internal/standing-orders/due" -Method Get
```

### Expected Result (`403 Forbidden`):
```json
{
  "status": "FORBIDDEN",
  "message": "Direct access to internal endpoints is forbidden via Gateway"
}
```

> 📸 **SCREENSHOT #5: Gateway Internal Endpoint Protection**
> - **Target:** Postman / Terminal calling `GET http://localhost:8080/internal/standing-orders/due`
> - **What to capture:** `403 Forbidden` rejection blocking external access to internal routes.
> 
> ```
> [PASTE SCREENSHOT 5 HERE: Internal Block 403]
> ```

---

### 6.2. Source Account Ownership Validation
Asuna attempts to create a standing order using Kirito's account (`EWB-KIR-5001`):

```powershell
$asunaToken = $authResponse.token

$hijackPayload = @{
    sourceAccountId = "EWB-KIR-5001"
    destinationAccountId = "EWB-ASU-2001"
    amount = 5000.00
    currency = "PHP"
    frequency = "MONTHLY"
    dayOfMonth = 25
    executionTime = "09:00"
    timeZone = "Asia/Manila"
    startDate = "2026-10-25"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/standing-orders" -Method Post -ContentType "application/json" `
    -Headers @{ Authorization = "Bearer $asunaToken" } -Body $hijackPayload
```

### Expected Result (`403 Forbidden`):
```json
{
  "status": "FORBIDDEN",
  "message": "Source account EWB-KIR-5001 does not belong to authenticated customer"
}
```

> 📸 **SCREENSHOT #6: Gateway Account Ownership Rejection**
> - **Target:** Postman / Terminal creating order with unauthorized account
> - **What to capture:** `403 Forbidden` with "does not belong to authenticated customer" error message.
> 
> ```
> [PASTE SCREENSHOT 6 HERE: Account Ownership 403]
> ```

---

## Step 7: Test Notification Service (Ingestion, Deduplication & Outage)

### 7.1. Ingest Execution Outbox Event (Normal Delivery)

```powershell
$eventPayload = @{
    eventId = "evt-test-8899"
    executionId = "exec-101"
    standingOrderId = "so-9001"
    customerId = "asuna"
    eventType = "EXECUTION_COMPLETED"
    status = "SUCCESS"
    amount = 5000.00
    currency = "PHP"
    sourceAccountId = "EWB-ASU-1001"
    destinationAccountId = "EWB-ASU-2001"
    paymentReference = "TRF-20261025-001"
    errorMessage = $null
    timestamp = "2026-10-25T01:00:03Z"
} | ConvertTo-Json

Invoke-WebRequest -Uri "http://localhost:8084/notifications/events" -Method Post -ContentType "application/json" -Body $eventPayload
```

### Expected Response (`202 Accepted`):
```json
{
  "status": "DELIVERED",
  "deliveryId": "notif-501"
}
```

> 📸 **SCREENSHOT #7: Notification Event Ingested (202 Accepted)**
> - **Target:** Postman / Terminal
> - **What to capture:** HTTP status code `202 Accepted` and assigned `deliveryId`.
> 
> ```
> [PASTE SCREENSHOT 7 HERE: Notification Ingestion 202 Accepted]
> ```

---

### 7.2. Ingest Duplicate Event (Idempotent Deduplication)

Submit the exact same payload with `eventId = "evt-test-8899"` again:

```powershell
Invoke-WebRequest -Uri "http://localhost:8084/notifications/events" -Method Post -ContentType "application/json" -Body $eventPayload
```

### Expected Response (`200 OK`):
```json
{
  "status": "SKIPPED",
  "message": "Duplicate event ID"
}
```

> 📸 **SCREENSHOT #8: Notification Deduplication (200 OK SKIPPED)**
> - **Target:** Postman / Terminal
> - **What to capture:** HTTP status code `200 OK` and `"status": "SKIPPED"`, `"message": "Duplicate event ID"`.
> 
> ```
> [PASTE SCREENSHOT 8 HERE: Notification Deduplication 200 OK]
> ```

---

### 7.3. Simulate Notification Gateway Outage

Submit an event with the `X-Simulate-Outage: true` header:

```powershell
Invoke-WebRequest -Uri "http://localhost:8084/notifications/events" -Method Post -ContentType "application/json" `
    -Headers @{ "X-Simulate-Outage" = "true" } -Body $eventPayload
```

### Expected Response (`500 Internal Server Error`):
```json
{
  "status": "FAILED",
  "message": "Simulated notification delivery gateway outage"
}
```

> 📸 **SCREENSHOT #9: Notification Outage Simulation (500 Internal Server Error)**
> - **Target:** Postman / Terminal
> - **What to capture:** HTTP status code `500` confirming that notification outage simulation triggers cleanly.
> 
> ```
> [PASTE SCREENSHOT 9 HERE: Outage Simulation 500]
> ```

---

## Step 8: Run Automated Live Demo Script

Execute the provided end-to-end automated PowerShell runner:

```powershell
.\scripts\run-demo.ps1
```

### Expected Output:
Color-coded terminal showing all test scenarios with `[PASS]` tags:
- Mock IdP Authentication (Asuna, Agil, Sinon)
- Gateway RBAC & Security Perimeter Enforcement
- Account Ownership & Read-Only Checks
- Notification Ingestion & Deduplication
- Core Banking Outage Simulation

> 📸 **SCREENSHOT #10: Automated Demo Script Execution**
> - **Target:** Terminal running `.\scripts\run-demo.ps1`
> - **What to capture:** Colorized green `[PASS]` results and demonstration summary banner.
> 
> ```
> [PASTE SCREENSHOT 10 HERE: Automated Demo Script]
> ```

---

## Step 9: Postman Collection Verification

1. Open Postman.
2. Click **Import** and select:
   `postman/ewb-standing-order.postman_collection.json`
3. Click **Run collection**.
4. Verify all requests execute and assertions pass.

> 📸 **SCREENSHOT #11: Postman Collection Runner**
> - **Target:** Postman Collection Runner window
> - **What to capture:** All requests in `EWB Standing Order Platform - API Suite` passing with 100% green tests.
> 
> ```
> [PASTE SCREENSHOT 11 HERE: Postman Collection Runner]
> ```

---

## Summary Checklist for Member 4 Acceptance

- [x] Parent `pom.xml` and `common` module clean build.
- [x] Config Server (`:8888`) serves native configurations from `config-repo/`.
- [x] Eureka Server (`:8761`) service discovery active.
- [x] Gateway Mock IdP (`:8080/auth/login`) issues signed JWTs for all SAO personas.
- [x] Gateway RBAC and source account ownership validation enforced.
- [x] Gateway blocks external access to `/internal/**`.
- [x] Notification Service (`:8084/notifications/events`) ingests events (`202 Accepted`).
- [x] Notification Service safely skips duplicate event IDs (`200 OK SKIPPED`).
- [x] Notification Service simulates delivery outages (`500 Internal Server Error`).
- [x] Dockerfile and `docker-compose.yml` orchestrates all 7 services.
- [x] Complete Postman collection provided.
- [x] Automated live demonstration script `scripts/run-demo.ps1` provided.
- [x] Step-by-step verification guide with screenshot placeholders completed.
