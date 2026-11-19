-- V2: Create merchants and branding
CREATE TABLE merchants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    contact_email VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    address TEXT,
    subdomain VARCHAR(100) NOT NULL,
    custom_domain VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_merchants_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'PENDING', 'CLOSED'))
);
CREATE UNIQUE INDEX uq_merchants_subdomain_lower ON merchants (LOWER(subdomain));
CREATE UNIQUE INDEX uq_merchants_custom_domain_lower
    ON merchants (LOWER(custom_domain)) WHERE custom_domain IS NOT NULL;
CREATE INDEX idx_merchants_user_id ON merchants(user_id);

CREATE TABLE merchant_branding (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id UUID NOT NULL UNIQUE REFERENCES merchants(id) ON DELETE RESTRICT,
    logo_url TEXT,
    favicon_url TEXT,
    primary_color VARCHAR(20) DEFAULT '#0B7F7B',
    secondary_color VARCHAR(20) DEFAULT '#F4B63F',
    custom_css TEXT,
    footer_text TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
