-- HardwareHub Milestone 6: Customer credit ledger

CREATE SEQUENCE payment_number_seq START WITH 1001 INCREMENT BY 1;

CREATE TABLE customer_payments (
    id                  BIGSERIAL PRIMARY KEY,
    payment_number      VARCHAR(40) NOT NULL,
    customer_id         BIGINT NOT NULL REFERENCES customers (id),
    amount              NUMERIC(14, 2) NOT NULL,
    payment_method      VARCHAR(30) NOT NULL,
    reference_no        VARCHAR(80),
    notes               TEXT,
    paid_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    balance_before      NUMERIC(14, 2) NOT NULL,
    balance_after       NUMERIC(14, 2) NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(50)
);

CREATE UNIQUE INDEX idx_customer_payments_number ON customer_payments (payment_number);
CREATE INDEX idx_customer_payments_customer_id ON customer_payments (customer_id);
CREATE INDEX idx_customer_payments_paid_at ON customer_payments (paid_at DESC);
