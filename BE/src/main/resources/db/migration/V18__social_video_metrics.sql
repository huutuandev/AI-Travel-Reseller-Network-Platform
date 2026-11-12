-- Store provider identifiers and the latest fetched metrics for reseller posts.
-- Historical measurements are kept separately so refreshes do not overwrite them.

ALTER TABLE reseller_campaign_posts
    ADD COLUMN platform VARCHAR(30),
    ADD COLUMN external_video_id VARCHAR(255),
    ADD COLUMN metrics_source VARCHAR(30),
    ADD COLUMN metrics_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN metrics_fetched_at TIMESTAMPTZ,
    ADD COLUMN metrics_error TEXT;

-- Existing posts already reference a social account; use its platform for backfill.
UPDATE reseller_campaign_posts post
SET platform = account.platform
FROM social_accounts account
WHERE account.id = post.social_account_id;

ALTER TABLE reseller_campaign_posts
    ALTER COLUMN platform SET NOT NULL,
    ADD CONSTRAINT chk_rcp_metrics_platform
        CHECK (platform IN ('TIKTOK', 'FACEBOOK', 'YOUTUBE', 'INSTAGRAM', 'OTHER')),
    ADD CONSTRAINT chk_rcp_metrics_source
        CHECK (metrics_source IS NULL OR metrics_source IN ('TIKTOK_API', 'META_GRAPH_API', 'MANUAL')),
    ADD CONSTRAINT chk_rcp_metrics_status
        CHECK (metrics_status IN ('PENDING', 'SUCCESS', 'FAILED', 'UNAVAILABLE')),
    ADD CONSTRAINT chk_rcp_external_video_id
        CHECK (external_video_id IS NULL OR length(btrim(external_video_id)) > 0),
    ADD CONSTRAINT chk_rcp_metrics_success_timestamp
        CHECK (metrics_status <> 'SUCCESS' OR metrics_fetched_at IS NOT NULL);

CREATE UNIQUE INDEX uq_rcp_social_video
    ON reseller_campaign_posts(social_account_id, platform, external_video_id)
    WHERE external_video_id IS NOT NULL;

CREATE INDEX idx_rcp_platform_video
    ON reseller_campaign_posts(platform, external_video_id)
    WHERE external_video_id IS NOT NULL;

CREATE TABLE social_video_metric_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id UUID NOT NULL
        REFERENCES reseller_campaign_posts(id) ON DELETE CASCADE,
    views_count BIGINT,
    likes_count BIGINT,
    fetched_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    source VARCHAR(30) NOT NULL,
    fetch_status VARCHAR(20) NOT NULL,
    error_message TEXT,
    CONSTRAINT chk_svms_views_nonnegative
        CHECK (views_count IS NULL OR views_count >= 0),
    CONSTRAINT chk_svms_likes_nonnegative
        CHECK (likes_count IS NULL OR likes_count >= 0),
    CONSTRAINT chk_svms_source
        CHECK (source IN ('TIKTOK_API', 'META_GRAPH_API', 'MANUAL')),
    CONSTRAINT chk_svms_fetch_status
        CHECK (fetch_status IN ('SUCCESS', 'FAILED', 'UNAVAILABLE')),
    CONSTRAINT chk_svms_success_metrics_present
        CHECK (
            fetch_status <> 'SUCCESS'
            OR (views_count IS NOT NULL AND likes_count IS NOT NULL)
        )
);

CREATE INDEX idx_svms_post_fetched_at
    ON social_video_metric_snapshots(post_id, fetched_at DESC);
