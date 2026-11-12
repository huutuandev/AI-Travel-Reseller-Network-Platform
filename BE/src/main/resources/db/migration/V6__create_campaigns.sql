-- V6: Create campaigns
CREATE TABLE campaigns (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    commission_type VARCHAR(20) NOT NULL,
    commission_rate NUMERIC(10,2) NOT NULL,
    cookie_window_days INTEGER NOT NULL DEFAULT 30,
    start_date TIMESTAMPTZ,
    end_date TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_campaign_commission_type CHECK (commission_type IN ('PERCENTAGE', 'FIXED_AMOUNT')),
    CONSTRAINT chk_campaign_commission_rate CHECK (
        commission_rate >= 0
        AND (commission_type <> 'PERCENTAGE' OR commission_rate <= 100)
    ),
    CONSTRAINT chk_campaign_cookie_window CHECK (cookie_window_days > 0),
    CONSTRAINT chk_campaign_dates CHECK (
        start_date IS NULL OR end_date IS NULL OR end_date >= start_date
    ),
    CONSTRAINT chk_campaign_status CHECK (
        status IN ('DRAFT', 'PENDING_REVIEW', 'ACTIVE', 'PAUSED', 'ENDED', 'CANCELLED')
    )
);
CREATE INDEX idx_campaigns_product_id ON campaigns(product_id);
