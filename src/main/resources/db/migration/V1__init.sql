CREATE TABLE users (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name          VARCHAR(120) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    phone         VARCHAR(20),
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'CUSTOMER' CHECK (role IN ('CUSTOMER', 'ADMIN')),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- id is the URL slug used by the front end (?dealer=hp)
CREATE TABLE dealers (
    id         VARCHAR(40) PRIMARY KEY,
    name       VARCHAR(80) NOT NULL,
    logo_url   VARCHAR(500),
    sort_order INT NOT NULL DEFAULT 0
);

CREATE TABLE products (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    category    VARCHAR(60)  NOT NULL,
    price       NUMERIC(12, 2) CHECK (price IS NULL OR price >= 0),  -- NULL = "ask on WhatsApp"
    image_url   VARCHAR(500),
    in_stock    BOOLEAN NOT NULL DEFAULT TRUE,
    specs       JSONB,
    dealer_id   VARCHAR(40) NOT NULL REFERENCES dealers (id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_products_dealer   ON products (dealer_id);
CREATE INDEX idx_products_category ON products (lower(category));
