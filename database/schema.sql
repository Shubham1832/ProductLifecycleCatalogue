-- Product Lifecycle and Catalogue Management System
-- Run this script while connected to the database you created for the project.

CREATE TABLE IF NOT EXISTS categories (
    category_id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE IF NOT EXISTS sales_channels (
    channel_id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE IF NOT EXISTS products (
    product_id SERIAL PRIMARY KEY,
    sku VARCHAR(60) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    description TEXT,
    price NUMERIC(12,2) NOT NULL CHECK (price >= 0),
    category_id INTEGER REFERENCES categories(category_id) ON UPDATE CASCADE ON DELETE SET NULL,
    lifecycle_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
        CHECK (lifecycle_status IN ('DRAFT','REVIEW','PUBLISHED','REJECTED','DISCONTINUED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS product_channels (
    product_id INTEGER NOT NULL REFERENCES products(product_id) ON DELETE CASCADE,
    channel_id INTEGER NOT NULL REFERENCES sales_channels(channel_id) ON DELETE CASCADE,
    listed BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (product_id, channel_id)
);

CREATE TABLE IF NOT EXISTS inventory (
    product_id INTEGER PRIMARY KEY REFERENCES products(product_id) ON DELETE CASCADE,
    quantity INTEGER NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    reorder_level INTEGER NOT NULL DEFAULT 5 CHECK (reorder_level >= 0),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS lifecycle_history (
    history_id BIGSERIAL PRIMARY KEY,
    product_id INTEGER NOT NULL REFERENCES products(product_id) ON DELETE CASCADE,
    old_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    note TEXT,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Optional sample data (uncomment to insert sample categories/channels):
-- INSERT INTO categories(name, description) VALUES ('Electronics', 'Electronic products')
-- ON CONFLICT (name) DO NOTHING;
-- INSERT INTO sales_channels(name, description) VALUES ('Company Website', 'Official online store')
-- ON CONFLICT (name) DO NOTHING;
