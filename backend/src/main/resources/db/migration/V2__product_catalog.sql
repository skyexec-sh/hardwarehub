-- HardwareHub Milestone 2: Product Catalog

CREATE TABLE categories (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    description     VARCHAR(500),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at      TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(50),
    updated_by      VARCHAR(50)
);

CREATE INDEX idx_categories_name ON categories (name);

CREATE TABLE brands (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    description     VARCHAR(500),
    logo_url        VARCHAR(500),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at      TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(50),
    updated_by      VARCHAR(50)
);

CREATE INDEX idx_brands_name ON brands (name);

CREATE TABLE products (
    id                  BIGSERIAL PRIMARY KEY,
    sku                 VARCHAR(50) NOT NULL,
    barcode             VARCHAR(64),
    name                VARCHAR(200) NOT NULL,
    description         TEXT,
    brand_id            BIGINT REFERENCES brands(id),
    category_id         BIGINT REFERENCES categories(id),
    unit                VARCHAR(30) NOT NULL DEFAULT 'PCS',
    cost_price          NUMERIC(14, 2) NOT NULL DEFAULT 0,
    selling_price       NUMERIC(14, 2) NOT NULL DEFAULT 0,
    current_stock       NUMERIC(14, 3) NOT NULL DEFAULT 0,
    minimum_stock       NUMERIC(14, 3) NOT NULL DEFAULT 0,
    maximum_stock       NUMERIC(14, 3),
    image_url           VARCHAR(500),
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at          TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(50),
    updated_by          VARCHAR(50)
);

CREATE INDEX idx_products_sku ON products (sku);
CREATE INDEX idx_products_barcode ON products (barcode);
CREATE INDEX idx_products_name ON products (name);
CREATE INDEX idx_products_category_id ON products (category_id);
CREATE INDEX idx_products_brand_id ON products (brand_id);

INSERT INTO categories (name, description, created_by, updated_by) VALUES
    ('Plumbing', 'Pipes, fittings, and plumbing supplies', 'system', 'system'),
    ('Electrical', 'Wires, breakers, and electrical fittings', 'system', 'system'),
    ('Paint', 'Paints, primers, and coatings', 'system', 'system'),
    ('Roofing', 'Roofing sheets and accessories', 'system', 'system'),
    ('Cement', 'Cement and masonry materials', 'system', 'system'),
    ('Lumber', 'Wood and lumber products', 'system', 'system'),
    ('Steel', 'Steel bars and metal products', 'system', 'system'),
    ('Power Tools', 'Electric and power tools', 'system', 'system'),
    ('Hand Tools', 'Manual hand tools', 'system', 'system'),
    ('Fasteners', 'Screws, nails, bolts, and fasteners', 'system', 'system');

INSERT INTO brands (name, description, created_by, updated_by) VALUES
    ('Boysen', 'Paint and coatings brand', 'system', 'system'),
    ('Holcim', 'Cement and building materials', 'system', 'system'),
    ('Pioneer', 'Construction materials brand', 'system', 'system'),
    ('Omni', 'Electrical and hardware brand', 'system', 'system'),
    ('Atlanta', 'Hardware and tools brand', 'system', 'system');
