# HardwareHub SRS (Milestone context)



See the full Software Requirements Specification for the long-term product vision.

## Implemented

| Milestone | Scope |
| --- | --- |
| **M1** | Auth & users (JWT, roles, Docker Compose, Flyway) |
| **M2** | Product catalog (categories, brands, products, barcode) |
| **M3** | Customers (CRM fields, credit limit / outstanding) |
| **M4** | Inventory movements (in/out/adjust, history, low stock) |
| **M5** | Sales / POS & printable invoices (cash, card, credit sale) |
| **M6** | Customer credit ledger (payments, ledger, printable SOA) |
| **M7** | Price levels (Retail / Contractor / VIP) + price history |
| **M8** | Fulfillment: Quote → SO → Delivery (partial) → Invoice → Payment |

## Planned

| Milestone | Scope |
| --- | --- |
| **M9** | Catalog intelligence (smart search, alternatives, bundles) |
| **M10** | Reports & analytics |
| **M11** | Stock days-remaining prediction |
| **M12** | Production readiness |

## M8 notes

Project/contractor jobs use the fulfillment pipeline; POS remains the walk-in counter path. Stock is reduced only when a delivery receipt is posted (partial deliveries allowed). Invoices bill delivered-but-uninvoiced quantities. Credit invoices increase customer outstanding balance and appear on the M6 ledger; collections reuse `customer_payments` (optional `invoice_id`). Printable quotation, DR, and invoice use the same browser-print pattern as POS receipts.
