-- V4: Create social accounts
CREATE TABLE social_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reseller_id UUID NOT NULL REFERENCES reseller_profiles(id) ON DELETE CASCADE,
    platform VARCHAR(30) NOT NULL,
    username VARCHAR(150) NOT NULL,
    profile_url TEXT,
    followers_count BIGINT NOT NULL DEFAULT 0 CHECK (followers_count >= 0),
    average_views BIGINT NOT NULL DEFAULT 0 CHECK (average_views >= 0),
    metrics_source VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    metrics_updated_at TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_social_accounts_identity UNIQUE (reseller_id, platform, username),
    CONSTRAINT chk_social_accounts_platform CHECK (platform IN ('TIKTOK', 'FACEBOOK', 'YOUTUBE', 'INSTAGRAM', 'OTHER')),
    CONSTRAINT chk_social_accounts_source CHECK (metrics_source IN ('API', 'MANUAL')),
    CONSTRAINT chk_social_accounts_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'DISCONNECTED'))
);
CREATE INDEX idx_social_accounts_reseller_id ON social_accounts(reseller_id);
