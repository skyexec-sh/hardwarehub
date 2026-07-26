-- HardwareHub Milestone 3: Customer Management

CREATE TABLE customers (
    id                      BIGSERIAL PRIMARY KEY,
    customer_code           VARCHAR(30) NOT NULL,
    business_name           VARCHAR(200) NOT NULL,
    contact_person          VARCHAR(150),
    phone                   VARCHAR(30),
    email                   VARCHAR(255),
    address                 VARCHAR(500),
    city                    VARCHAR(100),
    province                VARCHAR(100),
    tax_identification_number VARCHAR(50),
    notes                   TEXT,
    credit_limit            NUMERIC(14, 2) NOT NULL DEFAULT 0,
    outstanding_balance     NUMERIC(14, 2) NOT NULL DEFAULT 0,
    status                  VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    deleted_at              TIMESTAMP WITH TIME ZONE,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by              VARCHAR(50),
    updated_by              VARCHAR(50)
);

CREATE INDEX idx_customers_code ON customers (customer_code);
CREATE INDEX idx_customers_business_name ON customers (business_name);
CREATE INDEX idx_customers_phone ON customers (phone);
CREATE INDEX idx_customers_status ON customers (status);
