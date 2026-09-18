# EastWest Bank (EWB) Standing Order Processor Microservices Platform

Enterprise-grade, resilient microservices system for EastWest Bank (EWB) built with **Java 21 LTS**, **Spring Boot 3.3.4**, and **Spring Cloud 2023.0.3**.

---

## 1. System Architecture & Module Breakdown

The platform is designed with a contract-first multi-module architecture:

```
day-29-neo/
├── pom.xml                                   # Parent POM (Spring Boot 3.3.4, Spring Cloud 2023.0.3, Java 21)
├── common/                                   # Shared DTOs, Enums, Security, and Money Utilities
├── config-repo/                              # Native centralized YAML repository for Config Server
├── config-server/                            # Spring Cloud Config Server (Port 8888)
├── eureka-server/                            # Netflix Eureka Discovery Registry (Port 8761)
├── gateway-service/                          # API Gateway & Mock IdP (Port 8080)
├── standing-order-service/                   # Standing Order Lifecycle & Schedules (Port 8081)
├── execution-service/                        # Execution Worker, Lease Locks & Outbox (Port 8082)
├── payment-service/                          # Mock Core Banking & Double-Entry Ledger (Port 8083)
├── notification-service/                     # Outbox Event Consumer & Deduplication (Port 8084)
├── contracts.md                              # Single Source of Truth API Contracts & Enums
└── implementation_plan.md                    # System Architecture & Acceptance Test Plan
```

### Team Responsibilities & Port Allocations

| Module | Port | Owner | Database | Purpose |
| :--- | :--- | :--- | :--- | :--- |
| **`config-server`** | `8888` | Member 4 | N/A | Centralized dynamic configuration |
| **`eureka-server`** | `8761` | Member 4 | N/A | Service discovery & registry |
| **`gateway-service`** | `8080` | Member 4 | N/A | Mock IdP (`POST /auth/login`), JWT validation & routing |
| **`standing-order-service`** | `8081` | Member 1 | `standing_order_db` | CRUD, schedule calculation, versioning, audit |
| **`execution-service`** | `8082` | Member 3 | `execution_db` | Due discovery, lease claiming, timeout recovery, outbox |
| **`payment-service`** | `8083` | Member 2 | `payment_db` | Double-entry ledger, balance check, idempotency store |
| **`notification-service`** | `8084` | Member 4 | `notification_db` | Event ingestion, event-ID deduplication, status tracking |
| **`common`** | Shared Lib | Member 4 | N/A | Shared Enums, DTOs, `JwtUtil`, `MoneyUtil`, `MaskingUtil` |

---

## 2. Prerequisites

- **Java Development Kit (JDK):** Version 21 LTS
- **Apache Maven:** Version 3.9+
- **Git**

---

## 3. Building the Project

Compile, test, and package all 8 modules from the root directory:

```bash
mvn clean install
```

To compile without running tests:
```bash
mvn clean install -DskipTests
```

To test a specific module (e.g. `common`):
```bash
mvn test -pl common
```

---

## 4. Local Development & Zero-Blocker Rules

Refer strictly to [`contracts.md`](./contracts.md) for all API payloads, HTTP status codes, and test personas.

1. **Independent Port Access:** Internal microservices (`8081`, `8082`, `8083`, `8084`) can be tested directly without the Gateway by providing `X-User-Id` and `X-User-Role` HTTP headers.
2. **Shared Models:** All DTOs (`TransferRequest`, `TransferResponse`, `CreateStandingOrderRequest`, `StandingOrderResponse`, `DueStandingOrderDto`, `NotificationEventDto`, `AuthRequest`, `AuthResponse`) and Enums (`Frequency`, `OrderStatus`, `ExecutionStatus`, `EntryType`, `TransferStatus`, `NotificationStatus`) are located in the `common` module.
3. **Dedicated In-Memory Databases:** Each service uses its own isolated H2 database instance (`standing_order_db`, `execution_db`, `payment_db`, `notification_db`) with H2 web console enabled at `/h2-console`.
