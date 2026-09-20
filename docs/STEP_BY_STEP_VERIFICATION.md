# EWB Standing Order Processor: Step-by-Step Verification & Screenshot Guide

This guide provides a comprehensive, sequential walkthrough to verify all deliverables for **Member 4 (Platform, Gateway/IdP, Notifications & Acceptance Lead)** and system-wide acceptance criteria.

Each step provides the exact execution command, under-the-hood context, expected outputs, and a labeled **Screenshot Placeholder** directly matching the 13 verification sections in [`docs/Bernabe-Docs.docx`](./Bernabe-Docs.docx) and [`docs/DOCUMENTATION.md`](./DOCUMENTATION.md).

---

## Prerequisites

- **Java 21 LTS** installed (`java -version`)
- **Apache Maven 3.9+** installed (`mvn -version`)
- **PowerShell 7+** or Windows PowerShell
- Working directory set to root: `c:\Users\MSB83776\Documents\antigravity\day-29-neo`

---

## Step 1: Automated Unit & Integration Test Suite

Execute Maven across the entire multi-module project to verify that all common contracts, gateway security rules, mock IdP, and notification deduplication tests pass with 100% success.

```powershell
mvn test
```

### Expected Output:
```text
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0 -- in com.ewb.gateway.SecurityRbacTest & AuthControllerTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0 -- in com.ewb.notification.NotificationServiceTest & NotificationControllerTest
[INFO] Reactor Summary for EWB Standing Order Platform 1.0.0-SNAPSHOT:
[INFO]   EWB Common Module .................................. SUCCESS
[INFO]   EWB Config Server .................................. SUCCESS
[INFO]   EWB Eureka Server .................................. SUCCESS
[INFO]   EWB Gateway Service ................................ SUCCESS
[INFO]   EWB Notification Service ........................... SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
```

> 📸 **SCREENSHOT #1: Maven Automated Test Suite Success**  
> - **Target:** Terminal / PowerShell  
> - **Verification Item:** Terminal output displaying `Reactor Summary` with all modules SUCCESS and `[INFO] BUILD SUCCESS`.  
> 
> ```
> [ PASTE SCREENSHOT 1 HERE: Maven Test Suite Success ]
> ```

---

## Step 2: Spring Cloud Eureka Service Discovery Registry

Start Eureka discovery server on port 8761 and inspect the web console in your browser:

```powershell
# In a dedicated terminal:
mvn spring-boot:run -pl eureka-server
```

Open Browser: **`http://localhost:8761`**

### Expected Output:
The Spring Cloud Netflix Eureka dashboard displays with active System Status and all registered service instances (`GATEWAY-SERVICE`, `STANDING-ORDER-SERVICE`, `EXECUTION-SERVICE`, `PAYMENT-SERVICE`, `NOTIFICATION-SERVICE`).

> 📸 **SCREENSHOT #2: Eureka Discovery Dashboard**  
> - **Target:** Browser window at `http://localhost:8761`  
> - **Verification Item:** Eureka web console displaying active server status and registered microservices.  
> 
> ```
> [ PASTE SCREENSHOT 2 HERE: Eureka Dashboard ]
> ```

---

## Step 3: Spring Cloud Centralized Config Server

Verify that `config-server` on port 8888 is serving centralized YAML configuration profiles natively from `config-repo/`:

```powershell
Invoke-RestMethod -Uri "http://localhost:8888/gateway-service/default" -Method Get | ConvertTo-Json -Depth 5
```

### Expected Output (`HTTP 200 OK`):
JSON payload containing `propertySources` with route definitions and logging configurations for `gateway-service`.

```json
{
  "name": "gateway-service",
  "profiles": ["default"],
  "propertySources": [
    {
      "name": "file:../config-repo/gateway-service.yml",
      "source": {
        "server.port": 8080,
        "spring.application.name": "gateway-service",
        "ewb.routes.standing-order-service": "http://localhost:8081",
        "ewb.routes.execution-service": "http://localhost:8082",
        "ewb.routes.payment-service": "http://localhost:8083",
        "ewb.routes.notification-service": "http://localhost:8084"
      }
    }
  ]
}
```

> 📸 **SCREENSHOT #3: Config Server Native Profile Distribution**  
> - **Target:** Browser or Terminal at `http://localhost:8888/gateway-service/default`  
> - **Verification Item:** JSON response confirming `config-server` is serving `gateway-service.yml`.  
> 
> ```
> [ PASTE SCREENSHOT 3 HERE: Config Server Response ]
> ```

---

## Step 4: Mock Identity Provider Login & JWT Generation

Submit a POST request to `/auth/login` with username `asuna` to verify JWT token generation matching contracts.md Section 2:

```powershell
$body = @{ username = "asuna" } | ConvertTo-Json
$auth = Invoke-RestMethod -Uri "http://localhost:8080/auth/login" -Method Post -ContentType "application/json" -Body $body
$auth | Format-List
```

### Expected Output (`HTTP 200 OK`):
```text
token      : eyJhbGciOiJIUzM4NCJ9...
username   : asuna
fullName   : Asuna Yuuki
role       : ROLE_CUSTOMER
accountIds : {EWB-ASU-1001, EWB-ASU-2001}
```

> 📸 **SCREENSHOT #4: Mock IdP Login for Asuna (ROLE_CUSTOMER)**  
> - **Target:** Postman or Terminal: `POST http://localhost:8080/auth/login`  
> - **Verification Item:** HTTP 200 response displaying signed JWT token, persona full name, and account assignments.  
> 
> ```
> [ PASTE SCREENSHOT 4 HERE: Mock IdP Login ]
> ```

---

## Step 5: Unknown User Login Rejection (401 Unauthorized)

Attempt to authenticate with an unrecognized persona to ensure perimeter security rejects unauthorized credentials:

```powershell
$unknownBody = @{ username = "unknown_hacker" } | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8080/auth/login" -Method Post -ContentType "application/json" -Body $unknownBody
```

### Expected Output (`HTTP 401 Unauthorized`):
```json
{
  "status": "UNAUTHORIZED",
  "message": "Unknown persona: unknown_hacker"
}
```

> 📸 **SCREENSHOT #5: Mock IdP Rejection of Unknown Persona (401 Unauthorized)**  
> - **Target:** Postman or Terminal: `POST http://localhost:8080/auth/login`  
> - **Verification Item:** HTTP 401 Unauthorized error response confirming perimeter authentication rejection.  
> 
> ```
> [ PASTE SCREENSHOT 5 HERE: Mock IdP 401 Unauthorized ]
> ```

---

## Step 6: Gateway Security - Direct Internal Endpoint Block

Attempt to directly access an internal inter-service endpoint (`/internal/**`) from outside the perimeter:

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/internal/standing-orders/due" -Method Get
```

### Expected Output (`HTTP 403 Forbidden`):
```json
{
  "status": "FORBIDDEN",
  "message": "Direct access to internal endpoints is forbidden via Gateway"
}
```

> 📸 **SCREENSHOT #6: Perimeter Block of External /internal/** Access (403 Forbidden)**  
> - **Target:** Postman or Terminal: `GET http://localhost:8080/internal/standing-orders/due`  
> - **Verification Item:** HTTP 403 Forbidden response proving the Gateway blocks external access to internal endpoints.  
> 
> ```
> [ PASTE SCREENSHOT 6 HERE: Internal Block 403 ]
> ```

---

## Step 7: Gateway Security - Account Ownership Validation

Asuna (`ROLE_CUSTOMER`) attempts to create a standing order specifying Kirito's account (`EWB-KIR-5001`):

```powershell
$hijackOrder = @{
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
    -Headers @{ Authorization = "Bearer $($auth.token)" } -Body $hijackOrder
```

### Expected Output (`HTTP 403 Forbidden`):
```json
{
  "status": "FORBIDDEN",
  "message": "Source account EWB-KIR-5001 does not belong to authenticated customer"
}
```

> 📸 **SCREENSHOT #7: Source Account Hijacking Rejection (403 Forbidden)**  
> - **Target:** Postman or Terminal: `POST http://localhost:8080/standing-orders`  
> - **Verification Item:** HTTP 403 Forbidden response proving Gateway enforces source account ownership validation.  
> 
> ```
> [ PASTE SCREENSHOT 7 HERE: Account Ownership 403 ]
> ```

---

## Step 8: Gateway Security - Auditor Read-Only Enforcement

Sinon (`ROLE_AUDITOR`) attempts to execute a mutating POST request:

```powershell
$sinonAuth = Invoke-RestMethod -Uri "http://localhost:8080/auth/login" -Method Post -ContentType "application/json" -Body (@{ username = "sinon" } | ConvertTo-Json)

Invoke-RestMethod -Uri "http://localhost:8080/standing-orders" -Method Post -ContentType "application/json" `
    -Headers @{ Authorization = "Bearer $($sinonAuth.token)" } -Body $hijackOrder
```

### Expected Output (`HTTP 403 Forbidden`):
```json
{
  "status": "FORBIDDEN",
  "message": "Auditor role has read-only access"
}
```

> 📸 **SCREENSHOT #8: Auditor Read-Only Restriction Enforced (403 Forbidden)**  
> - **Target:** Postman or Terminal: `POST http://localhost:8080/standing-orders`  
> - **Verification Item:** HTTP 403 Forbidden response proving the Gateway restricts Auditor accounts to read-only actions.  
> 
> ```
> [ PASTE SCREENSHOT 8 HERE: Auditor Read-Only 403 ]
> ```

---

## Step 9: Notification Consumer - Outbox Event Ingestion

Submit a new execution outbox event to `POST /notifications/events`:

```powershell
$event = @{
    eventId = "evt-doc-1001"
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

Invoke-WebRequest -Uri "http://localhost:8084/notifications/events" -Method Post -ContentType "application/json" -Body $event -UseBasicParsing
```

### Expected Output (`HTTP 202 Accepted`):
```json
{
  "status": "DELIVERED",
  "deliveryId": "notif-501"
}
```

> 📸 **SCREENSHOT #9: Notification Event Ingestion Success (202 Accepted)**  
> - **Target:** Postman or Terminal: `POST http://localhost:8084/notifications/events`  
> - **Verification Item:** HTTP status 202 Accepted, status `DELIVERED`, and an assigned `deliveryId`.  
> 
> ```
> [ PASTE SCREENSHOT 9 HERE: Notification Ingestion 202 Accepted ]
> ```

---

## Step 10: Notification Deduplication - Duplicate Event Handling

Submit the exact same event payload (`eventId = "evt-doc-1001"`) again to verify idempotency:

```powershell
Invoke-WebRequest -Uri "http://localhost:8084/notifications/events" -Method Post -ContentType "application/json" -Body $event -UseBasicParsing
```

### Expected Output (`HTTP 200 OK`):
```json
{
  "status": "SKIPPED",
  "message": "Duplicate event ID"
}
```

> 📸 **SCREENSHOT #10: Notification Deduplication Skipping Duplicate Event (200 OK SKIPPED)**  
> - **Target:** Postman or Terminal: `POST http://localhost:8084/notifications/events`  
> - **Verification Item:** HTTP 200 OK with status `SKIPPED` and message `"Duplicate event ID"`.  
> 
> ```
> [ PASTE SCREENSHOT 10 HERE: Notification Deduplication 200 OK ]
> ```

---

## Step 11: Notification Outage Simulation

Submit an event with the `X-Simulate-Outage: true` header to simulate a downstream gateway delivery failure:

```powershell
Invoke-WebRequest -Uri "http://localhost:8084/notifications/events" -Method Post -ContentType "application/json" `
    -Headers @{ "X-Simulate-Outage" = "true" } -Body $event -UseBasicParsing
```

### Expected Output (`HTTP 500 Internal Server Error`):
```json
{
  "status": "FAILED",
  "message": "Simulated notification delivery gateway outage"
}
```

> 📸 **SCREENSHOT #11: Notification Outage Simulation Clean Failure (500 Error)**  
> - **Target:** Postman or Terminal: `POST http://localhost:8084/notifications/events`  
> - **Verification Item:** HTTP 500 status confirming that downstream notification outages fail cleanly without impacting Core Banking transactions.  
> 
> ```
> [ PASTE SCREENSHOT 11 HERE: Outage Simulation 500 Error ]
> ```

---

## Step 12: Automated PowerShell Live Demo Runner

Execute the automated end-to-end live demonstration script:

```powershell
.\scripts\run-demo.ps1
```

### Expected Output:
Color-coded terminal execution showing all contract test scenarios passing with `[PASS]` tags:
- Mock IdP Authentication (Asuna, Agil, Sinon, Stranger rejection)
- Gateway RBAC & Security Perimeter Enforcement (`/internal/**` block, account ownership, auditor read-only)
- Notification Ingestion, Deduplication, and Delivery Gateway Outage Simulation

> 📸 **SCREENSHOT #12: Automated Demonstration Runner (run-demo.ps1)**  
> - **Target:** PowerShell Terminal: `.\scripts\run-demo.ps1`  
> - **Verification Item:** Terminal output displaying green `[PASS]` results for all scenarios and final success banner.  
> 
> ```
> [ PASTE SCREENSHOT 12 HERE: Automated Demo Script ]
> ```

---

## Step 13: Postman Collection Test Suite Execution

Import and execute the postman test suite in the Postman app:

1. Open **Postman**.
2. Click **Import** $\rightarrow$ select `postman/ewb-standing-order.postman_collection.json`.
3. Click **Run Collection**.
4. Verify all 15 requests pass with 100% green tests.

> 📸 **SCREENSHOT #13: Postman Collection Runner Execution (100% Passed)**  
> - **Target:** Postman App: Collection Runner  
> - **Verification Item:** Postman Collection Runner window displaying all requests passing with 100% green assertions.  
> 
> ```
> [ PASTE SCREENSHOT 13 HERE: Postman Collection Runner ]
> ```
