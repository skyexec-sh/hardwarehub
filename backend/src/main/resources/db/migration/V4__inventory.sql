-- HardwareHub Milestone 4: Inventory movements & low-stock tracking

CREATE TABLE inventory_transactions (
    id                  BIGSERIAL PRIMARY KEY,
    product_id          BIGINT NOT NULL REFERENCES products (id),
    transaction_type    VARCHAR(30) NOT NULL,
    quantity            NUMERIC(14, 3) NOT NULL,
    quantity_before     NUMERIC(14, 3) NOT NULL,
    quantity_after      NUMERIC(14, 3) NOT NULL,
    unit_cost           NUMERIC(14, 2),
    reference_no        VARCHAR(50),
    notes               TEXT,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(50)
);

CREATE INDEX idx_inventory_tx_product ON inventory_transactions (product_id);
CREATE INDEX idx_inventory_tx_type ON inventory_transactions (transaction_type);
CREATE INDEX idx_inventory_tx_created_at ON inventory_transactions (created_at DESC);
CREATE INDEX idx_inventory_tx_reference ON inventory_transactions (reference_no);
