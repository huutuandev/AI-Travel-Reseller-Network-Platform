-- V5: Create products
CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id UUID NOT NULL REFERENCES merchants(id) ON DELETE RESTRICT,
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    description TEXT,
    images JSONB NOT NULL DEFAULT '[]'::jsonb,
    price NUMERIC(12,2) NOT NULL CHECK (price >= 0),
    sale_price NUMERIC(12,2),
    availability_status VARCHAR(20) NOT NULL,
    official_checkout_url TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_products_merchant_slug UNIQUE (merchant_id, slug),
    CONSTRAINT chk_products_images_array CHECK (jsonb_typeof(images) = 'array'),
    CONSTRAINT chk_products_sale_price CHECK (sale_price IS NULL OR (sale_price >= 0 AND sale_price <= price)),
    CONSTRAINT chk_products_status CHECK (availability_status IN ('AVAILABLE', 'UNAVAILABLE', 'SOLD_OUT', 'DRAFT'))
);
CREATE INDEX idx_products_merchant_id ON products(merchant_id);
