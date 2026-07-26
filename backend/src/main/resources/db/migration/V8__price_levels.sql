-- HardwareHub Milestone 7: Price levels + product price history

CREATE TABLE price_levels (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(30) NOT NULL,
    name            VARCHAR(100) NOT NULL,
    description     VARCHAR(255),
    sort_order      INT NOT NULL DEFAULT 0,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_price_levels_code ON price_levels (code);

INSERT INTO price_levels (code, name, description, sort_order) VALUES
    ('RETAIL', 'Retail', 'Standard walk-in / over-the-counter price', 1),
    ('CONTRACTOR', 'Contractor', 'Trade / contractor discounted price', 2),
    ('VIP', 'VIP', 'Preferred / VIP customer price', 3);

CREATE TABLE product_level_prices (
    id              BIGSERIAL PRIMARY KEY,
    product_id      BIGINT NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    price_level_id  BIGINT NOT NULL REFERENCES price_levels (id),
    unit_price      NUMERIC(14, 2) NOT NULL DEFAULT 0,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(50)
);

CREATE UNIQUE INDEX idx_product_level_prices_unique ON product_level_prices (product_id, price_level_id);
CREATE INDEX idx_product_level_prices_product ON product_level_prices (product_id);

-- Seed each level from current selling_price for existing catalog rows
INSERT INTO product_level_prices (product_id, price_level_id, unit_price, updated_by)
SELECT p.id, pl.id, p.selling_price, 'system'
FROM products p
CROSS JOIN price_levels pl
WHERE p.deleted_at IS NULL;

ALTER TABLE customers
    ADD COLUMN price_level_id BIGINT REFERENCES price_levels (id);

UPDATE customers
SET price_level_id = (SELECT id FROM price_levels WHERE code = 'RETAIL')
WHERE price_level_id IS NULL;

CREATE TABLE product_price_history (
    id              BIGSERIAL PRIMARY KEY,
    product_id      BIGINT NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    price_type      VARCHAR(30) NOT NULL,
    price_level_id  BIGINT REFERENCES price_levels (id),
    old_price       NUMERIC(14, 2),
    new_price       NUMERIC(14, 2) NOT NULL,
    reason          VARCHAR(255),
    changed_by      VARCHAR(50),
    changed_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_product_price_history_product ON product_price_history (product_id, changed_at DESC);
