# EWB Standing Order Processor: System Contracts & API Specifications

This document serves as the **contract-first single source of truth** for all 4 team members. By adhering strictly to these request/response payloads, status codes, and headers, each member can develop, unit-test, and mock their respective services in complete isolation without being blocked by teammates.

---

## 1. Common Enums & Constants

These definitions live in the `common` module (`com.ewb.common.model`):

```java
public enum Frequency {
    MONTHLY
}

public enum OrderStatus {
    ACTIVE,
    PAUSED,
    CANCELLED
}

public enum ExecutionStatus {
    PENDING,
    CLAIMED,
    SUCCESS,
    FAILED,
    CANCELLED,
    UNRESOLVED
}

public enum EntryType {
    DEBIT,
    CREDIT
}

public enum TransferStatus {
    COMPLETED,
    FAILED
}

public enum NotificationStatus {
    DELIVERED,
    FAILED,
    SKIPPED
}
```

---

## 2. Authentication & Identity Contracts (Gateway & Mock IdP)

* **Port:** `8080` (Gateway)
* **Auth Endpoint:** `POST /auth/login`

### Request Payload:
```json
{
  "username": "asuna"
}
```

### Supported Personas & Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "username": "asuna",
  "fullName": "Asuna Yuuki",
  "role": "ROLE_CUSTOMER",
  "accountIds": ["EWB-ASU-1001", "EWB-ASU-2001"]
}
```

| Username | Name | Role | Primary Account |
| :--- | :--- | :--- | :--- |
| `asuna` | Asuna Yuuki | `ROLE_CUSTOMER` | `EWB-ASU-1001` (₱20,000.00) |
| `kirito` | Kazuto Kirigaya | `ROLE_CUSTOMER` | `EWB-KIR-5001` (₱50,000.00) |
| `klein` | Ryoutarou Tsuboi | `ROLE_CUSTOMER` | `EWB-KLN-4001` (₱2,000.00 - Low Balance) |
| `heathcliff` | Akihiko Kayaba | `ROLE_CUSTOMER` | `EWB-HTH-3001` (₱10,000.00 - FROZEN) |
| `agil` | Andrew Gilbert Mills | `ROLE_OPERATIONS` | N/A |
| `sinon` | Shino Asada | `ROLE_AUDITOR` | N/A |

### Downstream Forwarded Headers (Gateway to Microservices):
The Gateway validates the JWT and passes user context down to internal microservices:
* `X-User-Id`: e.g. `asuna`
* `X-User-Role`: e.g. `ROLE_CUSTOMER`
* `X-User-Accounts`: e.g. `EWB-ASU-1001,EWB-ASU-2001`

*(Note for local isolated testing: Services must honor these headers if present so you don't even need the Gateway running to test local controllers).*

---

## 3. Standing Order Service Contracts (Port 8081)

### 3.1. Create Standing Order
* **Endpoint:** `POST /standing-orders`
* **RBAC:** `ROLE_CUSTOMER` (sourceAccountId must belong to authenticated user)

#### Request:
```json
{
  "sourceAccountId": "EWB-ASU-1001",
  "destinationAccountId": "EWB-ASU-2001",
  "amount": 5000.00,
  "currency": "PHP",
  "frequency": "MONTHLY",
  "dayOfMonth": 25,
  "executionTime": "09:00",
  "timeZone": "Asia/Manila",
  "startDate": "2026-10-25"
}
```

#### Response: `201 Created`
```json
{
  "id": "so-9001",
  "customerId": "asuna",
  "sourceAccountId": "EWB-ASU-1001",
  "destinationAccountId": "EWB-ASU-2001",
  "amount": 5000.00,
  "currency": "PHP",
  "frequency": "MONTHLY",
  "dayOfMonth": 25,
  "executionTime": "09:00",
  "timeZone": "Asia/Manila",
  "startDate": "2026-10-25",
  "nextOccurrence": "2026-10-25T09:00:00+08:00",
  "status": "ACTIVE",
  "version": 1,
  "createdAt": "2026-09-18T08:00:00Z"
}
```

### 3.2. List Customer Standing Orders
* **Endpoint:** `GET /standing-orders`
* **RBAC:** `ROLE_CUSTOMER` (returns only authenticated customer's orders)
* **Response:** `200 OK` (Array of standing order objects)

### 3.3. Lifecycle Operations
* `POST /standing-orders/{id}/pause` $\rightarrow$ `200 OK` (`status: "PAUSED"`)
* `POST /standing-orders/{id}/resume` $\rightarrow$ `200 OK` (`status: "ACTIVE"`)
* `POST /standing-orders/{id}/cancel` $\rightarrow$ `200 OK` (`status: "CANCELLED"`)
* `PATCH /standing-orders/{id}`:
  * Request: `{"amount": 6000.00}` or `{"dayOfMonth": 28}`
  * Response: `200 OK` (`version: 2`, creates version record & audit log)

### 3.4. Internal Query for Execution Service
* **Endpoint:** `GET /internal/standing-orders/due?cutoff={isoTimestamp}`
* **Response:** `200 OK`
```json
[
  {
    "standingOrderId": "so-9001",
    "customerId": "asuna",
    "sourceAccountId": "EWB-ASU-1001",
    "destinationAccountId": "EWB-ASU-2001",
    "amount": 5000.00,
    "currency": "PHP",
    "scheduledOccurrenceUtc": "2026-10-25T01:00:00Z",
    "version": 1,
    "status": "ACTIVE"
  }
]
```

---

## 4. Payment Service Contracts (Port 8083 - Mock Core Banking)

### 4.1. Execute Idempotent Transfer
* **Endpoint:** `POST /transfers`
* **Idempotency Key Header:** `Idempotency-Key: {standingOrderId}:{scheduledOccurrenceUtc}` (e.g. `so-9001:2026-10-25T01:00:00Z`)
* **Simulation Header (Optional):** `X-Simulate-Timeout: true` (commits ledger, but returns HTTP 504)

#### Request:
```json
{
  "reference": "TRF-20261025-001",
  "sourceAccountId": "EWB-ASU-1001",
  "destinationAccountId": "EWB-ASU-2001",
  "amount": 5000.00,
  "currency": "PHP"
}
```

#### Success Response: `200 OK`
```json
{
  "reference": "TRF-20261025-001",
  "idempotencyKey": "so-9001:2026-10-25T01:00:00Z",
  "status": "COMPLETED",
  "sourceAccountId": "EWB-ASU-1001",
  "destinationAccountId": "EWB-ASU-2001",
  "amount": 5000.00,
  "sourceBalanceAfter": 15000.00,
  "destinationBalanceAfter": 6000.00,
  "debitLedgerId": "LED-D-101",
  "creditLedgerId": "LED-C-102",
  "timestamp": "2026-10-25T01:00:02Z"
}
```

#### Failure Responses:
* **Insufficient Funds (`422 Unprocessable Entity`):**
  ```json
  {"status": "FAILED", "errorCode": "INSUFFICIENT_FUNDS", "message": "Account balance 2000.00 is less than 5000.00"}
  ```
* **Frozen Account (`422 Unprocessable Entity`):**
  ```json
  {"status": "FAILED", "errorCode": "ACCOUNT_FROZEN", "message": "Source account EWB-HTH-3001 is frozen"}
  ```
* **Idempotency Conflict (`409 Conflict`):**
  ```json
  {"status": "FAILED", "errorCode": "IDEMPOTENCY_MISMATCH", "message": "Idempotency key reused with mismatched transfer parameters"}
  ```

### 4.2. Resolve Uncertain Outcome by Reference
* **Endpoint:** `GET /transfers/by-reference/{reference}`
* **Response:** `200 OK` (returns original transfer payload) or `404 Not Found`

---

## 5. Execution Service Contracts (Port 8082)

### 5.1. View Execution History
* **Endpoint:** `GET /standing-orders/{id}/executions`
* **RBAC:** `ROLE_CUSTOMER` (own orders), `ROLE_OPERATIONS`, `ROLE_AUDITOR`
* **Response:** `200 OK`
```json
[
  {
    "executionId": "exec-101",
    "standingOrderId": "so-9001",
    "scheduledOccurrenceUtc": "2026-10-25T01:00:00Z",
    "instructionVersion": 1,
    "status": "SUCCESS",
    "paymentReference": "TRF-20261025-001",
    "attemptsCount": 1,
    "createdAt": "2026-10-25T01:00:00Z",
    "completedAt": "2026-10-25T01:00:02Z"
  }
]
```

### 5.2. Operations Manual Recovery Trigger
* **Endpoint:** `POST /internal/executions/{id}/recover`
* **RBAC:** `ROLE_OPERATIONS`
* **Response:** `200 OK` (`{"status": "RECOVERED", ...}`)

---

## 6. Outbox & Notification Event Contracts (Port 8084)

### 6.1. Outbox Event Message Schema
Dispatched by Execution Service (`POST /notifications/events` or message broker):

```json
{
  "eventId": "evt-778899",
  "executionId": "exec-101",
  "standingOrderId": "so-9001",
  "customerId": "asuna",
  "eventType": "EXECUTION_COMPLETED",
  "status": "SUCCESS",
  "amount": 5000.00,
  "currency": "PHP",
  "sourceAccountId": "EWB-ASU-1001",
  "destinationAccountId": "EWB-ASU-2001",
  "paymentReference": "TRF-20261025-001",
  "errorMessage": null,
  "timestamp": "2026-10-25T01:00:03Z"
}
```

### 6.2. Ingest Notification Event
* **Endpoint:** `POST /notifications/events`
* **Simulation Header (Optional):** `X-Simulate-Outage: true` (simulates notification gateway failure, returns `500 Internal Server Error`)
* **Behavior:** Checks `processed_events` table by `eventId`. If already seen, returns `200 OK` (`{"status": "SKIPPED", "message": "Duplicate event ID"}`).
* **Success Response:** `202 Accepted` (`{"status": "DELIVERED", "deliveryId": "notif-501"}`)

---

## 7. Zero-Blocker Local Development Rules

1. **Direct Port Access:** During local development, call services directly on ports 8081, 8082, 8083, 8084. Pass `X-User-Id` and `X-User-Role` headers to bypass Gateway JWT verification.
2. **WireMock / Mock Handlers:** Member 3 can use a 20-line mock HTTP stub returning Section 4 responses to develop the entire Execution Service before Member 2 finishes the Payment Service.
3. **Seed Data Guarantee:** Member 2 guarantees that test accounts (`EWB-ASU-1001`, `EWB-KLN-4001`, `EWB-HTH-3001`) are automatically seeded into `payment_db` on startup via `DataSeeder.java`.
