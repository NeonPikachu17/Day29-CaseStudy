# EastWest Bank (EWB) Standing Order Platform: Technical & Verification Documentation

> **Role:** Member 4 (Platform Architect, Gateway/Security Engineer & Acceptance Lead)  
> **Target Modules:** `common/`, `config-server/`, `eureka-server/`, `gateway-service/`, `notification-service/`  
> **Global Contracts:** [`contracts.md`](../contracts.md)  
> **Primary Submission Document:** [`docs/Bernabe-Docs.docx`](./Bernabe-Docs.docx)  
> **Architecture Reference:** [`docs/ARCHITECTURE.md`](./ARCHITECTURE.md)

---

## 1. Executive Summary & Microservices Topology

The **EastWest Bank (EWB) Standing Order Platform** is an enterprise-grade distributed banking system built on **Java 21 LTS**, **Spring Boot 3.3.4**, and **Spring Cloud 2023.0.3**. It coordinates recurring scheduled bank transfers, guarantees strict idempotency against Core Banking systems, prevents duplicate debits, and provides asynchronous, deduplicated notification delivery.

Member 4 oversees foundational platform infrastructure, perimeter security, reverse proxy routing, asynchronous notification ingestion, containerized orchestration, and end-to-end integration acceptance.

### Microservices Port Allocation & Responsibility Matrix

| Service Name | Port | Database | Primary Responsibility | Owner |
| :--- | :---: | :--- | :--- | :---: |
| **`config-server`** | `8888` | Native YAML (`config-repo/`) | Centralized dynamic configuration repository across environments | Member 4 |
| **`eureka-server`** | `8761` | In-Memory Registry | Netflix Eureka service discovery & heartbeat health monitoring | Member 4 |
| **`gateway-service`** | `8080` | Stateless (Mock IdP) | Reverse proxy routing, Mock IdP (`/auth/login`), JWT signing, RBAC, header forwarding | Member 4 |
| **`standing-order-service`** | `8081` | `standing_order_db` (H2) | Standing order lifecycle (create, pause, resume, cancel), versioning, schedule calendar | Member 1 |
| **`execution-service`** | `8082` | `execution_db` (H2) | Due instruction discovery, worker lease claiming, cut-off checks, transactional outbox | Member 3 |
| **`payment-service`** | `8083` | `payment_db` (H2) | Mock Core Banking: double-entry ledger, balance checks, frozen account checks, idempotency | Member 2 |
| **`notification-service`** | `8084` | `notification_db` (H2) | Asynchronous outbox event consumer, event-ID deduplication, delivery tracking | Member 4 |

---

## 2. Authentication Model & System Personas

Perimeter security is centralized inside `gateway-service`. The gateway hosts an embedded **Mock Identity Provider (IdP)** accessible via `POST /auth/login`. Clients present a persona username and receive a cryptographically signed HMAC-SHA256 JWT containing identity, role, and authorized account IDs.

### Supported Test Personas (contracts.md Section 2)

| Username | Full Name | System Role | Primary Account ID | Initial Balance & Status |
| :--- | :--- | :--- | :--- | :--- |
| `asuna` | Asuna Yuuki | `ROLE_CUSTOMER` | `EWB-ASU-1001` | ₱20,000.00 (Active) |
| `kirito` | Kazuto Kirigaya | `ROLE_CUSTOMER` | `EWB-KIR-5001` | ₱50,000.00 (Active) |
| `klein` | Ryoutarou Tsuboi | `ROLE_CUSTOMER` | `EWB-KLN-4001` | ₱2,000.00 (Low Balance) |
| `heathcliff` | Akihiko Kayaba | `ROLE_CUSTOMER` | `EWB-HTH-3001` | ₱10,000.00 (FROZEN) |
| `agil` | Andrew Gilbert Mills | `ROLE_OPERATIONS` | N/A | Operations Officer (Audit / Management) |
| `sinon` | Shino Asada | `ROLE_AUDITOR` | N/A | Read-Only Auditor (Mutation Prohibited) |

### Downstream Injected Security Headers

Downstream microservices (`:8081`, `:8082`, `:8083`, `:8084`) are decoupled from direct JWT parsing. The gateway's `ReverseProxyFilter` validates the JWT token, extracts claims, and injects trusted identity headers:
- `X-User-Id`: Authenticated username (e.g. `asuna`).
- `X-User-Role`: Authorized system role (e.g. `ROLE_CUSTOMER`, `ROLE_OPERATIONS`, `ROLE_AUDITOR`).
- `X-User-Accounts`: Comma-delimited list of owned account IDs (e.g. `EWB-ASU-1001,EWB-ASU-2001`).

---

## 3. End-to-End Verification & Screenshot Guide

This section outlines the 13 verification steps corresponding directly to the screenshot submissions in [`docs/Bernabe-Docs.docx`](./Bernabe-Docs.docx). Each step describes the underlying architectural concept, the exact execution command, the internal handling mechanism, expected output, and verification criteria.

---

### Step 1: Automated Unit & Integration Test Suite

* **Architectural Purpose:** Ensures zero code regressions, verifies Maven module dependencies, and proves that core business rules (JWT generation, RBAC rules, deduplication stores) pass before deployment.
* **Under-the-Hood Mechanism:** Maven executes Surefire test runners across all modules. `gateway-service` runs `SecurityRbacTest` and `AuthControllerTest`; `notification-service` runs `NotificationServiceTest` and `NotificationControllerTest`.
* **Execution Command:**
  ```powershell
  mvn test
  ```
* **Expected Result:** `BUILD SUCCESS` across all 9 reactor modules with 0 test failures (15/15 tests passing).

> 📸 **SCREENSHOT #1: Maven Automated Test Suite Success**  
> - **Target:** Terminal / PowerShell  
> - **Verification Item:** `Reactor Summary` displaying `SUCCESS` for all 9 modules and `[INFO] BUILD SUCCESS`.  
> 
> ```
> [ PASTE SCREENSHOT 1 HERE: Maven Test Suite Success ]
> ```

---

### Step 2: Spring Cloud Eureka Service Discovery Registry

* **Architectural Purpose:** Microservices register their dynamic network locations (IP and port) with Eureka, eliminating hardcoded URLs and enabling dynamic lookup and client-side load balancing.
* **Under-the-Hood Mechanism:** Each service runs `spring-cloud-starter-netflix-eureka-client`, sending periodic heartbeats (10s renewal interval, 30s lease expiry) to the Eureka server at `:8761`.
* **Execution Command:**
  ```powershell
  # Terminal 1: Start Eureka Server
  mvn spring-boot:run -pl eureka-server
  ```
  Open Browser: **`http://localhost:8761`**
* **Expected Result:** Eureka dashboard renders showing System Status and the `Instances currently registered with Eureka` table.

> 📸 **SCREENSHOT #2: Eureka Discovery Dashboard**  
> - **Target:** Browser at `http://localhost:8761`  
> - **Verification Item:** Active Eureka console showing running system status and registered instances (`GATEWAY-SERVICE`, `STANDING-ORDER-SERVICE`, `EXECUTION-SERVICE`, `PAYMENT-SERVICE`, `NOTIFICATION-SERVICE`).  
> 
> ```
> [ PASTE SCREENSHOT 2 HERE: Eureka Dashboard ]
> ```

---

### Step 3: Spring Cloud Centralized Config Server

* **Architectural Purpose:** Centralizes configuration management across all microservices. Services pull environment-specific parameters from `config-repo/` at startup, ensuring configuration changes do not require code rebuilds.
* **Under-the-Hood Mechanism:** `config-server` on port 8888 operates with Spring Cloud native profile (`spring.profiles.active=native`), reading YAML files from `config-repo/` and serving them as JSON property sources.
* **Execution Command:**
  ```powershell
  Invoke-RestMethod -Uri "http://localhost:8888/gateway-service/default" -Method Get | ConvertTo-Json -Depth 5
  ```
* **Expected Result (`HTTP 200 OK`):** JSON response containing property sources for `gateway-service.yml` and `application.yml`, including route mappings (`ewb.routes.*`).

> 📸 **SCREENSHOT #3: Config Server Native Profile Distribution**  
> - **Target:** Browser or Terminal at `http://localhost:8888/gateway-service/default`  
> - **Verification Item:** JSON output confirming `config-server` delivers `gateway-service.yml` and route definitions.  
> 
> ```
> [ PASTE SCREENSHOT 3 HERE: Config Server Response ]
> ```

---

### Step 4: Mock Identity Provider Login & JWT Generation

* **Architectural Purpose:** Validates the authentication boundary. Legitimate users authenticate and receive a signed JWT token containing their claims, preventing downstream services from needing to query identity databases.
* **Under-the-Hood Mechanism:** `AuthController` validates the username against the mock persona registry, invokes `JwtUtil` to sign an HMAC-SHA256 token with embedded claims (`sub`, `role`, `accountIds`), and returns the bearer token.
* **Execution Command:**
  ```powershell
  $body = @{ username = "asuna" } | ConvertTo-Json
  $auth = Invoke-RestMethod -Uri "http://localhost:8080/auth/login" -Method Post -ContentType "application/json" -Body $body
  $auth | Format-List
  ```
* **Expected Result (`HTTP 200 OK`):** JSON response with a signed JWT token, `fullName: "Asuna Yuuki"`, `role: "ROLE_CUSTOMER"`, and account array `["EWB-ASU-1001", "EWB-ASU-2001"]`.

> 📸 **SCREENSHOT #4: Mock IdP Login for Asuna (ROLE_CUSTOMER)**  
> - **Target:** Postman or Terminal: `POST http://localhost:8080/auth/login`  
> - **Verification Item:** HTTP 200 OK containing valid JWT token, persona profile, and account list.  
> 
> ```
> [ PASTE SCREENSHOT 4 HERE: Mock IdP Login ]
> ```

---

### Step 5: Unknown User Login Rejection (401 Unauthorized)

* **Architectural Purpose:** Verifies perimeter defense against unauthorized access and credential brute-forcing. Only recognized personas in the mock registry are issued tokens.
* **Under-the-Hood Mechanism:** `AuthController` detects an unmapped username, halts token issuance, and responds with HTTP 401 Unauthorized and an informative error payload.
* **Execution Command:**
  ```powershell
  $unknownBody = @{ username = "unknown_hacker" } | ConvertTo-Json
  Invoke-RestMethod -Uri "http://localhost:8080/auth/login" -Method Post -ContentType "application/json" -Body $unknownBody
  ```
* **Expected Result (`HTTP 401 Unauthorized`):**
  ```json
  {
    "status": "UNAUTHORIZED",
    "message": "Unknown persona: unknown_hacker"
  }
  ```

> 📸 **SCREENSHOT #5: Mock IdP Rejection of Unknown Persona (401 Unauthorized)**  
> - **Target:** Postman or Terminal: `POST http://localhost:8080/auth/login`  
> - **Verification Item:** HTTP 401 Unauthorized status and rejection message confirming perimeter authentication integrity.  
> 
> ```
> [ PASTE SCREENSHOT 5 HERE: Mock IdP 401 Unauthorized ]
> ```

---

### Step 6: Gateway Security - Direct Internal Endpoint Block

* **Architectural Purpose:** Enforces network perimeter isolation. Internal inter-service endpoints (such as batch polling routes at `/internal/**`) must never be accessible to external consumers or end-users.
* **Under-the-Hood Mechanism:** `ReverseProxyFilter` intercepts incoming requests matching `/internal/**` or `/api/internal/**` and terminates the request immediately with HTTP 403 Forbidden before routing occurs.
* **Execution Command:**
  ```powershell
  Invoke-RestMethod -Uri "http://localhost:8080/internal/standing-orders/due" -Method Get
  ```
* **Expected Result (`HTTP 403 Forbidden`):**
  ```json
  {
    "status": "FORBIDDEN",
    "message": "Direct access to internal endpoints is forbidden via Gateway"
  }
  ```

> 📸 **SCREENSHOT #6: Perimeter Block of External /internal/** Access (403 Forbidden)**  
> - **Target:** Postman or Terminal: `GET http://localhost:8080/internal/standing-orders/due`  
> - **Verification Item:** HTTP 403 Forbidden response proving internal endpoints are shielded from public access.  
> 
> ```
> [ PASTE SCREENSHOT 6 HERE: Internal Block 403 ]
> ```

---

### Step 7: Gateway Security - Account Ownership Validation

* **Architectural Purpose:** Prevents Insecure Direct Object References (IDOR) and account hijacking. A customer cannot initiate standing orders or debit transactions using another customer's bank account.
* **Under-the-Hood Mechanism:** When processing order creation requests, `ReverseProxyFilter` inspects the request payload `sourceAccountId` and checks it against the authenticated user's `accountIds` in the JWT claims. Mismatches are rejected with HTTP 403 Forbidden.
* **Execution Command:**
  ```powershell
  $asunaToken = $auth.token
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
      -Headers @{ Authorization = "Bearer $asunaToken" } -Body $hijackOrder
  ```
* **Expected Result (`HTTP 403 Forbidden`):**
  ```json
  {
    "status": "FORBIDDEN",
    "message": "Source account EWB-KIR-5001 does not belong to authenticated customer"
  }
  ```

> 📸 **SCREENSHOT #7: Source Account Hijacking Rejection (403 Forbidden)**  
> - **Target:** Postman or Terminal: `POST http://localhost:8080/standing-orders`  
> - **Verification Item:** HTTP 403 Forbidden response stating the source account does not belong to the authenticated customer.  
> 
> ```
> [ PASTE SCREENSHOT 7 HERE: Account Ownership 403 ]
> ```

---

### Step 8: Gateway Security - Auditor Read-Only Enforcement

* **Architectural Purpose:** Enforces the Principle of Least Privilege. Compliance and audit personas (`ROLE_AUDITOR`) must have read-only visibility into banking transactions and must never be permitted to create, mutate, or delete records.
* **Under-the-Hood Mechanism:** `ReverseProxyFilter` checks the HTTP method against the user's role. If `ROLE_AUDITOR` attempts any state-mutating verb (`POST`, `PUT`, `PATCH`, `DELETE`), the gateway aborts with HTTP 403 Forbidden.
* **Execution Command:**
  ```powershell
  $sinonAuth = Invoke-RestMethod -Uri "http://localhost:8080/auth/login" -Method Post -ContentType "application/json" -Body (@{ username = "sinon" } | ConvertTo-Json)
  
  Invoke-RestMethod -Uri "http://localhost:8080/standing-orders" -Method Post -ContentType "application/json" `
      -Headers @{ Authorization = "Bearer $($sinonAuth.token)" } -Body $hijackOrder
  ```
* **Expected Result (`HTTP 403 Forbidden`):**
  ```json
  {
    "status": "FORBIDDEN",
    "message": "Auditor role has read-only access"
  }
  ```

> 📸 **SCREENSHOT #8: Auditor Read-Only Restriction Enforced (403 Forbidden)**  
> - **Target:** Postman or Terminal: `POST http://localhost:8080/standing-orders`  
> - **Verification Item:** HTTP 403 Forbidden error response confirming that mutating requests from `ROLE_AUDITOR` are blocked.  
> 
> ```
> [ PASTE SCREENSHOT 8 HERE: Auditor Read-Only 403 ]
> ```

---

### Step 9: Notification Consumer - Outbox Event Ingestion

* **Architectural Purpose:** Validates asynchronous, decoupled communication. When the execution worker finishes a transfer, it records an outbox event that is ingested by `notification-service` without holding or blocking Core Banking ledger operations.
* **Under-the-Hood Mechanism:** `NotificationController` receives the event payload, generates a unique delivery ID, stores the notification in `notification_db`, records the `eventId` in the `processed_events` table, and returns HTTP 202 Accepted.
* **Execution Command:**
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
* **Expected Result (`HTTP 202 Accepted`):**
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

### Step 10: Notification Deduplication - Duplicate Event Handling

* **Architectural Purpose:** Prevents duplicate customer alerts (SMS/Email) in distributed environments where network retries, broker re-deliveries, or worker timeouts can resubmit identical event payloads.
* **Under-the-Hood Mechanism:** `NotificationService` checks `processed_events` for the incoming `eventId`. Because `eventId` is unique, existing records are detected immediately; the service skips dispatch and returns HTTP 200 OK (`status: SKIPPED`).
* **Execution Command:**
  ```powershell
  # Re-send identical event with same eventId = "evt-doc-1001"
  Invoke-WebRequest -Uri "http://localhost:8084/notifications/events" -Method Post -ContentType "application/json" -Body $event -UseBasicParsing
  ```
* **Expected Result (`HTTP 200 OK`):**
  ```json
  {
    "status": "SKIPPED",
    "message": "Duplicate event ID"
  }
  ```

> 📸 **SCREENSHOT #10: Notification Deduplication Skipping Duplicate Event (200 OK SKIPPED)**  
> - **Target:** Postman or Terminal: `POST http://localhost:8084/notifications/events`  
> - **Verification Item:** HTTP status 200 OK, status `SKIPPED`, and message `"Duplicate event ID"` confirming idempotency.  
> 
> ```
> [ PASTE SCREENSHOT 10 HERE: Notification Deduplication 200 OK ]
> ```

---

### Step 11: Notification Outage Simulation

* **Architectural Purpose:** Demonstrates fault domain isolation and non-blocking resilience. If a downstream notification gateway (e.g. third-party SMS or email provider) goes down, the failure is isolated and does **not** roll back or invalidate completed bank ledger transfers.
* **Under-the-Hood Mechanism:** When the header `X-Simulate-Outage: true` is present, `NotificationController` throws `NotificationOutageException`, triggering an `@ExceptionHandler` that returns HTTP 500 Internal Server Error cleanly without cascading errors.
* **Execution Command:**
  ```powershell
  Invoke-WebRequest -Uri "http://localhost:8084/notifications/events" -Method Post -ContentType "application/json" `
      -Headers @{ "X-Simulate-Outage" = "true" } -Body $event -UseBasicParsing
  ```
* **Expected Result (`HTTP 500 Internal Server Error`):**
  ```json
  {
    "status": "FAILED",
    "message": "Simulated notification delivery gateway outage"
  }
  ```

> 📸 **SCREENSHOT #11: Notification Outage Simulation Clean Failure (500 Error)**  
> - **Target:** Postman or Terminal: `POST http://localhost:8084/notifications/events`  
> - **Verification Item:** HTTP 500 status with `"Simulated notification delivery gateway outage"`, proving cleanly isolated error handling.  
> 
> ```
> [ PASTE SCREENSHOT 11 HERE: Outage Simulation 500 Error ]
> ```

---

### Step 12: Automated PowerShell Live Demo Runner

* **Architectural Purpose:** Provides a single, deterministic automated test script that exercises the entire platform lifecycle across authentication, RBAC, perimeter security, deduplication, and outage simulation in sequential order.
* **Under-the-Hood Mechanism:** `scripts/run-demo.ps1` executes sequential REST calls against `:8080` (Gateway) and `:8084` (Notification Service), evaluates returned HTTP status codes and JSON payloads, and outputs color-coded `[PASS]` / `[FAIL]` tags.
* **Execution Command:**
  ```powershell
  .\scripts\run-demo.ps1
  ```
* **Expected Result:** Clean execution completing in ~2 seconds with all scenarios passing:
  - Mock IdP Authentication (Asuna, Agil, Sinon, Stranger rejection)
  - Gateway RBAC & Security Perimeter Enforcement (`/internal/**` block, ownership check, auditor check)
  - Notification Service Ingestion, Deduplication, and Outage Simulation

> 📸 **SCREENSHOT #12: Automated Demonstration Runner (run-demo.ps1)**  
> - **Target:** PowerShell Terminal: `.\scripts\run-demo.ps1`  
> - **Verification Item:** Complete console output showing color-coded `[PASS]` tags for all scenarios and the final success banner.  
> 
> ```
> [ PASTE SCREENSHOT 12 HERE: Automated Demo Script ]
> ```

---

### Step 13: Postman Collection Test Suite Execution

* **Architectural Purpose:** Provides a standardized API testing suite for QA and cross-team verification, ensuring all contract endpoints adhere to expected status codes, JSON schemas, and response latencies.
* **Under-the-Hood Mechanism:** Postman Collection Runner executes the 15 bundled requests in `postman/ewb-standing-order.postman_collection.json`, executing pre-request scripts and automated JavaScript assertions.
* **Execution Steps:**
  1. Open Postman.
  2. Click **Import** $\rightarrow$ select `postman/ewb-standing-order.postman_collection.json`.
  3. Click **Run Collection**.
  4. Ensure all requests pass with 100% green tests.
* **Expected Result:** 15 requests executed with 0 failures across Auth, Security, Standing Orders, Transfers, and Notifications.

> 📸 **SCREENSHOT #13: Postman Collection Runner Execution (100% Passed)**  
> - **Target:** Postman App: Collection Runner  
> - **Verification Item:** Collection Runner summary displaying 100% green test assertions across all requests.  
> 
> ```
> [ PASTE SCREENSHOT 13 HERE: Postman Collection Runner ]
> ```

---

## 4. Acceptance Criteria & Deliverables Matrix

| Requirement | Contract Section | Verification Evidence | Status |
| :--- | :--- | :--- | :---: |
| **Clean Multi-Module Build** | Section 1 | `mvn test` passing across all 9 modules | `PASSED` |
| **Service Discovery Registry** | Architecture §2 | Eureka dashboard at `:8761` showing all instances `UP` | `PASSED` |
| **Centralized Config Server** | Architecture §2 | Native YAML profile delivery from `:8888` | `PASSED` |
| **Mock IdP Authentication** | Section 2 | HMAC-SHA256 JWT generation for all personas | `PASSED` |
| **Unknown Persona Rejection** | Section 2 | Rejection with HTTP 401 Unauthorized | `PASSED` |
| **Internal Endpoint Protection** | Section 2 & 4 | Direct external access to `/internal/**` returns 403 | `PASSED` |
| **Account Ownership Validation** | Section 2 & 4 | Unauthorized source account usage returns 403 | `PASSED` |
| **Auditor Read-Only Rule** | Section 2 | Mutation attempts by `ROLE_AUDITOR` return 403 | `PASSED` |
| **Decoupled Notification Consumer** | Section 6 | Ingestion of execution outbox events returns 202 | `PASSED` |
| **Event Deduplication (Idempotency)** | Section 6 | Duplicate event submission safely skipped (200 OK) | `PASSED` |
| **Fault Isolation (Outage Simulation)** | Section 6 | Simulated gateway outage returns 500 without ledger impact | `PASSED` |
| **Automated End-to-End Suite** | Deliverables | `run-demo.ps1` executes with 100% green PASS tags | `PASSED` |
| **Postman Test Suite** | Deliverables | Complete test collection with assertions provided | `PASSED` |
