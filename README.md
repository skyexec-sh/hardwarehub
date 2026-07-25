# HardwareHub

Modern cloud-based Hardware Store Management Platform for SME hardware stores in the Philippines.


**Current milestone:** Milestone 8 — Fulfillment (Quote → SO → DR → Invoice)  
**Next:** Milestone 9 — Catalog intelligence

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

## Credit ledger API (Milestone 6)

Authenticated users can view ledgers and statements. Recording payments requires `OWNER`, `ADMIN`, `MANAGER`, or `CASHIER`.

- `GET /api/v1/credit/summary` — customers with balance + total outstanding
- `GET /api/v1/customers/{id}/ledger` — charge + payment lines with running balance
- `GET /api/v1/customers/{id}/payments` — payment history
- `POST /api/v1/customers/{id}/payments` — record collection (reduces outstanding balance)
- `GET /api/v1/customers/{id}/statement?from=&to=` — Statement of Account for a period

Collection methods: `CASH`, `CARD`, `BANK_TRANSFER`, `CHECK`, `GCASH`, `OTHER`. UI: Customers → **Account** (Overview / Ledger / Payments / SOA / Purchases).

## Pricing API (Milestone 7)

- `GET /api/v1/price-levels` — Retail / Contractor / VIP
- `PUT /api/v1/price-levels/{id}` — rename / activate (OWNER, ADMIN, MANAGER)
- `GET /api/v1/products/{id}/level-prices`
- `GET /api/v1/products/{id}/price-history`
- `GET /api/v1/pricing/resolve?productId=&customerId=&priceLevelId=`

Product create/update accepts `levelPrices[]` and optional `priceChangeReason`. Customer has `priceLevelId`. POS resolves unit price from the customer’s level; cashiers may override line price.

## Fulfillment API (Milestone 8)

Authenticated users can read quotes, orders, deliveries, and invoices. Writes require `OWNER`, `ADMIN`, `MANAGER`, or `CASHIER` (deliveries also allow `INVENTORY_STAFF`).

Document flow: **Quotation → Sales Order → Delivery Receipt (partial) → Invoice → Payment**

- `GET /api/v1/fulfillment/summary` — pending quotes, open orders, partial deliveries
- `GET/POST /api/v1/fulfillment/quotes` — list / create draft
- `GET/PUT /api/v1/fulfillment/quotes/{id}` — detail / update draft
- `POST /api/v1/fulfillment/quotes/{id}/send|accept|reject|cancel|convert`
- `GET/POST /api/v1/fulfillment/orders` — list / create SO (optional quotation link)
- `GET /api/v1/fulfillment/orders/{id}` — ordered vs delivered vs open vs billable qty
- `POST /api/v1/fulfillment/orders/{id}/cancel` — only when no deliveries
- `POST /api/v1/fulfillment/orders/{id}/deliveries` — partial DR; **stock-out on delivered qty only**
- `GET /api/v1/fulfillment/deliveries/{id}`
- `POST /api/v1/fulfillment/orders/{id}/invoices` — invoice delivered (uninvoiced) qty
- `GET /api/v1/fulfillment/invoices` · `GET /api/v1/fulfillment/invoices/{id}`
- `POST /api/v1/fulfillment/invoices/{id}/payments` — credit collections via M6 ledger (`customer_payments.invoice_id`)

Doc numbers: `QUO-`, `SO-`, `DR-`, `INV-` (date + sequence). UI: Sales → Quotes / Orders / Invoices; printable quote, DR, and invoice. Dashboard **Pending Quotes** is live.

## Roles

`OWNER`, `ADMIN`, `MANAGER`, `CASHIER`, `INVENTORY_STAFF`

## Tests

```bash
cd backend
mvn test
```

## Architecture notes

- Modular monolith packages: `common`, `auth`, `user`, `catalog`, `customer`, `inventory`, `sales`, `credit`, `pricing`, `fulfillment` (future: reports)
- DTOs only on the API boundary; JPA entities are never exposed
- JWT access tokens + rotating refresh tokens (SHA-256 hashed at rest)
- Soft delete + basic audit logging for auth/user/inventory/sales/credit/pricing/fulfillment actions

## Next milestone

Milestone 9 — Catalog intelligence (smart search, alternatives, bundles, rich purchase history)
