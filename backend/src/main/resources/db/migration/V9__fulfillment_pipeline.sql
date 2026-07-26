-- HardwareHub Milestone 8: Fulfillment pipeline (Quote → SO → DR → Invoice)

CREATE SEQUENCE quote_number_seq START WITH 1001 INCREMENT BY 1;
CREATE SEQUENCE so_number_seq START WITH 1001 INCREMENT BY 1;
CREATE SEQUENCE dr_number_seq START WITH 1001 INCREMENT BY 1;
CREATE SEQUENCE invoice_number_seq START WITH 1001 INCREMENT BY 1;

CREATE TABLE quotations (
    id                  BIGSERIAL PRIMARY KEY,
    quote_number        VARCHAR(40) NOT NULL,
    customer_id         BIGINT NOT NULL REFERENCES customers (id),
    status              VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    subtotal            NUMERIC(14, 2) NOT NULL DEFAULT 0,
    discount_amount     NUMERIC(14, 2) NOT NULL DEFAULT 0,
    tax_amount          NUMERIC(14, 2) NOT NULL DEFAULT 0,
    total_amount        NUMERIC(14, 2) NOT NULL DEFAULT 0,
    notes               TEXT,
    valid_until         DATE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(50),
    updated_by          VARCHAR(50)
);

CREATE UNIQUE INDEX idx_quotations_number ON quotations (quote_number);
CREATE INDEX idx_quotations_customer_id ON quotations (customer_id);
CREATE INDEX idx_quotations_status ON quotations (status);
CREATE INDEX idx_quotations_created_at ON quotations (created_at DESC);

CREATE TABLE quotation_items (
    id                  BIGSERIAL PRIMARY KEY,
    quotation_id        BIGINT NOT NULL REFERENCES quotations (id) ON DELETE CASCADE,
    line_no             INT NOT NULL,
    product_id          BIGINT REFERENCES products (id),
    product_sku         VARCHAR(40) NOT NULL,
    product_name        VARCHAR(200) NOT NULL,
    unit                VARCHAR(30) NOT NULL,
    quantity            NUMERIC(14, 3) NOT NULL,
    unit_price          NUMERIC(14, 2) NOT NULL,
    line_discount       NUMERIC(14, 2) NOT NULL DEFAULT 0,
    line_total          NUMERIC(14, 2) NOT NULL
);

CREATE INDEX idx_quotation_items_quotation_id ON quotation_items (quotation_id);

CREATE TABLE sales_orders (
    id                  BIGSERIAL PRIMARY KEY,
    so_number           VARCHAR(40) NOT NULL,
    quotation_id        BIGINT REFERENCES quotations (id),
    customer_id         BIGINT NOT NULL REFERENCES customers (id),
    status              VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    subtotal            NUMERIC(14, 2) NOT NULL DEFAULT 0,
    discount_amount     NUMERIC(14, 2) NOT NULL DEFAULT 0,
    tax_amount          NUMERIC(14, 2) NOT NULL DEFAULT 0,
    total_amount        NUMERIC(14, 2) NOT NULL DEFAULT 0,
    notes               TEXT,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(50),
    updated_by          VARCHAR(50)
);

CREATE UNIQUE INDEX idx_sales_orders_number ON sales_orders (so_number);
CREATE INDEX idx_sales_orders_customer_id ON sales_orders (customer_id);
CREATE INDEX idx_sales_orders_status ON sales_orders (status);
CREATE INDEX idx_sales_orders_quotation_id ON sales_orders (quotation_id);

CREATE TABLE sales_order_items (
    id                  BIGSERIAL PRIMARY KEY,
    sales_order_id      BIGINT NOT NULL REFERENCES sales_orders (id) ON DELETE CASCADE,
    line_no             INT NOT NULL,
    product_id          BIGINT REFERENCES products (id),
    product_sku         VARCHAR(40) NOT NULL,
    product_name        VARCHAR(200) NOT NULL,
    unit                VARCHAR(30) NOT NULL,
    quantity_ordered    NUMERIC(14, 3) NOT NULL,
    quantity_delivered  NUMERIC(14, 3) NOT NULL DEFAULT 0,
    quantity_invoiced   NUMERIC(14, 3) NOT NULL DEFAULT 0,
    unit_price          NUMERIC(14, 2) NOT NULL,
    line_discount       NUMERIC(14, 2) NOT NULL DEFAULT 0,
    line_total          NUMERIC(14, 2) NOT NULL
);

CREATE INDEX idx_sales_order_items_so_id ON sales_order_items (sales_order_id);

CREATE TABLE delivery_receipts (
    id                  BIGSERIAL PRIMARY KEY,
    dr_number           VARCHAR(40) NOT NULL,
    sales_order_id      BIGINT NOT NULL REFERENCES sales_orders (id),
    status              VARCHAR(30) NOT NULL DEFAULT 'POSTED',
    notes               TEXT,
    delivered_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(50)
);

CREATE UNIQUE INDEX idx_delivery_receipts_number ON delivery_receipts (dr_number);
CREATE INDEX idx_delivery_receipts_so_id ON delivery_receipts (sales_order_id);

CREATE TABLE delivery_receipt_items (
    id                      BIGSERIAL PRIMARY KEY,
    delivery_receipt_id     BIGINT NOT NULL REFERENCES delivery_receipts (id) ON DELETE CASCADE,
    sales_order_item_id     BIGINT NOT NULL REFERENCES sales_order_items (id),
    line_no                 INT NOT NULL,
    product_id              BIGINT REFERENCES products (id),
    product_sku             VARCHAR(40) NOT NULL,
    product_name            VARCHAR(200) NOT NULL,
    unit                    VARCHAR(30) NOT NULL,
    quantity                NUMERIC(14, 3) NOT NULL,
    unit_price              NUMERIC(14, 2) NOT NULL
);

CREATE INDEX idx_dr_items_dr_id ON delivery_receipt_items (delivery_receipt_id);

CREATE TABLE fulfillment_invoices (
    id                  BIGSERIAL PRIMARY KEY,
    invoice_number      VARCHAR(40) NOT NULL,
    sales_order_id      BIGINT NOT NULL REFERENCES sales_orders (id),
    customer_id         BIGINT NOT NULL REFERENCES customers (id),
    status              VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    payment_method      VARCHAR(30) NOT NULL,
    subtotal            NUMERIC(14, 2) NOT NULL DEFAULT 0,
    discount_amount     NUMERIC(14, 2) NOT NULL DEFAULT 0,
    tax_amount          NUMERIC(14, 2) NOT NULL DEFAULT 0,
    total_amount        NUMERIC(14, 2) NOT NULL DEFAULT 0,
    amount_paid         NUMERIC(14, 2) NOT NULL DEFAULT 0,
    notes               TEXT,
    invoiced_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(50),
    updated_by          VARCHAR(50)
);

CREATE UNIQUE INDEX idx_fulfillment_invoices_number ON fulfillment_invoices (invoice_number);
CREATE INDEX idx_fulfillment_invoices_so_id ON fulfillment_invoices (sales_order_id);
CREATE INDEX idx_fulfillment_invoices_customer_id ON fulfillment_invoices (customer_id);
CREATE INDEX idx_fulfillment_invoices_status ON fulfillment_invoices (status);

CREATE TABLE fulfillment_invoice_items (
    id                      BIGSERIAL PRIMARY KEY,
    invoice_id              BIGINT NOT NULL REFERENCES fulfillment_invoices (id) ON DELETE CASCADE,
    sales_order_item_id     BIGINT NOT NULL REFERENCES sales_order_items (id),
    line_no                 INT NOT NULL,
    product_id              BIGINT REFERENCES products (id),
    product_sku             VARCHAR(40) NOT NULL,
    product_name            VARCHAR(200) NOT NULL,
    unit                    VARCHAR(30) NOT NULL,
    quantity                NUMERIC(14, 3) NOT NULL,
    unit_price              NUMERIC(14, 2) NOT NULL,
    line_discount           NUMERIC(14, 2) NOT NULL DEFAULT 0,
    line_total              NUMERIC(14, 2) NOT NULL
);

CREATE INDEX idx_fi_items_invoice_id ON fulfillment_invoice_items (invoice_id);

ALTER TABLE customer_payments
    ADD COLUMN invoice_id BIGINT REFERENCES fulfillment_invoices (id);

CREATE INDEX idx_customer_payments_invoice_id ON customer_payments (invoice_id);
