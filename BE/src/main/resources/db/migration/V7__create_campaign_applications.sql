-- V7: Create campaign applications
CREATE TABLE campaign_applications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_id UUID NOT NULL REFERENCES campaigns(id) ON DELETE RESTRICT,
    reseller_id UUID NOT NULL REFERENCES reseller_profiles(id) ON DELETE RESTRICT,
    status VARCHAR(20) NOT NULL,
    commission_type_snapshot VARCHAR(20) NOT NULL,
    commission_rate_snapshot NUMERIC(10,2) NOT NULL,
    applied_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    reviewed_at TIMESTAMPTZ,
    CONSTRAINT uq_campaign_application UNIQUE (campaign_id, reseller_id),
    CONSTRAINT chk_campaign_application_status CHECK (
        status IN ('PENDING', 'APPROVED', 'REJECTED', 'WITHDRAWN', 'SUSPENDED')
    ),
    CONSTRAINT chk_commission_snapshot CHECK (
        commission_rate_snapshot >= 0
        AND (commission_type_snapshot <> 'PERCENTAGE' OR commission_rate_snapshot <= 100)
    )
);
CREATE INDEX idx_campaign_applications_campaign_id ON campaign_applications(campaign_id);
CREATE INDEX idx_campaign_applications_reseller_id ON campaign_applications(reseller_id);
