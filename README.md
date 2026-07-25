# HardwareHub

Modern cloud-based Hardware Store Management Platform for SME hardware stores in the Philippines.


**Current milestone:** Milestone 5 — Sales / POS & receipts

## Stack

| Layer | Technology |
| --- | --- |
| Backend | Java 21, Spring Boot 3.4, Spring Security, JPA, Flyway, MapStruct |
| Frontend | Angular 21 (LTS), Angular Material, RxJS, Signals |
| Database | PostgreSQL 16 |
| Local DevOps | Docker Compose, Nginx (frontend image) |

## Project layout

```
hardwarehub/
  backend/           Spring Boot modular monolith
  frontend/          Angular web application
  database/          DB notes (Flyway lives in backend)
  docs/              SRS and design docs
  docker-compose.yml Postgres + backend + frontend
```

## Prerequisites

- JDK 21+ (project targets Java 21)
- Maven 3.9+
- Node.js 22+ and npm
- Docker Desktop (recommended for PostgreSQL / full stack)

## Quick start (Docker Compose)

```bash
docker compose up --build
```

| Service | URL |
| --- | --- |
| Web UI | http://localhost:8088 |
| API | http://localhost:8080/api/v1 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Health | http://localhost:8080/actuator/health |

### Default owner login

| Field | Value |
| --- | --- |
| Username | `owner` |
| Password | `Owner@123` |
| Email | `owner@hardwarehub.local` |

Change this password immediately after first login in non-dev environments.

## Local development (without full Compose)

### 1. Start PostgreSQL only

```bash
docker compose up -d postgres
```

### 2. Backend

```bash
cd backend
set JAVA_HOME=C:\Program Files\Java\jdk-24
mvn spring-boot:run
```

### Backend without Docker (H2 smoke profile)

If Docker/PostgreSQL is unavailable, you can smoke-test auth against in-memory H2:

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

Prefer PostgreSQL for normal development.

### 3. Frontend

```bash
cd frontend
npm start
```

Angular runs at http://localhost:4200 and calls the API at http://localhost:8080/api/v1.

## Auth API (Milestone 1)

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`
- `POST /api/v1/auth/forgot-password` (email stubbed; reset token logged server-side in M1)
- `POST /api/v1/auth/reset-password`
- `POST /api/v1/auth/change-password`

## Users API (OWNER / ADMIN)

- `GET/POST /api/v1/users`
- `GET/PUT /api/v1/users/{id}`
- `POST /api/v1/users/{id}/activate`
- `POST /api/v1/users/{id}/deactivate`
- `POST /api/v1/users/{id}/reset-password`
- `DELETE /api/v1/users/{id}` (soft delete)

## Inventory API (Milestone 4)

Authenticated users can read history and low-stock alerts. Writes require `OWNER`, `ADMIN`, `MANAGER`, or `INVENTORY_STAFF`.

- `GET /api/v1/inventory/transactions` — movement history (filters: `productId`, `type`, `search`)
- `POST /api/v1/inventory/transactions` — stock in / stock out / adjustment
- `GET /api/v1/inventory/low-stock` — products at or below minimum
- `GET /api/v1/inventory/summary` — dashboard counters

Stock in/out use a positive quantity delta. Adjustment sets the absolute stock level. Product `currentStock` is no longer overwritten on catalog edit.

## Sales API (Milestone 5)

Authenticated users can list/view receipts and summaries. Checkout requires `OWNER`, `ADMIN`, `MANAGER`, or `CASHIER`.

- `GET /api/v1/sales` — sale history (filters: `search`, `status`, `customerId`)
- `GET /api/v1/sales/summary` — today + month totals
- `GET /api/v1/sales/{id}` — receipt detail
- `GET /api/v1/sales/receipt/{receiptNumber}`
- `POST /api/v1/sales` — POS checkout (stock-out + optional credit balance)

Payment methods: `CASH`, `CARD`, `CREDIT`. Customer purchase history is populated from completed sales.

## Roles

`OWNER`, `ADMIN`, `MANAGER`, `CASHIER`, `INVENTORY_STAFF`

## Tests

```bash
cd backend
mvn test
```

## Architecture notes

- Modular monolith packages: `common`, `auth`, `user`, `catalog`, `customer`, `inventory`, `sales` (future: quotations/reports)
- DTOs only on the API boundary; JPA entities are never exposed
- JWT access tokens + rotating refresh tokens (SHA-256 hashed at rest)
- Soft delete + basic audit logging for auth/user/inventory/sales actions

## Next milestone

Milestone 6 — Quotations & PDF export
