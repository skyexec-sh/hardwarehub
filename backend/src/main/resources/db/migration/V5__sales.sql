-- HardwareHub Milestone 5: Sales / POS

CREATE SEQUENCE receipt_number_seq START WITH 1001 INCREMENT BY 1;

CREATE TABLE sales (
    id                  BIGSERIAL PRIMARY KEY,
    receipt_number      VARCHAR(40) NOT NULL,
    customer_id         BIGINT REFERENCES customers (id),
    cashier_username    VARCHAR(50) NOT NULL,
    status              VARCHAR(30) NOT NULL DEFAULT 'COMPLETED',
    payment_method      VARCHAR(30) NOT NULL,
    subtotal            NUMERIC(14, 2) NOT NULL DEFAULT 0,
    discount_amount     NUMERIC(14, 2) NOT NULL DEFAULT 0,
    tax_amount          NUMERIC(14, 2) NOT NULL DEFAULT 0,
    total_amount        NUMERIC(14, 2) NOT NULL DEFAULT 0,
    amount_tendered     NUMERIC(14, 2),
    change_amount       NUMERIC(14, 2),
    notes               TEXT,
    sold_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(50),
    updated_by          VARCHAR(50)
);

CREATE UNIQUE INDEX idx_sales_receipt_number ON sales (receipt_number);
CREATE INDEX idx_sales_customer_id ON sales (customer_id);
CREATE INDEX idx_sales_sold_at ON sales (sold_at DESC);
CREATE INDEX idx_sales_status ON sales (status);
CREATE INDEX idx_sales_payment_method ON sales (payment_method);

CREATE TABLE sale_items (
    id                  BIGSERIAL PRIMARY KEY,
    sale_id             BIGINT NOT NULL REFERENCES sales (id) ON DELETE CASCADE,
    product_id          BIGINT NOT NULL REFERENCES products (id),
    product_sku         VARCHAR(50) NOT NULL,
    product_name        VARCHAR(200) NOT NULL,
    unit                VARCHAR(30) NOT NULL,
    quantity            NUMERIC(14, 3) NOT NULL,
    unit_price          NUMERIC(14, 2) NOT NULL,
    line_discount       NUMERIC(14, 2) NOT NULL DEFAULT 0,
    line_total          NUMERIC(14, 2) NOT NULL,
    line_no             INT NOT NULL DEFAULT 1
);

CREATE INDEX idx_sale_items_sale_id ON sale_items (sale_id);
CREATE INDEX idx_sale_items_product_id ON sale_items (product_id);
