-- V9: Create tracking links
CREATE TABLE tracking_links (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_application_id UUID NOT NULL REFERENCES campaign_applications(id) ON DELETE RESTRICT,
    reseller_id UUID NOT NULL REFERENCES reseller_profiles(id) ON DELETE RESTRICT,
    tracking_code VARCHAR(100) NOT NULL UNIQUE,
    target_url TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    click_count INTEGER NOT NULL DEFAULT 0 CHECK (click_count >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_tracking_links_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);
CREATE INDEX idx_tracking_links_application_id ON tracking_links(campaign_application_id);
CREATE INDEX idx_tracking_links_reseller_id ON tracking_links(reseller_id);
