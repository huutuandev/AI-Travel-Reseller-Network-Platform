-- V8: Create vouchers
CREATE TABLE vouchers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id UUID NOT NULL REFERENCES merchants(id) ON DELETE RESTRICT,
    campaign_application_id UUID REFERENCES campaign_applications(id) ON DELETE RESTRICT,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    discount_type VARCHAR(20) NOT NULL DEFAULT 'FIXED_AMOUNT',
    discount_value NUMERIC(12,2) NOT NULL CHECK (discount_value >= 0),
    max_discount_amount NUMERIC(12,2),
    min_order_amount NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (min_order_amount >= 0),
    usage_limit BIGINT,
    usage_count BIGINT NOT NULL DEFAULT 0,
    per_customer_limit INTEGER,
    starts_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_vouchers_merchant_code UNIQUE (merchant_id, code),
    CONSTRAINT chk_voucher_discount_type CHECK (discount_type IN ('PERCENTAGE', 'FIXED_AMOUNT')),
    CONSTRAINT chk_voucher_discount CHECK (
        discount_type <> 'PERCENTAGE' OR discount_value <= 100
    ),
    CONSTRAINT chk_voucher_max_discount CHECK (
        max_discount_amount IS NULL OR max_discount_amount >= 0
    ),
    CONSTRAINT chk_voucher_usage CHECK (
        usage_count >= 0
        AND (usage_limit IS NULL OR (usage_limit >= 0 AND usage_count <= usage_limit))
        AND (per_customer_limit IS NULL OR per_customer_limit > 0)
    ),
    CONSTRAINT chk_voucher_dates CHECK (
        starts_at IS NULL OR expires_at IS NULL OR expires_at > starts_at
    ),
    CONSTRAINT chk_voucher_status CHECK (status IN ('DRAFT', 'ACTIVE', 'EXPIRED', 'DISABLED'))
);
CREATE INDEX idx_vouchers_merchant_id ON vouchers(merchant_id);
CREATE INDEX idx_vouchers_application_id ON vouchers(campaign_application_id);
