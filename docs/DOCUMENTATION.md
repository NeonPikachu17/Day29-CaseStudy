# EastWest Bank (EWB) Standing Order Platform: Technical Documentation

> **Role:** Member 4 (Platform Architect, Gateway/Security Engineer & Acceptance Lead)  
> **Target Modules:** `common/`, `config-server/`, `eureka-server/`, `gateway-service/`, `notification-service/`  
> **Global Contracts:** [`contracts.md`](../contracts.md)  
> **Word Document:** [`docs/EWB_Standing_Order_Platform_Documentation.docx`](./EWB_Standing_Order_Platform_Documentation.docx)

---

## 1. Executive Summary & Microservices Topology

The **EWB Standing Order Platform** is an enterprise-grade microservices system built using **Java 21 LTS**, **Spring Boot 3.3.4**, and **Spring Cloud 2023.0.3**. It automates scheduled recurring bank transfers, guarantees idempotency against Core Banking systems, prevents duplicate debits, and provides asynchronous, deduplicated notification delivery.

### Microservices Port Allocation & Responsibility Matrix

| Service Name | Port | Database | Primary Responsibility | Owner |
| :--- | :--- | :--- | :--- | :--- |
| **`config-server`** | `8888` | Native Git/YAML | Centralized configuration repository for all environments | Member 4 |
| **`eureka-server`** | `8761` | In-Memory Registry | Netflix Eureka service discovery & heartbeat monitoring | Member 4 |
| **`gateway-service`** | `8080` | Stateless (Mock IdP) | Reverse proxy, Mock IdP (`/auth/login`), JWT validation, downstream header injection, RBAC | Member 4 |
| **`standing-order-service`** | `8081` | `standing_order_db` (H2) | Standing order lifecycle (create, pause, resume, cancel), versioning, schedule calendar | Member 1 |
| **`execution-service`** | `8082` | `execution_db` (H2) | Due instruction discovery, worker lease claiming, cut-off checks, transactional outbox | Member 3 |
| **`payment-service`** | `8083` | `payment_db` (H2) | Mock Core Banking: double-entry ledger, balance checks, frozen account check, idempotency | Member 2 |
| **`notification-service`** | `8084` | `notification_db` (H2) | Asynchronous outbox event consumer, event deduplication, delivery tracking | Member 4 |

---

## 2. Authentication & System Personas (contracts.md Section 2)

The system incorporates a Mock Identity Provider (IdP) within `gateway-service` on port `8080`. Clients authenticate via `POST /auth/login`, receiving a cryptographically signed HMAC-SHA256 JWT containing their user ID, role, and authorized account list.

| Username | Full Name | System Role | Primary Account ID | Initial Balance & Status |
| :--- | :--- | :--- | :--- | :--- |
| `asuna` | Asuna Yuuki | `ROLE_CUSTOMER` | `EWB-ASU-1001` | ₱20,000.00 (Active) |
| `kirito` | Kazuto Kirigaya | `ROLE_CUSTOMER` | `EWB-KIR-5001` | ₱50,000.00 (Active) |
| `klein` | Ryoutarou Tsuboi | `ROLE_CUSTOMER` | `EWB-KLN-4001` | ₱2,000.00 (Low Balance) |
| `heathcliff` | Akihiko Kayaba | `ROLE_CUSTOMER` | `EWB-HTH-3001` | ₱10,000.00 (FROZEN) |
| `agil` | Andrew Gilbert Mills | `ROLE_OPERATIONS` | N/A | Operations Officer |
| `sinon` | Shino Asada | `ROLE_AUDITOR` | N/A | Read-Only Auditor |

### Downstream Forwarded HTTP Headers

Upon validating the JWT at the API Gateway perimeter, `ReverseProxyFilter` extracts the identity claims and injects the following headers before dispatching the request to downstream internal microservices:
- `X-User-Id`: Authenticated username (e.g. `asuna`)
- `X-User-Role`: Assigned authorization role (e.g. `ROLE_CUSTOMER`)
- `X-User-Accounts`: Comma-separated list of customer-owned accounts (e.g. `EWB-ASU-1001,EWB-ASU-2001`)

---

## 3. Step-by-Step Manual Verification Guide & Screenshot Submissions

This section provides the complete step-by-step procedure to execute all test and verification scenarios. Each step includes the exact command, expected outputs, and a labeled placeholder to paste your screenshots.

---

### Step 1: Automated Unit & Integration Test Suite

Execute Maven across the entire multi-module project to verify that all common contracts, gateway security rules, mock IdP, and notification deduplication tests pass with 100% success:

```powershell
mvn test
```

**Expected Output:**
```
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0 -- in com.ewb.gateway.SecurityRbacTest & AuthControllerTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0 -- in com.ewb.notification.NotificationServiceTest & NotificationControllerTest
[INFO] BUILD SUCCESS
```

> 📸 **SCREENSHOT #1: Maven Automated Test Suite Success**  
> - **Target:** Terminal / PowerShell  
> - **What to capture:** Terminal output displaying `Reactor Summary` with all 9 modules SUCCESS and 0 test failures.  
> 
> ```
> [ PASTE SCREENSHOT 1 HERE: Maven Test Suite Success ]
> ```

---

### Step 2: Spring Cloud Eureka Service Discovery Registry

Start Eureka discovery server on port 8761 and inspect the web console in your browser:

```powershell
mvn spring-boot:run -pl eureka-server
```
Open Browser: **`http://localhost:8761`**

**Expected Output:** Spring Cloud Eureka dashboard showing System Status and Registered Instances.

> 📸 **SCREENSHOT #2: Eureka Discovery Dashboard**  
> - **Target:** Browser window at `http://localhost:8761`  
> - **What to capture:** Eureka web dashboard displaying active server status and registered microservices.  
> 
> ```
> [ PASTE SCREENSHOT 2 HERE: Eureka Dashboard ]
> ```

---

### Step 3: Spring Cloud Centralized Config Server

Verify that `config-server` on port 8888 is serving centralized YAML configuration profiles natively from `config-repo/`:

```powershell
Invoke-RestMethod -Uri "http://localhost:8888/gateway-service/default" | ConvertTo-Json -Depth 4
```

**Expected Output:** JSON payload containing `propertySources` with `ewb.routes` mappings.

> 📸 **SCREENSHOT #3: Config Server Native Profile Distribution**  
> - **Target:** Browser or Terminal at `http://localhost:8888/gateway-service/default`  
> - **What to capture:** JSON response confirming config-server is serving `gateway-service.yml` configuration.  
> 
> ```
> [ PASTE SCREENSHOT 3 HERE: Config Server Response ]
> ```

---

### Step 4: Mock Identity Provider Login & JWT Generation

Submit a POST request to `/auth/login` with username `asuna` to verify JWT generation matching contracts.md Section 2:

```powershell
$body = @{ username = "asuna" } | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8080/auth/login" -Method Post -ContentType "application/json" -Body $body
```

**Expected JSON Response (`200 OK`):**
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

> 📸 **SCREENSHOT #4: Mock IdP Login for Asuna (ROLE_CUSTOMER)**  
> - **Target:** Postman or Terminal: `POST http://localhost:8080/auth/login`  
> - **What to capture:** HTTP 200 response displaying signed JWT token and account assignments for Asuna Yuuki.  
> 
> ```
> [ PASTE SCREENSHOT 4 HERE: Mock IdP Login ]
> ```

---

### Step 5: Unknown User Login Rejection (401 Unauthorized)

Attempt to authenticate with an unlisted persona to ensure unauthorized credentials are rejected:

```powershell
$body = @{ username = "unknown_hacker" } | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8080/auth/login" -Method Post -ContentType "application/json" -Body $body
```

**Expected JSON Response (`401 Unauthorized`):**
```json
{
  "status": "UNAUTHORIZED",
  "message": "Unknown persona: unknown_hacker"
}
```

> 📸 **SCREENSHOT #5: Mock IdP Rejection of Unknown Persona (401 Unauthorized)**  
> - **Target:** Postman or Terminal: `POST http://localhost:8080/auth/login`  
> - **What to capture:** HTTP 401 Unauthorized error response confirming perimeter authentication enforcement.  
> 
> ```
> [ PASTE SCREENSHOT 5 HERE: Mock IdP 401 Unauthorized ]
> ```

---

### Step 6: Gateway Security - Direct Internal Endpoint Block

Attempt to call an internal inter-service endpoint (`/internal/**`) through the external API Gateway:

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/internal/standing-orders/due" -Method Get
```

**Expected JSON Response (`403 Forbidden`):**
```json
{
  "status": "FORBIDDEN",
  "message": "Direct access to internal endpoints is forbidden via Gateway"
}
```

> 📸 **SCREENSHOT #6: Perimeter Block of External /internal/** Access (403 Forbidden)**  
> - **Target:** Postman or Terminal: `GET http://localhost:8080/internal/standing-orders/due`  
> - **What to capture:** HTTP 403 Forbidden response proving the Gateway strictly protects internal service routes.  
> 
> ```
> [ PASTE SCREENSHOT 6 HERE: Internal Block 403 ]
> ```

---

### Step 7: Gateway Security - Account Ownership Validation

Asuna attempts to create a standing order specifying Kirito's account (`EWB-KIR-5001`) as the source account:

```powershell
$hijackOrder = @{
    sourceAccountId = "EWB-KIR-5001"
    destinationAccountId = "EWB-ASU-2001"
    amount = 5000.00
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/standing-orders" -Method Post -ContentType "application/json" `
    -Headers @{ Authorization = "Bearer $asunaToken" } -Body $hijackOrder
```

**Expected JSON Response (`403 Forbidden`):**
```json
{
  "status": "FORBIDDEN",
  "message": "Source account EWB-KIR-5001 does not belong to authenticated customer"
}
```

> 📸 **SCREENSHOT #7: Source Account Hijacking Rejection (403 Forbidden)**  
> - **Target:** Postman or Terminal: `POST http://localhost:8080/standing-orders`  
> - **What to capture:** HTTP 403 Forbidden response proving the Gateway checks sourceAccountId against JWT accounts.  
> 
> ```
> [ PASTE SCREENSHOT 7 HERE: Account Ownership 403 ]
> ```

---

### Step 8: Gateway Security - Auditor Read-Only Enforcement

Sinon (`ROLE_AUDITOR`) attempts to perform a state-modifying POST request:

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/standing-orders" -Method Post -ContentType "application/json" `
    -Headers @{ Authorization = "Bearer $sinonToken" } -Body $hijackOrder
```

**Expected JSON Response (`403 Forbidden`):**
```json
{
  "status": "FORBIDDEN",
  "message": "Auditor role has read-only access"
}
```

> 📸 **SCREENSHOT #8: Auditor Read-Only Restriction Enforced (403 Forbidden)**  
> - **Target:** Postman or Terminal: `POST http://localhost:8080/standing-orders`  
> - **What to capture:** HTTP 403 Forbidden response showing Auditor cannot perform mutating HTTP operations.  
> 
> ```
> [ PASTE SCREENSHOT 8 HERE: Auditor Read-Only 403 ]
> ```

---

### Step 9: Notification Consumer - Outbox Event Ingestion

Submit a new execution outbox event to `POST /notifications/events` matching contracts.md Section 6:

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
    timestamp = "2026-10-25T01:00:03Z"
} | ConvertTo-Json

Invoke-WebRequest -Uri "http://localhost:8084/notifications/events" -Method Post -ContentType "application/json" -Body $event
```

**Expected JSON Response (`202 Accepted`):**
```json
{
  "status": "DELIVERED",
  "deliveryId": "notif-501"
}
```

> 📸 **SCREENSHOT #9: Notification Event Ingestion Success (202 Accepted)**  
> - **Target:** Postman or Terminal: `POST http://localhost:8084/notifications/events`  
> - **What to capture:** HTTP 202 Accepted response showing successful notification processing and delivery tracking.  
> 
> ```
> [ PASTE SCREENSHOT 9 HERE: Notification Ingestion 202 Accepted ]
> ```

---

### Step 10: Notification Deduplication - Duplicate Event Handling

Re-submit the exact same event payload with `eventId = "evt-doc-1001"` to verify idempotent deduplication:

```powershell
Invoke-WebRequest -Uri "http://localhost:8084/notifications/events" -Method Post -ContentType "application/json" -Body $event
```

**Expected JSON Response (`200 OK`):**
```json
{
  "status": "SKIPPED",
  "message": "Duplicate event ID"
}
```

> 📸 **SCREENSHOT #10: Notification Deduplication Skipping Duplicate Event (200 OK SKIPPED)**  
> - **Target:** Postman or Terminal: `POST http://localhost:8084/notifications/events`  
> - **What to capture:** HTTP 200 OK response with status SKIPPED confirming duplicate events are not re-delivered.  
> 
> ```
> [ PASTE SCREENSHOT 10 HERE: Notification Deduplication 200 OK ]
> ```

---

### Step 11: Notification Outage Simulation

Submit an event with the `X-Simulate-Outage: true` header to simulate downstream notification gateway failure:

```powershell
Invoke-WebRequest -Uri "http://localhost:8084/notifications/events" -Method Post -ContentType "application/json" `
    -Headers @{ "X-Simulate-Outage" = "true" } -Body $event
```

**Expected JSON Response (`500 Internal Server Error`):**
```json
{
  "status": "FAILED",
  "message": "Simulated notification delivery gateway outage"
}
```

> 📸 **SCREENSHOT #11: Notification Outage Simulation Clean Failure (500 Error)**  
> - **Target:** Postman or Terminal: `POST http://localhost:8084/notifications/events`  
> - **What to capture:** HTTP 500 response confirming downstream notification failure simulation triggers cleanly.  
> 
> ```
> [ PASTE SCREENSHOT 11 HERE: Outage Simulation 500 Error ]
> ```

---

### Step 12: Automated PowerShell Live Demo Runner

Execute the automated live demonstration runner script in PowerShell:

```powershell
.\scripts\run-demo.ps1
```

**Expected Output:** Formatted color terminal output displaying all test scenarios passing with `[PASS]` tags.

> 📸 **SCREENSHOT #12: Automated Demonstration Runner (run-demo.ps1)**  
> - **Target:** PowerShell Terminal: `.\scripts\run-demo.ps1`  
> - **What to capture:** Green terminal output displaying all contracts and resilience test scenarios passing.  
> 
> ```
> [ PASTE SCREENSHOT 12 HERE: Automated Demo Script ]
> ```

---

### Step 13: Postman Collection Test Suite Execution

Import `postman/ewb-standing-order.postman_collection.json` into Postman and execute the Collection Runner.

**Expected Output:** All 15 requests in the Postman collection executing and passing with 100% green tests.

> 📸 **SCREENSHOT #13: Postman Collection Runner Execution (100% Passed)**  
> - **Target:** Postman App: Collection Runner  
> - **What to capture:** Postman runner view displaying all requests across Auth, Orders, Transfers, and Notifications passing.  
> 
> ```
> [ PASTE SCREENSHOT 13 HERE: Postman Collection Runner ]
> ```
