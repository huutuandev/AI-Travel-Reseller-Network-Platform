-- V13: Create AI contents
CREATE TABLE ai_contents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_id UUID REFERENCES campaigns(id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    content_type VARCHAR(30) NOT NULL,
    prompt_used TEXT,
    generated_content JSONB,
    media_url TEXT,
    thumbnail_url TEXT,
    duration_seconds INTEGER CHECK (duration_seconds IS NULL OR duration_seconds >= 0),
    generation_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_ai_content_type CHECK (
        content_type IN ('SOCIAL_POST', 'CAPTION', 'VIDEO_SCRIPT', 'VIDEO', 'BLOG', 'BANNER_TEXT', 'OTHER')
    ),
    CONSTRAINT chk_ai_generation_status CHECK (
        generation_status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')
    )
);
CREATE INDEX idx_ai_contents_campaign_id ON ai_contents(campaign_id);
