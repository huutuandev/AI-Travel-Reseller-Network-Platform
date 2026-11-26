-- V10: Create reseller campaign posts
CREATE TABLE reseller_campaign_posts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_application_id UUID NOT NULL REFERENCES campaign_applications(id) ON DELETE RESTRICT,
    social_account_id UUID NOT NULL REFERENCES social_accounts(id) ON DELETE RESTRICT,
    post_url TEXT NOT NULL,
    caption TEXT,
    views_count BIGINT NOT NULL DEFAULT 0 CHECK (views_count >= 0),
    likes_count BIGINT NOT NULL DEFAULT 0 CHECK (likes_count >= 0),
    comments_count BIGINT NOT NULL DEFAULT 0 CHECK (comments_count >= 0),
    shares_count BIGINT NOT NULL DEFAULT 0 CHECK (shares_count >= 0),
    published_at TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_rcp_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED', 'REMOVED'))
);
CREATE INDEX idx_rcp_campaign_application_id ON reseller_campaign_posts(campaign_application_id);
CREATE INDEX idx_rcp_social_account_id ON reseller_campaign_posts(social_account_id);
